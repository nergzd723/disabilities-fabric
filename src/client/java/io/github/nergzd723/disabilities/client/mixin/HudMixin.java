package io.github.nergzd723.disabilities.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;

@Mixin(Hud.class)
abstract class HudMixin {
	/** Asthma: while short of breath the air bar behaves exactly like it does underwater, popping bubbles included. */
	@ModifyExpressionValue(method = "extractAirBubbles", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
	private boolean disabilities$breathless(boolean underWater, @Local(argsOnly = true) Player player) {
		return underWater || ClientState.has(Disability.ASTHMA) && player.getAirSupply() < player.getMaxAirSupply();
	}

	/** Dementia: forgotten hotbar slots look empty. */
	@WrapOperation(method = "extractItemHotbar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack disabilities$forgottenHotbar(Inventory inventory, int slot, Operation<ItemStack> original) {
		return ClientState.isSlotHidden(inventory, slot) ? ItemStack.EMPTY : original.call(inventory, slot);
	}
}
