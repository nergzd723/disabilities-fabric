package io.github.nergzd723.disabilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Data-driven parts of the disabilities, so datapacks can tweak them. */
public final class ModTags {
	/** Diabetes: eating one of these deals 10 hearts of damage. */
	public static final TagKey<Item> SUGAR_HIGH = item("sugar/high");
	/** Diabetes: 5 hearts. */
	public static final TagKey<Item> SUGAR_MODERATE = item("sugar/moderate");
	/** Diabetes: 2 hearts. */
	public static final TagKey<Item> SUGAR_LOW = item("sugar/low");
	/** Diabetes: half a heart. */
	public static final TagKey<Item> SUGAR_TRACE = item("sugar/trace");
	/** Lactose Intolerance: consuming one of these is fatal (cake is handled separately, it isn't an item). */
	public static final TagKey<Item> CONTAINS_MILK = item("contains_milk");
	/** Pollen Allergy: standing within two blocks of one of these poisons you. */
	public static final TagKey<Block> POLLEN_SOURCES = block("pollen_sources");
	/** OCD: blocks a "mine N blocks" compulsion may ask for. */
	public static final TagKey<Block> OCD_MINEABLE = block("ocd_mineable");

	private ModTags() {
	}

	private static TagKey<Item> item(String path) {
		return TagKey.create(Registries.ITEM, DisabilitiesMod.id(path));
	}

	private static TagKey<Block> block(String path) {
		return TagKey.create(Registries.BLOCK, DisabilitiesMod.id(path));
	}
}
