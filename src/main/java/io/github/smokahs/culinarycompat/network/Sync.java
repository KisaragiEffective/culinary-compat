package io.github.smokahs.culinarycompat.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.github.smokahs.culinarycompat.bridges.Bridges;

//sync bridge entries so EMI/JEI can show them on dedicated servers
public record Sync(List<Bridges.Entry> entries) implements CustomPacketPayload {
	public static final Type<Sync> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath("culinarycompat", "bridges"));
	public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec
			.of((buf, msg) -> encode(msg, buf), Sync::decode);

	@Override
	public Type<Sync> type() {
		return TYPE;
	}

	public static void encode(Sync msg, RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(msg.entries.size());
		for (Bridges.Entry e : msg.entries) {
			buf.writeUtf(e.source());
			buf.writeResourceLocation(e.bridgeId());
			buf.writeVarInt(e.inputs().size());
			for (Ingredient ing : e.inputs()) {
				Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
			}
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.output());
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.workstation());
			buf.writeFloat(e.experience());
		}
	}

	public static Sync decode(RegistryFriendlyByteBuf buf) {
		int count = buf.readVarInt();
		List<Bridges.Entry> entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			String source = buf.readUtf();
			ResourceLocation bridgeId = buf.readResourceLocation();
			int inputCount = buf.readVarInt();
			List<Ingredient> inputs = new ArrayList<>(inputCount);
			for (int j = 0; j < inputCount; j++) {
				inputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
			}
			ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
			ItemStack workstation = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
			float experience = buf.readFloat();
			entries.add(new Bridges.Entry(source, bridgeId, inputs, output, workstation, experience));
		}
		return new Sync(entries);
	}

	public static void handle(Sync msg, IPayloadContext ctx) {
		ctx.enqueueWork(() -> applyOnClient(msg.entries));
	}

	// integrated server shares the static map with its client, so only remote
	// clients need the sync
	private static void applyOnClient(List<Bridges.Entry> entries) {
		if (Minecraft.getInstance().hasSingleplayerServer()) {
			return;
		}
		Bridges.replaceAll(entries);
		if (ModList.get().isLoaded("emi")) {
			io.github.smokahs.culinarycompat.compat.emi.Plugin.requestReload();
		} else if (ModList.get().isLoaded("jei")) {
			io.github.smokahs.culinarycompat.compat.jei.Plugin.applyRuntimeSync();
		}
	}
}
