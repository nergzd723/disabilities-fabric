package io.github.nergzd723.disabilities.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import io.github.nergzd723.disabilities.client.ClientState;

/**
 * Parkinson's: draws the vanilla crosshair where the shaky aim actually points. Picking uses the same offset (see
 * {@code MinecraftMixin}), so whatever is under this crosshair is what you hit.
 */
public final class TremorCrosshair implements HudElement {
	private final HudElement vanilla;

	public TremorCrosshair(HudElement vanilla) {
		this.vanilla = vanilla;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!ClientState.isTrembling() || !minecraft.options.getCameraType().isFirstPerson()) {
			vanilla.extractRenderState(graphics, deltaTracker);
			return;
		}
		float fov = minecraft.gameRenderer.mainCamera().getFov();
		if (fov <= 0.0F) {
			fov = minecraft.options.fov().get();
		}
		double focal = graphics.guiHeight() / 2.0 / Math.tan(Math.toRadians(fov) / 2.0);
		float dx = (float) (Math.tan(Math.toRadians(ClientState.tremorYaw())) * focal);
		float dy = (float) (Math.tan(Math.toRadians(ClientState.tremorPitch())) * focal);
		graphics.pose().pushMatrix();
		graphics.pose().translate(Math.round(dx), Math.round(dy));
		try {
			vanilla.extractRenderState(graphics, deltaTracker);
		} finally {
			graphics.pose().popMatrix();
		}
	}
}
