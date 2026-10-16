package io.github.nergzd723.disabilities.client.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.SyncedDisabilities;
import io.github.nergzd723.disabilities.client.ClientState;
import io.github.nergzd723.disabilities.client.render.RenderData;

@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
			at = @At("TAIL"))
	private void disabilities$tagRenderState(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
		state.setData(RenderData.IS_MOB, entity instanceof Mob);
		if (entity instanceof Player) {
			UUID id = entity.getUUID();
			state.setData(RenderData.AMPUTATIONS, new RenderData.Amputations(
					SyncedDisabilities.has(id, Disability.AMPUTATED_OFFHAND), SyncedDisabilities.has(id, Disability.AMPUTATED_LEG)));
		}
	}

	/** Prosopagnosia: a mob's shadow would give away its size. */
	@Inject(method = "getShadowRadius", at = @At("HEAD"), cancellable = true)
	private void disabilities$noMobShadow(EntityRenderState state, CallbackInfoReturnable<Float> cir) {
		if (ClientState.has(Disability.PROSOPAGNOSIA) && state.getDataOrDefault(RenderData.IS_MOB, false)) {
			cir.setReturnValue(0.0F);
		}
	}
}
