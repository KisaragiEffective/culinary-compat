package io.github.smokahs.culinarycompat.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import io.github.smokahs.culinarycompat.CulinaryCompat;
import io.github.smokahs.culinarycompat.config.Configs;

@net.neoforged.fml.common.EventBusSubscriber(modid = CulinaryCompat.MODID)
public final class DepCheck {
	private static final String[] DEPS = {"farmersdelight", "pamhc2foodcore", "pamhc2crops", "pamhc2trees",
			"pamhc2foodextended"};

	private static List<String> missing = List.of();

	private DepCheck() {
	}

	public static void onCommonSetup(FMLCommonSetupEvent event) {
		List<String> found = new ArrayList<>();
		for (String id : DEPS) {
			if (!ModList.get().isLoaded(id))
				found.add(id);
		}
		missing = List.copyOf(found);
		if (!missing.isEmpty()) {
			CulinaryCompat.LOGGER.warn("Culinary Compat running without {}. Install for all compat recipes/tags.",
					String.join(", ", missing));
		}
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (missing.isEmpty() || !Configs.Common.notifyMissingDeps)
			return;
		Component msg = Component.literal("[Culinary Compat] Missing optional deps: " + String.join(", ", missing)
				+ ". Install for full recipe/tag integration.").withStyle(ChatFormatting.GOLD);
		event.getEntity().sendSystemMessage(msg);
	}
}
