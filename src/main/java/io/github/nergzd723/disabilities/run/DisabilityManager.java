package io.github.nergzd723.disabilities.run;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.network.ModNetworking;
import io.github.nergzd723.disabilities.network.SyncStatePayload;

/** Server-side owner of the run: loads the roster with the world, drives the timer and keeps clients in sync. */
public final class DisabilityManager {
	private static final int SYNC_INTERVAL_TICKS = 20;
	private static final RandomGenerator RANDOM = new Random();

	private static @Nullable RunSavedData data;
	private static boolean syncPending;
	private static int ticksSinceSync;

	private DisabilityManager() {
	}

	public static void onServerStarted(MinecraftServer server) {
		RunSavedData loaded = server.getDataStorage().computeIfAbsent(RunSavedData.TYPE);
		loaded.roster().setChangeListener(() -> {
			loaded.setDirty();
			syncPending = true;
		});
		data = loaded;
		syncPending = true;
	}

	public static void onServerStopped(MinecraftServer server) {
		data = null;
		syncPending = false;
		ticksSinceSync = 0;
	}

	/** The running server's roster. Only valid while a server is running. */
	public static DisabilityRoster roster() {
		RunSavedData current = data;
		if (current == null) {
			throw new IllegalStateException("No server is running");
		}
		return current.roster();
	}

	public static boolean has(UUID player, Disability disability) {
		RunSavedData current = data;
		return current != null && current.roster().has(player, disability);
	}

	public static Set<Disability> get(UUID player) {
		RunSavedData current = data;
		return current == null ? Set.of() : current.roster().get(player);
	}

	public static RandomGenerator random() {
		return RANDOM;
	}

	public static void requestSync() {
		syncPending = true;
	}

	public static void tick(MinecraftServer server) {
		RunSavedData current = data;
		if (current == null) {
			return;
		}
		DisabilityRoster roster = current.roster();
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		// Like the original mod, the countdown only runs while somebody is online.
		if (!players.isEmpty() && roster.timerEnabled()) {
			boolean fired = roster.tickTimer();
			current.setDirty();
			if (fired) {
				for (ServerPlayer player : List.copyOf(players)) {
					progress(player);
				}
			}
		}

		if (syncPending || ++ticksSinceSync >= SYNC_INTERVAL_TICKS) {
			syncPending = false;
			ticksSinceSync = 0;
			sync(server);
		}
	}

	/**
	 * Runs one timer step for the player: in {@link TimerMode#REPLACE} their disabilities are cured and a new one
	 * is rolled, in {@link TimerMode#STACK} a new one is added. Returns what happened.
	 */
	public static DisabilityRoster.Progression progress(ServerPlayer player) {
		DisabilityRoster.Progression result = roster().progress(player.getUUID(), RANDOM);
		result.gained().ifPresent(gained -> announceProgress(player, result.cured(), gained));
		return result;
	}

	/** Grants a random disability the player doesn't have yet (used by {@code /disability give ... random}). */
	public static Optional<Disability> giveRandom(ServerPlayer player) {
		Optional<Disability> gained = roster().grantRandom(player.getUUID(), RANDOM);
		gained.ifPresent(disability -> announce(player, Component.translatable("message.disabilities.given", disability.displayName())
				.withStyle(disability.color())));
		return gained;
	}

	public static boolean give(ServerPlayer player, Disability disability) {
		if (!roster().give(player.getUUID(), disability)) {
			return false;
		}
		announce(player, Component.translatable("message.disabilities.given", disability.displayName()).withStyle(disability.color()));
		return true;
	}

	private static void announceProgress(ServerPlayer player, Set<Disability> cured, Disability gained) {
		MutableComponent message;
		if (cured.isEmpty()) {
			message = Component.translatable("message.disabilities.developed", gained.displayName());
		} else if (cured.size() == 1) {
			message = Component.translatable("message.disabilities.swapped", cured.iterator().next().displayName(), gained.displayName());
		} else {
			message = Component.translatable("message.disabilities.swapped_many", cured.size(), gained.displayName());
		}
		announce(player, message.withStyle(gained.color()));
	}

	/** Sends a disability chat message, unless the player has Anosognosia and can't know about it. */
	public static void announce(ServerPlayer player, Component message) {
		if (player.connection != null && !has(player.getUUID(), Disability.ANOSOGNOSIA)) {
			player.sendSystemMessage(message);
		}
	}

	private static void sync(MinecraftServer server) {
		DisabilityRoster roster = roster();
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		Map<UUID, Long> masks = new HashMap<>();
		for (ServerPlayer player : players) {
			long mask = Disability.toMask(roster.get(player.getUUID()));
			if (mask != 0L) {
				masks.put(player.getUUID(), mask);
			}
		}
		SyncStatePayload payload = new SyncStatePayload(masks, roster.ticksUntilNext(), roster.timerEnabled(), roster.mode());
		for (ServerPlayer player : players) {
			ModNetworking.send(player, payload);
		}
	}
}
