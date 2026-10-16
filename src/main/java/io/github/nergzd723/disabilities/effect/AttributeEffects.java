package io.github.nergzd723.disabilities.effect;

import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import io.github.nergzd723.disabilities.DisabilitiesMod;
import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.run.DisabilityManager;

/** Short, Amputated Leg, Arthritis, Brittle Bones and Cancer are all plain attribute modifiers. */
final class AttributeEffects {
	private static final Identifier SHORT_SCALE = DisabilitiesMod.id("short_scale");
	private static final Identifier SHORT_BLOCK_REACH = DisabilitiesMod.id("short_block_reach");
	private static final Identifier SHORT_ENTITY_REACH = DisabilitiesMod.id("short_entity_reach");
	private static final Identifier AMPUTATED_LEG = DisabilitiesMod.id("amputated_leg");
	private static final Identifier ARTHRITIS_ATTACK = DisabilitiesMod.id("arthritis_attack_speed");
	private static final Identifier ARTHRITIS_MINING = DisabilitiesMod.id("arthritis_mining_speed");
	private static final Identifier BRITTLE_FALL_DAMAGE = DisabilitiesMod.id("brittle_bones_fall_damage");
	private static final Identifier BRITTLE_SAFE_FALL = DisabilitiesMod.id("brittle_bones_safe_fall");
	private static final Identifier CANCER = DisabilitiesMod.id("cancer");

	/** Cancer takes half a heart roughly every 2 minutes... */
	private static final int CANCER_CHANCE = 2700;
	/** ...but always leaves at least 3 hearts. */
	private static final int CANCER_MAX_LOSS = 14;

	private AttributeEffects() {
	}

	static void tick(ServerPlayer player, RandomSource random) {
		UUID id = player.getUUID();

		// Short: half a block shorter (1.8 -> 1.5 blocks) and half a block less reach.
		boolean shorter = DisabilityManager.has(id, Disability.SHORT);
		toggle(player, Attributes.SCALE, SHORT_SCALE, shorter, -1.0 / 6.0, AttributeModifier.Operation.ADD_VALUE);
		toggle(player, Attributes.BLOCK_INTERACTION_RANGE, SHORT_BLOCK_REACH, shorter, -0.5, AttributeModifier.Operation.ADD_VALUE);
		toggle(player, Attributes.ENTITY_INTERACTION_RANGE, SHORT_ENTITY_REACH, shorter, -0.5, AttributeModifier.Operation.ADD_VALUE);

		// Amputated Leg: no walking speed at all, so the only way to get around is jumping (air control still works).
		toggle(player, Attributes.MOVEMENT_SPEED, AMPUTATED_LEG, DisabilityManager.has(id, Disability.AMPUTATED_LEG),
				-1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

		// Arthritis: 40% slower attacks and mining.
		boolean arthritis = DisabilityManager.has(id, Disability.ARTHRITIS);
		toggle(player, Attributes.ATTACK_SPEED, ARTHRITIS_ATTACK, arthritis, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		toggle(player, Attributes.BLOCK_BREAK_SPEED, ARTHRITIS_MINING, arthritis, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

		// Brittle Bones: double fall damage, and it starts after 1.5 blocks instead of 3.
		boolean brittle = DisabilityManager.has(id, Disability.BRITTLE_BONES);
		toggle(player, Attributes.FALL_DAMAGE_MULTIPLIER, BRITTLE_FALL_DAMAGE, brittle, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		toggle(player, Attributes.SAFE_FALL_DISTANCE, BRITTLE_SAFE_FALL, brittle, -1.5, AttributeModifier.Operation.ADD_VALUE);

		tickCancer(player, random);
	}

	private static void tickCancer(ServerPlayer player, RandomSource random) {
		UUID id = player.getUUID();
		boolean cancer = DisabilityManager.has(id, Disability.CANCER);
		int lost = cancer ? DisabilityManager.roster().cancerHealthLost(id) : 0;
		if (cancer && lost < CANCER_MAX_LOSS && random.nextInt(CANCER_CHANCE) == 0) {
			lost++;
			DisabilityManager.roster().setCancerHealthLost(id, lost);
		}

		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		double amount = -lost;
		AttributeModifier current = maxHealth.getModifier(CANCER);
		if (current == null ? amount == 0.0 : current.amount() == amount) {
			return;
		}
		maxHealth.removeModifier(CANCER);
		if (amount != 0.0) {
			maxHealth.addTransientModifier(new AttributeModifier(CANCER, amount, AttributeModifier.Operation.ADD_VALUE));
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	private static void toggle(ServerPlayer player, Holder<Attribute> attribute, Identifier id, boolean wanted, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		boolean present = instance.hasModifier(id);
		if (wanted && !present) {
			instance.addTransientModifier(new AttributeModifier(id, amount, operation));
		} else if (!wanted && present) {
			instance.removeModifier(id);
		}
	}
}
