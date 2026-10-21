package io.github.smokahs.culinarycompat.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import io.github.smokahs.culinarycompat.config.Configs;
import io.github.smokahs.culinarycompat.recipe.MultiCuttingExtras;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;

@GameTestHolder("culinarycompat")
@PrefixGameTestTemplate(false)
public final class CuttingBoardTests {
	private static CuttingBoardBlockEntity place(GameTestHelper helper) {
		helper.setBlock(BlockTestSupport.POS.below(), net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(BlockTestSupport.POS, BlockTestSupport.block("farmersdelight:cutting_board"));
		var board = (CuttingBoardBlockEntity) helper.getBlockEntity(BlockTestSupport.POS);
		helper.assertTrue(board instanceof MultiCuttingExtras, "Cutting board inventory Mixin must be applied");
		return board;
	}

	// Main retains its original FD 1.2 compile dependency; the test runtime uses
	// FD 1.3, whose addItem return type changed from boolean to ItemStack.
	private static ItemStack add(CuttingBoardBlockEntity board, ItemStack stack) {
		try {
			return (ItemStack) board.getClass().getMethod("addItem", ItemStack.class).invoke(board, stack);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Required FD 1.3 addItem API unavailable", e);
		}
	}

	private static ItemStackHandler extras(CuttingBoardBlockEntity board) {
		return ((MultiCuttingExtras) board).culinarycompat$getExtras();
	}

	private static List<ItemStack> storedItems(CuttingBoardBlockEntity board) {
		var items = new ArrayList<ItemStack>();
		items.add(board.getInventory().getStackInSlot(0).copy());
		for (int i = 0; i < extras(board).getSlots(); i++) {
			items.add(extras(board).getStackInSlot(i).copy());
		}
		return items;
	}

	private static void assertStoredItemsUnchanged(GameTestHelper helper, CuttingBoardBlockEntity board,
			List<ItemStack> expected, String context) {
		var actual = storedItems(board);
		for (int i = 0; i < expected.size(); i++) {
			helper.assertTrue(ItemStack.isSameItemSameComponents(actual.get(i), expected.get(i))
					&& actual.get(i).getCount() == expected.get(i).getCount(), context + " at board slot " + i);
		}
	}

	private static InteractionResult use(GameTestHelper helper, Player player, InteractionHand hand) {
		var pos = helper.absolutePos(BlockTestSupport.POS);
		return helper.getBlockState(BlockTestSupport.POS).useItemOn(player.getItemInHand(hand), helper.getLevel(),
				player, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)).result();
	}

