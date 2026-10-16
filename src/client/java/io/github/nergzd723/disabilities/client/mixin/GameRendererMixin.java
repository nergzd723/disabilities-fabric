package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;

import io.github.nergzd723.disabilities.DisabilitiesMod;
import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Unique
	private static final Identifier GRAYSCALE = DisabilitiesMod.id("grayscale");

	/** Color Blind: a black and white post effect on top of whatever else is active. */
	@Inject(method = "update", at = @At("TAIL"))
	private void disabilities$colorBlind(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (ClientState.has(Disability.COLOR_BLIND)) {
			((GameRenderer) (Object) this).getRequestedPostEffects().add(GRAYSCALE);
		}
	}
}
