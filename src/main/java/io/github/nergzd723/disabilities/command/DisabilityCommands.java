package io.github.nergzd723.disabilities.command;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.TimeFormat;
import io.github.nergzd723.disabilities.effect.ServerEffects;
import io.github.nergzd723.disabilities.run.DisabilityManager;
import io.github.nergzd723.disabilities.run.DisabilityRoster;
import io.github.nergzd723.disabilities.run.TimerMode;

/** {@code /disability} (alias {@code /dis}). */
public final class DisabilityCommands {
	private static final String ALL = "all";
	private static final String RANDOM = "random";

	private static final DynamicCommandExceptionType UNKNOWN_DISABILITY = new DynamicCommandExceptionType(
			name -> Component.translatable("commands.disabilities.unknown", name)
	);

	private static final SuggestionProvider<CommandSourceStack> DISABILITY_SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(Stream.concat(Stream.of(ALL, RANDOM), Arrays.stream(Disability.values()).map(Disability::id)), builder);

	private DisabilityCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("disability")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.literal("give")
							.then(Commands.argument("players", EntityArgument.players())
									.then(Commands.argument("disability", StringArgumentType.word())
											.suggests(DISABILITY_SUGGESTIONS)
											.executes(DisabilityCommands::give))))
					.then(Commands.literal("remove")
							.then(Commands.argument("players", EntityArgument.players())
									.then(Commands.argument("disability", StringArgumentType.word())
											.suggests(DISABILITY_SUGGESTIONS)
											.executes(DisabilityCommands::remove))))
					.then(Commands.literal("list")
							.executes(context -> list(context, List.of(context.getSource().getPlayerOrException())))
							.then(Commands.argument("players", EntityArgument.players())
									.executes(context -> list(context, EntityArgument.getPlayers(context, "players")))))
					.then(Commands.literal("next")
							.executes(context -> next(context, context.getSource().getServer().getPlayerList().getPlayers()))
							.then(Commands.argument("players", EntityArgument.players())
									.executes(context -> next(context, EntityArgument.getPlayers(context, "players")))))
					.then(Commands.literal("reset")
							.executes(DisabilityCommands::reset))
					.then(Commands.literal("timer")
							.executes(DisabilityCommands::timerStatus)
							.then(Commands.literal("off")
									.executes(context -> setTimerEnabled(context, false)))
							.then(Commands.literal("on")
									.executes(context -> setTimerEnabled(context, true)))
							.then(Commands.literal("mode")
									.executes(DisabilityCommands::timerStatus)
									.then(Commands.literal(TimerMode.REPLACE.id())
											.executes(context -> setMode(context, TimerMode.REPLACE)))
									.then(Commands.literal(TimerMode.STACK.id())
											.executes(context -> setMode(context, TimerMode.STACK))))
							.then(Commands.argument("minutes", DoubleArgumentType.doubleArg(0.05, 720.0))
									.executes(DisabilityCommands::setInterval))));

			dispatcher.register(Commands.literal("dis")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.redirect(root));
		});
	}

	private static int give(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "players");
		String name = StringArgumentType.getString(context, "disability");

		if (name.equalsIgnoreCase(ALL)) {
			for (ServerPlayer target : targets) {
				DisabilityManager.roster().giveAll(target.getUUID());
			}
			feedback(context, Component.translatable("commands.disabilities.give.all", names(targets)));
			return targets.size();
		}

		if (name.equalsIgnoreCase(RANDOM)) {
			List<Component> given = new ArrayList<>();
			for (ServerPlayer target : targets) {
				DisabilityManager.giveRandom(target)
						.ifPresent(disability -> given.add(Component.translatable("commands.disabilities.player_entry", target.getDisplayName(), disability.displayName())));
			}
			if (given.isEmpty()) {
				feedback(context, Component.translatable("commands.disabilities.give.random.none", names(targets)));
			} else {
				feedback(context, Component.translatable("commands.disabilities.give.random", ComponentUtils.formatList(given, Component.literal(", "))));
			}
			return given.size();
		}

		Disability disability = parse(name);
		int count = 0;
		for (ServerPlayer target : targets) {
			if (DisabilityManager.give(target, disability)) {
				count++;
			}
		}
		if (count == 0) {
			feedback(context, Component.translatable("commands.disabilities.give.already", names(targets), disability.displayName()));
		} else {
			feedback(context, Component.translatable("commands.disabilities.give.single", disability.displayName(), names(targets)));
		}
		return count;
	}

	private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "players");
		String name = StringArgumentType.getString(context, "disability");
		DisabilityRoster roster = DisabilityManager.roster();

		if (name.equalsIgnoreCase(ALL)) {
			for (ServerPlayer target : targets) {
				roster.removeAll(target.getUUID());
			}
			feedback(context, Component.translatable("commands.disabilities.remove.all", names(targets)));
			return targets.size();
		}

		if (name.equalsIgnoreCase(RANDOM)) {
			List<Component> cured = new ArrayList<>();
			for (ServerPlayer target : targets) {
				roster.removeRandom(target.getUUID(), DisabilityManager.random())
						.ifPresent(disability -> cured.add(Component.translatable("commands.disabilities.player_entry", target.getDisplayName(), disability.displayName())));
			}
			if (cured.isEmpty()) {
				feedback(context, Component.translatable("commands.disabilities.remove.random.none", names(targets)));
			} else {
				feedback(context, Component.translatable("commands.disabilities.remove.random", ComponentUtils.formatList(cured, Component.literal(", "))));
			}
			return cured.size();
		}

		Disability disability = parse(name);
		int count = 0;
		for (ServerPlayer target : targets) {
			if (roster.remove(target.getUUID(), disability)) {
				count++;
			}
		}
		if (count == 0) {
			feedback(context, Component.translatable("commands.disabilities.remove.missing", names(targets), disability.displayName()));
		} else {
			feedback(context, Component.translatable("commands.disabilities.remove.single", disability.displayName(), names(targets)));
		}
		return count;
	}

	private static int list(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
		for (ServerPlayer target : targets) {
			Set<Disability> active = DisabilityManager.get(target.getUUID());
			if (active.isEmpty()) {
				feedback(context, Component.translatable("commands.disabilities.list.none", target.getDisplayName()));
			} else {
				Component entries = ComponentUtils.formatList(active, Component.literal(", ").withStyle(ChatFormatting.GRAY), Disability::displayName);
				feedback(context, Component.translatable("commands.disabilities.list.entry", target.getDisplayName(), active.size(), entries));
			}
		}
		return targets.size();
	}

	private static int next(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
		int count = 0;
		for (ServerPlayer target : List.copyOf(targets)) {
			if (DisabilityManager.progress(target).gained().isPresent()) {
				count++;
			}
		}
		if (count == 0) {
			feedback(context, Component.translatable("commands.disabilities.next.none"));
		} else {
			feedback(context, Component.translatable(count == 1 ? "commands.disabilities.next.one" : "commands.disabilities.next.many", count));
		}
		return count;
	}

	private static int reset(CommandContext<CommandSourceStack> context) {
		DisabilityManager.roster().resetAll();
		for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
			ServerEffects.forget(player.getUUID());
		}
		feedback(context, Component.translatable("commands.disabilities.reset"));
		return 1;
	}

	private static int timerStatus(CommandContext<CommandSourceStack> context) {
		DisabilityRoster roster = DisabilityManager.roster();
		Component state = Component.translatable(roster.timerEnabled() ? "commands.disabilities.timer.state.on" : "commands.disabilities.timer.state.off");
		feedback(context, Component.translatable("commands.disabilities.timer.status",
				state,
				minutes(roster.intervalTicks() / (double) DisabilityRoster.TICKS_PER_MINUTE),
				Component.translatable("commands.disabilities.timer.mode." + roster.mode().id() + ".name"),
				TimeFormat.clock(roster.ticksUntilNext())));
		return roster.timerEnabled() ? 1 : 0;
	}

	private static int setTimerEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
		DisabilityManager.roster().setTimerEnabled(enabled);
		feedback(context, Component.translatable(enabled ? "commands.disabilities.timer.on" : "commands.disabilities.timer.off"));
		return 1;
	}

	private static int setMode(CommandContext<CommandSourceStack> context, TimerMode mode) {
		DisabilityManager.roster().setMode(mode);
		feedback(context, Component.translatable("commands.disabilities.timer.mode." + mode.id()));
		return 1;
	}

	private static int setInterval(CommandContext<CommandSourceStack> context) {
		double minutes = DoubleArgumentType.getDouble(context, "minutes");
		DisabilityManager.roster().setIntervalTicks((int) Math.round(minutes * DisabilityRoster.TICKS_PER_MINUTE));
		feedback(context, Component.translatable("commands.disabilities.timer.set", minutes(minutes)));
		return 1;
	}

	private static Disability parse(String name) throws CommandSyntaxException {
		Optional<Disability> disability = Disability.byId(name);
		if (disability.isEmpty()) {
			throw UNKNOWN_DISABILITY.create(name);
		}
		return disability.get();
	}

	private static Component names(Collection<ServerPlayer> players) {
		return ComponentUtils.formatList(players, ServerPlayer::getDisplayName);
	}

	private static Component minutes(double minutes) {
		String shown = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(minutes);
		return Component.translatable(minutes == 1.0 ? "commands.disabilities.minute" : "commands.disabilities.minutes", shown);
	}

	private static void feedback(CommandContext<CommandSourceStack> context, Component message) {
		MutableComponent styled = message.copy().withStyle(ChatFormatting.GRAY);
		context.getSource().sendSuccess(() -> styled, true);
	}
}
