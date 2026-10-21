package io.github.smokahs.culinarycompat.gametest;

import java.util.ArrayList;

import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import io.github.smokahs.culinarycompat.compat.ae2.Bridge;
import io.github.smokahs.culinarycompat.compat.ae2.Mode;
import io.github.smokahs.culinarycompat.compat.ae2.StationBlock;
import io.github.smokahs.culinarycompat.compat.ae2.StationBlockEntity;
import io.github.smokahs.culinarycompat.compat.ae2.StationItem;
import io.github.smokahs.culinarycompat.compat.ae2.StationMenu;
import io.github.smokahs.culinarycompat.compat.ae2.ViewMode;

@GameTestHolder("culinarycompat")
@PrefixGameTestTemplate(false)
public final class StationTests {
	private static StationBlockEntity place(GameTestHelper helper) {
		helper.assertTrue(Bridge.isActive(), "AE2, CFB and Balm integrations must be loaded");
		helper.setBlock(BlockTestSupport.POS, Bridge.KITCHEN_STATION.get());
		helper.assertTrue(helper.getBlockEntity(BlockTestSupport.POS) instanceof StationBlockEntity,
				"Station block must create its registered block entity");
		return (StationBlockEntity) helper.getBlockEntity(BlockTestSupport.POS);
	}

