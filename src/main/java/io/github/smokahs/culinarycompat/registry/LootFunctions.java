package io.github.smokahs.culinarycompat.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.smokahs.culinarycompat.CulinaryCompat;
import io.github.smokahs.culinarycompat.compat.ae2.StationBlockEntity;

public final class LootFunctions {
	public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = DeferredRegister
			.create(Registries.LOOT_FUNCTION_TYPE, CulinaryCompat.MODID);

	public static final Supplier<LootItemFunctionType<StationBlockEntity.LootFunction>> AE2_KITCHEN_STATION = LOOT_FUNCTIONS
			.register("ae2_kitchen_station", () -> new LootItemFunctionType<>(StationBlockEntity.LootFunction.CODEC));

	private LootFunctions() {
	}
}
