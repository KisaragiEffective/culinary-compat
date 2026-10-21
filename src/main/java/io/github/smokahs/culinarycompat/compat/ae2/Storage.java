package io.github.smokahs.culinarycompat.compat.ae2;

import java.util.ArrayList;
import java.util.List;

import net.blay09.mods.balm.api.container.BalmContainerProvider;
import net.blay09.mods.balm.api.provider.ProviderUtils;
import net.blay09.mods.cookingforblockheads.api.KitchenItemProvider;
import net.blay09.mods.cookingforblockheads.crafting.KitchenImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import io.github.smokahs.culinarycompat.compat.cfb.CFB;

public final class Storage implements MEStorage {
	private final StationBlockEntity station;
	public Storage(StationBlockEntity station) {
		this.station = station;
	}
	@Override
	public Component getDescription() {
		return station.getDisplayName();
	}
	private List<IItemHandler> inventories() {
		if (!(station.getLevel() instanceof ServerLevel level))
			return List.of();
		var kitchen = new KitchenImpl(level, station.getBlockPos());
		var result = new ArrayList<IItemHandler>();
		var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Container, Boolean>());
		for (var pos : ((CFB.KitchenMemberView) kitchen).culinarycompat$getMemberPositions()) {
			var be = level.getBlockEntity(pos);
			if (be == null || be instanceof StationBlockEntity
					|| ProviderUtils.getProvider(be, KitchenItemProvider.class) == null)
				continue;
			Container container = be instanceof BalmContainerProvider provider
					? provider.getContainer()
					: be instanceof Container c ? c : null;
			if (container != null) {
				if (seen.add(container))
					result.add(new InvWrapper(container));
			} else {
				var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
				if (handler != null)
					result.add(handler);
			}
		}
		return result;
	}
	@Override
	public void getAvailableStacks(KeyCounter out) {
		if (!station.getMode().allowsCFBToAE())
			return;
		for (var inventory : inventories())
			for (int i = 0; i < inventory.getSlots(); i++) {
				var stack = inventory.getStackInSlot(i);
				if (!stack.isEmpty())
					out.add(AEItemKey.of(stack), stack.getCount());
			}
	}
	@Override
	public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
		if (!station.getMode().allowsCFBToAE() || !(what instanceof AEItemKey key) || amount <= 0)
			return 0;
		long extracted = 0;
		for (var inventory : inventories())
			for (int i = 0; i < inventory.getSlots() && extracted < amount; i++) {
				if (!key.matches(inventory.getStackInSlot(i)))
					continue;
				extracted += inventory.extractItem(i, (int) Math.min(Integer.MAX_VALUE, amount - extracted),
						mode == Actionable.SIMULATE).getCount();
			}
		return extracted;
	}
	@Override
	public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
		if (!station.getMode().allowsCFBToAE() || !(what instanceof AEItemKey key) || amount <= 0)
			return 0;
		long inserted = 0;
		for (var inventory : inventories()) {
			for (int slot = 0; slot < inventory.getSlots() && inserted < amount; slot++) {
				int chunk = (int) Math.min(key.toStack().getMaxStackSize(), amount - inserted);
				var remainder = inventory.insertItem(slot, key.toStack(chunk), mode == Actionable.SIMULATE);
				inserted += chunk - remainder.getCount();
			}
		}
		return inserted;
	}
}
