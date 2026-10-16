package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void disabilities$involuntaryInput(CallbackInfo ci) {
		// Narcolepsy: asleep, so nothing you press does anything.
		if (ClientState.isAsleep()) {
			keyPresses = Input.EMPTY;
			moveVector = Vec2.ZERO;
			return;
		}
		// Seizures: you can't walk (jumping still works, barely).
		if (ClientState.isSeizing()) {
			keyPresses = new Input(false, false, false, false, keyPresses.jump(), false, false);
			moveVector = Vec2.ZERO;
			return;
		}
		// Cerebral Palsy: add whatever involuntary movement is happening right now.
		if (!ClientState.has(Disability.CEREBRAL_PALSY)) {
			return;
		}
		boolean forward = ClientState.spasmForward();
		boolean backward = ClientState.spasmBackward();
		boolean left = ClientState.spasmLeft();
		boolean right = ClientState.spasmRight();
		boolean jump = ClientState.consumeSpasmJump();
		boolean sneak = ClientState.consumeSpasmSneak();
		if (!forward && !backward && !left && !right && !jump && !sneak) {
			return;
		}
		Input pressed = keyPresses;
		forward |= pressed.forward();
		backward |= pressed.backward();
		left |= pressed.left();
		right |= pressed.right();
		keyPresses = new Input(forward, backward, left, right, pressed.jump() || jump, pressed.shift() || sneak, pressed.sprint());
		float sideways = (left ? 1.0F : 0.0F) - (right ? 1.0F : 0.0F);
		float forwards = (forward ? 1.0F : 0.0F) - (backward ? 1.0F : 0.0F);
		moveVector = new Vec2(sideways, forwards).normalized();
	}
}
