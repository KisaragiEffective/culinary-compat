package io.github.smokahs.culinarycompat.mixin.cfb;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.blay09.mods.cookingforblockheads.api.KitchenItemProvider;
import net.blay09.mods.cookingforblockheads.crafting.KitchenImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.smokahs.culinarycompat.compat.cfb.CFB;

@Mixin(value = KitchenImpl.class, remap = false)
public abstract class Multiblock implements CFB.KitchenMemberView {
	@Shadow
	@Final
	private List<KitchenItemProvider> itemProviderList;
	@Shadow
	@Final
	private Set<BlockPos> checkedPos;
	@Unique
	private final Set<ResourceLocation> culinarycompat$members = new HashSet<>();
	@Inject(method = "findNeighbourCraftingBlocks", at = @At("HEAD"))
	private void culinarycompat$record(Level level, BlockPos pos, boolean extended, CallbackInfo ci) {
		var state = level.getBlockState(pos);
		if (!state.isAir()) {
			culinarycompat$members.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
		}
		for (Direction direction : Direction.values()) {
			var id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.relative(direction)).getBlock());
			if (Set.of("culinarycompat", "farmersdelight", "endersdelight", "ends_delight", "mynethersdelight",
					"twilightdelight").contains(id.getNamespace()))
				culinarycompat$members.add(id);
		}
	}
	@Override
	public Set<ResourceLocation> culinarycompat$getMemberBlocks() {
		return culinarycompat$members;
	}
	@Override
	public List<KitchenItemProvider> culinarycompat$getKitchenItemProviders() {
		return itemProviderList;
	}
	@Override
	public Set<BlockPos> culinarycompat$getMemberPositions() {
		return Set.copyOf(checkedPos);
	}
}
