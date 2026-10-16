package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;

import io.github.nergzd723.disabilities.client.render.RenderData;

@Mixin(PlayerModel.class)
abstract class PlayerModelMixin {
	/** Amputations are visible on every player: the offhand arm and the right leg are simply not drawn. */
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void disabilities$hideMissingLimbs(AvatarRenderState state, CallbackInfo ci) {
		RenderData.Amputations amputations = state.getDataOrDefault(RenderData.AMPUTATIONS, RenderData.Amputations.NONE);
		PlayerModel self = (PlayerModel) (Object) this;
		if (amputations.offhandArm()) {
			(state.mainArm == HumanoidArm.LEFT ? self.rightArm : self.leftArm).visible = false;
		}
		if (amputations.leg()) {
			self.rightLeg.visible = false;
		}
	}
}
