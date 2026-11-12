package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.ChatFormatting;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;
import org.sgx.classes.entity.TrainingDummy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Манекен для тренировок: создание, подсчёт урона и «вылетающие» цифры.
 *
 * Цифры урона — text_display над манекеном: видят все рядом, плавно поднимаются и пропадают.
 * Режимы цели: гнилая плоть — нежить (Небесная кара даёт бонус), паучий глаз — членистоногие (Бич членистоногих).
 */
public final class TrainingDummyEvents {

    /**
     * Смещение «пугала» относительно стойки по высоте (пассажир крепится к верху хитбокса, 1.975).
     * Если модель окажется выше/ниже брони — правится только это число.
     */
    private static final double BODY_Y_OFFSET = 0.5 - 1.975;
    private static final long SERIES_TIMEOUT_TICKS = 100;
    private static final int FLOAT_LIFETIME_TICKS = 24;
    private static final String FLOAT_TAG_PREFIX = "ddmg:";

    private static final class Series {
        long firstTick;
        long lastTick;
        double total;
        int hits;
    }

    private record Task(int runAt, ServerLevel level, String command) {
    }

    private static final Map<UUID, Series> SERIES = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> STRENGTH = new ConcurrentHashMap<>();
    private static final Deque<Task> TASKS = new ArrayDeque<>();

    private TrainingDummyEvents() {
    }

