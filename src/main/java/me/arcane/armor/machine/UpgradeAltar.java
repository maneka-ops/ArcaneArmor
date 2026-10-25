package me.arcane.armor.machine;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorSlot;
import me.arcane.armor.api.armor.ArmorTier;
import me.arcane.armor.api.gear.ArcaneGearType;
import me.arcane.armor.api.registry.Registry;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.config.Messages;
import me.arcane.armor.content.armor.ArmorItemFactory;
import me.arcane.armor.content.gear.ArcaneGearFactory;
import me.arcane.armor.core.cosmetic.CosmeticService;
import me.arcane.armor.core.data.ArmorData;
import me.arcane.armor.core.data.ArmorDataService;
import me.arcane.armor.core.data.AscensionRecords;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Raises a piece of armor, or an Arcane weapon or tool, one tier.
 *
 * <p>Upgrading preserves everything the piece already carries — energy, modules, runes — and only
 * moves the tier, then rebuilds the name, lore and attribute modifiers to match. That is what makes
 * a tier climb feel like the same piece getting stronger rather than a replacement.
 *
 * <p>The catalyst cost scales with the destination tier, so the last step is by far the dearest.
 *
 * <p>While a piece sits in the slot and a viewer has the menu open, the progress slot shows what
 * the next step grants — stat multiplier, cooldowns, module slots and the exact catalyst cost —
 * so a player never has to price the upgrade by trial and error. A successful upgrade celebrates
 * on the crafter, and the first player ever to Ascend a piece of a set is announced server-wide.
 */
public class UpgradeAltar extends AbstractArcaneMachine {

    private static final int ARMOR_SLOT = 19;
    private static final int CATALYST_SLOT = 20;
    private static final int[] INPUT_SLOTS = {ARMOR_SLOT, CATALYST_SLOT};
    private static final int[] OUTPUT_SLOTS = {24, 25};

    private final Plugin plugin;
    private final ArmorDataService dataService;
    private final Registry<ArmorSetDefinition> sets;
    private final ArmorItemFactory itemFactory;
    private final ArcaneGearFactory gearFactory;
    private final ArcaneConfig config;
    private final CosmeticService cosmeticService;
    private final AscensionRecords ascensionRecords;
    private final Messages messages;
    private final ItemStack catalyst;

    public UpgradeAltar(
            @Nonnull ItemGroup itemGroup,
            @Nonnull SlimefunItemStack item,
            @Nonnull RecipeType recipeType,
            @Nonnull ItemStack[] recipe,
            @Nonnull ArmorDataService dataService,
            @Nonnull Registry<ArmorSetDefinition> sets,
            @Nonnull ArmorItemFactory itemFactory,
            @Nonnull ArcaneGearFactory gearFactory,
            @Nonnull ArcaneConfig config,
            @Nonnull ItemStack catalyst,
            @Nonnull Plugin plugin,
            @Nonnull CosmeticService cosmeticService,
            @Nonnull AscensionRecords ascensionRecords,
            @Nonnull Messages messages) {
        super(itemGroup, item, recipeType, recipe);
        this.dataService = dataService;
        this.sets = sets;
        this.itemFactory = itemFactory;
        this.gearFactory = gearFactory;
        this.config = config;
        this.cosmeticService = cosmeticService;
        this.ascensionRecords = ascensionRecords;
        this.messages = messages;
        this.catalyst = catalyst;
        this.plugin = plugin;
    }

    @Override
    protected void tick(@Nonnull BlockMenu menu, @Nonnull Block block) {
        ItemStack armor = menu.getItemInSlot(ARMOR_SLOT);
        if (armor == null || armor.getType().isAir()) {
            showAwaitingPiece(menu);
            return;
        }

        Plan plan = planFor(armor);
        if (plan == null) {
            showNoFurtherTier(menu);
            return;
        }

        showPreview(menu, plan);

        ItemStack fuel = menu.getItemInSlot(CATALYST_SLOT);
        boolean consumeMaterials = config.isUpgradeConsumingMaterials();
        int cost = costFor(plan.target());

        if (consumeMaterials
                && (fuel == null || !SlimefunUtils.isItemSimilar(fuel, catalyst, true) || fuel.getAmount() < cost)) {
            return;
        }

        if (!takeCharge(block.getLocation())) {
            return;
        }

        if (!InvUtils.fitAll(menu.toInventory(), new ItemStack[] {plan.upgraded()}, OUTPUT_SLOTS)) {
            return;
        }

        menu.consumeItem(ARMOR_SLOT, 1);
        if (consumeMaterials) {
            menu.consumeItem(CATALYST_SLOT, cost);
        }
        menu.pushItem(plan.upgraded(), OUTPUT_SLOTS);

        celebrate(menu);

        if (plan.target() == ArmorTier.ASCENDED && plan.set() != null) {
            // Counted once per successful upgrade, credited to whichever player the
            // celebration credits — not to every idle viewer of the machine.
            for (HumanEntity viewer : menu.toInventory().getViewers()) {
                if (viewer instanceof Player crafter) {
                    ascensionRecords.recordAscended(crafter.getUniqueId());
                    break;
                }
            }
            maybeAnnounceFirstAscended(menu, plan.set());
        }
    }

