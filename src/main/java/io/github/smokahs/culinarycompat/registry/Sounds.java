package io.github.smokahs.culinarycompat.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.smokahs.culinarycompat.CulinaryCompat;

public final class Sounds {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT,
			CulinaryCompat.MODID);

	public static final Supplier<SoundEvent> DING = SOUNDS.register("ding", () -> SoundEvent
			.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CulinaryCompat.MODID, "ding")));

	private Sounds() {
	}
}
