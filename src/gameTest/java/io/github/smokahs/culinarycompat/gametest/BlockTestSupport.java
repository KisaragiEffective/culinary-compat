package io.github.smokahs.culinarycompat.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;

public final class BlockTestSupport {
    public static final BlockPos POS = new BlockPos(1, 1, 1);

    private BlockTestSupport() {}

    public static Block block(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) {
            throw new AssertionError("Required integration block is missing: " + id);
        }
        return BuiltInRegistries.BLOCK.get(key);
    }

    public static void sameShape(GameTestHelper helper, VoxelShape actual, VoxelShape expected, String message) {
        helper.assertTrue(!Shapes.joinIsNotEmpty(actual, expected, BooleanOp.NOT_SAME), message);
    }

    public static ServerPlayer player(GameTestHelper helper) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "block-test"), net.minecraft.server.level.ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
        return player;
    }
}
