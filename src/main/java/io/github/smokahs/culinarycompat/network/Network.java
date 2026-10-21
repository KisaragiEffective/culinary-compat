package io.github.smokahs.culinarycompat.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.github.smokahs.culinarycompat.CulinaryCompat;
import io.github.smokahs.culinarycompat.bridges.Bridges;
import io.github.smokahs.culinarycompat.compat.ae2.Bridge;
import io.github.smokahs.culinarycompat.compat.ae2.StationSettingsPacket;

public final class Network {
	private Network() {
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		var registrar = event.registrar("2");
		registrar.playToClient(BakeStatusPacket.TYPE, BakeStatusPacket.STREAM_CODEC, BakeStatusPacket::handle);
		registrar.playToClient(Sync.TYPE, Sync.STREAM_CODEC, Sync::handle);
		if (Bridge.isActive()) {
			registrar.playToServer(StationSettingsPacket.TYPE, StationSettingsPacket.STREAM_CODEC,
					StationSettingsPacket::handle);
		}
	}

	public static void sendToClient(ServerPlayer player, BakeStatusPacket packet) {
		PacketDistributor.sendToPlayer(player, packet);
	}

	public static void sendBridges(ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, new Sync(new java.util.ArrayList<>(Bridges.getAll())));
	}

	public static void sendBridgesToAll() {
		PacketDistributor.sendToAllPlayers(new Sync(new java.util.ArrayList<>(Bridges.getAll())));
	}

	public enum BakePhase {
		CONFIRM, BAKING, FINISHED, CANCELLED
	}

	public record BakeStatusPacket(BakePhase phase, ResourceLocation outputId) implements CustomPacketPayload {
		public static final Type<BakeStatusPacket> TYPE = new Type<>(
				ResourceLocation.fromNamespaceAndPath(CulinaryCompat.MODID, "bake_status"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BakeStatusPacket> STREAM_CODEC = StreamCodec
				.of((buf, msg) -> {
					buf.writeEnum(msg.phase);
					buf.writeResourceLocation(msg.outputId);
				}, buf -> new BakeStatusPacket(buf.readEnum(BakePhase.class), buf.readResourceLocation()));

		@Override
		public Type<BakeStatusPacket> type() {
			return TYPE;
		}

		public static void handle(BakeStatusPacket msg, IPayloadContext context) {
			context.enqueueWork(() -> io.github.smokahs.culinarycompat.compat.cfb.BakeState.Client.onStatus(msg.phase,
					msg.outputId));
		}
	}
}
