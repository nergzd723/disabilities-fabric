package io.github.nergzd723.disabilities.test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.MixinEnvironment;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;
import io.github.nergzd723.disabilities.client.render.ShadowFigure;
import io.github.nergzd723.disabilities.network.EpisodePayload;
import io.github.nergzd723.disabilities.network.HiddenSlotsPayload;
import io.github.nergzd723.disabilities.run.DisabilityManager;

/** Plays through the mod in a real client: rendering hooks, input hooks and the timer, with screenshots. */
public class DisabilitiesClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		// Every common and client mixin must find its target.
		context.runOnClient(minecraft -> MixinEnvironment.getCurrentEnvironment().audit());

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("disability timer off");
			server.runCommand("gamemode creative @a");
			context.waitTicks(20);

			check(context.computeOnClient(minecraft -> minecraft.player != null), "joined the world (the mod check must not kick us)");

			hudAndScoliosis(context, server);
			colorBlind(context, server);
			prosopagnosia(context, server);
			dyslexia(context, server);
			amputations(context, server);
			kleptomania(context, server);
			toggleList(context, server);
			parkinsons(context, server);
			vertigoAndSeizure(context, server);
			dementiaAndInventory(context, server);
			schizophrenia(context, server);
			commands(context, server);
			replaceTimer(context, server);
			stackTimer(context, server);
			lactoseIntolerance(context, server);
		}
	}

	private static void hudAndScoliosis(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("disability give @a scoliosis");
		server.runCommand("disability give @a short");
		context.waitTicks(30);
		check(context.computeOnClient(minecraft -> ClientState.has(Disability.SCOLIOSIS) && ClientState.has(Disability.SHORT)),
				"client received the synced disabilities");
		context.takeScreenshot("disabilities-01-hud-scoliosis");
		reset(context, server);
	}

	private static void colorBlind(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("disability give @a color_blind");
		context.waitTicks(20);
		context.takeScreenshot("disabilities-02-color-blind");
		reset(context, server);
	}

	private static void prosopagnosia(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("tp @a 0 -60 0 0 0");
		server.runCommand("summon minecraft:zombie 1.5 -60 4 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
		server.runCommand("summon minecraft:pig -1.5 -60 4 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
		server.runCommand("summon minecraft:creeper 0 -60 6 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
		context.waitTicks(20);
		context.takeScreenshot("disabilities-03-before-prosopagnosia");
		server.runCommand("disability give @a prosopagnosia");
		context.waitTicks(20);
		context.takeScreenshot("disabilities-04-prosopagnosia");
		server.runCommand("kill @e[type=!minecraft:player]");
		reset(context, server);
	}

	private static void dyslexia(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("disability give @a dyslexia");
		server.runCommand("say The quick brown fox jumps over the lazy dog");
		context.waitTicks(20);
		context.takeScreenshot("disabilities-05-dyslexia");
		reset(context, server);
	}

	private static void amputations(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("disability give @a amputated_offhand");
		server.runCommand("disability give @a amputated_leg");
		context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(20);
		context.takeScreenshot("disabilities-06-amputations");
		context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.FIRST_PERSON));
		reset(context, server);
	}

	private static void kleptomania(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("gamemode survival @a");
		server.runCommand("item replace entity @a hotbar.0 with minecraft:stone 16");
		context.runOnClient(minecraft -> minecraft.player.getInventory().setSelectedSlot(0));
		context.waitTicks(5);

		// Control: without Kleptomania the drop key works.
		context.getInput().pressKey(options -> options.keyDrop);
		context.waitTicks(5);
		check(server.computeOnServer(minecraft -> player(minecraft).getInventory().getItem(0).getCount()) == 15, "dropping works normally");

		server.runCommand("disability give @a kleptomania");
		context.waitTicks(5);
		context.getInput().pressKey(options -> options.keyDrop);
		context.waitTicks(5);
		check(server.computeOnServer(minecraft -> player(minecraft).getInventory().getItem(0).getCount()) == 15, "kleptomaniacs can't drop items");

		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
		server.runCommand("gamemode creative @a");
		reset(context, server);
	}

	private static void toggleList(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("disability give @a deaf");
		context.waitTicks(5);
		context.getInput().pressKey(InputConstants.KEY_GRAVE);
		context.waitTicks(5);
		check(!context.computeOnClient(minecraft -> ClientState.listVisible()), "the toggle key hides the list");
		context.takeScreenshot("disabilities-07-list-hidden");
		context.getInput().pressKey(InputConstants.KEY_GRAVE);
		context.waitTicks(5);
		check(context.computeOnClient(minecraft -> ClientState.listVisible()), "the toggle key shows the list again");
		reset(context, server);
	}

	private static void parkinsons(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("disability give @a parkinsons");
		context.waitTicks(100);
		check(context.computeOnClient(minecraft -> ClientState.isTrembling()), "the crosshair trembles");
		context.takeScreenshot("disabilities-10-parkinsons");
		reset(context, server);
		context.waitTicks(40);
		check(!context.computeOnClient(minecraft -> ClientState.isTrembling()), "the crosshair settles once cured");
	}

	private static void vertigoAndSeizure(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("tp @a 0 -60 0 0 0");
		server.runCommand("disability give @a vertigo");
		// Spells are random; start one directly so the test is deterministic.
		server.runOnServer(minecraft -> ServerPlayNetworking.send(player(minecraft), new EpisodePayload(EpisodePayload.Kind.VERTIGO, 160, 1)));
		context.waitTicks(30);
		check(context.computeOnClient(minecraft -> ClientState.vertigoTicks() > 0), "a vertigo spell is running");
		context.takeScreenshot("disabilities-11-vertigo");
		server.runOnServer(minecraft -> ServerPlayNetworking.send(player(minecraft), new EpisodePayload(EpisodePayload.Kind.VERTIGO, 0, 1)));
		reset(context, server);

		server.runCommand("tp @a 0 -60 0 0 0");
		context.waitTicks(5);
		float yawBefore = context.computeOnClient(minecraft -> minecraft.player.getYRot());
		server.runOnServer(minecraft -> ServerPlayNetworking.send(player(minecraft), new EpisodePayload(EpisodePayload.Kind.SEIZURE, 80, 0)));
		context.waitTicks(10);
		check(context.computeOnClient(minecraft -> ClientState.isSeizing() && minecraft.player.getYRot() != yawBefore), "a seizure shakes the view");
		context.waitTicks(80);
		check(!context.computeOnClient(minecraft -> ClientState.isSeizing()), "the seizure ends");
		reset(context, server);
	}

	private static void dementiaAndInventory(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("gamemode survival @a");
		server.runCommand("item replace entity @a hotbar.1 with minecraft:stone 32");
		server.runCommand("item replace entity @a hotbar.2 with minecraft:dirt 8");
		server.runCommand("disability give @a insomnia");
		context.runOnClient(minecraft -> minecraft.player.getInventory().setSelectedSlot(0));
		// Shuffles are random; tell the client directly that hotbar slot 1 was forgotten.
		server.runOnServer(minecraft -> ServerPlayNetworking.send(player(minecraft), new HiddenSlotsPayload(1L << 1)));
		context.waitTicks(10);
		check(context.computeOnClient(minecraft -> ClientState.isSlotHidden(minecraft.player.getInventory(), 1)), "hotbar slot 1 is forgotten");
		context.takeScreenshot("disabilities-12-dementia-hotbar");

		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		int scale = context.computeOnClient(minecraft -> minecraft.getWindow().getGuiScale());
		// Hover the "Insomnia" entry of the list to get its description.
		context.getInput().setCursorPos(14 * scale, 21 * scale);
		context.waitTicks(5);
		context.takeScreenshot("disabilities-13-inventory-tooltip");
		context.setScreen(() -> null);

		server.runOnServer(minecraft -> ServerPlayNetworking.send(player(minecraft), new HiddenSlotsPayload(0L)));
		server.runCommand("clear @a");
		server.runCommand("gamemode creative @a");
		reset(context, server);
	}

	private static void schizophrenia(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("tp @a 0 -60 0 0 0");
		server.runCommand("disability give @a schizophrenia");
		int fov = context.computeOnClient(minecraft -> minecraft.options.fov().get());
		context.runOnClient(minecraft -> minecraft.options.fov().set(110));
		context.waitTicks(5);
		// 60 degrees to the right of where we look: inside a wide field of view, but not "looked at".
		context.runOnClient(minecraft -> ShadowFigure.appearAt(5.2, -60.0, 3.0));
		context.waitTicks(3);
		context.takeScreenshot("disabilities-14-schizophrenia");
		context.runOnClient(minecraft -> minecraft.options.fov().set(fov));
		reset(context, server);
	}

	private static void commands(ClientGameTestContext context, TestServerContext server) {
		// The alias works like the full command.
		run(server, "dis give @a vertigo");
		check(disabilities(server).equals(EnumSet.of(Disability.VERTIGO)), "/dis give");
		check(run(server, "disability list @a").stream().anyMatch(line -> line.contains("(1)") && line.contains("Vertigo")), "/disability list");
		check(run(server, "disability give @a vertigo").stream().anyMatch(line -> line.contains("already had")), "giving twice is reported");
		run(server, "disability remove @a vertigo");
		check(disabilities(server).isEmpty(), "/disability remove");
		check(run(server, "disability give @a nonsense").stream().anyMatch(line -> line.contains("Unknown disability")), "unknown names are rejected");

		run(server, "disability give @a random");
		check(disabilities(server).size() == 1, "/disability give random");
		run(server, "disability remove @a random");
		check(disabilities(server).isEmpty(), "/disability remove random");

		run(server, "disability give @a all");
		check(disabilities(server).size() == Disability.values().length, "/disability give all");
		// next uses the timer's mode: replace cures all 30 and leaves one.
		run(server, "disability next @a");
		check(disabilities(server).size() == 1, "/disability next in replace mode");
		run(server, "disability remove @a all");
		check(disabilities(server).isEmpty(), "/disability remove all");

		run(server, "disability timer 3");
		check(run(server, "disability timer").stream().anyMatch(line -> line.contains("Timer is on") && line.contains("3 minutes")
				&& line.contains("replace")), "/disability timer shows the status");
		run(server, "disability timer off");
		check(!server.computeOnServer(minecraft -> DisabilityManager.roster().timerEnabled()), "/disability timer off");
		run(server, "disability timer on");
		check(server.computeOnServer(minecraft -> DisabilityManager.roster().timerEnabled()), "/disability timer on");
		run(server, "disability timer off");
		reset(context, server);
	}

	/** Runs a command as the server console and returns everything it printed. */
	private static List<String> run(TestServerContext server, String command) {
		return server.computeOnServer(minecraft -> {
			List<String> output = new ArrayList<>();
			CommandSource collector = new CommandSource() {
				@Override
				public void sendSystemMessage(Component message) {
					output.add(message.getString());
				}

				@Override
				public boolean acceptsSuccess() {
					return true;
				}

				@Override
				public boolean acceptsFailure() {
					return true;
				}

				@Override
				public boolean shouldInformAdmins() {
					return false;
				}
			};
			minecraft.getCommands().performPrefixedCommand(minecraft.createCommandSourceStack().withSource(collector), command);
			return output;
		});
	}

	/** The new behaviour: the timer cures everything and hands out exactly one new disability. */
	private static void replaceTimer(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("disability timer mode replace");
		server.runCommand("disability give @a deaf");
		server.runCommand("disability give @a mute");
		server.runCommand("disability give @a adhd");
		EnumSet<Disability> before = EnumSet.of(Disability.DEAF, Disability.MUTE, Disability.ADHD);
		// 0.05 minutes = 60 ticks.
		server.runCommand("disability timer 0.05");
		context.waitTicks(80);
		Set<Disability> after = disabilities(server);
		check(after.size() == 1, "replace mode leaves exactly one disability, got " + after);
		check(after.stream().noneMatch(before::contains), "replace mode rolls something new, got " + after);
		context.takeScreenshot("disabilities-08-after-replace-timer");

		// A second cycle swaps it again rather than adding to it.
		context.waitTicks(60);
		Set<Disability> later = disabilities(server);
		check(later.size() == 1, "still exactly one disability after another cycle, got " + later);
		server.runCommand("disability timer off");
		reset(context, server);
	}

	/** The original behaviour is still available. */
	private static void stackTimer(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("disability timer mode stack");
		server.runCommand("disability timer 0.05");
		context.waitTicks(130);
		server.runCommand("disability timer off");
		int count = disabilities(server).size();
		check(count == 2, "stack mode adds one disability per cycle, got " + count);
		server.runCommand("disability timer mode replace");
		reset(context, server);
	}

	private static void lactoseIntolerance(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("gamemode survival @a");
		server.runCommand("disability give @a lactose_intolerance");
		context.waitTicks(5);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			new ItemStack(Items.MILK_BUCKET).finishUsingItem(player.level(), player);
		});
		context.waitTicks(5);
		check(server.computeOnServer(minecraft -> player(minecraft).isDeadOrDying()), "milk is fatal with Lactose Intolerance");
		context.waitForScreen(DeathScreen.class);
		context.waitTicks(30);
		context.takeScreenshot("disabilities-09-lactose-death");
		context.clickScreenButton("deathScreen.respawn");
		context.waitForScreen(null);
		server.runCommand("gamemode creative @a");
		reset(context, server);
	}

	private static void reset(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("disability reset");
		context.waitTicks(10);
		check(context.computeOnClient(minecraft -> ClientState.mine().isEmpty()), "reset cleared the client's disabilities");
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static Set<Disability> disabilities(TestServerContext server) {
		return server.computeOnServer(minecraft -> {
			EnumSet<Disability> copy = EnumSet.noneOf(Disability.class);
			copy.addAll(DisabilityManager.get(player(minecraft).getUUID()));
			return copy;
		});
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
