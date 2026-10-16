package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;

import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(Screen.class)
abstract class ScreenMixin {
	@Unique
	private boolean disabilities$wasExempt;

	/** Dyslexia doesn't apply to the pause menu and settings, so you can still play the game. */
	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
	private void disabilities$readableMenus(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		disabilities$wasExempt = ClientState.dyslexiaExempt();
		if (disabilities$isMenu()) {
			ClientState.setDyslexiaExempt(true);
		}
	}

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("RETURN"))
	private void disabilities$scrambledAgain(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		ClientState.setDyslexiaExempt(disabilities$wasExempt);
	}

	@Unique
	private boolean disabilities$isMenu() {
		Object self = this;
		return self instanceof PauseScreen
				|| self instanceof OptionsScreen
				|| self instanceof OptionsSubScreen
				|| self instanceof PackSelectionScreen
				|| self instanceof CreditsAndAttributionScreen
				|| self instanceof TelemetryInfoScreen;
	}
}
