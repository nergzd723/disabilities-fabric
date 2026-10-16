package io.github.nergzd723.disabilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/** Damage types defined in {@code data/disabilities/damage_type}. */
public final class ModDamageTypes {
	public static final ResourceKey<DamageType> ASTHMA = key("asthma");
	public static final ResourceKey<DamageType> DIABETES = key("diabetes");
	public static final ResourceKey<DamageType> LACTOSE = key("lactose");
	public static final ResourceKey<DamageType> OCD = key("ocd");
	public static final ResourceKey<DamageType> OCD_OVERDID = key("ocd_overdid");
	public static final ResourceKey<DamageType> SEIZURE = key("seizure");

	private ModDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String path) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, DisabilitiesMod.id(path));
	}

	public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type) {
		return level.damageSources().source(type);
	}
}
