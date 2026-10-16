package io.github.nergzd723.disabilities;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Every disability a player can develop. The declaration order is part of the network format
 * (disabilities are synced as a bit mask), so new entries must only ever be appended.
 */
public enum Disability {
	BLIND(ChatFormatting.DARK_GRAY),
	COLOR_BLIND(ChatFormatting.GRAY),
	DEAF(ChatFormatting.AQUA),
	DIABETES(ChatFormatting.LIGHT_PURPLE),
	SCHIZOPHRENIA(ChatFormatting.DARK_PURPLE),
	MUTE(ChatFormatting.WHITE),
	SEIZURES(ChatFormatting.YELLOW),
	CANCER(ChatFormatting.DARK_GREEN),
	OCD(ChatFormatting.BLUE),
	AMPUTATED_OFFHAND(ChatFormatting.GOLD),
	AMPUTATED_LEG(ChatFormatting.RED),
	DYSLEXIA(ChatFormatting.GREEN),
	SHORT(ChatFormatting.DARK_AQUA),
	DEMENTIA(ChatFormatting.DARK_BLUE),
	ASTHMA(ChatFormatting.DARK_RED),
	CEREBRAL_PALSY(ChatFormatting.GOLD),
	NARCOLEPSY(ChatFormatting.YELLOW),
	PARKINSONS(ChatFormatting.AQUA),
	POLLEN_ALLERGY(ChatFormatting.GREEN),
	INSOMNIA(ChatFormatting.DARK_GRAY),
	ARTHRITIS(ChatFormatting.GRAY),
	BRITTLE_BONES(ChatFormatting.WHITE),
	ADHD(ChatFormatting.YELLOW),
	TOURETTES(ChatFormatting.LIGHT_PURPLE),
	VERTIGO(ChatFormatting.DARK_PURPLE),
	PROSOPAGNOSIA(ChatFormatting.GOLD),
	SCOLIOSIS(ChatFormatting.DARK_AQUA),
	KLEPTOMANIA(ChatFormatting.RED),
	LACTOSE_INTOLERANCE(ChatFormatting.WHITE),
	ANOSOGNOSIA(ChatFormatting.GRAY);

	private static final Disability[] VALUES = values();

	private final String id;
	private final ChatFormatting color;

	Disability(ChatFormatting color) {
		this.id = name().toLowerCase(Locale.ROOT);
		this.color = color;
	}

	/** The lowercase identifier used in commands, save data and translation keys. */
	public String id() {
		return id;
	}

	public ChatFormatting color() {
		return color;
	}

	public String translationKey() {
		return "disability." + DisabilitiesMod.MOD_ID + "." + id;
	}

	public MutableComponent displayName() {
		return Component.translatable(translationKey()).withStyle(color);
	}

	public MutableComponent description() {
		return Component.translatable(translationKey() + ".desc");
	}

	public static Optional<Disability> byId(String id) {
		for (Disability disability : VALUES) {
			if (disability.id.equalsIgnoreCase(id)) {
				return Optional.of(disability);
			}
		}
		return Optional.empty();
	}

	public static long toMask(Set<Disability> disabilities) {
		long mask = 0L;
		for (Disability disability : disabilities) {
			mask |= 1L << disability.ordinal();
		}
		return mask;
	}

	public static EnumSet<Disability> fromMask(long mask) {
		EnumSet<Disability> set = EnumSet.noneOf(Disability.class);
		for (Disability disability : VALUES) {
			if ((mask & (1L << disability.ordinal())) != 0L) {
				set.add(disability);
			}
		}
		return set;
	}
}
