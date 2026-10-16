package io.github.nergzd723.disabilities.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.TimeFormat;
import io.github.nergzd723.disabilities.client.ClientState;
import io.github.nergzd723.disabilities.run.TimerMode;

/**
 * The list of your disabilities in the top-left corner, with the countdown to the next one. Toggled with the
 * "Toggle Disability List" key. Inside the inventory, hovering an entry shows its description.
 */
public final class DisabilityListHud {
	private static final int X = 4;
	private static final int Y = 4;
	private static final int LINE_HEIGHT = 10;
	private static final int TOOLTIP_WIDTH = 180;
	private static final int WHITE = 0xFFFFFFFF;

	private DisabilityListHud() {
	}

	public static void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gui.hud.isHidden() || !ClientState.listVisible()) {
			return;
		}
		// Your own list stays readable with Dyslexia, otherwise there would be no way to know what you have.
		boolean wasExempt = ClientState.dyslexiaExempt();
		ClientState.setDyslexiaExempt(true);
		try {
			draw(graphics, minecraft.font, mouseX, mouseY);
		} finally {
			ClientState.setDyslexiaExempt(wasExempt);
		}
	}

	private static void draw(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
		Set<Disability> active = ClientState.mine();
		// Anosognosia: you can't tell what's wrong with you.
		boolean unaware = ClientState.has(Disability.ANOSOGNOSIA);
		int y = Y;

		MutableComponent title = Component.translatable("hud.disabilities.title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
				.append(Component.literal(" (" + (unaware ? "?" : String.valueOf(active.size())) + ")")
						.withStyle(style -> style.withBold(false).withColor(ChatFormatting.GRAY)));
		graphics.text(font, title, X, y, WHITE);
		y += 12;

		@Nullable Disability hovered = null;
		if (!unaware) {
			for (Disability disability : active) {
				Component line = Component.literal("• ").append(disability.displayName()).withStyle(disability.color());
				graphics.text(font, line, X, y, WHITE);
				if (mouseX >= X && mouseX <= X + font.width(line) && mouseY >= y - 1 && mouseY < y + LINE_HEIGHT - 1) {
					hovered = disability;
				}
				y += LINE_HEIGHT;
			}
		}

		Component footer = footer(active, unaware);
		if (footer != null) {
			graphics.text(font, footer, X, y + 2, WHITE);
		}

		if (hovered != null) {
			List<FormattedCharSequence> lines = new ArrayList<>();
			lines.add(hovered.displayName().getVisualOrderText());
			lines.addAll(font.split(hovered.description().withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH));
			graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
			graphics.extractDeferredElements(mouseX, mouseY, 0.0F);
		}
	}

	private static @Nullable Component footer(Set<Disability> active, boolean unaware) {
		if (!unaware && ClientState.mode() == TimerMode.STACK && active.size() >= Disability.values().length) {
			return Component.translatable("hud.disabilities.all_acquired").withStyle(ChatFormatting.DARK_GRAY);
		}
		if (!ClientState.timerEnabled()) {
			return Component.translatable("hud.disabilities.timer_off").withStyle(ChatFormatting.DARK_GRAY);
		}
		int ticks = ClientState.ticksUntilNext();
		if (ticks < 0) {
			return null;
		}
		return Component.translatable("hud.disabilities.next_in", Component.literal(TimeFormat.clock(ticks)).withStyle(ChatFormatting.WHITE))
				.withStyle(ChatFormatting.GRAY);
	}
}
