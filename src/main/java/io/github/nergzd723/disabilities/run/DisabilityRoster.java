package io.github.nergzd723.disabilities.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

import io.github.nergzd723.disabilities.Disability;

/**
 * Who has which disability, plus the shared countdown that hands out new ones.
 *
 * <p>This class is plain Java on purpose: it holds all of the run's rules (what the timer does in each
 * {@link TimerMode}, which disabilities can be rolled) so they can be unit tested without a server.
 */
public final class DisabilityRoster {
	public static final int TICKS_PER_MINUTE = 60 * 20;
	public static final int DEFAULT_INTERVAL_TICKS = 10 * TICKS_PER_MINUTE;
	public static final int MIN_INTERVAL_TICKS = 20;

	private final Map<UUID, EnumSet<Disability>> active = new HashMap<>();
	private final Map<UUID, Integer> cancerHealthLost = new HashMap<>();
	private int intervalTicks = DEFAULT_INTERVAL_TICKS;
	private int elapsedTicks;
	private boolean timerEnabled = true;
	private TimerMode mode = TimerMode.REPLACE;
	private Runnable changeListener = () -> { };

	/** Called whenever anything other than the per-tick countdown changes. */
	public void setChangeListener(Runnable listener) {
		this.changeListener = listener;
	}

	private void changed() {
		changeListener.run();
	}

	// ---------------------------------------------------------------- queries

	public Set<Disability> get(UUID player) {
		EnumSet<Disability> set = active.get(player);
		return set == null ? Set.of() : Collections.unmodifiableSet(set);
	}

	public boolean has(UUID player, Disability disability) {
		EnumSet<Disability> set = active.get(player);
		return set != null && set.contains(disability);
	}

	public int intervalTicks() {
		return intervalTicks;
	}

	public int elapsedTicks() {
		return elapsedTicks;
	}

	public int ticksUntilNext() {
		return Math.max(0, intervalTicks - elapsedTicks);
	}

	public boolean timerEnabled() {
		return timerEnabled;
	}

	public TimerMode mode() {
		return mode;
	}

	public int cancerHealthLost(UUID player) {
		return cancerHealthLost.getOrDefault(player, 0);
	}

	// ---------------------------------------------------------------- per-player changes

	public boolean give(UUID player, Disability disability) {
		if (!active.computeIfAbsent(player, id -> EnumSet.noneOf(Disability.class)).add(disability)) {
			return false;
		}
		changed();
		return true;
	}

	/** Gives every disability; returns how many were new. */
	public int giveAll(UUID player) {
		EnumSet<Disability> set = active.computeIfAbsent(player, id -> EnumSet.noneOf(Disability.class));
		int before = set.size();
		set.addAll(EnumSet.allOf(Disability.class));
		int added = set.size() - before;
		if (added > 0) {
			changed();
		}
		return added;
	}

	public boolean remove(UUID player, Disability disability) {
		EnumSet<Disability> set = active.get(player);
		if (set == null || !set.remove(disability)) {
			return false;
		}
		if (disability == Disability.CANCER) {
			cancerHealthLost.remove(player);
		}
		changed();
		return true;
	}

	/** Cures everything; returns what was cured. */
	public Set<Disability> removeAll(UUID player) {
		EnumSet<Disability> set = active.get(player);
		if (set == null || set.isEmpty()) {
			return Set.of();
		}
		EnumSet<Disability> cured = EnumSet.copyOf(set);
		set.clear();
		cancerHealthLost.remove(player);
		changed();
		return Collections.unmodifiableSet(cured);
	}

	public Optional<Disability> removeRandom(UUID player, RandomGenerator random) {
		List<Disability> held = new ArrayList<>(get(player));
		if (held.isEmpty()) {
			return Optional.empty();
		}
		Disability cured = held.get(random.nextInt(held.size()));
		remove(player, cured);
		return Optional.of(cured);
	}

	/** Adds one disability the player does not have yet, if there is any left. */
	public Optional<Disability> grantRandom(UUID player, RandomGenerator random) {
		Set<Disability> held = get(player);
		List<Disability> pool = new ArrayList<>();
		for (Disability disability : Disability.values()) {
			if (!held.contains(disability)) {
				pool.add(disability);
			}
		}
		if (pool.isEmpty()) {
			return Optional.empty();
		}
		Disability gained = pool.get(random.nextInt(pool.size()));
		give(player, gained);
		return Optional.of(gained);
	}

