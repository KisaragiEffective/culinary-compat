package io.github.smokahs.culinarycompat.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.smokahs.culinarycompat.CulinaryCompat;
import io.github.smokahs.culinarycompat.compat.patchouli.Guidebook;

public final class Items {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM,
			CulinaryCompat.MODID);

	public static final Supplier<Item> BAKEWARE = ITEMS.register("bakeware",
			() -> new BlockItem(Blocks.BAKEWARE.get(), new Item.Properties().stacksTo(1)));

	public static final Supplier<Item> GUIDEBOOK = ITEMS.register("guidebook",
			() -> new Guidebook(new Item.Properties()));

	private Items() {
	}
}
