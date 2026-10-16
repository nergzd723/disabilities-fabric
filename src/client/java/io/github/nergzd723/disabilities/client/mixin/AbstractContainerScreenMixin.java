package io.github.nergzd723.disabilities.client.mixin;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {
	@Shadow
	protected @Nullable Slot hoveredSlot;

	/** Dementia: forgotten slots look empty until you click them. */
	@WrapOperation(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack disabilities$forgottenSlot(Slot slot, Operation<ItemStack> original) {
		return ClientState.isSlotHidden(slot.container, slot.getContainerSlot()) ? ItemStack.EMPTY : original.call(slot);
	}

	@Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
	private void disabilities$noForgottenTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (hoveredSlot != null && ClientState.isSlotHidden(hoveredSlot.container, hoveredSlot.getContainerSlot())) {
			ci.cancel();
		}
	}
}
