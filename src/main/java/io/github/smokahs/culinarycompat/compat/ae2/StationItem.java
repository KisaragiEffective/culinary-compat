package io.github.smokahs.culinarycompat.compat.ae2;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import appeng.api.features.IGridLinkableHandler;

public class StationItem extends BlockItem {

	public static final IGridLinkableHandler LINKABLE_HANDLER = new LinkableHandler();
	public static final String TAG_ACCESS_POINT_POS = "accessPointPos";

	public StationItem(StationBlock block, Properties props) {
		super(block, props);
	}

	private static class LinkableHandler implements IGridLinkableHandler {
		@Override
		public boolean canLink(ItemStack stack) {
			return stack.getItem() instanceof StationItem;
		}

		@Override
		public void link(ItemStack itemStack, GlobalPos pos) {
			itemStack.set(io.github.smokahs.culinarycompat.registry.Components.ACCESS_POINT.get(), pos);
		}

		@Override
		public void unlink(ItemStack itemStack) {
			itemStack.remove(io.github.smokahs.culinarycompat.registry.Components.ACCESS_POINT.get());
		}
	}
}
