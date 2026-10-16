package io.github.nergzd723.disabilities.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import io.github.nergzd723.disabilities.client.ClientState;

/** The current OCD compulsion, shown just above the hotbar. */
public final class OcdHud {
	private OcdHud() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		Component line = ClientState.ocdLine();
		if (line == null || minecraft.player == null || minecraft.gui.hud.isHidden()) {
			return;
		}
		boolean wasExempt = ClientState.dyslexiaExempt();
		ClientState.setDyslexiaExempt(true);
		try {
			graphics.centeredText(minecraft.font, line, graphics.guiWidth() / 2, graphics.guiHeight() - 68, 0xFFFFFFFF);
		} finally {
			ClientState.setDyslexiaExempt(wasExempt);
		}
	}
}
