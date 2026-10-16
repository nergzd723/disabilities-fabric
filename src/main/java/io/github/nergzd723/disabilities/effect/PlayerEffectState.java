package io.github.nergzd723.disabilities.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Transient, per-player bookkeeping for the server-side effects. Never saved; rebuilt when a player rejoins. */
final class PlayerEffectState {
	// Consumption (Diabetes, Lactose Intolerance)
	final List<ItemStack> consumed = new ArrayList<>();
	int lastCakeSlices = -1;

	// Seizures
	int seizureTicks;
	int seizureRespawnGuard;

	// Vertigo
	int vertigoTicks;

	// Asthma
	double lungAir = -1.0;
	int asthmaDamageTimer;

	// Pollen Allergy
	int pollenTimer;

	// Dementia
	long hiddenSlots;
	long sentHiddenSlots;
	final @Nullable Item[] hiddenItems = new Item[Inventory.INVENTORY_SIZE];

	// OCD
	final OcdCompulsions ocd = new OcdCompulsions();
	Optional<Component> sentOcdLine = Optional.empty();

	boolean seizing() {
		return seizureTicks > 0;
	}
}