	/**
	 * Cures every disability the player has and rolls exactly one new one. The new disability is never one
	 * the player was just cured of, unless they had all of them.
	 */
	public Progression reroll(UUID player, RandomGenerator random) {
		Set<Disability> cured = removeAll(player);
		List<Disability> pool = new ArrayList<>();
		for (Disability disability : Disability.values()) {
			if (!cured.contains(disability)) {
				pool.add(disability);
			}
		}
		if (pool.isEmpty()) {
			pool.addAll(List.of(Disability.values()));
		}
		Disability gained = pool.get(random.nextInt(pool.size()));
		give(player, gained);
		return new Progression(cured, Optional.of(gained));
	}

	/** What the timer does to one player, according to the current {@link #mode()}. */
	public Progression progress(UUID player, RandomGenerator random) {
		return switch (mode) {
			case REPLACE -> reroll(player, random);
			case STACK -> new Progression(Set.of(), grantRandom(player, random));
		};
	}

	public void setCancerHealthLost(UUID player, int lost) {
		if (lost <= 0) {
			if (cancerHealthLost.remove(player) != null) {
				changed();
			}
		} else if (!Integer.valueOf(lost).equals(cancerHealthLost.put(player, lost))) {
			changed();
		}
	}

	// ---------------------------------------------------------------- timer

	/**
	 * Advances the shared countdown by one tick. Returns {@code true} when it ran out, in which case it has
	 * already restarted. Does nothing while the timer is off.
	 */
	public boolean tickTimer() {
		if (!timerEnabled) {
			return false;
		}
		if (++elapsedTicks < intervalTicks) {
			return false;
		}
		elapsedTicks = 0;
		return true;
	}

	/** Sets the countdown length, restarts it and turns the timer on. */
	public void setIntervalTicks(int ticks) {
		intervalTicks = Math.max(MIN_INTERVAL_TICKS, ticks);
		elapsedTicks = 0;
		timerEnabled = true;
		changed();
	}

	public void setTimerEnabled(boolean enabled) {
		if (timerEnabled != enabled) {
			timerEnabled = enabled;
			changed();
		}
	}

	public void setMode(TimerMode mode) {
		if (this.mode != mode) {
			this.mode = mode;
			changed();
		}
	}

	/** Cures everyone and restarts the countdown. Timer settings are kept. */
	public void resetAll() {
		active.clear();
		cancerHealthLost.clear();
		elapsedTicks = 0;
		changed();
	}

	// ---------------------------------------------------------------- persistence

	public List<PlayerRecord> players() {
		List<PlayerRecord> records = new ArrayList<>();
		for (Map.Entry<UUID, EnumSet<Disability>> entry : active.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				records.add(new PlayerRecord(entry.getKey(), EnumSet.copyOf(entry.getValue()), cancerHealthLost(entry.getKey())));
			}
		}
		return records;
	}

	public static DisabilityRoster restore(int intervalTicks, int elapsedTicks, boolean timerEnabled, TimerMode mode, List<PlayerRecord> players) {
		DisabilityRoster roster = new DisabilityRoster();
		roster.intervalTicks = Math.max(MIN_INTERVAL_TICKS, intervalTicks);
		roster.elapsedTicks = Math.clamp(elapsedTicks, 0, roster.intervalTicks);
		roster.timerEnabled = timerEnabled;
		roster.mode = mode;
		for (PlayerRecord record : players) {
			if (!record.disabilities().isEmpty()) {
				roster.active.put(record.player(), EnumSet.copyOf(record.disabilities()));
				if (record.cancerHealthLost() > 0) {
					roster.cancerHealthLost.put(record.player(), record.cancerHealthLost());
				}
			}
		}
		return roster;
	}

	/** One player's saved state. */
	public record PlayerRecord(UUID player, Set<Disability> disabilities, int cancerHealthLost) {
	}

	/**
	 * The outcome of one timer step for a player: what they were cured of (always empty in
	 * {@link TimerMode#STACK}) and what they developed (empty if there was nothing left to give).
	 */
	public record Progression(Set<Disability> cured, Optional<Disability> gained) {
	}
}
