package io.github.nergzd723.disabilities;

import java.util.Locale;

public final class TimeFormat {
	private TimeFormat() {
	}

	/** Formats a number of ticks as {@code m:ss}. */
	public static String clock(int ticks) {
		int seconds = Math.max(0, ticks) / 20;
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}
}
