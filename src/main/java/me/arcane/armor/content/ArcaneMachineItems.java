package me.arcane.armor.content;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineTier;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineType;
import io.github.thebusybiscuit.slimefun4.utils.LoreBuilder;
import org.bukkit.Material;

/**
 * The machine blocks themselves.
 *
 * <p>Kept apart from {@link ArcaneMaterials} because machines are placed rather than consumed, and
 * because their lore has to advertise power draw, which materials never do.
 */
public final class ArcaneMachineItems {

    private ArcaneMachineItems() {
    }

    public static final SlimefunItemStack ARCANE_FORGE = new SlimefunItemStack(
            "ARCANE_FORGE",
            Material.SMITHING_TABLE,
            "&5Arcane Forge",
            "",
            "&7Forges Arcane armor from",
            "&7infused components.",
            "",
            LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE),
            LoreBuilder.powerBuffer(2048),
            LoreBuilder.powerPerSecond(64));

    public static final SlimefunItemStack ARCANE_INFUSER = new SlimefunItemStack(
            "ARCANE_INFUSER",
            Material.ENCHANTING_TABLE,
            "&dArcane Infuser",
            "",
            "&7Teaches a crystal to hold",
            "&7more than it should.",
            "",
            LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE),
            LoreBuilder.powerBuffer(1024),
            LoreBuilder.powerPerSecond(48));

    public static final SlimefunItemStack ESSENCE_EXTRACTOR = new SlimefunItemStack(
            "ARCANE_ESSENCE_EXTRACTOR",
            Material.BREWING_STAND,
            "&bEssence Extractor",
            "",
            "&7Pulls the magic out of",
            "&7ordinary materials.",
            "",
            LoreBuilder.machine(MachineTier.BASIC, MachineType.MACHINE),
            LoreBuilder.powerBuffer(1024),
            LoreBuilder.powerPerSecond(32));

    public static final SlimefunItemStack ARCANE_COMPRESSOR = new SlimefunItemStack(
            "ARCANE_COMPRESSOR",
            Material.PISTON,
            "&7Arcane Compressor",
            "",
            "&7Presses loose magic dust",
            "&7into solid crystal.",
            "",
            LoreBuilder.machine(MachineTier.BASIC, MachineType.MACHINE),
            LoreBuilder.powerBuffer(1024),
            LoreBuilder.powerPerSecond(32));

    public static final SlimefunItemStack CRYSTAL_CHARGER = new SlimefunItemStack(
            "ARCANE_CRYSTAL_CHARGER",
            Material.LIGHTNING_ROD,
            "&bCrystal Charger",
            "",
            "&7Fills crystals with the",
            "&7energy your armor drinks.",
            "",
            LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE),
            LoreBuilder.powerBuffer(4096),
            LoreBuilder.powerPerSecond(128));

    public static final SlimefunItemStack RUNE_ENGRAVER = new SlimefunItemStack(
            "ARCANE_RUNE_ENGRAVER",
            Material.CHISELED_DEEPSLATE,
            "&3Rune Engraver",
            "",
            "&7Cuts a rune permanently",
            "&7into a piece of armor.",
            "",
            LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE),
            LoreBuilder.powerBuffer(4096),
            LoreBuilder.powerPerSecond(96));

    public static final SlimefunItemStack UPGRADE_ALTAR = new SlimefunItemStack(
            "ARCANE_UPGRADE_ALTAR",
            Material.LODESTONE,
            "&6Upgrade Altar",
            "",
            "&7Raises a piece one tier,",
            "&7keeping everything on it.",
            "",
            LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE),
            LoreBuilder.powerBuffer(8192),
            LoreBuilder.powerPerSecond(192));
}
