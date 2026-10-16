package io.github.nergzd723.disabilities.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import io.github.nergzd723.disabilities.DisabilitiesMod;
import io.github.nergzd723.disabilities.run.TimerMode;

/**
 * The disabilities of every online player (as {@link io.github.nergzd723.disabilities.Disability} bit masks) and
 * the state of the shared countdown. Other players' disabilities are needed to render missing limbs.
 */
public record SyncStatePayload(Map<UUID, Long> disabilities, int ticksUntilNext, boolean timerEnabled, TimerMode mode)
		implements CustomPacketPayload {
	public static final Type<SyncStatePayload> TYPE = new Type<>(DisabilitiesMod.id("state"));

	private static final StreamCodec<RegistryFriendlyByteBuf, TimerMode> MODE_CODEC = ByteBufCodecs.VAR_INT.map(
			ordinal -> TimerMode.values()[Math.floorMod(ordinal, TimerMode.values().length)],
			TimerMode::ordinal
	).cast();

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncStatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.<RegistryFriendlyByteBuf, UUID, Long, Map<UUID, Long>>map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.VAR_LONG),
			SyncStatePayload::disabilities,
			ByteBufCodecs.VAR_INT, SyncStatePayload::ticksUntilNext,
			ByteBufCodecs.BOOL, SyncStatePayload::timerEnabled,
			MODE_CODEC, SyncStatePayload::mode,
			SyncStatePayload::new
	);

	@Override
	public Type<SyncStatePayload> type() {
		return TYPE;
	}
}