    /**
     * Resolves whatever is in the armor slot into a pending upgrade.
     *
     * <p>Armor goes through {@link ArmorData}, gear is matched against the set/gear item grid. The
     * build is done up front so the preview and the consumption commit see exactly the same result.
     */
    @Nullable
    private Plan planFor(@Nonnull ItemStack armor) {
        ArmorData data = dataService.read(armor).orElse(null);
        if (data != null) {
            ArmorTier target = data.tier().next();
            ArmorSetDefinition set = sets.get(data.setId());
            if (target == null || set == null) {
                return null;
            }

            ItemStack upgraded = armor.clone();
            upgraded.setAmount(1);

            ArmorData updated = data.withTier(target);
            dataService.write(upgraded, updated);

            ArmorSlot slot = slotOf(upgraded);
            if (slot != null) {
                itemFactory.refreshPresentation(upgraded, set, slot, updated, config.getDefenseMultiplier());
            }
            return new Plan(upgraded, data.tier(), target, set);
        }

        SlimefunItem slimefunItem = SlimefunItem.getByItem(armor);
        if (slimefunItem == null) {
            return null;
        }

        for (ArmorSetDefinition set : sets.values()) {
            for (ArcaneGearType type : ArcaneGearType.values()) {
                if (!type.getItemId(set.getId()).equals(slimefunItem.getId())) {
                    continue;
                }

                ArmorTier current = gearFactory.readTier(armor, set.getStartingTier());
                ArmorTier target = current.next();
                if (target == null) {
                    return null;
                }

                ItemStack upgraded = armor.clone();
                upgraded.setAmount(1);
                gearFactory.setTier(upgraded, set, type, target);
                return new Plan(upgraded, current, target, set);
            }
        }
        return null;
    }

    /** Pending upgrade like it lives in the slot right now. */
    private record Plan(
            @Nonnull ItemStack upgraded,
            @Nonnull ArmorTier current,
            @Nonnull ArmorTier target,
            @Nonnull ArmorSetDefinition set) {}

    /** Catalysts needed to reach a tier: 4, 8, 16, 24, 40. */
    private static int costFor(@Nonnull ArmorTier target) {
        return switch (target) {
            case TIER_1, TIER_2 -> 4;
            case TIER_3 -> 8;
            case TIER_4 -> 16;
            case TIER_5 -> 24;
            case ASCENDED -> 40;
        };
    }

    // ---- Celebration and milestones -----------------------------------------------------------

    /** Totem burst and level-up sound on whoever is watching the slot. */
    private void celebrate(@Nonnull BlockMenu menu) {
        for (HumanEntity viewer : menu.toInventory().getViewers()) {
            if (viewer instanceof Player crafter) {
                Bukkit.getScheduler().runTask(plugin, () -> cosmeticService.upgrade(crafter));
                break;
            }
        }
    }

