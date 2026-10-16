package io.github.nergzd723.disabilities.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.Minecraft;

/** Cerebral Palsy: involuntary left and right clicks. */
@Mixin(Minecraft.class)
public interface MinecraftInvoker {
	@Invoker("startAttack")
	boolean disabilities$startAttack();

	@Invoker("startUseItem")
	void disabilities$startUseItem();
}
