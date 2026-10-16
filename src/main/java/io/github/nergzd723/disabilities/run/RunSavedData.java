package io.github.nergzd723.disabilities.run;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import io.github.nergzd723.disabilities.DisabilitiesMod;
import io.github.nergzd723.disabilities.Disability;

/** Stores the {@link DisabilityRoster} in the world's {@code data/disabilities/run.dat}. */
public final class RunSavedData extends SavedData {
	/** Unknown names (e.g. from a newer version of the mod) are skipped instead of failing the whole load. */
	private static final Codec<Set<Disability>> DISABILITIES_CODEC = Codec.STRING.listOf().xmap(
			names -> {
				EnumSet<Disability> set = EnumSet.noneOf(Disability.class);
				names.forEach(name -> Disability.byId(name).ifPresent(set::add));
				return set;
			},
			set -> set.stream().map(Disability::id).toList()
	);

	private static final Codec<TimerMode> MODE_CODEC = Codec.STRING.xmap(
			id -> TimerMode.byId(id).orElse(TimerMode.REPLACE),
			TimerMode::id
	);

	private static final Codec<DisabilityRoster.PlayerRecord> PLAYER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("uuid").forGetter(DisabilityRoster.PlayerRecord::player),
			DISABILITIES_CODEC.optionalFieldOf("disabilities", Set.of()).forGetter(DisabilityRoster.PlayerRecord::disabilities),
			Codec.INT.optionalFieldOf("cancer_health_lost", 0).forGetter(DisabilityRoster.PlayerRecord::cancerHealthLost)
	).apply(instance, DisabilityRoster.PlayerRecord::new));

	public static final Codec<RunSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("interval_ticks", DisabilityRoster.DEFAULT_INTERVAL_TICKS).forGetter(data -> data.roster.intervalTicks()),
			Codec.INT.optionalFieldOf("elapsed_ticks", 0).forGetter(data -> data.roster.elapsedTicks()),
			Codec.BOOL.optionalFieldOf("timer_enabled", true).forGetter(data -> data.roster.timerEnabled()),
			MODE_CODEC.optionalFieldOf("timer_mode", TimerMode.REPLACE).forGetter(data -> data.roster.mode()),
			PLAYER_CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(data -> data.roster.players())
	).apply(instance, (interval, elapsed, enabled, mode, players) ->
			new RunSavedData(DisabilityRoster.restore(interval, elapsed, enabled, mode, players))));

	// No vanilla data fixer applies to mod data; Fabric API accepts a null fixer type.
	public static final SavedDataType<RunSavedData> TYPE = new SavedDataType<>(
			DisabilitiesMod.id("run"), RunSavedData::new, CODEC, null
	);

	private final DisabilityRoster roster;

	public RunSavedData() {
		this(new DisabilityRoster());
	}

	private RunSavedData(DisabilityRoster roster) {
		this.roster = roster;
	}

	public DisabilityRoster roster() {
		return roster;
	}
}