	@GameTest(template = "empty")
	public static void directionalShapesAndSupportOffsets(GameTestHelper helper) {
		place(helper);
		var pos = helper.absolutePos(BlockTestSupport.POS);
		for (String support : new String[]{"minecraft:air", "minecraft:stone", "minecraft:oak_slab",
				"cookingforblockheads:counter", "cookingforblockheads:cooking_table"}) {
			helper.setBlock(BlockTestSupport.POS.below(), BlockTestSupport.block(support));
			boolean offset = support.equals("minecraft:stone") || support.equals("cookingforblockheads:cooking_table");
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				var state = Bridge.KITCHEN_STATION.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
						direction);
				helper.setBlock(BlockTestSupport.POS, state);
				VoxelShape expected = switch (direction) {
					case NORTH -> Block.box(3, -1, 6, 13, 7.5, 13);
					case SOUTH -> Block.box(3, -1, 3, 13, 7.5, 10);
					case EAST -> Block.box(3, -1, 3, 10, 7.5, 13);
					case WEST -> Block.box(6, -1, 3, 13, 7.5, 13);
					default -> throw new AssertionError(direction);
				};
				BlockTestSupport.sameShape(helper, state.getShape(helper.getLevel(), pos),
						offset ? expected.move(0, 1.0 / 16.0, 0) : expected,
						"Station shape for " + direction + " on " + support);
			}
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void placementPickupAndPersistentSettings(GameTestHelper helper) {
		var station = place(helper);
		var pos = helper.absolutePos(BlockTestSupport.POS);
		var item = new ItemStack(Bridge.KITCHEN_STATION_ITEM.get());
		var link = GlobalPos.of(helper.getLevel().dimension(), pos.offset(2, 0, 0));
		StationItem.LINKABLE_HANDLER.link(item, link);
		var block = Bridge.KITCHEN_STATION.get();
		block.setPlacedBy(helper.getLevel(), pos, station.getBlockState(), null, item);
		ItemStack pickedUp = new ItemStack(Bridge.KITCHEN_STATION_ITEM.get());
		station.applyDataFromBlockEntityToItem(pickedUp);
		helper.assertTrue(ItemStack.isSameItemSameTags(item, pickedUp), "Placement and pickup must preserve link NBT");
		for (Mode mode : Mode.values()) {
			for (ViewMode view : ViewMode.values()) {
				station.setMode(mode);
				station.setPriority(-123);
				station.setViewMode(view);
				CompoundTag saved = new CompoundTag();
				station.saveAdditional(saved);
				var restorePos = BlockTestSupport.POS.offset(0, 0, 2);
				helper.setBlock(restorePos, Bridge.KITCHEN_STATION.get());
				var restored = (StationBlockEntity) helper.getBlockEntity(restorePos);
				restored.load(saved);
				helper.assertTrue(
						restored.getMode() == mode && restored.getPriority() == -123 && restored.getViewMode() == view,
						"Settings must round-trip: " + mode + "/" + view);
				var restoredItem = new ItemStack(Bridge.KITCHEN_STATION_ITEM.get());
				restored.applyDataFromBlockEntityToItem(restoredItem);
				helper.assertTrue(ItemStack.isSameItemSameTags(item, restoredItem),
						"Saved station must preserve its link");
			}
		}
		station.applyDataFromItemToBlockEntity(ItemStack.EMPTY);
		var unlinked = new ItemStack(Bridge.KITCHEN_STATION_ITEM.get());
		station.applyDataFromBlockEntityToItem(unlinked);
		helper.assertTrue(!unlinked.hasTag(), "Placing an unlinked item clears the prior link");
		CompoundTag invalid = new CompoundTag();
		invalid.putString("Mode", "invalid");
		invalid.putString("ViewMode", "invalid");
		station.load(invalid);
		helper.assertTrue(station.getMode() == Mode.BIDIRECTIONAL && station.getViewMode() == ViewMode.NORMAL,
				"Unknown saved enum names must use defaults");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void connectionDisplayAndStorageDirection(GameTestHelper helper) {
		var station = place(helper);
		for (ViewMode view : ViewMode.values()) {
			station.setViewMode(view);
			for (boolean connected : new boolean[]{false, true, true, false}) {
				station.setConnected(connected);
				helper.assertTrue(
						helper.getBlockState(BlockTestSupport.POS)
								.getValue(StationBlock.CONNECTED) == (view == ViewMode.NORMAL && connected),
						"OFF view must suppress connected display");
			}
		}
		for (Mode mode : Mode.values()) {
			station.setMode(mode);
			station.setMode(mode);
			station.setMode(null);
			station.setPriority(42);
			var mounted = new ArrayList<Integer>();
			station.mountInventories((storage, priority) -> mounted.add(priority));
			helper.assertTrue(mounted.size() == (mode == Mode.AE_TO_CFB ? 0 : 1), "Storage mount direction: " + mode);
			helper.assertTrue(mounted.isEmpty() || mounted.get(0) == 42, "Storage mount must use configured priority");
		}
		station.setViewMode(null);
		station.serverTick();
		helper.assertTrue(station.getActionHost() == null && station.getNetworkStorage() == null
				&& station.getAccessPointPos().isEmpty(), "Unlinked server tick must stay disconnected");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void invalidLinksAndServerInteraction(GameTestHelper helper) {
		var station = place(helper);
		var pos = helper.absolutePos(BlockTestSupport.POS);
		for (GlobalPos link : new GlobalPos[]{GlobalPos.of(helper.getLevel().dimension(), pos.offset(2, 0, 0)),
				GlobalPos.of(
						ResourceKey.create(Registries.DIMENSION, new ResourceLocation("culinarycompat", "missing")),
						pos)}) {
			var item = new ItemStack(Bridge.KITCHEN_STATION_ITEM.get());
			StationItem.LINKABLE_HANDLER.link(item, link);
			station.applyDataFromItemToBlockEntity(item);
			station.serverTick();
			helper.assertTrue(station.getActionHost() == null && station.getNetworkStorage() == null,
					"Missing dimension or non-access-point block must not connect");
		}
		var player = BlockTestSupport.player(helper);
		var state = station.getBlockState();
		var result = state.use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
		helper.assertTrue(result == InteractionResult.CONSUME && player.containerMenu instanceof StationMenu,
				"Server use must open the station menu");
		player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		helper.assertTrue(player.containerMenu.stillValid(player), "Nearby player may use station menu");
		player.setPos(pos.getX() + 20, pos.getY(), pos.getZ());
		helper.assertTrue(!player.containerMenu.stillValid(player), "Distant player must lose access to station menu");
		player.closeContainer();
		var block = (StationBlock) Bridge.KITCHEN_STATION.get();
		var ticker = block.getTicker(helper.getLevel(), state, Bridge.KITCHEN_STATION_BE.get());
		helper.assertTrue(ticker != null, "Registered station BE must have a server ticker");
		helper.assertTrue(
				block.getTicker(helper.getLevel(), state,
						net.minecraft.world.level.block.entity.BlockEntityType.CHEST) == null,
				"Unrelated BE type must not receive a station ticker");
		ticker.tick(helper.getLevel(), pos, state, station);
		helper.succeed();
	}
}
