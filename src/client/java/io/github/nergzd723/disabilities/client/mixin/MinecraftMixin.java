package io.github.nergzd723.disabilities.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	/**
	 * Parkinson's: aim where the shaking crosshair points, not where the camera looks. The camera entity is turned
	 * by the tremor just for the duration of the vanilla raycast, so blocks, entities and spear reach all agree.
	 */
	@WrapOperation(method = "pick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;"))
	private HitResult disabilities$tremorAim(LocalPlayer player, float partialTicks, Entity camera, Operation<HitResult> original) {
		if (!ClientState.isTrembling()) {
			return original.call(player, partialTicks, camera);
		}
		float yRot = camera.getYRot();
		float xRot = camera.getXRot();
		float yRotO = camera.yRotO;
		float xRotO = camera.xRotO;
		float yaw = ClientState.tremorYaw();
		float pitch = ClientState.tremorPitch();
		camera.setYRot(yRot + yaw);
		camera.setXRot(Mth.clamp(xRot + pitch, -90.0F, 90.0F));
		camera.yRotO = yRotO + yaw;
		camera.xRotO = Mth.clamp(xRotO + pitch, -90.0F, 90.0F);
		try {
			return original.call(player, partialTicks, camera);
		} finally {
			camera.setYRot(yRot);
			camera.setXRot(xRot);
			camera.yRotO = yRotO;
			camera.xRotO = xRotO;
		}
	}
}
