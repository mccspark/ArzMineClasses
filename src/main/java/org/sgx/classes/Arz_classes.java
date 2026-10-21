package org.sgx.classes;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;

import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.attachment.PlayerDataFiles;
import org.sgx.classes.block.ModBlockEntities;
import org.sgx.classes.block.ModMenus;
import org.sgx.classes.block.chest.ModChests;
import org.sgx.classes.command.ProfessionCommand;
import org.sgx.classes.event.*;
import org.sgx.classes.item.BonePouchItem;
import org.sgx.classes.item.FoodPouchItem;
import org.sgx.classes.item.ModBlocks;
import org.sgx.classes.item.ModCreativeTabs;
import org.sgx.classes.item.ModItems;
import org.sgx.classes.network.*;
import org.sgx.classes.profession.BoosterTimers;
import org.sgx.classes.profession.ClassStats;
import org.sgx.classes.profession.SubclassUnlockAll;
import org.sgx.classes.profession.WorldRarityHooks;
import org.sgx.classes.profession.impl.FarmerBonemealHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Arz_classes implements ModInitializer {
	public static final String MOD_ID = "arz_classes";


	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(OpenSharpeningPayload.TYPE, OpenSharpeningPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SharpeningResultPayload.TYPE, SharpeningResultPayload.CODEC);
		org.sgx.classes.item.ModDataComponents.init();
		ModAttachments.init();
		PlayerDataFiles.init();
		ModItems.init();
		PlayerDataSaveOnDisconnect.init();
		ProfessionNetworking.registerCommon();
		RarityUpgradeNetworking.registerCommon();
		ModBlocks.init();
		org.sgx.classes.block.ModWeaponRacks.init();
		ProfessionRespawnCopy.init();
		ModBlockEntities.init();
		ModChests.init();
		FarmerBonemealHandler.register();
		BoosterTimers.init();
		SubclassUnlockAll.init();
		ModMenus.init();
		WorldRarityLoot.init();
		WorldRarityEquipment.init();
		WorldRarityHooks.init();
		ProfessionEvents.init();
		ClassStats.init();
		MinerHelmetLight.init();
		SharpeningTrigger.init();
		BonePouchItem.register();
		FoodPouchItem.register();
		org.sgx.classes.item.HarvestBookItem.register();
		ModCreativeTabs.init();
		SkillUnlockEvents.init();
		HammerArmorGuard.init();
		FishingRodRepair.init();
		EnchantFullPreviewPayload.registerCommon();
		DiamondShardCrafting.init();
		FarmerAnimalKillEvents.registerBehavior(new org.sgx.classes.profession.impl.FarmerBehavior());
		FarmerAnimalKillEvents.init();
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> ProfessionCommand.register(dispatcher, registryAccess)
		);
		de.siphalor.capsaicin.api.food.FoodModifications.PROPERTIES_MODIFIERS.register(
				new org.sgx.classes.event.CookFoodModifier(),
				Arz_classes.id("cook_food_stars"),
				1000);
		de.siphalor.capsaicin.api.food.FoodModifications.CONSUMABLE_MODIFIERS.register(
				new org.sgx.classes.event.CookEatingSpeedModifier(),
				Arz_classes.id("cook_food_speed"),
				1000);
		LOGGER.info("Привет мир Арзмайна!!!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}