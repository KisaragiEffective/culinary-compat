package io.github.smokahs.culinarycompat.registry;

import java.util.function.Supplier;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.smokahs.culinarycompat.CulinaryCompat;

public final class Components {
	public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister
			.create(Registries.DATA_COMPONENT_TYPE, CulinaryCompat.MODID);
	public static final Supplier<DataComponentType<GlobalPos>> ACCESS_POINT = COMPONENTS.register("access_point",
			() -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC)
					.networkSynchronized(GlobalPos.STREAM_CODEC).build());
	private Components() {
	}
}
