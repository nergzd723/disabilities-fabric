package io.github.nergzd723.disabilities.effect;

import java.util.List;
import java.util.UUID;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.ModDamageTypes;
import io.github.nergzd723.disabilities.ModTags;
import io.github.nergzd723.disabilities.run.DisabilityManager;

/** Diabetes and Lactose Intolerance react to what the player just ate or drank. */
final class Consumption {
	static final float HIGH_SUGAR_DAMAGE = 20.0F;
	static final float MODERATE_SUGAR_DAMAGE = 10.0F;
	static final float LOW_SUGAR_DAMAGE = 4.0F;
	static final float TRACE_SUGAR_DAMAGE = 1.0F;

	private Consumption() {
	}

	static void tick(ServerPlayer player, ServerLevel level, PlayerEffectState state) {
		// Cake is eaten straight off the block, so it never goes through the item consumption hook.
		int cakeSlices = player.getStats().getValue(Stats.CUSTOM, Stats.EAT_CAKE_SLICE);
		boolean ateCake = state.lastCakeSlices >= 0 && cakeSlices > state.lastCakeSlices;
		state.lastCakeSlices = cakeSlices;

		if (state.consumed.isEmpty() && !ateCake) {
			return;
		}
		List<ItemStack> consumed = List.copyOf(state.consumed);
		state.consumed.clear();

		UUID id = player.getUUID();
		boolean diabetes = DisabilityManager.has(id, Disability.DIABETES);
		boolean lactose = DisabilityManager.has(id, Disability.LACTOSE_INTOLERANCE);

		for (ItemStack stack : consumed) {
			if (diabetes) {
				hurt(player, level, ModDamageTypes.DIABETES, sugarDamage(stack));
			}
			if (lactose && stack.is(ModTags.CONTAINS_MILK)) {
				hurt(player, level, ModDamageTypes.LACTOSE, Float.MAX_VALUE);
			}
		}
		if (ateCake) {
			if (diabetes) {
				hurt(player, level, ModDamageTypes.DIABETES, HIGH_SUGAR_DAMAGE);
			}
			if (lactose) {
				hurt(player, level, ModDamageTypes.LACTOSE, Float.MAX_VALUE);
			}
		}
	}

	static float sugarDamage(ItemStack stack) {
		if (stack.is(ModTags.SUGAR_HIGH)) {
			return HIGH_SUGAR_DAMAGE;
		}
		if (stack.is(ModTags.SUGAR_MODERATE)) {
			return MODERATE_SUGAR_DAMAGE;
		}
		if (stack.is(ModTags.SUGAR_LOW)) {
			return LOW_SUGAR_DAMAGE;
		}
		if (stack.is(ModTags.SUGAR_TRACE)) {
			return TRACE_SUGAR_DAMAGE;
		}
		return 0.0F;
	}

	private static void hurt(ServerPlayer player, ServerLevel level, ResourceKey<DamageType> type, float damage) {
		if (damage > 0.0F && !player.isDeadOrDying()) {
			player.hurtServer(level, ModDamageTypes.source(level, type), damage);
		}
	}
}
