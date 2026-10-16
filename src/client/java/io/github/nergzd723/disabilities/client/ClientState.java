package io.github.nergzd723.disabilities.client;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.SyncedDisabilities;
import io.github.nergzd723.disabilities.run.TimerMode;

/** Everything the client knows about the local player's disabilities, plus the state of the client-side effects. */
public final class ClientState {
	// Synced from the server
	private static int ticksUntilNext = -1;
	private static boolean timerEnabled = true;
	private static TimerMode mode = TimerMode.REPLACE;
	private static @Nullable Component ocdLine;
	private static int seizureTicks;
	private static int vertigoTicks;
	private static int vertigoDirection = 1;
	private static long hiddenSlots;

	// The local player's disabilities, refreshed every tick so per-frame hooks only do a set lookup.
	private static Set<Disability> mine = Set.of();

	private static boolean listVisible = true;
	private static boolean dyslexiaExempt;

	// Cerebral Palsy
	static int spasmDirection = -1;
	static int spasmTicks;
	static boolean spasmJump;
	static boolean spasmSneak;

	// Narcolepsy
	static final int SLEEP_TICKS = 90;
	static int sleepTicks;

	// Parkinson's
	static float tremorYaw;
	static float tremorPitch;
	static int tremorEpisodeTicks;

	private ClientState() {
	}

	static void refresh(Minecraft minecraft) {
		mine = minecraft.player == null ? Set.of() : SyncedDisabilities.get(minecraft.player.getUUID());
	}

	public static boolean has(Disability disability) {
		return mine.contains(disability);
	}

	public static Set<Disability> mine() {
		return mine;
	}

	// ---------------------------------------------------------------- timer

	static void setTimer(int ticks, boolean enabled, TimerMode timerMode) {
		ticksUntilNext = ticks;
		timerEnabled = enabled;
		mode = timerMode;
	}

	public static int ticksUntilNext() {
		return ticksUntilNext;
	}

	public static boolean timerEnabled() {
		return timerEnabled;
	}

	public static TimerMode mode() {
		return mode;
	}

	// ---------------------------------------------------------------- episodes

	static void setSeizure(int ticks) {
		seizureTicks = Math.max(0, ticks);
	}

	public static boolean isSeizing() {
		return seizureTicks > 0;
	}

	static void setVertigo(int ticks, int direction) {
		vertigoTicks = Math.max(0, ticks);
		vertigoDirection = direction;
	}

	public static int vertigoTicks() {
		return vertigoTicks;
	}

	static int vertigoDirection() {
		return vertigoDirection;
	}

	static void tickEpisodes() {
		if (seizureTicks > 0) {
			seizureTicks--;
		}
		if (vertigoTicks > 0) {
			vertigoTicks--;
		}
		if (timerEnabled && ticksUntilNext > 0) {
			ticksUntilNext--;
		}
	}

	// ---------------------------------------------------------------- OCD

	static void setOcdLine(@Nullable Component line) {
		ocdLine = line;
	}

	public static @Nullable Component ocdLine() {
		return ocdLine;
	}

	// ---------------------------------------------------------------- Dementia

	static void setHiddenSlots(long mask) {
		hiddenSlots = mask;
	}

	/** Whether this slot of the local player's inventory should be drawn as empty. */
	public static boolean isSlotHidden(Container container, int slot) {
		if (hiddenSlots == 0L || slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
			return false;
		}
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null && container == minecraft.player.getInventory() && (hiddenSlots & (1L << slot)) != 0L;
	}

	// ---------------------------------------------------------------- HUD list

	public static boolean listVisible() {
		return listVisible;
	}

	static void toggleList() {
		listVisible = !listVisible;
	}

	// ---------------------------------------------------------------- Dyslexia

	public static boolean isDyslexic() {
		return !dyslexiaExempt && mine.contains(Disability.DYSLEXIA);
	}

	public static boolean dyslexiaExempt() {
		return dyslexiaExempt;
	}

	/** Lets one piece of UI (this mod's own HUD, menus, the chat input) stay readable. */
	public static void setDyslexiaExempt(boolean exempt) {
		dyslexiaExempt = exempt;
	}

	// ---------------------------------------------------------------- Cerebral Palsy, Narcolepsy, Parkinson's

	public static boolean spasmForward() {
		return spasmTicks > 0 && spasmDirection == 0;
	}

	public static boolean spasmBackward() {
		return spasmTicks > 0 && spasmDirection == 1;
	}

	public static boolean spasmLeft() {
		return spasmTicks > 0 && spasmDirection == 2;
	}

	public static boolean spasmRight() {
		return spasmTicks > 0 && spasmDirection == 3;
	}

	public static boolean consumeSpasmJump() {
		boolean jump = spasmJump;
		spasmJump = false;
		return jump;
	}

	public static boolean consumeSpasmSneak() {
		boolean sneak = spasmSneak;
		spasmSneak = false;
		return sneak;
	}

	public static boolean isAsleep() {
		return sleepTicks > 0;
	}

	/** How black the screen is while asleep: fades in over a second, holds, fades out over a second. */
	public static float sleepDarkness() {
		if (sleepTicks <= 0) {
			return 0.0F;
		}
		if (sleepTicks > SLEEP_TICKS - 20) {
			return (SLEEP_TICKS - sleepTicks) / 20.0F;
		}
		return sleepTicks < 20 ? sleepTicks / 20.0F : 1.0F;
	}

	public static float tremorYaw() {
		return tremorYaw;
	}

	public static float tremorPitch() {
		return tremorPitch;
	}

	/** True while the crosshair is off-centre, including while it settles back after Parkinson's is cured. */
	public static boolean isTrembling() {
		return mine.contains(Disability.PARKINSONS) || Math.abs(tremorYaw) > 0.05F || Math.abs(tremorPitch) > 0.05F;
	}

	static void clear() {
		SyncedDisabilities.clear();
		mine = Set.of();
		ticksUntilNext = -1;
		timerEnabled = true;
		mode = TimerMode.REPLACE;
		ocdLine = null;
		seizureTicks = 0;
		vertigoTicks = 0;
		hiddenSlots = 0L;
		spasmTicks = 0;
		spasmJump = false;
		spasmSneak = false;
		sleepTicks = 0;
		tremorYaw = 0.0F;
		tremorPitch = 0.0F;
		tremorEpisodeTicks = 0;
		dyslexiaExempt = false;
	}
}