	@GameTest(template = "empty")
	public static void singleBaseSlotAndLastInFirstOut(GameTestHelper helper) {
		var board = place(helper);
		helper.assertTrue(board.isEmpty() && board.removeItem().isEmpty(), "New board is empty");
		helper.assertTrue(add(board, ItemStack.EMPTY) == ItemStack.EMPTY, "Empty insertion remains empty");
		ItemStack many = new ItemStack(Items.CARROT, 5);
		helper.assertTrue(add(board, many).getCount() == 4 && board.getInventory().getStackInSlot(0).getCount() == 1,
				"Base slot must accept exactly one ingredient");
		ItemStack rejected = new ItemStack(Items.APPLE, 2);
		helper.assertTrue(add(board, rejected) == rejected && rejected.getCount() == 2,
				"Occupied base slot must reject insertion without mutation");
		extras(board).setStackInSlot(0, new ItemStack(Items.APPLE));
		extras(board).setStackInSlot(7, new ItemStack(Items.POTATO));
		helper.assertTrue(
				board.removeItem().is(Items.POTATO) && board.removeItem().is(Items.APPLE)
						&& board.removeItem().is(Items.CARROT) && board.removeItem().isEmpty() && board.isEmpty(),
				"Removal must pop highest extra slots before base, skipping empty slots");
		extras(board).setStackInSlot(3, new ItemStack(Items.APPLE));
		helper.assertTrue(!board.isEmpty(), "Extras-only board must not be reported empty");
		helper.assertTrue(board.removeItem().is(Items.APPLE) && board.isEmpty(), "Extras-only board can be emptied");
		ItemStack one = new ItemStack(Items.CARROT);
		helper.assertTrue(add(board, one) == ItemStack.EMPTY, "Consumed insertion must return EMPTY singleton");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void extrasSlotLimitAndPersistence(GameTestHelper helper) {
		var board = place(helper);
		helper.assertTrue(extras(board).getSlots() == 8, "Board must expose eight extra slots");
		var stack = new ItemStack(Items.APPLE, 3);
		var customData = new CompoundTag();
		customData.putString("test", "preserved");
		stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.of(customData));
		var rest = extras(board).insertItem(4, stack, false);
		helper.assertTrue(rest.getCount() == 2 && extras(board).getStackInSlot(4).getCount() == 1,
				"Each extra slot is limited to one ingredient");
		var tag = new CompoundTag();
		board.saveAdditional(tag, helper.getLevel().registryAccess());
		var other = BlockTestSupport.POS.offset(2, 0, 0);
		helper.setBlock(other.below(), net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(other, BlockTestSupport.block("farmersdelight:cutting_board"));
		var restored = (CuttingBoardBlockEntity) helper.getBlockEntity(other);
		restored.loadWithComponents(tag, helper.getLevel().registryAccess());
		helper.assertTrue(
				ItemStack.isSameItemSameComponents(extras(board).getStackInSlot(4), extras(restored).getStackInSlot(4)),
				"World block entity save/load preserves extra slot items and NBT");
		restored.loadWithComponents(new CompoundTag(), helper.getLevel().registryAccess());
		helper.assertTrue(extras(restored).getStackInSlot(4).is(Items.APPLE),
				"Legacy NBT without extras must leave them unchanged");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void useAddsExtrasWithSurvivalAndCreativeRules(GameTestHelper helper) {
		boolean previous = Configs.Common.multiCutting;
		try {
			Configs.Common.multiCutting = true;
			var board = place(helper);
			add(board, new ItemStack(Items.CARROT));
			var player = BlockTestSupport.player(helper);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 3));
			helper.assertTrue(use(helper, player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS,
					"Non-tool use on occupied board adds an extra");
			helper.assertTrue(
					extras(board).getStackInSlot(0).is(Items.DIRT) && player.getMainHandItem().getCount() == 2,
					"Survival insertion consumes exactly one");
			player.getAbilities().instabuild = true;
			use(helper, player, InteractionHand.MAIN_HAND);
			helper.assertTrue(
					extras(board).getStackInSlot(1).is(Items.DIRT) && player.getMainHandItem().getCount() == 2,
					"Creative insertion keeps held count");
			for (int i = 2; i < extras(board).getSlots(); i++)
				extras(board).setStackInSlot(i, new ItemStack(Items.DIRT));
			player.getAbilities().instabuild = false;
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 3));
			var fullBoard = storedItems(board);
			var heldBefore = player.getMainHandItem().copy();
			use(helper, player, InteractionHand.MAIN_HAND);
			helper.assertTrue(
					ItemStack.isSameItemSameComponents(player.getMainHandItem(), heldBefore)
							&& player.getMainHandItem().getCount() == heldBefore.getCount(),
					"Full board must not consume or replace the survival player's held item");
			assertStoredItemsUnchanged(helper, board, fullBoard, "Full board use must preserve every stored item");
			Configs.Common.multiCutting = false;
			extras(board).setStackInSlot(0, ItemStack.EMPTY);
			use(helper, player, InteractionHand.MAIN_HAND);
			helper.assertTrue(extras(board).getStackInSlot(0).isEmpty(), "Disabled multi-cutting must not add extras");
		} finally {
			Configs.Common.multiCutting = previous;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void crouchingEmptyHandRetrievesAllIngredients(GameTestHelper helper) {
		var board = place(helper);
		add(board, new ItemStack(Items.CARROT));
		extras(board).setStackInSlot(0, new ItemStack(Items.APPLE));
		extras(board).setStackInSlot(7, new ItemStack(Items.POTATO));
		var player = BlockTestSupport.player(helper);
		player.setShiftKeyDown(true);
		player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
		use(helper, player, InteractionHand.MAIN_HAND);
		helper.assertTrue(board.isEmpty(), "Crouching empty main hand empties every slot");
		helper.assertTrue(
				player.getInventory().countItem(Items.CARROT) == 1 && player.getInventory().countItem(Items.APPLE) == 1
						&& player.getInventory().countItem(Items.POTATO) == 1,
				"Survival pickup returns all ingredients");
		add(board, new ItemStack(Items.CARROT));
		extras(board).setStackInSlot(0, new ItemStack(Items.APPLE));
		// FD's native use reads mainhand even for offhand calls. Keep it nonempty
		// and non-tool so this assertion isolates the bulk-removal hand guard.
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
		player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		var beforeOffhand = storedItems(board);
		use(helper, player, InteractionHand.OFF_HAND);
		assertStoredItemsUnchanged(helper, board, beforeOffhand,
				"Crouching empty offhand must preserve every base and extra item");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void bulkPickupHandlesFullInventoryAndCreativePlayer(GameTestHelper helper) {
		var board = place(helper);
		add(board, new ItemStack(Items.CARROT));
		extras(board).setStackInSlot(0, new ItemStack(Items.APPLE));
		var player = BlockTestSupport.player(helper);
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		// Selected empty hand is also an inventory slot; fill it with another
		// stack only during add() by checking both returned items as a whole.
		player.setShiftKeyDown(true);
		player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
		use(helper, player, InteractionHand.MAIN_HAND);
		var pos = helper.absolutePos(BlockTestSupport.POS);
		int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
				.mapToInt(e -> e.getItem().getCount()).sum();
		int recovered = player.getInventory().countItem(Items.CARROT) + player.getInventory().countItem(Items.APPLE);
		helper.assertTrue(board.isEmpty() && dropped + recovered == 2 && dropped == 1,
				"Bulk pickup uses the free hand slot then drops overflow without losing ingredients");
		add(board, new ItemStack(Items.CARROT));
		extras(board).setStackInSlot(0, new ItemStack(Items.APPLE));
		var creative = new net.minecraft.world.entity.player.Player(helper.getLevel(), pos, 0,
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "creative-block-test")) {
			@Override
			public boolean isSpectator() {
				return false;
			}
			@Override
			public boolean isCreative() {
				return true;
			}
		};
		creative.setShiftKeyDown(true);
		creative.setPose(net.minecraft.world.entity.Pose.CROUCHING);
		use(helper, creative, InteractionHand.MAIN_HAND);
		helper.assertTrue(
				board.isEmpty() && creative.getInventory().countItem(Items.CARROT) == 0
						&& creative.getInventory().countItem(Items.APPLE) == 0,
				"Creative bulk removal clears ingredients without granting items");
		int after = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
				.mapToInt(e -> e.getItem().getCount()).sum();
		helper.assertTrue(after == dropped, "Creative removal must not spawn duplicate ingredient drops");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void replacementDropsExtrasButStateChangeDoesNot(GameTestHelper helper) {
		var board = place(helper);
		extras(board).setStackInSlot(0, new ItemStack(Items.APPLE));
		extras(board).setStackInSlot(7, new ItemStack(Items.POTATO));
		var state = helper.getBlockState(BlockTestSupport.POS);
		var pos = helper.absolutePos(BlockTestSupport.POS);
		var bounds = new AABB(pos).inflate(1);
		state.onRemove(helper.getLevel(), pos, state, false);
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).isEmpty(),
				"Same-block state update must not drop extra ingredients");
		helper.setBlock(BlockTestSupport.POS, net.minecraft.world.level.block.Blocks.AIR);
		var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds);
		helper.assertTrue(drops.stream().mapToInt(e -> e.getItem().is(Items.APPLE) ? e.getItem().getCount() : 0)
				.sum() == 1
				&& drops.stream().mapToInt(e -> e.getItem().is(Items.POTATO) ? e.getItem().getCount() : 0).sum() == 1,
				"Replacing block drops every occupied extra slot exactly once");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void counterAndCornerSupportAndShape(GameTestHelper helper) {
		var board = place(helper);
		var pos = helper.absolutePos(BlockTestSupport.POS);
		var state = board.getBlockState();
		for (String support : new String[]{"cookingforblockheads:counter"}) {
			helper.setBlock(BlockTestSupport.POS.below(), BlockTestSupport.block(support));
			helper.assertTrue(state.canSurvive(helper.getLevel(), pos), "Board must survive on " + support);
			BlockTestSupport.sameShape(helper, state.getShape(helper.getLevel(), pos), Block.box(1, -1, 1, 15, 0, 15),
					"Counter/corner support must lower shape");
		}
		helper.setBlock(BlockTestSupport.POS.below(), net.minecraft.world.level.block.Blocks.STONE);
		helper.assertTrue(state.canSurvive(helper.getLevel(), pos), "Native sturdy support must remain allowed");
		BlockTestSupport.sameShape(helper, state.getShape(helper.getLevel(), pos), Block.box(1, 0, 1, 15, 1, 15),
				"Native support must retain unshifted shape");
		helper.setBlock(BlockTestSupport.POS.below(), net.minecraft.world.level.block.Blocks.AIR);
		helper.assertTrue(!state.canSurvive(helper.getLevel(), pos), "Air must not support board");
		helper.succeed();
	}
}
