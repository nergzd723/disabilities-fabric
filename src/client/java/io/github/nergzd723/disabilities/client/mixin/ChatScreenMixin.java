package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;

import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {
	private static final String SCREEN_RENDER = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V";
	private static final String SUGGESTIONS_RENDER =
			"Lnet/minecraft/client/gui/components/CommandSuggestions;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V";

	@Unique
	private boolean disabilities$wasExempt;

	/** Dyslexia: you can read what you're typing (and command suggestions), but not the chat history. */
	@Inject(method = "extractRenderState", at = {@At(value = "INVOKE", target = SCREEN_RENDER), @At(value = "INVOKE", target = SUGGESTIONS_RENDER)})
	private void disabilities$readableInput(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		disabilities$wasExempt = ClientState.dyslexiaExempt();
		ClientState.setDyslexiaExempt(true);
	}

	@Inject(method = "extractRenderState", at = {@At(value = "INVOKE", target = SCREEN_RENDER, shift = At.Shift.AFTER),
			@At(value = "INVOKE", target = SUGGESTIONS_RENDER, shift = At.Shift.AFTER)})
	private void disabilities$scrambledAgain(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		ClientState.setDyslexiaExempt(disabilities$wasExempt);
	}
}
