package io.github.nergzd723.disabilities.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(SyncStatePayload.TYPE, SyncStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EpisodePayload.TYPE, EpisodePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OcdStatusPayload.TYPE, OcdStatusPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HiddenSlotsPayload.TYPE, HiddenSlotsPayload.CODEC);
	}

	/** Sends a payload unless the player can't receive it (fake players from other mods, test players...). */
	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		if (player.connection != null && ServerPlayNetworking.canSend(player, payload.type())) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
