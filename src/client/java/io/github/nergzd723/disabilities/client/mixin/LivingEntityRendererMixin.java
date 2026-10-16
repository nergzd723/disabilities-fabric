package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;
import io.github.nergzd723.disabilities.client.render.MobDisguise;
import io.github.nergzd723.disabilities.client.render.RenderData;

@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
	/** Prosopagnosia: draw a Steve instead of the mob. */
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;"
			+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("HEAD"), cancellable = true)
	private void disabilities$everyoneLooksTheSame(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera, CallbackInfo ci) {
		if (ClientState.has(Disability.PROSOPAGNOSIA) && state.getDataOrDefault(RenderData.IS_MOB, false)) {
			MobDisguise.submit(state, poseStack, collector);
			ci.cancel();
		}
	}
}
