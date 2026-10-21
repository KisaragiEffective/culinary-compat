package io.github.smokahs.culinarycompat.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.registries.RegistryObject;

import io.github.smokahs.culinarycompat.CulinaryCompat;

public final class Sounds {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
			CulinaryCompat.MODID);

	public static final RegistryObject<SoundEvent> DING = SOUNDS.register("ding",
			() -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CulinaryCompat.MODID, "ding")));

	private Sounds() {
	}
}