    public static void init() {
        // сила замаха на момент удара — нужна для бонуса Небесной кары / Бича членистоногих
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide() && entity instanceof TrainingDummy) {
                STRENGTH.put(player.getUUID(), player.getAttackStrengthScale(0.5f));
            }
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof TrainingDummy dummy && player instanceof ServerPlayer serverPlayer
                    && !serverPlayer.isSpectator()) {
                return dummy.handleUse(serverPlayer, hand);
            }
            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int now = server.getTickCount();
            while (!TASKS.isEmpty() && TASKS.peekFirst().runAt() <= now) {
                Task task = TASKS.pollFirst();
                run(task.level(), task.command());
            }
        });

        // цифры, переживавшие перезапуск сервера, убираем
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            for (String tag : entity.entityTags()) {
                if (tag.startsWith(FLOAT_TAG_PREFIX)) {
                    try {
                        long born = Long.parseLong(tag.substring(FLOAT_TAG_PREFIX.length()));
                        if (world.getGameTime() - born > 60) {
                            schedule(world, 1, "kill " + entity.getUUID());
                        }
                    } catch (NumberFormatException ignored) {
                        schedule(world, 1, "kill " + entity.getUUID());
                    }
                }
            }
        });
    }

    // ---------- создание ----------

    /** Ставит манекен: невидимая стойка с руками + пассажир item_display с моделью-пугалом. */
    public static void spawn(ServerLevel level, Vec3 at, float yaw) {
        String bodyYaw = f(yaw + 180.0f);
        String body = "{id:\"minecraft:item_display\",Tags:[\"arz_dummy_body\"],Rotation:[" + bodyYaw + "f,0f],"
                + "item:{id:\"minecraft:stick\",count:1,components:{\"minecraft:item_model\":\"arz_classes:training_dummy_body\"}},"
                + "item_display:\"none\","
                + "transformation:{translation:[0f," + f(BODY_Y_OFFSET) + "f,0f],left_rotation:[0f,0f,0f,1f],"
                + "scale:[1f,1f,1f],right_rotation:[0f,0f,0f,1f]}}";
        String cmd = "summon arz_classes:training_dummy " + f(at.x) + " " + f(at.y) + " " + f(at.z)
                + " {Rotation:[" + f(yaw) + "f,0f],Invisible:1b,NoBasePlate:1b,Passengers:[" + body + "]}";
        run(level, cmd);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundType.WOOD.getPlaceSound(),
                SoundSource.BLOCKS, 1.0f, 0.9f);
    }

    public static void onModeChanged(ServerLevel level, TrainingDummy dummy, ServerPlayer player, boolean undead) {
        Component name = Component.translatable(undead ? "message.arz_classes.dummy_mode_undead"
                : "message.arz_classes.dummy_mode_arthropod");
        player.sendSystemMessage(name.copy().withStyle(ChatFormatting.GREEN), true);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, dummy.getX(), dummy.getY() + 1.2, dummy.getZ(),
                10, 0.3, 0.5, 0.3, 0.0);
    }

    // ---------- удар ----------

    public static void onDummyHit(ServerLevel level, TrainingDummy dummy, ServerPlayer player, float amount, boolean melee) {
        double damage = amount;
        double bonus = 0;
        boolean crit = false;

        if (melee) {
            float strength = STRENGTH.getOrDefault(player.getUUID(), 1.0f);
            ItemStack weapon = player.getMainHandItem();
            if (!weapon.isEmpty()) {
                var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                int smite = dummy.isUndeadMode()
                        ? EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.SMITE), weapon) : 0;
                int bane = dummy.isArthropodMode()
                        ? EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.BANE_OF_ARTHROPODS), weapon) : 0;
                bonus = 2.5 * (smite + bane) * strength;
            }
            crit = strength > 0.9f && player.fallDistance > 0 && !player.onGround()
                    && !player.onClimbable() && !player.isInWater() && !player.isPassenger() && !player.isSprinting();
        }
        damage += bonus;

        dummy.damageArmor((float) damage);

        long now = level.getGameTime();
        Series series = SERIES.get(player.getUUID());
        if (series == null || now - series.lastTick > SERIES_TIMEOUT_TICKS) {
            series = new Series();
            series.firstTick = now;
            SERIES.put(player.getUUID(), series);
        }
        series.lastTick = now;
        series.total += damage;
        series.hits++;
        double seconds = Math.max(1.0, (series.lastTick - series.firstTick) / 20.0);
        double dps = series.hits > 1 ? series.total / seconds : damage;

        Component stats = Component.translatable("message.arz_classes.dummy_hit",
                String.format(Locale.ROOT, "%.1f", damage),
                String.format(Locale.ROOT, "%.1f", series.total / series.hits),
                String.format(Locale.ROOT, "%.1f", dps),
                series.hits).withStyle(crit ? ChatFormatting.GOLD : ChatFormatting.WHITE);
        player.sendSystemMessage(stats, true);

        String color = crit ? "gold" : (bonus > 0 ? "green" : "white");
        floatNumber(level, dummy, String.format(Locale.ROOT, "%.1f", damage) + (crit ? "!" : ""), color, crit);

        level.sendParticles(crit ? ParticleTypes.CRIT : ParticleTypes.DAMAGE_INDICATOR,
                dummy.getX(), dummy.getY() + 1.3, dummy.getZ(), crit ? 12 : 4, 0.25, 0.3, 0.25, 0.1);
        level.playSound(null, dummy.blockPosition(), SoundType.WOOD.getHitSound(), SoundSource.NEUTRAL, 1.0f, 0.9f);
    }

    // ---------- цифры над манекеном ----------

    private static void floatNumber(ServerLevel level, TrainingDummy dummy, String text, String color, boolean big) {
        UUID id = UUID.randomUUID();
        double ox = (level.getRandom().nextDouble() - 0.5) * 0.6;
        double oz = (level.getRandom().nextDouble() - 0.5) * 0.6;
        double x = dummy.getX() + ox;
        double y = dummy.getY() + 2.3;
        double z = dummy.getZ() + oz;
        float scale = big ? 1.8f : 1.3f;
        String scaleNbt = "[" + f(scale) + "f," + f(scale) + "f," + f(scale) + "f]";
        String uuidNbt = "[I;" + (int) (id.getMostSignificantBits() >> 32) + "," + (int) id.getMostSignificantBits()
                + "," + (int) (id.getLeastSignificantBits() >> 32) + "," + (int) id.getLeastSignificantBits() + "]";

        run(level, "summon minecraft:text_display " + f(x) + " " + f(y) + " " + f(z)
                + " {UUID:" + uuidNbt + ",Tags:[\"" + FLOAT_TAG_PREFIX + level.getGameTime() + "\"],"
                + "text:{text:\"" + text + "\",color:\"" + color + "\",bold:1b},"
                + "billboard:\"center\",see_through:0b,shadow:1b,background:0,"
                + "transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],scale:" + scaleNbt
                + ",right_rotation:[0f,0f,0f,1f]}}");
        // на следующем тике запускаем плавный полёт вверх и в сторону
        schedule(level, 1, "data merge entity " + id + " {start_interpolation:0,interpolation_duration:20,"
                + "transformation:{translation:[" + f(ox * 1.5) + "f,1.4f," + f(oz * 1.5) + "f],"
                + "left_rotation:[0f,0f,0f,1f],scale:" + scaleNbt + ",right_rotation:[0f,0f,0f,1f]}}");
        schedule(level, FLOAT_LIFETIME_TICKS, "kill " + id);
    }

    // ---------- команды ----------

    private static void schedule(ServerLevel level, int delayTicks, String command) {
        TASKS.addLast(new Task(level.getServer().getTickCount() + delayTicks, level, command));
    }

    private static void run(ServerLevel level, String command) {
        CommandSourceStack source = level.getServer().createCommandSourceStack()
                .withLevel(level).withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(source, command);
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
