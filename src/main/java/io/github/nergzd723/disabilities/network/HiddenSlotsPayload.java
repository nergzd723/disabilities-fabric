package io.github.nergzd723.disabilities.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import io.github.nergzd723.disabilities.DisabilitiesMod;

/** Dementia: bit {@code i} set means main inventory slot {@code i} (0-35) should look empty. */
public record HiddenSlotsPayload(long mask) implements CustomPacketPayload {
	public static final Type<HiddenSlotsPayload> TYPE = new Type<>(DisabilitiesMod.id("hidden_slots"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HiddenSlotsPayload> CODEC = ByteBufCodecs.LONG
			.<RegistryFriendlyByteBuf>cast()
			.map(HiddenSlotsPayload::new, HiddenSlotsPayload::mask);

	@Override
	public Type<HiddenSlotsPayload> type() {
		return TYPE;
	}
}
