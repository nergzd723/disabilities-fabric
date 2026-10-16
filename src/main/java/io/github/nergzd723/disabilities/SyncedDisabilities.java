package io.github.nergzd723.disabilities;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The client's copy of every online player's disabilities, as last synced by the server.
 *
 * <p>It lives in common code because a few common mixins (e.g. Kleptomania) run on the client too. On a dedicated
 * server it simply stays empty.
 */
public final class SyncedDisabilities {
	private static volatile Map<UUID, Set<Disability>> players = Map.of();

	private SyncedDisabilities() {
	}

	public static boolean has(UUID player, Disability disability) {
		Set<Disability> set = players.get(player);
		return set != null && set.contains(disability);
	}

	public static Set<Disability> get(UUID player) {
		return players.getOrDefault(player, Set.of());
	}

	public static void update(Map<UUID, Long> masks) {
		Map<UUID, Set<Disability>> next = new HashMap<>();
		masks.forEach((player, mask) -> {
			EnumSet<Disability> set = Disability.fromMask(mask);
			if (!set.isEmpty()) {
				next.put(player, Collections.unmodifiableSet(set));
			}
		});
		players = Map.copyOf(next);
	}

	public static void clear() {
		players = Map.of();
	}
}
