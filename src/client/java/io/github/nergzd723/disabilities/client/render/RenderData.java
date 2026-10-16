package io.github.nergzd723.disabilities.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

/** Extra data this mod attaches to entity render states. */
public final class RenderData {
	/** Set on every entity render state: whether the entity is a mob (Prosopagnosia turns those into Steves). */
	public static final RenderStateDataKey<Boolean> IS_MOB = RenderStateDataKey.create(() -> "disabilities:is_mob");
	/** Set on player render states: which limbs are missing. */
	public static final RenderStateDataKey<Amputations> AMPUTATIONS = RenderStateDataKey.create(() -> "disabilities:amputations");

	private RenderData() {
	}

	public record Amputations(boolean offhandArm, boolean leg) {
		public static final Amputations NONE = new Amputations(false, false);
	}
}
