package io.github.nergzd723.disabilities.effect;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.network.HiddenSlotsPayload;
import io.github.nergzd723.disabilities.network.ModNetworking;
import io.github.nergzd723.disabilities.run.DisabilityManager;

/**
 * Dementia: about once a minute an item is swapped with another stack or moved to an empty slot, and both slots are
 * "forgotten" (drawn as empty) until the player clicks them, selects them on the hotbar, or their contents change.
 */
final class Dementia {
	private static final int SHUFFLE_CHANCE = 1200;

	private Dementia() {
	}

	static void tick(ServerPlayer player, PlayerEffectState state, RandomSource random) {
		Inventory inventory = player.getInventory();
		boolean shuffled = false;
		if (!DisabilityManager.has(player.getUUID(), Disability.DEMENTIA)) {
			state.hiddenSlots = 0L;
		} else {
			if (random.nextInt(SHUFFLE_CHANCE) == 0) {
				shuffled = shuffle(inventory, state, random);
			}
			for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
				if (isHidden(state, slot)) {
					ItemStack stack = inventory.getItem(slot);
					if (slot == inventory.getSelectedSlot() || stack.isEmpty() || !stack.is(state.hiddenItems[slot])) {
						reveal(state, slot);
					}
				}
			}
		}
		pushHiddenSlots(player, state);
		if (shuffled) {
			player.inventoryMenu.broadcastChanges();
			if (player.containerMenu != player.inventoryMenu) {
				player.containerMenu.broadcastChanges();
			}
		}
	}

	private static boolean shuffle(Inventory inventory, PlayerEffectState state, RandomSource random) {
		List<Integer> filled = new ArrayList<>();
		List<Integer> empty = new ArrayList<>();
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			(inventory.getItem(slot).isEmpty() ? empty : filled).add(slot);
		}
		if (filled.isEmpty()) {
			return false;
		}

		int from = filled.get(random.nextInt(filled.size()));
		List<Integer> partners = new ArrayList<>();
		for (int slot : filled) {
			if (!ItemStack.matches(inventory.getItem(slot), inventory.getItem(from))) {
				partners.add(slot);
			}
		}
		boolean swap = !partners.isEmpty() && (empty.isEmpty() || random.nextBoolean());
		if (!swap && empty.isEmpty()) {
			return false;
		}
		int to = swap ? partners.get(random.nextInt(partners.size())) : empty.get(random.nextInt(empty.size()));

		ItemStack moving = inventory.getItem(from);
		inventory.setItem(from, inventory.getItem(to));
		inventory.setItem(to, moving);
		for (int slot : new int[] {from, to}) {
			ItemStack now = inventory.getItem(slot);
			if (!now.isEmpty()) {
				state.hiddenSlots |= 1L << slot;
				state.hiddenItems[slot] = now.getItem();
			}
		}
		return true;
	}

	/** Clicking a forgotten slot (or number-key swapping with it) makes the player remember it. */
	static void onContainerClick(ServerPlayer player, PlayerEffectState state, AbstractContainerMenu menu, int slotIndex, int button,
			ContainerInput input) {
		if (state.hiddenSlots == 0L) {
			return;
		}
		if (slotIndex >= 0 && slotIndex < menu.slots.size()) {
			Slot slot = menu.getSlot(slotIndex);
			if (slot.container == player.getInventory()) {
				reveal(state, slot.getContainerSlot());
			}
		}
		if (input == ContainerInput.SWAP) {
			reveal(state, button);
		}
		pushHiddenSlots(player, state);
	}

	private static boolean isHidden(PlayerEffectState state, int slot) {
		return (state.hiddenSlots & (1L << slot)) != 0L;
	}

	private static void reveal(PlayerEffectState state, int slot) {
		if (slot >= 0 && slot < Inventory.INVENTORY_SIZE) {
			state.hiddenSlots &= ~(1L << slot);
			state.hiddenItems[slot] = null;
		}
	}

	private static void pushHiddenSlots(ServerPlayer player, PlayerEffectState state) {
		if (state.hiddenSlots != state.sentHiddenSlots) {
			state.sentHiddenSlots = state.hiddenSlots;
			ModNetworking.send(player, new HiddenSlotsPayload(state.hiddenSlots));
		}
	}
}
