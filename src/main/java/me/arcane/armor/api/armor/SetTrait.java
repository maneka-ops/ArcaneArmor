package me.arcane.armor.api.armor;

/**
 * Passive behaviours that are not expressible as a potion effect.
 *
 * <p>Traits are declarative: a set lists the traits it grants and the listeners/tasks query the
 * player's active trait set, so adding a new themed set never means touching listener code.
 */
public enum SetTrait {

    /** Immune to lightning damage. */
    LIGHTNING_IMMUNE,
    /** Melee hits arc to a nearby second target. */
    CHAIN_LIGHTNING,
    /** Immune to fire, lava and burning. */
    FIRE_IMMUNE,
    /** Lava is walkable, like frost walker for the nether. */
    LAVA_WALK,
    /** Mined ores drop their smelted result. */
    AUTO_SMELT,
    /** Attackers are slowed and take freeze damage. */
    FREEZE_ATTACKERS,
    /** Water freezes underfoot. */
    SNOW_WALKER,
    /** Immune to poison and wither. */
    POISON_IMMUNE,
    /** Harvesting fully grown crops yields extra drops. */
    CROP_BONUS,
    /** No fall damage. */
    FALL_IMMUNE,
    /** Endermen ignore eye contact. */
    ENDERMAN_PACIFY,
    /** Ender pearls travel further and do not hurt. */
    TELEPORT_RANGE,
    /** Footsteps make no sound and mobs notice the player later. */
    SILENT_STEPS,
    /** Critical hits deal extra damage. */
    CRIT_BONUS,
    /** Knockback taken is heavily reduced. */
    KNOCKBACK_RESIST,
    /** Absorption hearts regenerate out of combat. */
    SHIELD_RECHARGE,
    /** Extra experience from all sources. */
    XP_BONUS,
    /** Extra loot rolls on mob kills. */
    LUCK,
    /** Melee damage dealt heals a fraction back. */
    LIFE_STEAL,
    /** Nearby dropped items are pulled to the player. */
    MAGNET,
    /** Melee hits set the struck target on fire. */
    MOLTEN_EDGE,
    /** A melee hit taken while charged discharges a lightning strike back at the attacker. */
    THUNDER_STEP,
    /** Standing still charges a shell that hard-freezes the next attacker who hits. */
    GLACIAL_SHELL,
    /** Worn armor pieces are kept on death instead of dropping. */
    ARCANE_BOND,
    /** Ender pearls thrown shortly after taking a hit cost nothing. */
    PHASE_WALK,
    /** Arcane Energy refills faster and a banked bubble absorbs part of the next hit. */
    MANA_SPRING
}
