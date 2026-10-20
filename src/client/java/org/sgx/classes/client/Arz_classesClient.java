package org.sgx.classes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.util.datafix.fixes.TrappedChestBlockEntityFix;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import org.sgx.classes.block.ModMenus;
import org.sgx.classes.client.gui.*;
import org.sgx.classes.client.network.ProfessionClientNetworking;
import org.sgx.classes.network.EnchantFullPreviewPayload;
import org.sgx.classes.network.OpenRarityUpgradeWheelPayload;
import org.sgx.classes.network.OpenRarityWheelPayload;
import org.sgx.classes.network.OpenSharpeningPayload;
import org.sgx.classes.profession.impl.TreeChopSpeed;

public class Arz_classesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ProfessionClientNetworking.init();
        // замедление рубки больших деревьев: на клиенте данные профессии лежат в ClientProfessionData
        TreeChopSpeed.setClientDataSupplier(ClientProfessionData::get);
        ModKeyBindings.init();
        RarityTooltip.init();
        RingTooltip.init();
        BonePouchTooltip.init();
        BonePouchHint.register();
        FoodPouchTooltip.init();
        HarvestBookTooltip.init();
        org.sgx.classes.item.HarvestBookItem.clientOpener = hand ->
                net.minecraft.client.Minecraft.getInstance().gui.setScreen(new HarvestBookScreen(hand));
        MealStarsTooltip.init();
        MenuScreens.register(ModMenus.BONE_POUCH, BonePouchScreen::new);
        MenuScreens.register(ModMenus.FOOD_POUCH, FoodPouchScreen::new);
        MenuScreens.register(ModMenus.RARITY_UPGRADE_TABLE, RarityUpgradeTableScreen::new);
        MenuScreens.register(ModMenus.CARPENTER_WORKBENCH, CarpenterWorkbenchScreen::new);

        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                org.sgx.classes.block.chest.ModChests.CHEST_ENTITY,
                org.sgx.classes.client.chest.WoodChestRenderer::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                org.sgx.classes.block.chest.ModChests.TRAPPED_CHEST_ENTITY,
                org.sgx.classes.client.chest.WoodChestRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(OpenSharpeningPayload.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().gui.setScreen(new SharpeningScreen(
                        payload.center(), payload.brightWidth(), payload.greenWidth(),
                        payload.yellowWidth(), payload.barSpeed(), payload.repair()))));
        ClientPlayNetworking.registerGlobalReceiver(EnchantFullPreviewPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientEnchantPreview.set(payload.slots())));

        // Колесо ставится в очередь — открывается только когда игрок сам закроет
        // текущий экран (крафт-стол), чтобы не прерывать штатный возврат предмета.
        ClientPlayNetworking.registerGlobalReceiver(OpenRarityWheelPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PendingRarityWheel.queue(payload)));

        // То же самое для колеса апгрейда — сервер уже вызвал closeContainer() перед
        // отправкой, так что экран откроется на следующем тике после закрытия стола.
        ClientPlayNetworking.registerGlobalReceiver(OpenRarityUpgradeWheelPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PendingRarityUpgradeWheel.queue(payload)));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            OpenRarityWheelPayload readyCraft = PendingRarityWheel.takeIfReady(mc.gui.screen() == null);
            if (readyCraft != null) {
                mc.gui.setScreen(new RarityWheelScreen(readyCraft));
            }

            OpenRarityUpgradeWheelPayload readyUpgrade = PendingRarityUpgradeWheel.takeIfReady(mc.gui.screen() == null);
            if (readyUpgrade != null) {
                mc.gui.setScreen(new RarityUpgradeWheelScreen(readyUpgrade));
            }
        });
    }
}