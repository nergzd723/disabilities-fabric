package io.github.nergzd723.disabilities.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerMixin {
	/** Amputated Leg zeroes your walking speed; don't let that zoom the field of view all the way in. */
	@ModifyExpressionValue(method = "getFieldOfViewModifier", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
	private double disabilities$ignoreMissingLeg(double movementSpeed) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		if (self == Minecraft.getInstance().player && ClientState.has(Disability.AMPUTATED_LEG)) {
			return self.getAbilities().getWalkingSpeed();
		}
		return movementSpeed;
	}
}
