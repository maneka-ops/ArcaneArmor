package me.arcane.armor.setup;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorSlot;
import me.arcane.armor.api.gear.ArcaneGearType;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.api.rune.Rune;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.config.Messages;
import me.arcane.armor.content.ArcaneItemGroups;
import me.arcane.armor.content.ArcaneMachineItems;
import me.arcane.armor.content.ArcaneMaterials;
import me.arcane.armor.content.ArcaneRecipeTypes;
import me.arcane.armor.content.armor.ArcaneArmorPiece;
import me.arcane.armor.content.armor.ArmorItemFactory;
import me.arcane.armor.content.gear.ArcaneGearFactory;
import me.arcane.armor.content.module.ArcaneModules;
import me.arcane.armor.content.rune.ArcaneRunes;
import me.arcane.armor.core.data.ArmorDataService;
import me.arcane.armor.core.cosmetic.CosmeticService;
import me.arcane.armor.core.data.AscensionRecords;
import me.arcane.armor.core.quest.QuestService;
import me.arcane.armor.machine.ArcaneForge;
import me.arcane.armor.machine.EssenceExtractor;
import me.arcane.armor.machine.RuneEngraver;
import me.arcane.armor.machine.SimpleArcaneMachine;
import me.arcane.armor.machine.UpgradeAltar;
import me.arcane.armor.registry.ArcaneRegistries;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Registers every Slimefun item the addon adds, in dependency order.
 *
 * <p>Setup is one class on purpose: the ordering constraints between item groups, recipe types,
 * machines and armor are real, and spreading them across the content classes would hide them.
 */
public final class ArcaneSetup {

    private final SlimefunAddon addon;
    private final ArcaneRegistries registries;
    private final ArmorDataService dataService;
    private final ArmorItemFactory itemFactory;
    private final ArcaneGearFactory gearFactory;
    private final ArcaneConfig config;
    private final QuestService questService;
    private final CosmeticService cosmeticService;
    private final Messages messages;

    /** Set in {@link #registerMachines()}; armor, modules and runes all register recipes into it. */
    private ArcaneForge forge;

    public ArcaneSetup(
            @Nonnull SlimefunAddon addon,
            @Nonnull ArcaneRegistries registries,
            @Nonnull ArmorDataService dataService,
            @Nonnull ArmorItemFactory itemFactory,
            @Nonnull ArcaneGearFactory gearFactory,
            @Nonnull ArcaneConfig config,
            @Nonnull QuestService questService,
            @Nonnull CosmeticService cosmeticService,
            @Nonnull Messages messages) {
        this.addon = addon;
        this.registries = registries;
        this.dataService = dataService;
        this.itemFactory = itemFactory;
        this.gearFactory = gearFactory;
        this.config = config;
        this.questService = questService;
        this.cosmeticService = cosmeticService;
        this.messages = messages;
    }

    /** Runs the whole registration pass. Called once from {@code onEnable}. */
    public void run() {
        ArcaneItemGroups.init(addon.getJavaPlugin());
        ArcaneRecipeTypes.init(addon.getJavaPlugin());

        registerMaterials();
        registerMachines();
        registerModuleItems();
        registerRuneItems();
        registerArmor();
    }

    // ---- Materials ---------------------------------------------------------------------------

