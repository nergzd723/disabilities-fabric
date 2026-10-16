package io.github.nergzd723.disabilities.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import io.github.nergzd723.disabilities.Disabilities;
import io.github.nergzd723.disabilities.Disability;

@Mixin(Inventory.class)
abstract class InventoryMixin {
	@Shadow
	@Final
	public Player player;

	/** Kleptomania: the drop key does nothing. Runs on both sides so the client doesn't predict a drop either. */
	@Inject(method = "removeFromSelected", at = @At("HEAD"), cancellable = true)
	private void disabilities$cantLetGo(boolean all, CallbackInfoReturnable<ItemStack> cir) {
		if (Disabilities.has(player, Disability.KLEPTOMANIA)) {
			cir.setReturnValue(ItemStack.EMPTY);
		}
	}
}
