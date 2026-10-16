package io.github.nergzd723.disabilities.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

import io.github.nergzd723.disabilities.Disabilities;
import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.effect.ServerEffects;

@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuMixin {
	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void disabilities$onClicked(int slotIndex, int button, ContainerInput input, Player player, CallbackInfo ci) {
		// Kleptomania: no throwing items out of a container, either with the drop key or by clicking outside.
		if (Disabilities.has(player, Disability.KLEPTOMANIA)
				&& (input == ContainerInput.THROW || input == ContainerInput.PICKUP && slotIndex == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE)) {
			ci.cancel();
			return;
		}
		// Dementia: clicking a forgotten slot makes you remember what's in it.
		if (player instanceof ServerPlayer serverPlayer) {
			ServerEffects.onContainerClick(serverPlayer, (AbstractContainerMenu) (Object) this, slotIndex, button, input);
		}
	}
}
