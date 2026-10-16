package io.github.nergzd723.disabilities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.mixin.MinecraftInvoker;

/** The disabilities that only exist on the client: things you see, hear and involuntarily do. */
final class ClientEffects {
	private static final RandomSource RANDOM = RandomSource.create();
	private static final SoundEvent[] PHANTOM_STEPS = {SoundEvents.GRASS_STEP, SoundEvents.STONE_STEP, SoundEvents.WOOD_STEP, SoundEvents.GRAVEL_STEP};
	private static final SoundEvent[] PHANTOM_MOBS = {SoundEvents.ZOMBIE_AMBIENT, SoundEvents.SKELETON_AMBIENT, SoundEvents.SPIDER_AMBIENT,
			SoundEvents.ENDERMAN_AMBIENT};

	private static boolean wasSeizing;
	private static float seizureYaw;
	private static float seizurePitch;

	private ClientEffects() {
	}

	static void tick(Minecraft minecraft) {
		ClientState.refresh(minecraft);
		ClientState.setDyslexiaExempt(false);
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.isPaused()) {
			return;
		}
		ClientState.tickEpisodes();
		boolean noScreen = minecraft.gui.screen() == null;

		if (ClientState.has(Disability.SCHIZOPHRENIA)) {
			phantomSounds(minecraft, player);
		}
		tickCerebralPalsy(minecraft, noScreen);
		tickNarcolepsy(noScreen);
		tickParkinsons();

		// ADHD: your attention wanders to another hotbar slot every ~15 seconds.
		if (ClientState.has(Disability.ADHD) && noScreen && RANDOM.nextInt(300) == 0) {
			player.getInventory().setSelectedSlot(RANDOM.nextInt(9));
		}

		tickTourettes(minecraft, player, noScreen);

		// Vertigo: the world slowly spins while a spell lasts (the tilt itself is done by the camera mixin).
		if (ClientState.vertigoTicks() > 0) {
			player.setYRot(player.getYRot() + ClientState.vertigoDirection() * 0.25F);
		}

