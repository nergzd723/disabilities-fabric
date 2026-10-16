package io.github.nergzd723.disabilities.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.player.LocalPlayer;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
	/** Blind is permanent Blindness for your eyes only: unlike the potion effect, it doesn't stop you sprinting. */
	@ModifyExpressionValue(method = "isSprintingPossible", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isMobilityRestricted()Z"))
	private boolean disabilities$blindCanSprint(boolean restricted) {
		return restricted && !ClientState.has(Disability.BLIND);
	}
}
