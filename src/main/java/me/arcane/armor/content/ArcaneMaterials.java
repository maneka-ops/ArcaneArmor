package me.arcane.armor.content;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import org.bukkit.Material;

/**
 * Every crafting material the addon adds.
 *
 * <p>Ordered by progression: dust and essences feed crystals, crystals feed cores and plates, and
 * those feed armor. Each constant is only the item; recipes live in
 * {@link me.arcane.armor.setup.ArcaneSetup} so the material list stays readable as a bill of
 * materials.
 */
public final class ArcaneMaterials {

    private ArcaneMaterials() {
    }

    // ---- Base dusts and crystals -------------------------------------------------------------

    public static final SlimefunItemStack MAGIC_DUST = new SlimefunItemStack(
            "ARCANE_MAGIC_DUST",
            Material.GLOWSTONE_DUST,
            "&dMagic Dust",
            "",
            "&7The residue of a broken enchantment.",
            "&8Compresses into an Arcane Crystal");

    public static final SlimefunItemStack ARCANE_CRYSTAL = new SlimefunItemStack(
            "ARCANE_CRYSTAL",
            Material.AMETHYST_SHARD,
            "&5Arcane Crystal",
            "",
            "&7Nine measures of dust, held in one lattice.",
            "&8Base of every Arcane recipe");

    public static final SlimefunItemStack INFUSED_CRYSTAL = new SlimefunItemStack(
            "ARCANE_INFUSED_CRYSTAL",
            Material.AMETHYST_SHARD,
            "&dInfused Crystal",
            "",
            "&7A crystal that has been taught to hold a charge.",
            "&8Arcane Infuser output");

    public static final SlimefunItemStack CHARGED_CRYSTAL = new SlimefunItemStack(
            "ARCANE_CHARGED_CRYSTAL",
            Material.AMETHYST_CLUSTER,
            "&bCharged Crystal",
            "",
            "&7Humming with stored Arcane Energy.",
            "&8Right click while wearing armor to refill it");

    public static final SlimefunItemStack ENERGY_CRYSTAL = new SlimefunItemStack(
            "ARCANE_ENERGY_CRYSTAL",
            Material.DIAMOND,
            "&bEnergy Crystal",
            "",
            "&7Dense enough to power a machine on its own.");

    public static final SlimefunItemStack PURE_MANA = new SlimefunItemStack(
            "ARCANE_PURE_MANA",
            Material.GHAST_TEAR,
            "&bPure Mana",
            "",
            "&7Mana with nothing left in it but mana.");

    public static final SlimefunItemStack ECHO_CRYSTAL = new SlimefunItemStack(
            "ARCANE_ECHO_CRYSTAL",
            Material.ECHO_SHARD,
            "&3Echo Crystal",
            "",
            "&7Remembers the shape of the spell that made it.",
            "&8Used in rune work");

    // ---- Essences, from the Essence Extractor ------------------------------------------------

    public static final SlimefunItemStack LIGHTNING_ESSENCE = new SlimefunItemStack(
            "ARCANE_LIGHTNING_ESSENCE",
            Material.REDSTONE,
            "&eLightning Essence",
            "",
            "&7Extracted from redstone.",
            "&8Storm set component");

    public static final SlimefunItemStack FIRE_ESSENCE = new SlimefunItemStack(
            "ARCANE_FIRE_ESSENCE",
            Material.BLAZE_POWDER,
            "&cFire Essence",
            "",
            "&7Extracted from a blaze rod.",
            "&8Infernal set component");

    public static final SlimefunItemStack VOID_ESSENCE = new SlimefunItemStack(
            "ARCANE_VOID_ESSENCE",
            Material.ENDER_EYE,
            "&5Void Essence",
            "",
            "&7Extracted from an ender pearl.",
            "&8Void set component");

    public static final SlimefunItemStack NATURE_ESSENCE = new SlimefunItemStack(
            "ARCANE_NATURE_ESSENCE",
            Material.EMERALD,
            "&aNature Essence",
            "",
            "&7Extracted from an emerald.",
            "&8Nature set component");

    public static final SlimefunItemStack WATER_ESSENCE = new SlimefunItemStack(
            "ARCANE_WATER_ESSENCE",
            Material.PRISMARINE_SHARD,
            "&bWater Essence",
            "",
            "&7Extracted from prismarine.");

    // ---- Refined components ------------------------------------------------------------------

    public static final SlimefunItemStack MANA_CORE = new SlimefunItemStack(
            "ARCANE_MANA_CORE",
            Material.HEART_OF_THE_SEA,
            "&bMana Core",
            "",
            "&7The beating centre of a set of armor.",
            "&8One per chestplate");

    public static final SlimefunItemStack RUNIC_PLATE = new SlimefunItemStack(
            "ARCANE_RUNIC_PLATE",
            Material.NETHERITE_SCRAP,
            "&6Runic Plate",
            "",
            "&7Metal that will accept an engraving.");

    public static final SlimefunItemStack ANCIENT_ALLOY = new SlimefunItemStack(
            "ARCANE_ANCIENT_ALLOY",
            Material.NETHERITE_INGOT,
            "&8Ancient Alloy",
            "",
            "&7Older than the deepslate it was found in.");

    public static final SlimefunItemStack SPIRIT_CLOTH = new SlimefunItemStack(
            "ARCANE_SPIRIT_CLOTH",
            Material.PHANTOM_MEMBRANE,
            "&fSpirit Cloth",
            "",
            "&7Weightless, and colder than it looks.");

    public static final SlimefunItemStack MAGIC_THREAD = new SlimefunItemStack(
            "ARCANE_MAGIC_THREAD",
            Material.STRING,
            "&dMagic Thread",
            "",
            "&7Holds a set together at the seams.");

    public static final SlimefunItemStack ARCANE_CATALYST = new SlimefunItemStack(
            "ARCANE_CATALYST",
            Material.BLAZE_POWDER,
            "&6Arcane Catalyst",
            "",
            "&7Makes a reaction happen that would not.",
            "&8Consumed by the Arcane Infuser");

    public static final SlimefunItemStack ARCANE_CIRCUIT = new SlimefunItemStack(
            "ARCANE_CIRCUIT",
            Material.REPEATER,
            "&bArcane Circuit",
            "",
            "&7Routes energy through a module.",
            "&8Every module needs one");
}
