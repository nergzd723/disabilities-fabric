package io.github.nergzd723.disabilities.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import io.github.nergzd723.disabilities.DisabilitiesMod;
import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.SyncedDisabilities;
import io.github.nergzd723.disabilities.client.hud.DisabilityListHud;
import io.github.nergzd723.disabilities.client.hud.NarcolepsyOverlay;
import io.github.nergzd723.disabilities.client.hud.OcdHud;
import io.github.nergzd723.disabilities.client.hud.TremorCrosshair;
import io.github.nergzd723.disabilities.client.render.ShadowFigure;
import io.github.nergzd723.disabilities.network.EpisodePayload;
import io.github.nergzd723.disabilities.network.HiddenSlotsPayload;
import io.github.nergzd723.disabilities.network.OcdStatusPayload;
import io.github.nergzd723.disabilities.network.SyncStatePayload;

public class DisabilitiesClient implements ClientModInitializer {
	private static KeyMapping toggleListKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(DisabilitiesMod.id("disabilities"));
		toggleListKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.disabilities.toggle_list", InputConstants.Type.KEYBOARD, InputConstants.KEY_GRAVE, category));

		registerReceivers();
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientState.clear();
			ClientEffects.reset();
			ShadowFigure.reset();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleListKey.consumeClick()) {
				ClientState.toggleList();
			}
			ClientEffects.tick(client);
			ShadowFigure.tick(client);
		});

		// Mute: chat messages never leave the client (commands still work).
		ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
			if (!ClientState.has(Disability.MUTE)) {
				return true;
			}
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player != null && !ClientState.has(Disability.ANOSOGNOSIA)) {
				minecraft.player.sendOverlayMessage(Component.translatable("message.disabilities.mute").withStyle(ChatFormatting.GRAY));
			}
			return false;
		});

		HudElementRegistry.addLast(DisabilitiesMod.id("disability_list"), (graphics, deltaTracker) -> {
			// Inside the inventory the list is drawn by the screen instead, so its entries can be hovered.
			if (!(Minecraft.getInstance().gui.screen() instanceof InventoryScreen)) {
				DisabilityListHud.render(graphics, -1, -1);
			}
		});
		HudElementRegistry.addLast(DisabilitiesMod.id("ocd"), (graphics, deltaTracker) -> OcdHud.render(graphics));
		HudElementRegistry.addLast(DisabilitiesMod.id("narcolepsy"), (graphics, deltaTracker) -> NarcolepsyOverlay.render(graphics));
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, TremorCrosshair::new);

		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof InventoryScreen) {
				ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, tickProgress) ->
						DisabilityListHud.render(graphics, mouseX, mouseY));
			}
		});

		LevelRenderEvents.COLLECT_SUBMITS.register(ShadowFigure::submit);
	}

	private static void registerReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(SyncStatePayload.TYPE, (payload, context) -> {
			SyncedDisabilities.update(payload.disabilities());
			ClientState.setTimer(payload.ticksUntilNext(), payload.timerEnabled(), payload.mode());
			ClientState.refresh(context.client());
		});
		ClientPlayNetworking.registerGlobalReceiver(EpisodePayload.TYPE, (payload, context) -> {
			switch (payload.kind()) {
				case SEIZURE -> ClientState.setSeizure(payload.ticks());
				case VERTIGO -> ClientState.setVertigo(payload.ticks(), payload.direction());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(OcdStatusPayload.TYPE, (payload, context) ->
				ClientState.setOcdLine(payload.line().orElse(null)));
		ClientPlayNetworking.registerGlobalReceiver(HiddenSlotsPayload.TYPE, (payload, context) ->
				ClientState.setHiddenSlots(payload.mask()));
	}
}
