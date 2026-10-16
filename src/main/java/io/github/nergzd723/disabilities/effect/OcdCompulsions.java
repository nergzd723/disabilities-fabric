package io.github.nergzd723.disabilities.effect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.ModDamageTypes;
import io.github.nergzd723.disabilities.ModTags;
import io.github.nergzd723.disabilities.run.DisabilityManager;

/**
 * OCD: every one to three minutes the player gets a compulsion (place, mine, jump or crouch an exact number of
 * times) and 10 seconds to do it. Missing the deadline hurts every second until it is done; doing it too many times,
 * even during the 3 second "satisfied" window afterwards, is punished with 5 hearts of damage.
 */
final class OcdCompulsions {
	private static final int FIRST_TASK_DELAY = 300;
	private static final int TASK_TICKS = 200;
	private static final int GRACE_TICKS = 60;
	private static final int VIOLATED_TICKS = 60;
	private static final int MIN_COOLDOWN = 1200;
	private static final int EXTRA_COOLDOWN = 2400;
	private static final int RETRY_COOLDOWN = 200;
	private static final int DEATH_COOLDOWN = 600;
	private static final int PUNISH_INTERVAL = 20;
	private static final float PUNISH_DAMAGE = 2.0F;
	private static final float OVERDID_DAMAGE = 10.0F;

	private enum Kind {
		PLACE,
		MINE,
		JUMP,
		CROUCH
	}

	private boolean wasActive;
	private int cooldown;
	private int violatedTicks;
	private @Nullable Kind kind;
	private @Nullable Item placeItem;
	private @Nullable Block mineBlock;
	private int target;
	private int baseline;
	private int ticksLeft;
	private int graceTicks;
	private boolean punishing;
	private int punishTimer;
	private int mined;
	private int sneaks;
	private boolean wasSneaking;

	void onBlockBroken(BlockState state) {
		if (mineBlock != null && state.is(mineBlock)) {
			mined++;
		}
	}

	void tick(ServerPlayer player, ServerLevel level, RandomSource random) {
		boolean sneaking = player.isShiftKeyDown();
		if (sneaking && !wasSneaking) {
			sneaks++;
		}
		wasSneaking = sneaking;

		if (violatedTicks > 0) {
			violatedTicks--;
		}
		if (!DisabilityManager.has(player.getUUID(), Disability.OCD)) {
			clearTask();
			violatedTicks = 0;
			wasActive = false;
			return;
		}
		if (!wasActive) {
			wasActive = true;
			cooldown = FIRST_TASK_DELAY;
		}
		if (player.isDeadOrDying()) {
			if (hasTask()) {
				clearTask();
				cooldown = DEATH_COOLDOWN;
			}
			return;
		}

		if (graceTicks > 0) {
			if (progress(player) > target) {
				overdid(player, level, random);
			} else if (--graceTicks == 0) {
				scheduleNext(random);
			}
		} else if (punishing) {
			int done = progress(player);
			if (done > target) {
				overdid(player, level, random);
			} else if (done == target) {
				punishing = false;
				graceTicks = GRACE_TICKS;
			} else if (++punishTimer >= PUNISH_INTERVAL) {
				punishTimer = 0;
				player.hurtServer(level, ModDamageTypes.source(level, ModDamageTypes.OCD), PUNISH_DAMAGE);
			}
		} else if (ticksLeft > 0) {
			int done = progress(player);
			if (done > target) {
				overdid(player, level, random);
			} else if (done == target) {
				ticksLeft = 0;
				graceTicks = GRACE_TICKS;
			} else if (--ticksLeft <= 0) {
				punishing = true;
				punishTimer = 0;
			}
		} else if (cooldown > 0) {
			cooldown--;
		} else if (!assign(player, level, random)) {
			cooldown = RETRY_COOLDOWN;
		}
	}

