package io.github.smokahs.culinarycompat.compat.cfb;

import java.util.List;
import java.util.Set;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.neoforge.provider.NeoForgeBalmProviders;
import net.blay09.mods.cookingforblockheads.ItemHandlerKitchenItemProvider;
import net.blay09.mods.cookingforblockheads.api.KitchenItemProvider;
import net.blay09.mods.cookingforblockheads.kitchen.ContainerKitchenItemProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;

import io.github.smokahs.culinarycompat.compat.cfb.addons.farmersdelight.StorageDelight;

public final class CFB {
	public static final String CFB_MODID = "cookingforblockheads";
	public static final String FD_MODID = "farmersdelight";
	private static boolean active;
	private CFB() {
	}
	public static boolean isActive() {
		return active;
	}
	public static void init(IEventBus bus) {
		if (!ModList.get().isLoaded(CFB_MODID) || !ModList.get().isLoaded(FD_MODID))
			return;
		active = true;
		bus.addListener(CFB::setupProviders);
		NeoForge.EVENT_BUS.register(FarmersDelight.Cutting.class);
		NeoForge.EVENT_BUS.register(UI.CraftSound.class);
		NeoForge.EVENT_BUS.register(BakeState.Manager.class);
		NeoForge.EVENT_BUS.register(CuttingBoardInteract.class);
		if (FMLEnvironment.dist == Dist.CLIENT) {
			NeoForge.EVENT_BUS.register(UI.Tooltip.class);
			NeoForge.EVENT_BUS.register(UI.MissingToolsTooltip.class);
			net.blay09.mods.balm.api.client.BalmClient.onRuntimeAvailable(BakeState.Tooltip::register);
		}
		StorageDelight.init();
		if (ModList.get().isLoaded(Pam.PAM_MODID)) {
			bus.register(Pam.CreativeTabFilter.class);
			NeoForge.EVENT_BUS.register(Pam.RecipeStripper.class);
		}
		if (ModList.get().isLoaded(Croptopia.CROPTOPIA_MODID))
			NeoForge.EVENT_BUS.register(Croptopia.Stripper.class);
	}

	private static void setupProviders(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			var providers = (NeoForgeBalmProviders) Balm.getProviders();
			providers.registerFallbackBlockProvider(KitchenItemProvider.class, (be, side) -> {
				var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock());
				if (!Set.of(FD_MODID, "storagedelight").contains(id.getNamespace())
						|| !id.getPath().contains("cabinet") && !"storagedelight".equals(id.getNamespace()))
					return null;
				if (be instanceof Container container)
					return new ContainerKitchenItemProvider(container);
				var level = be.getLevel();
				var handler = level == null
						? null
						: level.getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), side);
				return handler == null ? null : new ItemHandlerKitchenItemProvider(handler);
			});
		});
	}

	public interface KitchenMemberView {
		Set<ResourceLocation> culinarycompat$getMemberBlocks();
		List<KitchenItemProvider> culinarycompat$getKitchenItemProviders();
		Set<BlockPos> culinarycompat$getMemberPositions();
	}
}
