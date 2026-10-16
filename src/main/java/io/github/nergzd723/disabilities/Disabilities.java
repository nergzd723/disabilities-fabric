package io.github.nergzd723.disabilities;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import io.github.nergzd723.disabilities.run.DisabilityManager;

/** Side-agnostic lookup: server players are checked against the run, client players against the last sync. */
public final class Disabilities {
	private Disabilities() {
	}

	public static boolean has(@Nullable Player player, Disability disability) {
		if (player == null) {
			return false;
		}
		if (player instanceof ServerPlayer) {
			return DisabilityManager.has(player.getUUID(), disability);
		}
		return SyncedDisabilities.has(player.getUUID(), disability);
	}
}
