package io.github.nergzd723.disabilities.effect;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.bee.Bee;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.ModTags;
import io.github.nergzd723.disabilities.network.EpisodePayload;
import io.github.nergzd723.disabilities.network.ModNetworking;
import io.github.nergzd723.disabilities.run.DisabilityManager;

/** Disabilities that boil down to vanilla status effects: Blind, Pollen Allergy and Vertigo. */
final class StatusEffects {
	private static final int POLLEN_CHECK_INTERVAL = 10;
	private static final int POLLEN_RANGE = 2;
	private static final int VERTIGO_CHANCE = 1500;
	private static final int VERTIGO_TICKS = 160;

	private StatusEffects() {
	}

	static void tick(ServerPlayer player, ServerLevel level, PlayerEffectState state, RandomSource random) {
		UUID id = player.getUUID();
		if (DisabilityManager.has(id, Disability.BLIND)) {
			// Hidden and icon-less, so it never counts as a "negative effect" for Seizures.
			player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false));
		}
		tickPollenAllergy(player, level, state);
		tickVertigo(player, state, random);
	}

	private static void tickPollenAllergy(ServerPlayer player, ServerLevel level, PlayerEffectState state) {
		if (!DisabilityManager.has(player.getUUID(), Disability.POLLEN_ALLERGY) || ++state.pollenTimer < POLLEN_CHECK_INTERVAL) {
			return;
		}
		state.pollenTimer = 0;
		if (nearPollen(player, level)) {
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
		}
	}

	private static boolean nearPollen(ServerPlayer player, ServerLevel level) {
		BlockPos center = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-POLLEN_RANGE, -POLLEN_RANGE, -POLLEN_RANGE),
				center.offset(POLLEN_RANGE, POLLEN_RANGE, POLLEN_RANGE))) {
			if (level.getBlockState(pos).is(ModTags.POLLEN_SOURCES)) {
				return true;
			}
		}
		return !level.getEntitiesOfClass(Bee.class, player.getBoundingBox().inflate(POLLEN_RANGE), Bee::hasNectar).isEmpty();
	}

	private static void tickVertigo(ServerPlayer player, PlayerEffectState state, RandomSource random) {
		if (!DisabilityManager.has(player.getUUID(), Disability.VERTIGO)) {
			if (state.vertigoTicks > 0) {
				state.vertigoTicks = 0;
				ModNetworking.send(player, new EpisodePayload(EpisodePayload.Kind.VERTIGO, 0, 1));
			}
			return;
		}
		if (state.vertigoTicks > 0) {
			state.vertigoTicks--;
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
		} else if (random.nextInt(VERTIGO_CHANCE) == 0) {
			state.vertigoTicks = VERTIGO_TICKS;
			ModNetworking.send(player, new EpisodePayload(EpisodePayload.Kind.VERTIGO, VERTIGO_TICKS, random.nextBoolean() ? 1 : -1));
		}
	}
}
