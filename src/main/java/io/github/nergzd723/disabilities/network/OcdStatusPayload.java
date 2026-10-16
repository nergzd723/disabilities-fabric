package io.github.nergzd723.disabilities.network;

import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import io.github.nergzd723.disabilities.DisabilitiesMod;

/** The OCD compulsion line shown above the hotbar, or empty to hide it. */
public record OcdStatusPayload(Optional<Component> line) implements CustomPacketPayload {
	public static final Type<OcdStatusPayload> TYPE = new Type<>(DisabilitiesMod.id("ocd_status"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OcdStatusPayload> CODEC = ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC
			.map(OcdStatusPayload::new, OcdStatusPayload::line);

	@Override
	public Type<OcdStatusPayload> type() {
		return TYPE;
	}
}
