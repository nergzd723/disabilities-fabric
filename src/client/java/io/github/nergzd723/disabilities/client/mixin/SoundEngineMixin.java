package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
	/** Deaf: everything is silent, except "voices" (which is where Schizophrenia's hallucinations play). */
	@Inject(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F", at = @At("HEAD"), cancellable = true)
	private void disabilities$deaf(float volume, SoundSource source, CallbackInfoReturnable<Float> cir) {
		if (source != SoundSource.VOICE && ClientState.has(Disability.DEAF)) {
			cir.setReturnValue(0.0F);
		}
	}
}
