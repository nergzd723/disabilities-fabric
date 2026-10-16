package io.github.nergzd723.disabilities.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.ModDamageTypes;
import io.github.nergzd723.disabilities.run.DisabilityManager;

/**
 * Asthma: sprinting drains your breath (a full bar lasts 10 seconds) and it only refills while you aren't
 * sprinting. Out of breath means a heart of damage every second, just like drowning.
 */
final class Asthma {
	private static final double SECONDS_TO_EMPTY = 10.0;
	private static final int DAMAGE_INTERVAL_TICKS = 20;
	private static final float DAMAGE = 2.0F;

	private Asthma() {
	}

	static void tick(ServerPlayer player, ServerLevel level, PlayerEffectState state) {
		if (!DisabilityManager.has(player.getUUID(), Disability.ASTHMA)) {
			state.lungAir = -1.0;
			state.asthmaDamageTimer = 0;
			return;
		}
		if (player.isDeadOrDying() || player.isSpectator() || player.isCreative()) {
			return;
		}

		int maxAir = player.getMaxAirSupply();
		if (player.isUnderWater()) {
			// Vanilla drowning takes over; remember how much air is left for when we surface.
			state.lungAir = player.getAirSupply();
			state.asthmaDamageTimer = 0;
			return;
		}
		if (state.lungAir < 0.0) {
			state.lungAir = maxAir;
		}

		double perTick = maxAir / (SECONDS_TO_EMPTY * 20.0);
		state.lungAir = Math.clamp(state.lungAir + (player.isSprinting() ? -perTick : perTick), 0.0, maxAir);
		player.setAirSupply((int) Math.round(state.lungAir));

		if (state.lungAir > 0.0) {
			state.asthmaDamageTimer = 0;
		} else if (++state.asthmaDamageTimer >= DAMAGE_INTERVAL_TICKS) {
			state.asthmaDamageTimer = 0;
			player.hurtServer(level, ModDamageTypes.source(level, ModDamageTypes.ASTHMA), DAMAGE);
		}
	}
}
