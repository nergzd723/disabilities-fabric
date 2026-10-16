package io.github.nergzd723.disabilities.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.ModDamageTypes;
import io.github.nergzd723.disabilities.network.EpisodePayload;
import io.github.nergzd723.disabilities.network.ModNetworking;
import io.github.nergzd723.disabilities.run.DisabilityManager;

/**
 * Seizures: big hits (more than two hearts) have a 50% chance to trigger one, and being hungry or having a
 * negative status effect gives a small chance every tick. A seizure lasts 4 seconds, freezes your movement, shakes
 * your view and hurts you every 12 ticks.
 */
final class Seizures {
	private static final int DURATION_TICKS = 80;
	private static final int DAMAGE_INTERVAL_TICKS = 12;
	private static final float DAMAGE = 2.0F;
	private static final float TRIGGERING_HIT = 4.0F;
	private static final int PASSIVE_CHANCE = 200;
	private static final int HUNGRY_BELOW = 10;
	private static final int RESPAWN_GRACE_TICKS = 100;

	private Seizures() {
	}

	static void tick(ServerPlayer player, ServerLevel level, PlayerEffectState state, RandomSource random) {
		if (player.isDeadOrDying()) {
			if (state.seizing()) {
				stop(player, state);
			}
			state.seizureRespawnGuard = RESPAWN_GRACE_TICKS;
			return;
		}
		if (state.seizureRespawnGuard > 0) {
			state.seizureRespawnGuard--;
			if (state.seizing()) {
				stop(player, state);
			}
			return;
		}
		if (!DisabilityManager.has(player.getUUID(), Disability.SEIZURES)) {
			if (state.seizing()) {
				stop(player, state);
			}
			return;
		}
		if (state.seizing()) {
			if (state.seizureTicks % DAMAGE_INTERVAL_TICKS == 0) {
				player.hurtServer(level, ModDamageTypes.source(level, ModDamageTypes.SEIZURE), DAMAGE);
			}
			state.seizureTicks--;
			return;
		}
		boolean hungry = player.getFoodData().getFoodLevel() < HUNGRY_BELOW;
		if ((hungry || hasNegativeEffect(player)) && random.nextInt(PASSIVE_CHANCE) == 0) {
			start(player, state);
		}
	}

	static void afterDamage(ServerPlayer player, PlayerEffectState state, float damageTaken, RandomSource random) {
		if (!DisabilityManager.has(player.getUUID(), Disability.SEIZURES) || state.seizing() || state.seizureRespawnGuard > 0
				|| player.isDeadOrDying()) {
			return;
		}
		if (damageTaken > TRIGGERING_HIT && random.nextBoolean()) {
			start(player, state);
		}
	}

	private static boolean hasNegativeEffect(ServerPlayer player) {
		for (MobEffectInstance effect : player.getActiveEffects()) {
			if (effect.showIcon() && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				return true;
			}
		}
		return false;
	}

	private static void start(ServerPlayer player, PlayerEffectState state) {
		state.seizureTicks = DURATION_TICKS;
		ModNetworking.send(player, new EpisodePayload(EpisodePayload.Kind.SEIZURE, DURATION_TICKS, 0));
	}

	private static void stop(ServerPlayer player, PlayerEffectState state) {
		state.seizureTicks = 0;
		ModNetworking.send(player, new EpisodePayload(EpisodePayload.Kind.SEIZURE, 0, 0));
	}
}