    private void registerMaterials() {
        // Entry point into the whole tree: glowstone plus lapis, craftable the moment a player has
        // an Enhanced Crafting Table.
        register(ArcaneMaterials.MAGIC_DUST, RecipeType.ENHANCED_CRAFTING_TABLE, shaped(
                Material.GLOWSTONE_DUST, Material.LAPIS_LAZULI, Material.GLOWSTONE_DUST,
                Material.LAPIS_LAZULI, Material.AMETHYST_SHARD, Material.LAPIS_LAZULI,
                Material.GLOWSTONE_DUST, Material.LAPIS_LAZULI, Material.GLOWSTONE_DUST));

        register(ArcaneMaterials.ARCANE_CRYSTAL, ArcaneRecipeTypes.compressor(),
                filled(ArcaneMaterials.MAGIC_DUST.item(), 9));

        register(ArcaneMaterials.INFUSED_CRYSTAL, ArcaneRecipeTypes.infuser(), new ItemStack[] {
                amount(ArcaneMaterials.ARCANE_CRYSTAL.item(), 2), ArcaneMaterials.ARCANE_CATALYST.item()});

        register(ArcaneMaterials.CHARGED_CRYSTAL, ArcaneRecipeTypes.charger(), new ItemStack[] {
                amount(ArcaneMaterials.INFUSED_CRYSTAL.item(), 2),
                amount(ArcaneMaterials.ENERGY_CRYSTAL.item(), 2)});

        register(ArcaneMaterials.ENERGY_CRYSTAL, ArcaneRecipeTypes.infuser(), new ItemStack[] {
                new ItemStack(Material.DIAMOND, 3), amount(ArcaneMaterials.MAGIC_DUST.item(), 2)});

        register(ArcaneMaterials.PURE_MANA, ArcaneRecipeTypes.infuser(), new ItemStack[] {
                amount(ArcaneMaterials.INFUSED_CRYSTAL.item(), 2), new ItemStack(Material.GHAST_TEAR, 2)});

        register(ArcaneMaterials.ECHO_CRYSTAL, ArcaneRecipeTypes.infuser(), new ItemStack[] {
                new ItemStack(Material.ECHO_SHARD, 3), amount(ArcaneMaterials.ARCANE_CRYSTAL.item(), 2)});

        // Essences: bulk-processed from ordinary materials plus one Slimefun elemental rune, which
        // puts the Ancient Altar on the path to every set. These get consumed in every armor and
        // gear recipe and in the quest deliveries, so their cost is what actually paces a player
        // through the set ladder.
        register(ArcaneMaterials.LIGHTNING_ESSENCE, ArcaneRecipeTypes.extractor(), LIGHTNING_ESSENCE_INPUTS.get());
        register(ArcaneMaterials.FIRE_ESSENCE, ArcaneRecipeTypes.extractor(), FIRE_ESSENCE_INPUTS.get());
        register(ArcaneMaterials.VOID_ESSENCE, ArcaneRecipeTypes.extractor(), VOID_ESSENCE_INPUTS.get());
        register(ArcaneMaterials.NATURE_ESSENCE, ArcaneRecipeTypes.extractor(), NATURE_ESSENCE_INPUTS.get());
        register(ArcaneMaterials.WATER_ESSENCE, ArcaneRecipeTypes.extractor(), WATER_ESSENCE_INPUTS.get());

        register(ArcaneMaterials.ARCANE_CATALYST, RecipeType.ENHANCED_CRAFTING_TABLE, shaped(
                null, Material.BLAZE_POWDER, null,
                Material.BLAZE_POWDER, Material.GLOWSTONE_DUST, Material.BLAZE_POWDER,
                null, Material.BLAZE_POWDER, null));

        register(ArcaneMaterials.MAGIC_THREAD, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                new ItemStack(Material.STRING), ArcaneMaterials.MAGIC_DUST.item(), new ItemStack(Material.STRING),
                new ItemStack(Material.STRING), ArcaneMaterials.MAGIC_DUST.item(), new ItemStack(Material.STRING),
                new ItemStack(Material.STRING), ArcaneMaterials.MAGIC_DUST.item(), new ItemStack(Material.STRING)});

        register(ArcaneMaterials.SPIRIT_CLOTH, ArcaneRecipeTypes.infuser(), new ItemStack[] {
                new ItemStack(Material.PHANTOM_MEMBRANE, 8),
                amount(ArcaneMaterials.MAGIC_THREAD.item(), 2)});

