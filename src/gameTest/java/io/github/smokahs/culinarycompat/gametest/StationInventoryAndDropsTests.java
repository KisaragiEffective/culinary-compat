package io.github.smokahs.culinarycompat.gametest;

import java.util.ArrayList;
import java.util.List;

import net.blay09.mods.cookingforblockheads.block.entity.CounterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import io.github.smokahs.culinarycompat.compat.ae2.Bridge;
import io.github.smokahs.culinarycompat.compat.ae2.ItemProvider;
import io.github.smokahs.culinarycompat.compat.ae2.Mode;
import io.github.smokahs.culinarycompat.compat.ae2.StationBlockEntity;
import io.github.smokahs.culinarycompat.compat.ae2.StationItem;
import io.github.smokahs.culinarycompat.compat.ae2.StationMenu;

@GameTestHolder("culinarycompat")
@PrefixGameTestTemplate(false)
public final class StationInventoryAndDropsTests {
    private static StationBlockEntity place(GameTestHelper helper) {
        helper.assertTrue(Bridge.isActive(), "Station integrations must be loaded");
        helper.setBlock(BlockTestSupport.POS, Bridge.KITCHEN_STATION.get());
        return (StationBlockEntity) helper.getBlockEntity(BlockTestSupport.POS);
    }

    @GameTest(template = "empty")
    public static void registeredBlockLootPreservesOnlyPresentLinks(GameTestHelper helper) {
        var station = place(helper);
        var pos = helper.absolutePos(BlockTestSupport.POS);
        var item = new ItemStack(Bridge.KITCHEN_STATION_ITEM.get());
        StationItem.LINKABLE_HANDLER.link(item, GlobalPos.of(helper.getLevel().dimension(), pos.offset(2, 0, 0)));
        station.applyDataFromItemToBlockEntity(item);
        var linked = Block.getDrops(station.getBlockState(), helper.getLevel(), pos, station);
        helper.assertTrue(linked.size() == 1 && linked.get(0).getCount() == 1
                        && ItemStack.isSameItemSameComponents(linked.get(0), item),
                "Registered station loot function must preserve the access-point link on its drop");
        station.applyDataFromItemToBlockEntity(ItemStack.EMPTY);
        var unlinked = Block.getDrops(station.getBlockState(), helper.getLevel(), pos, station);
        helper.assertTrue(unlinked.size() == 1 && !unlinked.get(0).has(io.github.smokahs.culinarycompat.registry.Components.ACCESS_POINT.get()), "Unlinked station must drop an unlinked item");
        var noEntity = Block.getDrops(station.getBlockState(), helper.getLevel(), pos, null);
        helper.assertTrue(noEntity.size() == 1 && !noEntity.get(0).has(io.github.smokahs.culinarycompat.registry.Components.ACCESS_POINT.get()), "Loot without a block entity must remain safe");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mountedKitchenStorageMovesRealCounterItems(GameTestHelper helper) {
        var station = place(helper);
        var counterPos = BlockTestSupport.POS.east();
        helper.setBlock(counterPos, BlockTestSupport.block("cookingforblockheads:counter"));
        var container = ((CounterBlockEntity) helper.getBlockEntity(counterPos)).getContainer();
        container.clearContent();
        container.setItem(0, new ItemStack(Items.CARROT, 60));
        var tagged = new ItemStack(Items.CARROT, 2);
        var customData = new net.minecraft.nbt.CompoundTag();
        customData.putString("test", "separate");
        tagged.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(customData));
        container.setItem(1, tagged);
        container.setItem(2, new ItemStack(Items.POTATO, 64));
        var mounts = new ArrayList<MEStorage>();
        station.mountInventories((storage, priority) -> mounts.add(storage));
        helper.assertTrue(mounts.size() == 1, "Station must mount its real kitchen storage");
        var storage = mounts.get(0);
        var carrot = AEItemKey.of(new ItemStack(Items.CARROT));
        var source = IActionSource.empty();
        var available = storage.getAvailableStacks();
        helper.assertTrue(available.get(carrot) == 60 && available.get(AEItemKey.of(tagged)) == 2,
                "Kitchen inventory listing must preserve distinct item NBT");
        helper.assertTrue(storage.extract(carrot, 10, Actionable.SIMULATE, source) == 10
                        && container.getItem(0).getCount() == 60, "Simulated extraction must not mutate counter contents");
        helper.assertTrue(storage.extract(carrot, 63, Actionable.MODULATE, source) == 60
                        && container.getItem(0).isEmpty() && container.getItem(1).getCount() == 2,
                "Extraction takes available matching items without taking differently tagged stacks");
        helper.assertTrue(storage.insert(carrot, 130, Actionable.SIMULATE, source) == 130
                        && storage.getAvailableStacks().get(carrot) == 0, "Simulated insertion must not add real items");
        helper.assertTrue(storage.insert(carrot, 130, Actionable.MODULATE, source) == 130
                        && storage.getAvailableStacks().get(carrot) == 130, "Insertion must split across real counter slots");
        helper.assertTrue(storage.insert(carrot, 5, Actionable.MODULATE, source) == 5
                        && storage.getAvailableStacks().get(carrot) == 135, "Insertion must merge existing matching stacks");
        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, new ItemStack(Items.STONE, 64));
        container.setItem(0, new ItemStack(Items.CARROT, 60));
        helper.assertTrue(storage.insert(carrot, 10, Actionable.MODULATE, source) == 4
                        && container.getItem(0).getCount() == 64, "Almost-full kitchen accepts only actual free capacity");
        helper.assertTrue(storage.insert(carrot, 1, Actionable.MODULATE, source) == 0, "Full kitchen must reject insertion");
        var water = AEFluidKey.of(Fluids.WATER);
        helper.assertTrue(storage.insert(water, 1000, Actionable.MODULATE, source) == 0
                        && storage.extract(water, 1000, Actionable.MODULATE, source) == 0, "Item storage must reject fluid keys");
        for (Mode mode : Mode.values()) {
            station.setMode(mode);
            boolean allowed = mode != Mode.AE_TO_CFB;
            helper.assertTrue(storage.getAvailableStacks().get(carrot) == (allowed ? 64 : 0),
                    "Kitchen item visibility must respect mode " + mode);
            long extracted = storage.extract(carrot, 2, Actionable.MODULATE, source);
            long inserted = storage.insert(carrot, 2, Actionable.MODULATE, source);
            helper.assertTrue(extracted == (allowed ? 2 : 0) && inserted == (allowed ? 2 : 0)
                            && container.getItem(0).getCount() == 64, "Actual transfers must respect mode " + mode);
        }
        helper.succeed();
    }

    private static final class MemoryStorage implements MEStorage {
        final KeyCounter contents = new KeyCounter();
        @Override public Component getDescription() { return Component.literal("test ME inventory"); }
        @Override public void getAvailableStacks(KeyCounter out) { out.addAll(contents); }
        @Override public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
            long taken = Math.min(amount, contents.get(key));
            if (mode == Actionable.MODULATE) contents.remove(key, taken);
            return taken;
        }
        @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
            long accepted = Math.min(amount, Math.max(0, 20 - contents.get(key)));
            if (mode == Actionable.MODULATE) contents.add(key, accepted);
            return accepted;
        }
    }

    private static final class NetworkStation extends StationBlockEntity {
        final MemoryStorage inventory = new MemoryStorage();
        final IActionHost host = () -> null;
        NetworkStation(BlockPos pos, BlockState state) { super(pos, state); }
        @Override public MEStorage getNetworkStorage() { return inventory; }
        @Override public IActionHost getActionHost() { return host; }
    }

    @GameTest(template = "empty")
    public static void kitchenProviderReservesConsumesAndReturnsNetworkItems(GameTestHelper helper) {
        var original = place(helper);
        var station = new NetworkStation(original.getBlockPos(), original.getBlockState());
        helper.getLevel().setBlockEntity(station);
        var carrot = AEItemKey.of(new ItemStack(Items.CARROT));
        station.inventory.contents.add(carrot, 20);
        var kitchenProvider = station.getProviders().stream().map(p -> p.getInstance()).filter(ItemProvider.class::isInstance)
                .map(ItemProvider.class::cast).findFirst().orElseThrow();
        var provider = new ProviderApi(kitchenProvider, List.of(new ItemStack(Items.CARROT)));
        helper.assertTrue(provider.getSlots() == 1, "Connected provider must expose network item kinds");
        provider.useItemStack(new ItemStack(Items.CARROT, 20), 3, true, List.of(), false);
        helper.assertTrue(provider.getSimulatedUseCount("minecraft:carrot") == 3
                        && station.inventory.contents.get(carrot) == 20, "Simulation reserves items without extracting real network items");
        provider.useItemStack(new ItemStack(Items.CARROT, 20), 18, true, List.of(), false);
        helper.assertTrue(provider.getSimulatedUseCount("minecraft:carrot") == 3,
                "Insufficient unreserved amount must not create further reservations");
        provider.resetSimulation();
        helper.assertTrue(provider.getSimulatedUseCount("minecraft:carrot") == 0, "Reset releases network reservations");
        provider.useItemStack(new ItemStack(Items.CARROT, 20), 7, false, List.of(), false);
        helper.assertTrue(station.inventory.contents.get(carrot) == 13, "Real use removes items from ME storage");
        var remainder = provider.returnItemStack(new ItemStack(Items.CARROT, 10), null);
        helper.assertTrue(remainder.getCount() == 3 && station.inventory.contents.get(carrot) == 20,
                "Returning items respects network capacity and returns the unaccepted remainder");
        station.setMode(Mode.CFB_TO_AE);
        helper.assertTrue(provider.getSlots() == 0, "CFB-to-AE-only mode must hide ME ingredients from kitchen");
        provider.useItemStack(new ItemStack(Items.CARROT, 20), 2, false, List.of(), false);
        var rejected = new ItemStack(Items.CARROT, 2);
        helper.assertTrue(provider.returnItemStack(rejected, null) == rejected && station.inventory.contents.get(carrot) == 20,
                "Disabled direction must neither consume nor return network items");
        for (Mode mode : new Mode[]{Mode.AE_TO_CFB, Mode.BIDIRECTIONAL}) {
            station.setMode(mode);
            long before = station.inventory.contents.get(carrot);
            provider.useItemStack(new ItemStack(Items.CARROT, 20), 2, false, List.of(), false);
            helper.assertTrue(station.inventory.contents.get(carrot) == before - 2, "Enabled ME-to-kitchen direction: " + mode);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void serverUseHandlesGenericPlayersAndMissingBlockEntities(GameTestHelper helper) {
        var station = place(helper);
        var pos = station.getBlockPos();
        var state = station.getBlockState();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        helper.assertTrue(state.useWithoutItem(helper.getLevel(), helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE), hit)
                        == InteractionResult.CONSUME, "Generic player use must safely consume server interaction");
        var player = BlockTestSupport.player(helper);
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        var menu = new StationMenu(1, player.getInventory(), station);
        helper.assertTrue(menu.stillValid(player), "Menu must initially recognize its nearby station");
        helper.setBlock(BlockTestSupport.POS, net.minecraft.world.level.block.Blocks.AIR);
        helper.assertTrue(!menu.stillValid(player), "Removed station block entity must invalidate menu");
        helper.assertTrue(state.useWithoutItem(helper.getLevel(), player, hit) == InteractionResult.CONSUME
                        && player.containerMenu == player.inventoryMenu, "Missing block entity use must not open a menu");
        helper.succeed();
    }
}
