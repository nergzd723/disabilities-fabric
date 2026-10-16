package io.github.nergzd723.disabilities.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import io.github.nergzd723.disabilities.DisabilitiesMod;

/**
 * Starts (or, with {@code ticks == 0}, stops) a timed episode that the server decides on but the client has to
 * play out: a seizure or a vertigo spell. {@code direction} is the spin direction for vertigo (+1 or -1).
 */
public record EpisodePayload(Kind kind, int ticks, int direction) implements CustomPacketPayload {
	public static final Type<EpisodePayload> TYPE = new Type<>(DisabilitiesMod.id("episode"));

	public enum Kind {
		SEIZURE,
		VERTIGO
	}

	private static final StreamCodec<RegistryFriendlyByteBuf, Kind> KIND_CODEC = ByteBufCodecs.VAR_INT.map(
			ordinal -> Kind.values()[Math.floorMod(ordinal, Kind.values().length)],
			Kind::ordinal
	).cast();

	public static final StreamCodec<RegistryFriendlyByteBuf, EpisodePayload> CODEC = StreamCodec.composite(
			KIND_CODEC, EpisodePayload::kind,
			ByteBufCodecs.VAR_INT, EpisodePayload::ticks,
			ByteBufCodecs.VAR_INT, EpisodePayload::direction,
			EpisodePayload::new
	);

	@Override
	public Type<EpisodePayload> type() {
		return TYPE;
	}
}
