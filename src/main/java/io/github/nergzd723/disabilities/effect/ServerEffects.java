package io.github.nergzd723.disabilities.effect;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.network.ModNetworking;
import io.github.nergzd723.disabilities.network.OcdStatusPayload;
import io.github.nergzd723.disabilities.run.DisabilityManager;

/** Runs every server-side disability effect once per tick for each online player. */
public final class ServerEffects {
	private static final Map<UUID, PlayerEffectState> STATES = new ConcurrentHashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();

	private ServerEffects() {
	}

	private static PlayerEffectState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), id -> new PlayerEffectState());
	}

	/** Drops all transient effect state (pending seizures, OCD tasks, hidden slots...) for a player. */
	public static void forget(UUID player) {
		STATES.remove(player);
	}

	public static void forgetAll() {
		STATES.clear();
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			tickPlayer(player);
		}
	}

	public static void tickPlayer(ServerPlayer player) {
		PlayerEffectState state = state(player);
		ServerLevel level = player.level();
		Consumption.tick(player, level, state);
		StatusEffects.tick(player, level, state, RANDOM);
		AttributeEffects.tick(player, RANDOM);
		tickAmputatedOffhand(player, level);
		Seizures.tick(player, level, state, RANDOM);
		Dementia.tick(player, state, RANDOM);
		Asthma.tick(player, level, state);
		state.ocd.tick(player, level, RANDOM);
		pushOcdLine(player, state);
	}

	// ---------------------------------------------------------------- hooks called from events and mixins

	public static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (entity instanceof ServerPlayer player) {
			Seizures.afterDamage(player, state(player), damageTaken, RANDOM);
		}
	}

	public static void onItemConsumed(ServerPlayer player, ItemStack stack) {
		state(player).consumed.add(stack.copyWithCount(1));
	}

	public static void onBlockBroken(ServerPlayer player, BlockState state) {
		state(player).ocd.onBlockBroken(state);
	}

	public static void onContainerClick(ServerPlayer player, AbstractContainerMenu menu, int slotIndex, int button, ContainerInput input) {
		Dementia.onContainerClick(player, state(player), menu, slotIndex, button, input);
	}

	// ---------------------------------------------------------------- small effects

	/** Amputated Offhand: anything that ends up in the offhand falls out of it (or back into the bag, for kleptomaniacs). */
	private static void tickAmputatedOffhand(ServerPlayer player, ServerLevel level) {
		if (!DisabilityManager.has(player.getUUID(), Disability.AMPUTATED_OFFHAND)) {
			return;
		}
		ItemStack offhand = player.getOffhandItem();
		if (offhand.isEmpty()) {
			return;
		}
		if (DisabilityManager.has(player.getUUID(), Disability.KLEPTOMANIA)) {
			ItemStack moving = offhand.copy();
			player.getInventory().add(moving);
			player.setItemSlot(EquipmentSlot.OFFHAND, moving.isEmpty() ? ItemStack.EMPTY : moving);
			player.inventoryMenu.broadcastChanges();
		} else {
			ItemEntity drop = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), offhand.copy());
			drop.setDefaultPickUpDelay();
			level.addFreshEntity(drop);
			player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		}
	}

	private static void pushOcdLine(ServerPlayer player, PlayerEffectState state) {
		Optional<Component> line = state.ocd.hudLine(player);
		if (!line.equals(state.sentOcdLine)) {
			state.sentOcdLine = line;
			ModNetworking.send(player, new OcdStatusPayload(line));
		}
	}
}
