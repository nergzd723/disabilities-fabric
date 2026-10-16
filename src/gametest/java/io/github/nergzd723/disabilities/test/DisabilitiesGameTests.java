package io.github.nergzd723.disabilities.test;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.spongepowered.asm.mixin.MixinEnvironment;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.ModDamageTypes;
import io.github.nergzd723.disabilities.ModTags;
import io.github.nergzd723.disabilities.effect.ServerEffects;
import io.github.nergzd723.disabilities.run.DisabilityManager;
import io.github.nergzd723.disabilities.run.DisabilityRoster;
import io.github.nergzd723.disabilities.run.TimerMode;

public class DisabilitiesGameTests {
	@GameTest
	public void everyMixinApplies(GameTestHelper helper) {
		MixinEnvironment.getCurrentEnvironment().audit();
		helper.succeed();
	}

	@GameTest
	public void damageTypesAndTagsAreLoaded(GameTestHelper helper) {
		Registry<DamageType> damageTypes = helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
		for (ResourceKey<DamageType> key : List.of(ModDamageTypes.ASTHMA, ModDamageTypes.DIABETES, ModDamageTypes.LACTOSE, ModDamageTypes.OCD,
				ModDamageTypes.OCD_OVERDID, ModDamageTypes.SEIZURE)) {
			helper.assertTrue(damageTypes.get(key).isPresent(), "missing damage type " + key.identifier());
		}
		helper.assertTrue(damageTypes.getOrThrow(ModDamageTypes.LACTOSE).is(DamageTypeTags.BYPASSES_INVULNERABILITY), "dairy must kill even with a totem");
		helper.assertTrue(damageTypes.getOrThrow(ModDamageTypes.ASTHMA).is(DamageTypeTags.BYPASSES_ARMOR), "armor must not help against asthma");
		helper.assertTrue(new ItemStack(Items.COOKIE).is(ModTags.SUGAR_HIGH), "cookies are very sugary");
		helper.assertTrue(new ItemStack(Items.BREAD).is(ModTags.SUGAR_LOW), "bread has a little sugar");
		helper.assertTrue(new ItemStack(Items.MILK_BUCKET).is(ModTags.CONTAINS_MILK), "milk contains milk");
		helper.assertTrue(Blocks.POPPY.defaultBlockState().is(ModTags.POLLEN_SOURCES), "flowers have pollen");
		helper.assertTrue(Blocks.GRAVEL.defaultBlockState().is(ModTags.OCD_MINEABLE), "gravel can be an OCD target");
		helper.succeed();
	}

	@GameTest
	public void timerCuresEverythingAndRollsOneNewDisability(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		UUID id = player.getUUID();
		DisabilityRoster roster = DisabilityManager.roster();
		TimerMode previousMode = roster.mode();
		try {
			roster.setMode(TimerMode.REPLACE);
			roster.give(id, Disability.DEAF);
			roster.give(id, Disability.MUTE);
			roster.give(id, Disability.ADHD);

			DisabilityRoster.Progression result = DisabilityManager.progress(player);

			helper.assertValueEqual(result.cured(), EnumSet.of(Disability.DEAF, Disability.MUTE, Disability.ADHD), "cured disabilities");
			helper.assertValueEqual(roster.get(id).size(), 1, "disabilities left after the timer");
			helper.assertFalse(result.cured().contains(result.gained().orElseThrow()), "re-rolled a disability that was just cured");
		} finally {
			roster.removeAll(id);
			roster.setMode(previousMode);
		}
		helper.succeed();
	}

	@GameTest
	public void stackModeKeepsTheOldOnes(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		UUID id = player.getUUID();
		DisabilityRoster roster = DisabilityManager.roster();
		TimerMode previousMode = roster.mode();
		try {
			roster.setMode(TimerMode.STACK);
			roster.give(id, Disability.DEAF);

			DisabilityManager.progress(player);

			helper.assertValueEqual(roster.get(id).size(), 2, "disabilities after the timer");
			helper.assertTrue(roster.has(id, Disability.DEAF), "kept the old disability");
		} finally {
			roster.removeAll(id);
			roster.setMode(previousMode);
		}
		helper.succeed();
	}

	@GameTest
	public void attributeDisabilitiesComeAndGo(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		UUID id = player.getUUID();
		DisabilityRoster roster = DisabilityManager.roster();
		double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
		try {
			for (Disability disability : List.of(Disability.SHORT, Disability.AMPUTATED_LEG, Disability.ARTHRITIS, Disability.BRITTLE_BONES,
					Disability.CANCER)) {
				roster.give(id, disability);
			}
			// The maximum, so the random tick can't take another half heart mid-test.
			roster.setCancerHealthLost(id, 14);
			ServerEffects.tickPlayer(player);

			helper.assertValueEqual(player.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.0, "walking speed without a leg");
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - 5.0 / 6.0) < 1.0E-6, "short players are 1.5 blocks tall");
			helper.assertValueEqual(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 2.0, "brittle bones fall damage");
			helper.assertValueEqual(player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE), 1.5, "brittle bones safe fall distance");
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - attackSpeed * 0.6) < 1.0E-6, "arthritis attack speed");
			helper.assertValueEqual(player.getMaxHealth(), 6.0F, "cancer never leaves less than three hearts");

			roster.removeAll(id);
			ServerEffects.tickPlayer(player);

			helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0, "walking speed restored");
			helper.assertValueEqual(player.getAttributeValue(Attributes.SCALE), 1.0, "normal height restored");
			helper.assertValueEqual(player.getMaxHealth(), 20.0F, "max health restored");
		} finally {
			roster.removeAll(id);
			ServerEffects.forget(id);
		}
		helper.succeed();
	}
}