    /**
     * Announces the first player ever to bring a piece of this set to Ascended tier.
     *
     * <p>The claim is always taken, so the milestone cannot be won twice even if broadcasts are
     * off at the moment; only the announcement itself is skipped. Like the forge broadcasts, the
     * chat message fires from the main thread because the machines tick async.
     */
    private void maybeAnnounceFirstAscended(@Nonnull BlockMenu menu, @Nonnull ArmorSetDefinition set) {
        for (HumanEntity viewer : menu.toInventory().getViewers()) {
            if (!(viewer instanceof Player crafter)) {
                continue;
            }

            if (!ascensionRecords.claim(set.getId(), crafter.getName())) {
                break;
            }

            if (config.isFirstAscendedBroadcast()) {
                Map<String, String> placeholders = Map.of(
                        "player", crafter.getName(),
                        "set", set.getColor() + set.getDisplayName());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.getServer().sendMessage(messages.get("first-ascended",
                            "&6&lMilestone! &e{player} &7is the first player to Ascend a {set} &7piece!",
                            placeholders));
                    if (config.isSoundsEnabled()) {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
                        }
                    }
                });
            }
            break;
        }
    }

    // ---- Status panel -------------------------------------------------------------------------

    private void showAwaitingPiece(@Nonnull BlockMenu menu) {
        if (!menu.hasViewer()) {
            return;
        }

        menu.replaceExistingItem(getProgressSlot(), CustomItemStack.create(
                Material.BLACK_STAINED_GLASS_PANE,
                ChatColor.GRAY + "Upgrade Altar",
                "",
                ChatColor.DARK_GRAY + "Place an Arcane piece",
                ChatColor.DARK_GRAY + "in the left slot."));
    }

    private void showNoFurtherTier(@Nonnull BlockMenu menu) {
        if (!menu.hasViewer()) {
            return;
        }

        menu.replaceExistingItem(getProgressSlot(), CustomItemStack.create(
                Material.GRAY_DYE,
                ChatColor.GRAY + "Nothing to upgrade",
                "",
                ChatColor.DARK_GRAY + "Only Arcane armor and gear",
                ChatColor.DARK_GRAY + "can climb, and only while",
                ChatColor.DARK_GRAY + "a higher tier exists."));
    }

    /**
     * What the next tier grants, drawn while the piece waits for catalysts or power.
     *
     * <p>The numbers come straight off the destination {@link ArmorTier}, so the guide, the preview
     * and the piece the machine outputs can never disagree.
     */
    private void showPreview(@Nonnull BlockMenu menu, @Nonnull Plan plan) {
        if (!menu.hasViewer()) {
            return;
        }

        ArmorTier current = plan.current();
        ArmorTier target = plan.target();

        menu.replaceExistingItem(getProgressSlot(), CustomItemStack.create(
                Material.NETHER_STAR,
                ChatColor.AQUA + "Upgrade preview",
                "",
                ChatColor.GRAY + "Tier: " + current.getColor() + current.getLabel()
                        + ChatColor.GRAY + " -> " + target.getColor() + target.getLabel(),
                "",
                ChatColor.GRAY + "Stats: " + ChatColor.YELLOW + "x" + formatMultiplier(target.getStatMultiplier()),
                ChatColor.GRAY + "Cooldowns: " + ChatColor.YELLOW
                        + Math.round((1.0D - target.getCooldownMultiplier()) * 100) + "% faster",
                ChatColor.GRAY + "Module slots: " + ChatColor.YELLOW + target.getModuleSlots(),
                ChatColor.GRAY + "Rune slots: " + ChatColor.YELLOW + target.getRuneSlots(),
                "",
                ChatColor.GRAY + "Cost: " + ChatColor.YELLOW + costFor(target)
                        + ChatColor.GRAY + "x Infused Crystal",
                "",
                takeChargeHint()));
    }

    /** A quiet hint line so "why is it not running?" is answered without opening anything else. */
    @Nonnull
    private static String takeChargeHint() {
        return ChatColor.DARK_GRAY + "Waiting: " + ChatColor.GRAY + "catalysts "
                + ChatColor.DARK_GRAY + "or " + ChatColor.GRAY + "power";
    }

    @Nonnull
    private static String formatMultiplier(double value) {
        // Sheet multipliers are always one or two decimals; Locale.ROOT keeps the dot.
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.?0+$", "");
    }

    /** Works the slot out from the item's material, which is enough for both armor bases. */
    @Nullable
    private static ArmorSlot slotOf(@Nonnull ItemStack item) {
        String type = item.getType().name();
        if (type.endsWith("_HELMET")) {
            return ArmorSlot.HELMET;
        }
        if (type.endsWith("_CHESTPLATE")) {
            return ArmorSlot.CHESTPLATE;
        }
        if (type.endsWith("_LEGGINGS")) {
            return ArmorSlot.LEGGINGS;
        }
        if (type.endsWith("_BOOTS")) {
            return ArmorSlot.BOOTS;
        }
        return null;
    }

    @Nonnull
    @Override
    protected int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Nonnull
    @Override
    protected int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }

    @Nonnull
    @Override
    protected ItemStack getProgressBar() {
        return new ItemStack(Material.NETHER_STAR);
    }
}
