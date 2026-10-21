package io.github.smokahs.culinarycompat.compat.ae2;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// server side

public record StationSettingsPacket(BlockPos pos, Action action, int value) implements CustomPacketPayload {
	public static final Type<StationSettingsPacket> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath("culinarycompat", "station_settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StationSettingsPacket> STREAM_CODEC = StreamCodec
			.of((buf, msg) -> encode(msg, buf), StationSettingsPacket::decode);
	@Override
	public Type<StationSettingsPacket> type() {
		return TYPE;
	}

	public enum Action {
		SET_MODE, SET_PRIORITY, SET_VIEW, OPEN_PRIORITY;

		public static Action fromOrdinal(int o) {
			Action[] values = Action.values();
			return values[Math.max(0, Math.min(values.length - 1, o))];
		}
	}

	public static void encode(StationSettingsPacket msg, FriendlyByteBuf buf) {
		buf.writeBlockPos(msg.pos);
		buf.writeByte(msg.action.ordinal());
		buf.writeInt(msg.value);
	}

	public static StationSettingsPacket decode(FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		Action action = Action.fromOrdinal(buf.readByte());
		int value = buf.readInt();
		return new StationSettingsPacket(pos, action, value);
	}

	public static void handle(StationSettingsPacket msg, IPayloadContext ctx) {
		ctx.enqueueWork(() -> {
			if (!(ctx.player() instanceof ServerPlayer player)) {
				return;
			}
			if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) {
				return;
			}
			BlockEntity be = player.level().getBlockEntity(msg.pos);
			if (!(be instanceof StationBlockEntity station)) {
				return;
			}
			switch (msg.action) {
				case SET_MODE -> {
					Mode[] modes = Mode.values();
					station.setMode(modes[Math.max(0, Math.min(modes.length - 1, msg.value))]);
				}
				case SET_PRIORITY -> station.setPriority(msg.value);
				case SET_VIEW -> {
					ViewMode[] views = ViewMode.values();
					station.setViewMode(views[Math.max(0, Math.min(views.length - 1, msg.value))]);
				}
				case OPEN_PRIORITY -> station.openPriorityMenu(player);
			}
		});
	}
}
