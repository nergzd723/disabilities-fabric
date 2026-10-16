package io.github.nergzd723.disabilities.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.network.chat.Style;

import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(Style.class)
abstract class StyleMixin {
	/** Dyslexia: all text renders scrambled, except the bits that are explicitly exempt. */
	@ModifyReturnValue(method = "isObfuscated", at = @At("RETURN"))
	private boolean disabilities$dyslexia(boolean obfuscated) {
		return obfuscated || ClientState.isDyslexic();
	}
}
