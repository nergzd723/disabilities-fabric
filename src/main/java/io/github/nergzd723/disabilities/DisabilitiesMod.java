package io.github.nergzd723.disabilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;

import io.github.nergzd723.disabilities.command.DisabilityCommands;
import io.github.nergzd723.disabilities.effect.ServerEffects;
import io.github.nergzd723.disabilities.network.ModNetworking;
import io.github.nergzd723.disabilities.network.SyncStatePayload;
import io.github.nergzd723.disabilities.run.DisabilityManager;

public class DisabilitiesMod implements ModInitializer {
	public static final String MOD_ID = "disabilities";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Player.BedSleepingProblem CANT_SLEEP = new Player.BedSleepingProblem(Component.translatable("message.disabilities.insomnia"));

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModNetworking.register();
		DisabilityCommands.register();

		ServerLifecycleEvents.SERVER_STARTED.register(DisabilityManager::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			DisabilityManager.onServerStopped(server);
			ServerEffects.forgetAll();
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerEffects.tick(server);
			DisabilityManager.tick(server);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			// Most disabilities are implemented on the client, so playing without the mod would skip them.
			if (!(handler.player instanceof FakePlayer) && !ServerPlayNetworking.canSend(handler, SyncStatePayload.TYPE)) {
				handler.disconnect(Component.translatable("multiplayer.disabilities.missing_mod"));
				return;
			}
			DisabilityManager.requestSync();
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerEffects.forget(handler.player.getUUID());
			DisabilityManager.requestSync();
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register(ServerEffects::afterDamage);
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				ServerEffects.onBlockBroken(serverPlayer, state);
			}
		});

		// Insomnia: the bed still sets your spawn point, you just never fall asleep...
		EntitySleepEvents.ALLOW_SLEEPING.register((player, sleepingPos) ->
				Disabilities.has(player, Disability.INSOMNIA) ? CANT_SLEEP : null);
		// ...and you can't use a bed at all where it would explode, so no bed bombing either.
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (!Disabilities.has(player, Disability.INSOMNIA)) {
				return InteractionResult.PASS;
			}
			BlockState state = level.getBlockState(hitResult.getBlockPos());
			if (state.getBlock() instanceof AbstractBedBlock bed && bed.getBedRule(level, hitResult.getBlockPos()).destroyOnUse()) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});

		// Mute: the client refuses to send chat, and the server double-checks.
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, boundChatType) ->
				!DisabilityManager.has(sender.getUUID(), Disability.MUTE));
	}
}
