package io.github.nergzd723.disabilities.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

import io.github.nergzd723.disabilities.effect.ServerEffects;

@Mixin(Consumable.class)
abstract class ConsumableMixin {
	/** Diabetes / Lactose Intolerance: remember what was eaten or drunk; the effects react at the end of the tick. */
	@Inject(method = "onConsume", at = @At("HEAD"))
	private void disabilities$recordConsumption(Level level, LivingEntity user, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
		if (user instanceof ServerPlayer player && !stack.isEmpty()) {
			ServerEffects.onItemConsumed(player, stack);
		}
	}
}
