package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(OptionInstance.class)
abstract class OptionInstanceMixin {
	/** Vertigo: you can't dodge the nausea by turning down "Distortion Effects". */
	@Inject(method = "get", at = @At("HEAD"), cancellable = true)
	private void disabilities$fullDistortion(CallbackInfoReturnable<Object> cir) {
		if (!ClientState.has(Disability.VERTIGO)) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null && minecraft.options != null && (Object) this == minecraft.options.screenEffectScale()) {
			cir.setReturnValue(1.0);
		}
	}
}
