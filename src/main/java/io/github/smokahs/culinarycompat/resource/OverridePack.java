package io.github.smokahs.culinarycompat.resource;

import java.nio.file.Path;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforgespi.locating.IModFile;

import io.github.smokahs.culinarycompat.CulinaryCompat;

public final class OverridePack {
	private OverridePack() {
	}

	private static final String ROOT = "overrides";
	private static final String PACK_ID = "builtin/" + CulinaryCompat.MODID + "_overrides";

	public static void onAddPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() != PackType.SERVER_DATA) {
			return;
		}
		IModFile modFile = ModList.get().getModFileById(CulinaryCompat.MODID).getFile();
		Path root = modFile.findResource(ROOT);
		Pack pack = Pack.readMetaAndCreate(
				new net.minecraft.server.packs.PackLocationInfo(PACK_ID, Component.literal("Culinary Compat Overrides"),
						PackSource.BUILT_IN, java.util.Optional.empty()),
				new net.minecraft.server.packs.PathPackResources.PathResourcesSupplier(root), PackType.SERVER_DATA,
				new net.minecraft.server.packs.PackSelectionConfig(true, Pack.Position.TOP, true));
		if (pack != null) {
			event.addRepositorySource(consumer -> consumer.accept(pack));
		}
	}
}
