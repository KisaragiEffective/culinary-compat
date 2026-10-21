package io.github.smokahs.culinarycompat;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import io.github.smokahs.culinarycompat.compat.DepCheck;
import io.github.smokahs.culinarycompat.compat.ae2.Bridge;
import io.github.smokahs.culinarycompat.compat.cfb.CFB;
import io.github.smokahs.culinarycompat.config.Configs;
import io.github.smokahs.culinarycompat.foodnerf.FoodNerf;
import io.github.smokahs.culinarycompat.network.Network;
import io.github.smokahs.culinarycompat.registry.Blocks;
import io.github.smokahs.culinarycompat.registry.Items;
import io.github.smokahs.culinarycompat.registry.LootFunctions;
import io.github.smokahs.culinarycompat.registry.Recipes;
import io.github.smokahs.culinarycompat.registry.Sounds;
import io.github.smokahs.culinarycompat.resource.OverridePack;

@Mod(CulinaryCompat.MODID)
public class CulinaryCompat {
	public static final String MODID = "culinarycompat";
	public static final Logger LOGGER = LogUtils.getLogger();

	public CulinaryCompat(IEventBus modEventBus, ModContainer container) {

		Items.ITEMS.register(modEventBus);
		io.github.smokahs.culinarycompat.registry.Components.COMPONENTS.register(modEventBus);
		Blocks.BLOCKS.register(modEventBus);
		Sounds.SOUNDS.register(modEventBus);
		Recipes.RECIPE_SERIALIZERS.register(modEventBus);
		Recipes.RECIPE_TYPES.register(modEventBus);
		LootFunctions.LOOT_FUNCTIONS.register(modEventBus);

		modEventBus.addListener(DepCheck::onCommonSetup);
		modEventBus.addListener(FoodNerf::modifyComponents);
		modEventBus.addListener(Network::register);
		modEventBus.addListener(OverridePack::onAddPackFinders);

		container.registerConfig(ModConfig.Type.STARTUP, Configs.Common.SPEC, "culinarycompat-common.toml");
		container.registerConfig(ModConfig.Type.CLIENT, Configs.Client.SPEC);

		CFB.init(modEventBus);
		Bridge.init(modEventBus);

	}

}
