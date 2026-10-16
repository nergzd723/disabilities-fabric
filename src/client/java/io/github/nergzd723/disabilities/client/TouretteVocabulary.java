package io.github.nergzd723.disabilities.client;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/** Tourette's: the random words you blurt out are names of blocks, mobs, effects, biomes, enchantments and items. */
final class TouretteVocabulary {
	private static final Set<EntityType<?>> NOT_MOBS = Set.of(EntityTypes.PLAYER, EntityTypes.ARMOR_STAND, EntityTypes.MANNEQUIN);

	private TouretteVocabulary() {
	}

	static String randomWord(Minecraft minecraft, RandomSource random) {
		return switch (random.nextInt(6)) {
			case 0 -> pick(BuiltInRegistries.BLOCK, block -> true, block -> block.getName(), random);
			case 1 -> pick(BuiltInRegistries.ENTITY_TYPE, type -> DefaultAttributes.hasSupplier(type) && !NOT_MOBS.contains(type),
					EntityType::getDescription, random);
			case 2 -> pick(BuiltInRegistries.MOB_EFFECT, effect -> true, effect -> effect.getDisplayName(), random);
			case 3 -> minecraft.level == null ? item(random) : minecraft.level.registryAccess().lookup(Registries.BIOME)
					.map(biomes -> pick(biomes, biome -> true,
							biome -> Component.translatable(biomes.getKey(biome).toLanguageKey("biome")), random))
					.orElseGet(() -> item(random));
			case 4 -> minecraft.level == null ? item(random) : minecraft.level.registryAccess().lookup(Registries.ENCHANTMENT)
					.map(enchantments -> pick(enchantments, enchantment -> true, enchantment -> enchantment.description(), random))
					.orElseGet(() -> item(random));
			default -> item(random);
		};
	}

	private static String item(RandomSource random) {
		return pick(BuiltInRegistries.ITEM, item -> true, item -> item.getName(item.getDefaultInstance()), random);
	}

	private static <T> String pick(Registry<T> registry, Predicate<T> filter, Function<T, Component> name, RandomSource random) {
		List<T> pool = registry.stream().filter(filter).toList();
		if (pool.isEmpty()) {
			return "";
		}
		T entry = pool.get(random.nextInt(pool.size()));
		Identifier id = registry.getKey(entry);
		return clean(name.apply(entry), id == null ? "" : id.getPath());
	}

	/** Untranslated names fall back to a prettified id, and everything gets a capital first letter. */
	private static String clean(Component name, String path) {
		String text = name.getContents() instanceof TranslatableContents translatable && !Language.getInstance().has(translatable.getKey())
				? fromPath(path)
				: name.getString();
		String stripped = ChatFormatting.stripFormatting(text);
		text = stripped == null ? "" : stripped.trim();
		return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
	}

	private static String fromPath(String path) {
		StringBuilder out = new StringBuilder();
		for (String word : path.split("[_/]")) {
			if (word.isEmpty()) {
				continue;
			}
			if (!out.isEmpty()) {
				out.append(' ');
			}
			out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
		}
		return out.toString();
	}
}
