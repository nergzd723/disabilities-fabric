package io.github.nergzd723.disabilities.run;

import java.util.Locale;
import java.util.Optional;

/** What happens to a player's disabilities when the run timer fires (or {@code /disability next} is used). */
public enum TimerMode {
	/** Every current disability is cured and exactly one new disability is rolled in its place. */
	REPLACE,
	/** A new disability is added on top of the ones the player already has (the original behaviour). */
	STACK;

	private final String id = name().toLowerCase(Locale.ROOT);

	public String id() {
		return id;
	}

	public static Optional<TimerMode> byId(String id) {
		for (TimerMode mode : values()) {
			if (mode.id.equalsIgnoreCase(id)) {
				return Optional.of(mode);
			}
		}
		return Optional.empty();
	}
}