	/** The line shown above the hotbar, if any. */
	Optional<Component> hudLine(ServerPlayer player) {
		if (violatedTicks > 0) {
			return Optional.of(Component.translatable("hud.disabilities.ocd.violated").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		}
		if (graceTicks > 0) {
			return Optional.of(Component.translatable("hud.disabilities.ocd.satisfied").withStyle(ChatFormatting.GREEN));
		}
		if (!punishing && ticksLeft <= 0) {
			return Optional.empty();
		}

		MutableComponent line = Component.empty()
				.append(Component.translatable("hud.disabilities.ocd.prefix")
						.withStyle(punishing ? ChatFormatting.DARK_RED : ChatFormatting.BLUE, ChatFormatting.BOLD))
				.append(" ")
				.append(describe().withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" — ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(progress(player) + "/" + target).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" — ").withStyle(punishing ? ChatFormatting.DARK_RED : ChatFormatting.GRAY));
		if (punishing) {
			line.append(Component.translatable("hud.disabilities.ocd.taking_damage").withStyle(ChatFormatting.DARK_RED));
		} else {
			line.append(Component.translatable("hud.disabilities.ocd.damage_in", (ticksLeft + 19) / 20).withStyle(ChatFormatting.RED));
		}
		return Optional.of(line);
	}

	private boolean hasTask() {
		return ticksLeft > 0 || graceTicks > 0 || punishing;
	}

	private void clearTask() {
		kind = null;
		placeItem = null;
		mineBlock = null;
		target = 0;
		baseline = 0;
		ticksLeft = 0;
		graceTicks = 0;
		punishing = false;
		punishTimer = 0;
		mined = 0;
	}

	private void scheduleNext(RandomSource random) {
		clearTask();
		cooldown = MIN_COOLDOWN + random.nextInt(EXTRA_COOLDOWN);
	}

	private void overdid(ServerPlayer player, ServerLevel level, RandomSource random) {
		violatedTicks = VIOLATED_TICKS;
		player.hurtServer(level, ModDamageTypes.source(level, ModDamageTypes.OCD_OVERDID), OVERDID_DAMAGE);
		scheduleNext(random);
	}

	private int progress(ServerPlayer player) {
		if (kind == null) {
			return 0;
		}
		return switch (kind) {
			case PLACE -> placeItem == null ? 0 : player.getStats().getValue(Stats.ITEM_USED, placeItem) - baseline;
			case MINE -> mined;
			case JUMP -> player.getStats().getValue(Stats.CUSTOM, Stats.JUMP) - baseline;
			case CROUCH -> sneaks - baseline;
		};
	}

	private MutableComponent describe() {
		if (kind == null) {
			return Component.empty();
		}
		return switch (kind) {
			case PLACE -> Component.translatable("hud.disabilities.ocd.place", target,
					placeItem == null ? Component.empty() : placeItem.getName(placeItem.getDefaultInstance()));
			case MINE -> Component.translatable("hud.disabilities.ocd.mine", target, mineBlock == null ? Component.empty() : mineBlock.getName());
			case JUMP -> Component.translatable("hud.disabilities.ocd.jump", target);
			case CROUCH -> Component.translatable("hud.disabilities.ocd.crouch", target);
		};
	}

	private boolean assign(ServerPlayer player, ServerLevel level, RandomSource random) {
		for (Kind candidate : Util.shuffledCopy(Kind.values(), random)) {
			boolean assigned = switch (candidate) {
				case PLACE -> assignPlace(player, random);
				case MINE -> assignMine(player, level, random);
				case JUMP -> assignJump(player, random);
				case CROUCH -> assignCrouch(random);
			};
			if (assigned) {
				kind = candidate;
				ticksLeft = TASK_TICKS;
				return true;
			}
		}
		return false;
	}

	/** Place 4-8 of a block the player is carrying at least 8 of. */
	private boolean assignPlace(ServerPlayer player, RandomSource random) {
		Map<Item, Integer> counts = new HashMap<>();
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
				counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}
		List<Item> candidates = new ArrayList<>();
		counts.forEach((item, count) -> {
			if (count >= 8) {
				candidates.add(item);
			}
		});
		if (candidates.isEmpty()) {
			return false;
		}
		placeItem = candidates.get(random.nextInt(candidates.size()));
		target = 4 + random.nextInt(Math.min(5, counts.get(placeItem) - 4));
		baseline = player.getStats().getValue(Stats.ITEM_USED, placeItem);
		return true;
	}

	/** Mine 2-4 of a common block that there are at least 6 of nearby. */
	private boolean assignMine(ServerPlayer player, ServerLevel level, RandomSource random) {
		Map<Block, Integer> counts = new HashMap<>();
		BlockPos center = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -3, -5), center.offset(5, 3, 5))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(ModTags.OCD_MINEABLE)) {
				counts.merge(state.getBlock(), 1, Integer::sum);
			}
		}
		List<Block> candidates = new ArrayList<>();
		counts.forEach((block, count) -> {
			if (count >= 6) {
				candidates.add(block);
			}
		});
		if (candidates.isEmpty()) {
			return false;
		}
		mineBlock = candidates.get(random.nextInt(candidates.size()));
		target = 2 + random.nextInt(3);
		mined = 0;
		return true;
	}

	private boolean assignJump(ServerPlayer player, RandomSource random) {
		target = 3 + random.nextInt(4);
		baseline = player.getStats().getValue(Stats.CUSTOM, Stats.JUMP);
		return true;
	}

	private boolean assignCrouch(RandomSource random) {
		target = 3 + random.nextInt(4);
		baseline = sneaks;
		return true;
	}
}
