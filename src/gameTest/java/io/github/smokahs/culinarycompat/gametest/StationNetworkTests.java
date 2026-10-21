package io.github.smokahs.culinarycompat.gametest;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageService;
import appeng.api.storage.MEStorage;
import appeng.api.util.DimensionalBlockPos;
import io.github.smokahs.culinarycompat.compat.ae2.Bridge;
import io.github.smokahs.culinarycompat.compat.ae2.Mode;
import io.github.smokahs.culinarycompat.compat.ae2.StationBlock;
import io.github.smokahs.culinarycompat.compat.ae2.StationBlockEntity;
import io.github.smokahs.culinarycompat.compat.ae2.StationItem;

@GameTestHolder("culinarycompat")
@PrefixGameTestTemplate(false)
public final class StationNetworkTests {
	// Keep the integration boundary controllable while using real world BE lookup,
	// link NBT, server tick, registration, settings, and removal paths.
	private static final class AccessPoint extends StationBlockEntity implements IWirelessAccessPoint {
		boolean active;
		IGrid grid;

		AccessPoint(BlockPos pos, BlockState state) {
			super(pos, state);
		}
		@Override
		public boolean isActive() {
			return active;
		}
		@Override
		public IGrid getGrid() {
			return grid;
		}
		@Override
		public IGridNode getActionableNode() {
			return null;
		}
		@Override
		public DimensionalBlockPos getLocation() {
			return new DimensionalBlockPos(this);
		}
		@Override
		public double getRange() {
			return 16;
		}
		@Override
		public void serverTick() {
		}
	}

	private static final class GridFixture {
		final List<String> events = new ArrayList<>();
		final MEStorage inventory = () -> Component.literal("test inventory");
		final IStorageService storage = (IStorageService) Proxy.newProxyInstance(IStorageService.class.getClassLoader(),
				new Class<?>[]{IStorageService.class}, (proxy, method, args) -> {
					if (method.getName().equals("getInventory"))
						return inventory;
					events.add(method.getName());
					return null;
				});
		final IGrid grid = (IGrid) Proxy.newProxyInstance(IGrid.class.getClassLoader(), new Class<?>[]{IGrid.class},
				(proxy, method, args) -> {
					if (method.getName().equals("getStorageService"))
						return storage;
					throw new AssertionError("Unexpected grid call: " + method.getName());
				});
	}

	@GameTest(template = "empty")
	public static void accessPointLifecycleAndStorageRegistration(GameTestHelper helper) {
		helper.assertTrue(Bridge.isActive(), "Station dependencies must be loaded");
		helper.setBlock(BlockTestSupport.POS, Bridge.KITCHEN_STATION.get());
		var station = (StationBlockEntity) helper.getBlockEntity(BlockTestSupport.POS);
		var pointPos = BlockTestSupport.POS.offset(2, 0, 0);
		helper.setBlock(pointPos, Bridge.KITCHEN_STATION.get());
		var point = new AccessPoint(helper.absolutePos(pointPos), helper.getBlockState(pointPos));
		helper.getLevel().setBlockEntity(point);
		var item = new ItemStack(Bridge.KITCHEN_STATION_ITEM.get());
		StationItem.LINKABLE_HANDLER.link(item, GlobalPos.of(helper.getLevel().dimension(), point.getBlockPos()));
		station.applyDataFromItemToBlockEntity(item);
		station.serverTick();
		helper.assertTrue(station.getActionHost() == point && station.getNetworkStorage() == null,
				"Inactive access point is recognized but cannot expose storage");
		point.active = true;
		station.serverTick();
		helper.assertTrue(station.getNetworkStorage() == null, "Active access point without grid remains disconnected");
		var first = new GridFixture();
		point.grid = first.grid;
		station.serverTick();
		station.serverTick();
		helper.assertTrue(
				station.getNetworkStorage() == first.inventory
						&& helper.getBlockState(BlockTestSupport.POS).getValue(StationBlock.CONNECTED),
				"Active grid must expose inventory and connected state");
		helper.assertTrue(first.events.equals(List.of("addGlobalStorageProvider")),
				"Same grid must only register once");
		station.setMode(Mode.AE_TO_CFB);
		station.setPriority(7);
		helper.assertTrue(first.events.equals(
				List.of("addGlobalStorageProvider", "refreshGlobalStorageProvider", "refreshGlobalStorageProvider")),
				"Mode and priority changes refresh registration");
		station.setMode(Mode.AE_TO_CFB);
		station.setPriority(7);
		helper.assertTrue(first.events.size() == 3, "Unchanged settings must not refresh registration");
		var second = new GridFixture();
		point.grid = second.grid;
		station.serverTick();
		helper.assertTrue(
				first.events.get(3).equals("removeGlobalStorageProvider")
						&& second.events.equals(List.of("addGlobalStorageProvider")),
				"Grid switch unregisters old and registers new");
		point.active = false;
		station.serverTick();
		helper.assertTrue(second.events.equals(List.of("addGlobalStorageProvider", "removeGlobalStorageProvider"))
				&& station.getNetworkStorage() == null, "Power loss removes storage registration");
		point.active = true;
		station.serverTick();
		station.setRemoved();
		helper.assertTrue(
				second.events.equals(List.of("addGlobalStorageProvider", "removeGlobalStorageProvider",
						"addGlobalStorageProvider", "removeGlobalStorageProvider")),
				"Block removal must unregister storage");
		helper.succeed();
	}
}
