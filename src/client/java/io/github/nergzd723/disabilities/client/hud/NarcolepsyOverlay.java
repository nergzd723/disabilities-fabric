package io.github.nergzd723.disabilities.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import io.github.nergzd723.disabilities.client.ClientState;

/** Narcolepsy: the screen fades to black while you're asleep. */
public final class NarcolepsyOverlay {
	private NarcolepsyOverlay() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		float darkness = ClientState.sleepDarkness();
		if (darkness > 0.0F) {
			graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (int) (darkness * 255.0F) << 24);
		}
	}
}
