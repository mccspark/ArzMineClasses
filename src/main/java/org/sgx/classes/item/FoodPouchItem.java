package org.sgx.classes.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.sgx.classes.network.CycleFoodPouchPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

/** Мешочек для еды (навык Фермера 7 уровня): 5 слотов, в каждом до 8 штук еды. */
public final class FoodPouchItem extends Item {

    public static final int SLOT_COUNT = 5;
    public static final int SLOT_CAPACITY = 8;
    public static final int MAX_CONTENT = SLOT_COUNT * SLOT_CAPACITY;

    public FoodPouchItem(Properties properties) {
        super(properties);
    }

    /** Еда = предмет с компонентом FOOD (у него есть насыщение и сытость). */
    public static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.FOOD);
    }

    public static NonNullList<ItemStack> readContents(ItemStack pouch) {
        ItemContainerContents contents = pouch.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        contents.copyInto(items);
        return items;
    }

    public static int storedAmount(ItemStack pouch) {
        int total = 0;
        for (ItemStack s : readContents(pouch)) {
            total += s.getCount();
        }
        return total;
    }

    /**
     * Пока мешочек в руке — рисуем его моделью выбранной еды (компонент item_model),
     * иначе (инвентарь, земля, сундук) — обычной моделью мешочка.
     */
    public static void refreshModel(ItemStack pouch, boolean held) {
        Identifier target = Arz_classes.id("food_pouch");
        if (held) {
            ItemStack food = selectedFood(pouch);
            if (!food.isEmpty()) {
                Identifier foodModel = food.get(DataComponents.ITEM_MODEL);
                if (foodModel != null) {
                    target = foodModel;
                }
            }
        }
        if (!target.equals(pouch.get(DataComponents.ITEM_MODEL))) {
            pouch.set(DataComponents.ITEM_MODEL, target);
        }
    }

    private static int sweepTicks = 0;

    /** Раз в несколько тиков возвращаем модель мешочка тем, что лежат не в руках (брошены, убраны в слот). */
    private static void sweepInventories(net.minecraft.server.MinecraftServer server) {
        if (++sweepTicks % 4 != 0) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.getItem() instanceof FoodPouchItem) {
                    refreshModel(s, s == p.getMainHandItem() || s == p.getOffhandItem());
                }
            }
        }
    }

    public static void playSound(Player player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ---------- полоска заполненности на иконке (как у бундла, 0..40 еды) ----------

    @Override
    public boolean isBarVisible(ItemStack pouch) {
        return storedAmount(pouch) > 0;
    }

    @Override
    public int getBarWidth(ItemStack pouch) {
        return Math.min(13, Math.round(13.0f * storedAmount(pouch) / MAX_CONTENT));
    }

    @Override
    public int getBarColor(ItemStack pouch) {
        return storedAmount(pouch) >= MAX_CONTENT ? 0xFFFF5555 : 0xFF55FF55;
    }

    // ---------- частицы и звук жевания при еде прямо из мешочка ----------

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack pouch, int remainingUseDuration) {
        ItemStack food = selectedFood(pouch);
        Consumable consumable = food.get(DataComponents.CONSUMABLE);
        if (consumable == null || !consumable.hasConsumeParticles()) {
            return;
        }
        int total = getUseDuration(pouch, entity);
        // те же условия, что у обычной еды: после 7-го тика, каждые 4 тика
        if (remainingUseDuration <= total - 7 && remainingUseDuration % 4 == 0) {
            consumable.emitParticlesAndSounds(entity.getRandom(), entity, food, 5);
        }
    }

    /** Выбранная еда — первая непустая ячейка мешочка (прокрутка переставляет выбранную в начало). */
    public static ItemStack selectedFood(ItemStack pouch) {
        for (ItemStack s : readContents(pouch)) {
            if (!s.isEmpty()) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Прокрутка: сдвигает еду по кругу, выбранная всегда оказывается в первой ячейке. */
    public static void cycle(ServerPlayer player, ItemStack pouch, int direction) {
        List<ItemStack> foods = new ArrayList<>();
        for (ItemStack s : readContents(pouch)) {
            if (!s.isEmpty()) {
                foods.add(s);
            }
        }
        if (foods.isEmpty()) {
            return;
        }
        if (foods.size() > 1) {
            if (direction > 0) {
                foods.add(foods.remove(0));
            } else {
                foods.add(0, foods.remove(foods.size() - 1));
            }
            NonNullList<ItemStack> out = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            for (int i = 0; i < foods.size(); i++) {
                out.set(i, foods.get(i));
            }
            pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(out));
        }
        refreshModel(pouch, true);
        if (foods.size() > 1) {
            playSound(player, SoundEvents.BUNDLE_INSERT, 0.35f, 1.6f);
        }
        ItemStack selected = foods.get(0);
        player.sendSystemMessage(Component.translatable("message.arz_classes.food_pouch_selected",
                selected.getHoverName(), selected.getCount()), true);
    }

    // ---------- поедание прямо из мешочка (ПКМ) ----------

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack food = selectedFood(player.getItemInHand(hand));
        FoodProperties props = food.get(DataComponents.FOOD);
        if (food.isEmpty() || props == null || !food.has(DataComponents.CONSUMABLE)) {
            return InteractionResult.PASS;
        }
        if (!player.canEat(props.canAlwaysEat())) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack pouch, LivingEntity entity) {
        Consumable consumable = selectedFood(pouch).get(DataComponents.CONSUMABLE);
        return consumable != null ? consumable.consumeTicks() : 0;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack pouch) {
        Consumable consumable = selectedFood(pouch).get(DataComponents.CONSUMABLE);
        return consumable != null ? consumable.animation() : ItemUseAnimation.NONE;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pouch, Level level, LivingEntity entity) {
        if (level.isClientSide()) {
            return pouch;
        }
        NonNullList<ItemStack> items = readContents(pouch);
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return pouch;
        }
        ItemStack food = items.get(index);
        ItemStack single = food.copyWithCount(1);
        // штатное поедание: эффекты, насыщение, звук; результат — остаток (миска, бутылка), если он есть
        ItemStack result = single.finishUsingItem(level, entity);

        boolean creative = entity instanceof Player p && p.getAbilities().instabuild;
        if (!creative) {
            food.shrink(1);
            if (!result.isEmpty() && !ItemStack.isSameItem(result, single)) {
                ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), result);
                level.addFreshEntity(drop);
            }
            pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            refreshModel(pouch, true);
        }
        return pouch;
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FoodPouchItem::sweepInventories);
        PayloadTypeRegistry.serverboundPlay().register(CycleFoodPouchPayload.TYPE, CycleFoodPouchPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CycleFoodPouchPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    ItemStack stack = player.getMainHandItem();
                    if (stack.getItem() instanceof FoodPouchItem) {
                        cycle(player, stack, payload.direction());
                    }
                }));

        // Shift + ПКМ в воздухе — открыть мешочек
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof FoodPouchItem) || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            openGui(serverPlayer, hand);
            return InteractionResult.SUCCESS;
        });
    }

    private static void openGui(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        NonNullList<ItemStack> items = readContents(stack);
        SimpleContainer container = new SimpleContainer(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            container.setItem(i, items.get(i));
        }

        playSound(player, SoundEvents.BUNDLE_REMOVE_ONE, 0.7f, 1.0f);
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("item.arz_classes.food_pouch");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
                return new FoodPouchMenu(containerId, inventory, container, hand, stack);
            }
        });
    }
}