		tickSeizureShake(player);
	}

	/** Schizophrenia: footsteps, mobs and a hissing creeper that aren't there. Played as "voice" so Deaf doesn't help. */
	private static void phantomSounds(Minecraft minecraft, LocalPlayer player) {
		if (minecraft.level == null || RANDOM.nextInt(120) != 0) {
			return;
		}
		double angle = RANDOM.nextDouble() * Math.PI * 2.0;
		double distance = 3.0 + RANDOM.nextDouble() * 6.0;
		double x = player.getX() + Math.cos(angle) * distance;
		double y = player.getY() + RANDOM.nextDouble() * 2.0 - 0.5;
		double z = player.getZ() + Math.sin(angle) * distance;
		int roll = RANDOM.nextInt(10);
		SoundEvent sound;
		if (roll < 4) {
			sound = PHANTOM_STEPS[RANDOM.nextInt(PHANTOM_STEPS.length)];
		} else if (roll < 8) {
			sound = PHANTOM_MOBS[RANDOM.nextInt(PHANTOM_MOBS.length)];
		} else {
			sound = SoundEvents.CREEPER_PRIMED;
		}
		minecraft.level.playLocalSound(x, y, z, sound, SoundSource.VOICE, 0.9F, 0.9F + RANDOM.nextFloat() * 0.2F, false);
	}

	/** Cerebral Palsy: every ~7 seconds an involuntary click, jump, crouch or half-second step. */
	private static void tickCerebralPalsy(Minecraft minecraft, boolean noScreen) {
		if (ClientState.spasmTicks > 0) {
			ClientState.spasmTicks--;
		}
		if (!ClientState.has(Disability.CEREBRAL_PALSY) || !noScreen || ClientState.isSeizing() || ClientState.isAsleep()
				|| RANDOM.nextInt(140) != 0) {
			return;
		}
		int action = RANDOM.nextInt(8);
		switch (action) {
			case 0 -> ((MinecraftInvoker) minecraft).disabilities$startAttack();
			case 1 -> ((MinecraftInvoker) minecraft).disabilities$startUseItem();
			case 2 -> ClientState.spasmJump = true;
			case 3 -> ClientState.spasmSneak = true;
			default -> {
				ClientState.spasmDirection = action - 4;
				ClientState.spasmTicks = 10;
			}
		}
	}

	/** Narcolepsy: roughly every three minutes you nod off for a few seconds. */
	private static void tickNarcolepsy(boolean noScreen) {
		if (ClientState.sleepTicks > 0) {
			ClientState.sleepTicks--;
		}
		if (ClientState.has(Disability.NARCOLEPSY) && ClientState.sleepTicks == 0 && noScreen && !ClientState.isSeizing()
				&& RANDOM.nextInt(4000) == 0) {
			ClientState.sleepTicks = ClientState.SLEEP_TICKS;
		}
	}

	/** Parkinson's: the crosshair wanders around the centre, with occasional worse episodes. */
	private static void tickParkinsons() {
		if (!ClientState.has(Disability.PARKINSONS)) {
			ClientState.tremorEpisodeTicks = 0;
			// Settle back to the centre.
			ClientState.tremorYaw *= 0.8F;
			ClientState.tremorPitch *= 0.8F;
			if (Math.abs(ClientState.tremorYaw) < 0.01F) {
				ClientState.tremorYaw = 0.0F;
			}
			if (Math.abs(ClientState.tremorPitch) < 0.01F) {
				ClientState.tremorPitch = 0.0F;
			}
			return;
		}
		boolean severe = ClientState.tremorEpisodeTicks > 0;
		if (severe) {
			ClientState.tremorEpisodeTicks--;
		} else if (RANDOM.nextInt(600) == 0) {
			ClientState.tremorEpisodeTicks = 80 + RANDOM.nextInt(80);
		}
		float yawStep = severe ? 3.5F : 0.7F;
		float pitchStep = severe ? 2.6F : 0.55F;
		float yawLimit = severe ? 12.0F : 10.0F;
		float pitchLimit = severe ? 9.0F : 7.0F;
		ClientState.tremorYaw = Mth.clamp((ClientState.tremorYaw + (RANDOM.nextFloat() - 0.5F) * yawStep) * 0.995F, -yawLimit, yawLimit);
		ClientState.tremorPitch = Mth.clamp((ClientState.tremorPitch + (RANDOM.nextFloat() - 0.5F) * pitchStep) * 0.995F, -pitchLimit, pitchLimit);
	}

	/** Tourette's: sudden camera jerks, and every so often you blurt out a random word. */
	private static void tickTourettes(Minecraft minecraft, LocalPlayer player, boolean noScreen) {
		if (!ClientState.has(Disability.TOURETTES)) {
			return;
		}
		if (RANDOM.nextInt(350) == 0) {
			double angle = RANDOM.nextDouble() * Math.PI * 2.0;
			player.setYRot(player.getYRot() + (float) (Math.cos(angle) * 15.0));
			player.setXRot(Mth.clamp(player.getXRot() + (float) (Math.sin(angle) * 15.0), -90.0F, 90.0F));
		}
		if (!ClientState.has(Disability.MUTE) && noScreen && RANDOM.nextInt(800) == 0) {
			String word = TouretteVocabulary.randomWord(minecraft, RANDOM);
			if (!word.isEmpty()) {
				player.connection.sendChat(word);
			}
		}
	}

	/** Seizures: the view convulses around where you were looking when it started. */
	private static void tickSeizureShake(LocalPlayer player) {
		boolean seizing = ClientState.isSeizing();
		if (seizing && !wasSeizing) {
			seizureYaw = player.getYRot();
			seizurePitch = player.getXRot();
		}
		if (seizing) {
			player.setYRot(seizureYaw + (RANDOM.nextFloat() - 0.5F) * 24.0F);
			player.setXRot(Mth.clamp(seizurePitch + (RANDOM.nextFloat() - 0.5F) * 24.0F, -90.0F, 90.0F));
		}
		wasSeizing = seizing;
	}

	static void reset() {
		wasSeizing = false;
	}
}
