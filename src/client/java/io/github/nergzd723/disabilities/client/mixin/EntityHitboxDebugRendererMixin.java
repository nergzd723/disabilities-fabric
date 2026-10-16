package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(EntityHitboxDebugRenderer.class)
abstract class EntityHitboxDebugRendererMixin {
	/** Prosopagnosia: no telling mobs apart by their F3+B hitboxes either. */
	@Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true)
	private void disabilities$hideMobHitboxes(Entity entity, float partialTicks, boolean isServerEntity, CallbackInfo ci) {
		if (entity instanceof Mob && ClientState.has(Disability.PROSOPAGNOSIA)) {
			ci.cancel();
		}
	}
}
