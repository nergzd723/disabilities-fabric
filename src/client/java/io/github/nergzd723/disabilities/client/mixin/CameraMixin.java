package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.Camera;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(Camera.class)
abstract class CameraMixin {
	/** Scoliosis tilts the view permanently; Vertigo rocks it back and forth during a spell. */
	@ModifyArg(method = "setRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"), index = 2)
	private float disabilities$roll(float roll) {
		float degrees = 0.0F;
		if (ClientState.has(Disability.SCOLIOSIS)) {
			degrees += 7.0F;
		}
		int vertigo = ClientState.vertigoTicks();
		if (vertigo > 0) {
			degrees += (float) (Math.sin(vertigo * 0.08) * 35.0);
		}
		return degrees == 0.0F ? roll : roll + (float) Math.toRadians(degrees);
	}
}
