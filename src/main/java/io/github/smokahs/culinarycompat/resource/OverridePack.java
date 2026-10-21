package io.github.smokahs.culinarycompat.resource;

import java.nio.file.Path;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.forgespi.locating.IModFile;

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
		Pack pack = Pack.readMetaAndCreate(PACK_ID, Component.literal("Culinary Compat Overrides"), true,
				id -> new net.neoforged.neoforge.resource.PathPackResources(id, true, root), PackType.SERVER_DATA,
				Pack.Position.TOP, PackSource.BUILT_IN);
		if (pack != null) {
			event.addRepositorySource(consumer -> consumer.accept(pack));
		}
	}
}