        // Forge-made components. The grid registered here is the same array the Forge matches
        // against, so what the guide draws is literally what the machine accepts.
        register(ArcaneMaterials.RUNIC_PLATE, ArcaneRecipeTypes.forge(), RUNIC_PLATE_GRID.get());
        register(ArcaneMaterials.ANCIENT_ALLOY, ArcaneRecipeTypes.forge(), ANCIENT_ALLOY_GRID.get());
        register(ArcaneMaterials.MANA_CORE, ArcaneRecipeTypes.forge(), MANA_CORE_GRID.get());
        register(ArcaneMaterials.ARCANE_CIRCUIT, ArcaneRecipeTypes.forge(), ARCANE_CIRCUIT_GRID.get());
    }

    // Essence inputs: a stack of the raw material, two runes of the matching element, and a
    // Slimefun catalyst - one per Extractor input slot. The guide and the machine both read them
    // from here, so they cannot disagree.
    private static final java.util.function.Supplier<ItemStack[]> LIGHTNING_ESSENCE_INPUTS = () -> new ItemStack[] {
            new ItemStack(Material.REDSTONE, 48),
            amount(SlimefunItems.LIGHTNING_RUNE.item(), 2),
            amount(SlimefunItems.MAGIC_LUMP_3.item(), 4)};
    private static final java.util.function.Supplier<ItemStack[]> FIRE_ESSENCE_INPUTS = () -> new ItemStack[] {
            new ItemStack(Material.BLAZE_ROD, 12),
            amount(SlimefunItems.FIRE_RUNE.item(), 2),
            amount(SlimefunItems.SULFATE.item(), 8)};
    private static final java.util.function.Supplier<ItemStack[]> VOID_ESSENCE_INPUTS = () -> new ItemStack[] {
            new ItemStack(Material.ENDER_PEARL, 16),
            amount(SlimefunItems.ENDER_RUNE.item(), 2),
            amount(SlimefunItems.ENDER_LUMP_3.item(), 4)};
    private static final java.util.function.Supplier<ItemStack[]> NATURE_ESSENCE_INPUTS = () -> new ItemStack[] {
            new ItemStack(Material.EMERALD, 24),
            amount(SlimefunItems.EARTH_RUNE.item(), 2),
            amount(SlimefunItems.MAGIC_LUMP_2.item(), 8)};
    private static final java.util.function.Supplier<ItemStack[]> WATER_ESSENCE_INPUTS = () -> new ItemStack[] {
            new ItemStack(Material.PRISMARINE_CRYSTALS, 48),
            amount(SlimefunItems.WATER_RUNE.item(), 2),
            amount(SlimefunItems.SYNTHETIC_SAPPHIRE.item(), 2)};

    /** Seconds the Extractor spends on one essence. */
    private static final int ESSENCE_SECONDS = 40;

    // Grids are suppliers because every registration needs its own item instances; handing the same
    // ItemStack objects to both the guide and the machine would let one mutate the other.
    private static final java.util.function.Supplier<ItemStack[]> RUNIC_PLATE_GRID = () -> grid(
            new ItemStack(Material.NETHERITE_SCRAP), new ItemStack(Material.NETHERITE_SCRAP),
            new ItemStack(Material.NETHERITE_SCRAP),
            ArcaneMaterials.ARCANE_CRYSTAL.item(), null, ArcaneMaterials.ARCANE_CRYSTAL.item(),
            null, null, null);

    private static final java.util.function.Supplier<ItemStack[]> ANCIENT_ALLOY_GRID = () -> grid(
            ArcaneMaterials.RUNIC_PLATE.item(), SlimefunItems.REINFORCED_ALLOY_INGOT.item(),
            ArcaneMaterials.RUNIC_PLATE.item(),
            SlimefunItems.REINFORCED_ALLOY_INGOT.item(), ArcaneMaterials.INFUSED_CRYSTAL.item(),
            SlimefunItems.REINFORCED_ALLOY_INGOT.item(),
            SlimefunItems.CARBONADO.item(), ArcaneMaterials.RUNIC_PLATE.item(), SlimefunItems.CARBONADO.item());

    private static final java.util.function.Supplier<ItemStack[]> MANA_CORE_GRID = () -> grid(
            null, ArcaneMaterials.PURE_MANA.item(), null,
            ArcaneMaterials.INFUSED_CRYSTAL.item(), ArcaneMaterials.PURE_MANA.item(),
            ArcaneMaterials.INFUSED_CRYSTAL.item(),
            null, ArcaneMaterials.PURE_MANA.item(), null);

    private static final java.util.function.Supplier<ItemStack[]> ARCANE_CIRCUIT_GRID = () -> grid(
            null, SlimefunItems.ELECTRO_MAGNET.item(), null,
            ArcaneMaterials.INFUSED_CRYSTAL.item(), SlimefunItems.ELECTRO_MAGNET.item(),
            ArcaneMaterials.INFUSED_CRYSTAL.item(),
            null, null, null);

    // ---- Machines ----------------------------------------------------------------------------

    private void registerMachines() {
        forge = new ArcaneForge(
                ArcaneItemGroups.machines(), ArcaneMachineItems.ARCANE_FORGE,
                RecipeType.ENHANCED_CRAFTING_TABLE, bootstrapRecipe(ArcaneMaterials.INFUSED_CRYSTAL.item(), ArcaneMaterials.ARCANE_CRYSTAL.item()),
                questService);
        forge.setCapacity(2048).setConsumption(64).setSpeed(1);
        forge.register(addon);

        SimpleArcaneMachine infuser = new SimpleArcaneMachine(
                ArcaneItemGroups.machines(), ArcaneMachineItems.ARCANE_INFUSER,
                RecipeType.ENHANCED_CRAFTING_TABLE, bootstrapRecipe(ArcaneMaterials.ARCANE_CRYSTAL.item(), ArcaneMaterials.MAGIC_DUST.item()),
                Material.ENCHANTING_TABLE);
        infuser.setCapacity(1024).setConsumption(48).setSpeed(1);
        infuser.register(addon);

        SimpleArcaneMachine extractor = new EssenceExtractor(
                ArcaneItemGroups.machines(), ArcaneMachineItems.ESSENCE_EXTRACTOR,
                RecipeType.ENHANCED_CRAFTING_TABLE, machineRecipe(ArcaneMaterials.MAGIC_DUST.item()));
        extractor.setCapacity(4096).setConsumption(96).setSpeed(1);
        extractor.register(addon);

        SimpleArcaneMachine compressor = new SimpleArcaneMachine(
                ArcaneItemGroups.machines(), ArcaneMachineItems.ARCANE_COMPRESSOR,
                RecipeType.ENHANCED_CRAFTING_TABLE, bootstrapRecipe(new ItemStack(Material.PISTON), ArcaneMaterials.MAGIC_DUST.item()),
                Material.PISTON);
        compressor.setCapacity(1024).setConsumption(32).setSpeed(1);
        compressor.register(addon);

        SimpleArcaneMachine charger = new SimpleArcaneMachine(
                ArcaneItemGroups.machines(), ArcaneMachineItems.CRYSTAL_CHARGER,
                RecipeType.ENHANCED_CRAFTING_TABLE, machineRecipe(ArcaneMaterials.ENERGY_CRYSTAL.item()),
                Material.LIGHTNING_ROD);
        charger.setCapacity(4096).setConsumption(128).setSpeed(1);
        charger.register(addon);

        RuneEngraver engraver = new RuneEngraver(
                ArcaneItemGroups.machines(), ArcaneMachineItems.RUNE_ENGRAVER,
                RecipeType.ENHANCED_CRAFTING_TABLE, machineRecipe(ArcaneMaterials.ECHO_CRYSTAL.item()),
                dataService, registries.runes());
        engraver.setCapacity(4096).setConsumption(96).setSpeed(1);
        engraver.register(addon);

        UpgradeAltar altar = new UpgradeAltar(
                ArcaneItemGroups.machines(), ArcaneMachineItems.UPGRADE_ALTAR,
                RecipeType.ENHANCED_CRAFTING_TABLE, machineRecipe(ArcaneMaterials.MANA_CORE.item()),
                dataService, registries.sets(), itemFactory, gearFactory, config,
                ArcaneMaterials.INFUSED_CRYSTAL.item(), addon.getJavaPlugin(),
                cosmeticService, new AscensionRecords(addon.getJavaPlugin()), messages);
        altar.setCapacity(8192).setConsumption(192).setSpeed(1);
        altar.register(addon);

        registerMachineRecipes(infuser, extractor, compressor, charger);
    }

    /**
     * Teaches each machine the recipes the guide already advertises for it.
     *
     * <p>Timings are in seconds and deliberately long for the deeper materials: the machines are
     * meant to be built in banks, not babysat one item at a time.
     */
    private void registerMachineRecipes(
            @Nonnull SimpleArcaneMachine infuser,
            @Nonnull SimpleArcaneMachine extractor,
            @Nonnull SimpleArcaneMachine compressor,
            @Nonnull SimpleArcaneMachine charger) {

        compressor.registerRecipe(8, amount(ArcaneMaterials.MAGIC_DUST.item(), 9),
                ArcaneMaterials.ARCANE_CRYSTAL.item());

        infuser.registerRecipe(16,
                new ItemStack[] {amount(ArcaneMaterials.ARCANE_CRYSTAL.item(), 2),
                        ArcaneMaterials.ARCANE_CATALYST.item()},
                new ItemStack[] {ArcaneMaterials.INFUSED_CRYSTAL.item()});
        infuser.registerRecipe(14,
                new ItemStack[] {new ItemStack(Material.DIAMOND, 3), amount(ArcaneMaterials.MAGIC_DUST.item(), 2)},
                new ItemStack[] {ArcaneMaterials.ENERGY_CRYSTAL.item()});
        infuser.registerRecipe(20,
                new ItemStack[] {amount(ArcaneMaterials.INFUSED_CRYSTAL.item(), 2),
                        new ItemStack(Material.GHAST_TEAR, 2)},
                new ItemStack[] {ArcaneMaterials.PURE_MANA.item()});
        infuser.registerRecipe(18,
                new ItemStack[] {new ItemStack(Material.ECHO_SHARD, 3), amount(ArcaneMaterials.ARCANE_CRYSTAL.item(), 2)},
                new ItemStack[] {ArcaneMaterials.ECHO_CRYSTAL.item()});
        infuser.registerRecipe(16,
                new ItemStack[] {amount(new ItemStack(Material.PHANTOM_MEMBRANE), 8),
                        amount(ArcaneMaterials.MAGIC_THREAD.item(), 2)},
                new ItemStack[] {ArcaneMaterials.SPIRIT_CLOTH.item()});

        extractor.registerRecipe(ESSENCE_SECONDS, LIGHTNING_ESSENCE_INPUTS.get(),
                new ItemStack[] {ArcaneMaterials.LIGHTNING_ESSENCE.item()});
        extractor.registerRecipe(ESSENCE_SECONDS, FIRE_ESSENCE_INPUTS.get(),
                new ItemStack[] {ArcaneMaterials.FIRE_ESSENCE.item()});
        extractor.registerRecipe(ESSENCE_SECONDS, VOID_ESSENCE_INPUTS.get(),
                new ItemStack[] {ArcaneMaterials.VOID_ESSENCE.item()});
        extractor.registerRecipe(ESSENCE_SECONDS, NATURE_ESSENCE_INPUTS.get(),
                new ItemStack[] {ArcaneMaterials.NATURE_ESSENCE.item()});
        extractor.registerRecipe(ESSENCE_SECONDS, WATER_ESSENCE_INPUTS.get(),
                new ItemStack[] {ArcaneMaterials.WATER_ESSENCE.item()});

        charger.registerRecipe(16,
                new ItemStack[] {amount(ArcaneMaterials.INFUSED_CRYSTAL.item(), 2),
                        amount(ArcaneMaterials.ENERGY_CRYSTAL.item(), 2)},
                new ItemStack[] {ArcaneMaterials.CHARGED_CRYSTAL.item()});

        forge.registerRecipe(14, RUNIC_PLATE_GRID.get(),
                new ItemStack[] {scaled(ArcaneMaterials.RUNIC_PLATE.item(), 4)});
        forge.registerRecipe(20, ANCIENT_ALLOY_GRID.get(),
                new ItemStack[] {ArcaneMaterials.ANCIENT_ALLOY.item()});
        forge.registerRecipe(24, MANA_CORE_GRID.get(),
                new ItemStack[] {ArcaneMaterials.MANA_CORE.item()});
        forge.registerRecipe(18, ARCANE_CIRCUIT_GRID.get(),
                new ItemStack[] {ArcaneMaterials.ARCANE_CIRCUIT.item()});
    }

    // ---- Modules and runes -------------------------------------------------------------------

    private void registerModuleItems() {
        for (ArmorModule module : registries.modules().values()) {
            SlimefunItemStack item = new SlimefunItemStack(
                    module.getItemId(),
                    module.getIcon(),
                    module.getColor() + module.getDisplayName(),
                    "",
                    ChatColor.GRAY + module.getDescription(),
                    "",
                    ChatColor.DARK_GRAY + "Left click this onto a piece of",
                    ChatColor.DARK_GRAY + "Arcane armor to install it.");

            // The module's own icon material sits in the middle, which is what keeps every module
            // recipe distinct under the Forge's shaped matching.
            ItemStack[] recipe = grid(
                    ArcaneMaterials.ARCANE_CIRCUIT.item(), new ItemStack(module.getIcon()),
                    ArcaneMaterials.ARCANE_CIRCUIT.item(),
                    ArcaneMaterials.INFUSED_CRYSTAL.item(), new ItemStack(module.getIcon()),
                    ArcaneMaterials.INFUSED_CRYSTAL.item(),
                    null, ArcaneMaterials.ARCANE_CIRCUIT.item(), null);

            register(item, ArcaneRecipeTypes.forge(), recipe);
            forge.registerRecipe(12, copyOf(recipe), new ItemStack[] {item.item()});
        }
    }

    private void registerRuneItems() {
        for (Rune rune : registries.runes().values()) {
            SlimefunItemStack item = new SlimefunItemStack(
                    rune.getItemId(),
                    rune.getIcon(),
                    rune.getColor() + rune.getDisplayName(),
                    "",
                    ChatColor.GRAY + rune.getDescription(),
                    "",
                    ChatColor.DARK_GRAY + "Engrave at the Rune Engraver",
                    ChatColor.DARK_GRAY + "Permanent once applied");

            ItemStack[] recipe = grid(
                    null, ArcaneMaterials.ECHO_CRYSTAL.item(), null,
                    ArcaneMaterials.RUNIC_PLATE.item(), new ItemStack(rune.getIcon()),
                    ArcaneMaterials.RUNIC_PLATE.item(),
                    null, ArcaneMaterials.ECHO_CRYSTAL.item(), null);

            // Runes are forged like anything else; the Engraver is what applies them to a piece.
            register(item, ArcaneRecipeTypes.forge(), recipe);
            forge.registerRecipe(16, copyOf(recipe), new ItemStack[] {item.item()});
        }
    }

    // ---- Armor -------------------------------------------------------------------------------

    private void registerArmor() {
        for (ArmorSetDefinition set : orderedSets()) {
            for (ArmorSlot slot : ArmorSlot.values()) {
                SlimefunItemStack item = itemFactory.createItem(
                        set, slot, set.getStartingTier(), config.getDefenseMultiplier());

                ItemStack[] recipe = armorRecipe(set, slot);

                new ArcaneArmorPiece(
                        ArcaneItemGroups.armor(),
                        item,
                        ArcaneRecipeTypes.forge(),
                        recipe,
                        set,
                        slot)
                        .register(addon);

                // The Forge is where the guide says armor is made, so it has to actually make it -
                // but only for a player who has finished the set's quest.
                forge.lockBehindQuest(
                        forge.registerRecipe(60 + depthOf(set) * 40, copyOf(recipe), new ItemStack[] {item.item()}),
                        set,
                        slot);
            }

            registerGear(set);
        }
    }

    /** Weapons and tools for one set, chained to the set below it exactly like the armor. */
    private void registerGear(@Nonnull ArmorSetDefinition set) {
        for (ArcaneGearType type : ArcaneGearType.values()) {
            SlimefunItemStack item = gearFactory.createItem(set, type, set.getStartingTier());
            ItemStack[] recipe = gearRecipe(set, type);

            new SlimefunItem(ArcaneItemGroups.gear(), item, ArcaneRecipeTypes.forge(), recipe)
                    .register(addon);

            forge.registerRecipe(45 + depthOf(set) * 25, copyOf(recipe), new ItemStack[] {item.item()});
        }
    }

    /**
     * Sets ordered so a set is always registered after the one it upgrades from.
     *
     * <p>The registry is a hash map and hands back its values in no particular order, but the
     * recipes reference the previous set's items by id — those have to exist first. Anything whose
     * parent is missing is appended at the end and simply loses its chain ingredient.
     */
    @Nonnull
    private List<ArmorSetDefinition> orderedSets() {
        List<ArmorSetDefinition> remaining = new ArrayList<>(registries.sets().values());
        List<ArmorSetDefinition> ordered = new ArrayList<>(remaining.size());
        Set<String> placed = new HashSet<>();

        boolean progress = true;
        while (progress && !remaining.isEmpty()) {
            progress = false;
            for (Iterator<ArmorSetDefinition> it = remaining.iterator(); it.hasNext();) {
                ArmorSetDefinition set = it.next();
                String parent = set.getPreviousSetId();
                if (parent == null || placed.contains(parent) || !registries.sets().contains(parent)) {
                    ordered.add(set);
                    placed.add(set.getId());
                    it.remove();
                    progress = true;
                }
            }
        }

        // Anything left is part of a cycle; register it anyway rather than dropping content.
        ordered.addAll(remaining);
        return ordered;
    }

    /** How many sets sit below this one in the chain; drives cost and craft time. */
    private int depthOf(@Nonnull ArmorSetDefinition set) {
        int depth = 0;
        ArmorSetDefinition current = set;
        while (current != null && current.getPreviousSetId() != null && depth < 16) {
            current = registries.sets().get(current.getPreviousSetId());
            depth++;
        }
        return depth;
    }

    /**
     * The matching piece of the set below, or {@code null} for an entry set.
     *
     * <p>Resolved through Slimefun rather than kept in a field because the previous set's items were
     * registered in an earlier pass of the same loop.
     */
    @Nullable
    private ItemStack previousPiece(@Nonnull ArmorSetDefinition set, @Nonnull ArmorSlot slot) {
        ArmorSetDefinition previous = set.getPreviousSetId() == null
                ? null
                : registries.sets().get(set.getPreviousSetId());
        if (previous == null) {
            return null;
        }

        SlimefunItem item = SlimefunItem.getById(previous.getItemId(slot));
        return item == null ? null : item.getItem();
    }

    @Nullable
    private ItemStack previousGear(@Nonnull ArmorSetDefinition set, @Nonnull ArcaneGearType type) {
        ArmorSetDefinition previous = set.getPreviousSetId() == null
                ? null
                : registries.sets().get(set.getPreviousSetId());
        if (previous == null) {
            return null;
        }

        SlimefunItem item = SlimefunItem.getById(type.getItemId(previous.getId()));
        return item == null ? null : item.getItem();
    }

    /**
     * Recipe grid for a piece: the vanilla armor shape, in Arcane materials, with the set's theme
     * material marking which set comes out.
     */
    @Nonnull
    /**
     * Armor recipe for one piece, built around the matching piece of the set below it.
     *
     * <p>Ingredient counts and the grade of the core both climb with the set's depth in the chain,
     * so the last armor costs every set before it plus the most expensive components in the addon.
     */
    private ItemStack[] armorRecipe(@Nonnull ArmorSetDefinition set, @Nonnull ArmorSlot slot) {
        int depth = depthOf(set);

        ItemStack plate = scaled(ArcaneMaterials.RUNIC_PLATE.item(), 3 + depth * 3);
        ItemStack cloth = scaled(ArcaneMaterials.SPIRIT_CLOTH.item(), 3 + depth * 3);
        ItemStack theme = new ItemStack(set.getThemeMaterial(), Math.min(64, 2 + depth * 3));
        ItemStack core = scaled(coreFor(depth), 1 + depth);

        // The chain ingredient. Entry sets fall back to a crystal so the grid keeps its shape.
        ItemStack previous = previousPiece(set, slot);
        ItemStack heart = previous != null ? previous : ArcaneMaterials.ARCANE_CRYSTAL.item();

        ItemStack[] recipe;
        if (depth == 0) {
            // The entry set stays cheap: no Slimefun end-game alloys, no runes.
            recipe = switch (slot) {
                case HELMET -> grid(
                        plate, theme, plate,
                        plate, heart, plate,
                        null, core, null);
                case CHESTPLATE -> grid(
                        plate, core, plate,
                        plate, heart, plate,
                        plate, theme, plate);
                case LEGGINGS -> grid(
                        plate, theme, plate,
                        plate, heart, plate,
                        cloth, core, cloth);
                case BOOTS -> grid(
                        null, core, null,
                        cloth, heart, cloth,
                        plate, theme, plate);
            };
        } else {
            // Every set past the entry set is reinforced with real Slimefun end-game alloys
            // (Reinforced Alloy, climbing to Carbonado and Blistering Ingot) and engraved with an
            // Ancient Rune matching the set's element, on top of everything the base recipe needs.
            ItemStack reinforcement = scaled(reinforcementFor(depth), 1 + depth);
            ItemStack rune = runeFor(set);

            recipe = switch (slot) {
                case HELMET -> grid(
                        plate, theme, plate,
                        reinforcement, heart, reinforcement,
                        rune, core, null);
                case CHESTPLATE -> grid(
                        plate, core, plate,
                        reinforcement, heart, reinforcement,
                        plate, theme, rune);
                case LEGGINGS -> grid(
                        rune, theme, plate,
                        reinforcement, heart, reinforcement,
                        cloth, core, cloth);
                case BOOTS -> grid(
                        rune, core, null,
                        cloth, heart, cloth,
                        plate, theme, plate);
            };
        }

        return fillGaps(recipe, set, depth);
    }

    /**
     * Reinforcement ingredient for a set's depth, drawn straight from Slimefun's own end-game
     * alloys rather than anything this addon makes itself.
     *
     * <p>Reinforced Alloy carries the early upgrade sets; Frost demands Carbonado and Void demands
     * a fully-refined Blistering Ingot, so the last two sets cost some of the hardest materials
     * Slimefun has.
     */
    @Nonnull
    private static ItemStack reinforcementFor(int depth) {
        return switch (Math.min(depth, 4)) {
            case 1, 2 -> SlimefunItems.REINFORCED_ALLOY_INGOT.item();
            case 3 -> SlimefunItems.CARBONADO.item();
            default -> SlimefunItems.BLISTERING_INGOT_3.item();
        };
    }

    /** Ancient Rune tied to a set's element; the default keeps API-registered sets craftable. */
    @Nonnull
    private static ItemStack runeFor(@Nonnull ArmorSetDefinition set) {
        return switch (set.getId()) {
            case "storm" -> SlimefunItems.LIGHTNING_RUNE.item();
            case "infernal" -> SlimefunItems.FIRE_RUNE.item();
            case "frost" -> SlimefunItems.WATER_RUNE.item();
            case "void" -> SlimefunItems.ENDER_RUNE.item();
            default -> SlimefunItems.BLANK_RUNE.item();
        };
    }

    /**
     * Fills every empty slot of a recipe with the set's own essence.
     *
     * <p>No Arcane recipe leaves a gap any more: all nine slots are paid for. The filler is the
     * essence matching the set's theme, which forces a player through the Essence Extractor chain
     * for each set rather than letting one stockpile of crystals cover the whole ladder.
     */
    @Nonnull
    private static ItemStack[] fillGaps(
            @Nonnull ItemStack[] recipe, @Nonnull ArmorSetDefinition set, int depth) {

        ItemStack filler = scaled(essenceFor(set), 2 + depth * 2);

        for (int index = 0; index < recipe.length; index++) {
            if (recipe[index] == null) {
                recipe[index] = filler.clone();
            }
        }
        return recipe;
    }

    /** Essence tied to a set's theme; the default keeps API-registered sets craftable. */
    @Nonnull
    private static ItemStack essenceFor(@Nonnull ArmorSetDefinition set) {
        return switch (set.getId()) {
            case "storm" -> ArcaneMaterials.LIGHTNING_ESSENCE.item();
            case "infernal" -> ArcaneMaterials.FIRE_ESSENCE.item();
            case "frost" -> ArcaneMaterials.WATER_ESSENCE.item();
            case "void" -> ArcaneMaterials.VOID_ESSENCE.item();
            case "apprentice" -> ArcaneMaterials.NATURE_ESSENCE.item();
            default -> ArcaneMaterials.ARCANE_CRYSTAL.item();
        };
    }

    /** Weapon and tool recipes, chained the same way the armor is. */
    private ItemStack[] gearRecipe(@Nonnull ArmorSetDefinition set, @Nonnull ArcaneGearType type) {
        int depth = depthOf(set);

        ItemStack plate = scaled(ArcaneMaterials.RUNIC_PLATE.item(), 2 + depth * 3);
        ItemStack alloy = scaled(ArcaneMaterials.ANCIENT_ALLOY.item(), 2 + depth * 3);
        ItemStack theme = new ItemStack(set.getThemeMaterial(), Math.min(64, 2 + depth * 3));
        ItemStack core = scaled(coreFor(depth), 1 + depth);
        ItemStack thread = scaled(ArcaneMaterials.MAGIC_THREAD.item(), 3 + depth * 3);

        ItemStack previous = previousGear(set, type);
        ItemStack heart = previous != null ? previous : ArcaneMaterials.ARCANE_CRYSTAL.item();

        ItemStack[] recipe = switch (type) {
            case SWORD -> grid(
                    null, theme, null,
                    null, heart, null,
                    alloy, core, alloy);
            case AXE -> grid(
                    theme, alloy, null,
                    heart, core, null,
                    null, plate, null);
            case PICKAXE -> grid(
                    alloy, theme, alloy,
                    null, heart, null,
                    null, core, null);
            case SHIELD -> grid(
                    plate, heart, plate,
                    plate, core, plate,
                    null, theme, null);
            case BOW -> grid(
                    thread, theme, null,
                    heart, null, thread,
                    core, alloy, null);
        };

        // Past the entry set, every weapon and tool is engraved with the set's Ancient Rune too,
        // exactly like its armor - Ancient Alloy already costs Carbonado, so this is the second
        // real Slimefun gate on gear rather than just the addon's own material chain.
        if (depth > 0) {
            setFirstEmpty(recipe, runeFor(set));
        }

        return fillGaps(recipe, set, depth);
    }

    /** Places an ingredient in the first open grid slot; leaves the rest for {@link #fillGaps}. */
    private static void setFirstEmpty(@Nonnull ItemStack[] recipe, @Nonnull ItemStack ingredient) {
        for (int index = 0; index < recipe.length; index++) {
            if (recipe[index] == null) {
                recipe[index] = ingredient;
                return;
            }
        }
    }

    /** Core component for a given depth; later sets demand the rarer ones. */
    @Nonnull
    private static ItemStack coreFor(int depth) {
        return switch (Math.min(depth, 4)) {
            case 0 -> ArcaneMaterials.MANA_CORE.item();
            case 1 -> ArcaneMaterials.ARCANE_CIRCUIT.item();
            case 2 -> ArcaneMaterials.ANCIENT_ALLOY.item();
            case 3 -> ArcaneMaterials.CHARGED_CRYSTAL.item();
            default -> ArcaneMaterials.PURE_MANA.item();
        };
    }

    @Nonnull
    private static ItemStack scaled(@Nonnull ItemStack item, int amount) {
        ItemStack copy = item.clone();
        copy.setAmount(Math.max(1, Math.min(64, amount)));
        return copy;
    }

    // ---- Helpers -----------------------------------------------------------------------------

    private void register(
            @Nonnull SlimefunItemStack item,
            @Nonnull RecipeType recipeType,
            @Nonnull ItemStack[] recipe) {
        new SlimefunItem(ArcaneItemGroups.materials(), item, recipeType, recipe).register(addon);
    }

    /** Nine-slot grid from materials, with {@code null} for empty slots. */
    @Nonnull
    private static ItemStack[] shaped(Material... materials) {
        ItemStack[] recipe = new ItemStack[9];
        for (int index = 0; index < materials.length && index < 9; index++) {
            recipe[index] = materials[index] == null ? null : new ItemStack(materials[index]);
        }
        return recipe;
    }

    /** Nine-slot grid, padded with {@code null} for the empty positions. */
    @Nonnull
    private static ItemStack[] grid(ItemStack... items) {
        ItemStack[] recipe = new ItemStack[9];
        System.arraycopy(items, 0, recipe, 0, Math.min(items.length, 9));
        return recipe;
    }

    /** Deep copy, so the guide's grid and the machine's grid never share item instances. */
    @Nonnull
    private static ItemStack[] copyOf(@Nonnull ItemStack[] recipe) {
        ItemStack[] copy = new ItemStack[recipe.length];
        for (int index = 0; index < recipe.length; index++) {
            copy[index] = recipe[index] == null ? null : recipe[index].clone();
        }
        return copy;
    }

    /** Nine copies of one ingredient, for compressor-style recipes. */
    @Nonnull
    private static ItemStack[] filled(@Nonnull ItemStack ingredient, int count) {
        List<ItemStack> recipe = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            recipe.add(ingredient.clone());
        }
        while (recipe.size() < 9) {
            recipe.add(null);
        }
        return recipe.toArray(new ItemStack[0]);
    }

    /**
     * Crafting grid for a machine block: a ring of the given part, doubled up, around an Electric
     * Motor. Doubling the part cost means the machine pipeline itself takes real investment to
     * stand up, not just the recipes it later unlocks.
     */
    @Nonnull
    private static ItemStack[] machineRecipe(@Nonnull ItemStack part) {
        return bootstrapRecipe(part, ArcaneMaterials.ARCANE_CRYSTAL.item(), ArcaneMaterials.ARCANE_CIRCUIT.item());
    }

    /**
     * Same layout as {@link #machineRecipe}, for the three machines that have to exist before an
     * Arcane Crystal or an Arcane Circuit can be made at all.
     *
     * <p>The Compressor makes the first crystal, the Infuser the first Infused Crystal, and the Forge
     * the first circuit, so none of the three may ask for its own output - or for anything further
     * down the chain. They take an Electro Magnet, a plain Slimefun part, where the circuit goes,
     * and the given accent where the crystal goes. Build order: Compressor, Infuser, Forge.
     */
    @Nonnull
    private static ItemStack[] bootstrapRecipe(@Nonnull ItemStack part, @Nonnull ItemStack accent) {
        return bootstrapRecipe(part, accent, SlimefunItems.ELECTRO_MAGNET.item());
    }

    @Nonnull
    private static ItemStack[] bootstrapRecipe(
            @Nonnull ItemStack part, @Nonnull ItemStack accent, @Nonnull ItemStack circuit) {
        ItemStack doubled = amount(part, Math.max(1, part.getAmount() * 2));
        return new ItemStack[] {
                doubled.clone(), amount(accent, 2), doubled.clone(),
                circuit.clone(), SlimefunItems.ELECTRIC_MOTOR.item(), circuit.clone(),
                doubled.clone(), amount(accent, 2), doubled.clone()};
    }

    @Nonnull
    private static ItemStack amount(@Nonnull ItemStack item, int amount) {
        ItemStack copy = item.clone();
        copy.setAmount(amount);
        return copy;
    }
}
