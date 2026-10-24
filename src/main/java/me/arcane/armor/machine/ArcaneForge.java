package me.arcane.armor.machine;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorSlot;
import me.arcane.armor.core.quest.QuestService;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The Arcane Forge: a 3x3 shaped crafter for armor pieces and modules.
 *
 * <p>Armor needs shaped matching, not the shapeless matching the other machines use. A helmet and a
 * pair of leggings share the same ingredients and differ only in where they sit in the grid, so a
 * shapeless forge would hand out whichever recipe it happened to check first. Matching by position
 * makes each piece unambiguous and lets the guide's recipe layout mean exactly what it shows.
 *
 * <p>Armor recipes are also gated by quests. The Forge remembers who placed it, and will only forge
 * a set once that player has finished the set's quest; until then the grid is left untouched and
 * the progress slot says what is missing. Forges placed before quests existed are claimed by the
 * first player to open them.
 */
public class ArcaneForge extends AbstractArcaneMachine {

    private static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] OUTPUT_SLOTS = {24, 25};
    private static final int PROGRESS_SLOT = 23;

    /** Record linking a recipe to its armor set and slot. */
    public record LockedPiece(@Nonnull ArmorSetDefinition set, @Nullable ArmorSlot slot) {}

    /** BlockStorage key holding the UUID of the player who claimed this Forge. */
    private static final String OWNER_KEY = "arcane-owner";

    /** BlockStorage key holding the UUID of the player who last interacted with this Forge. */
    private static final String CRAFTER_KEY = "arcane-crafter";

    private final QuestService questService;

    /** Recipe to the set and slot whose quest unlocks it. Filled during setup, read from the async ticker. */
    private final Map<MachineRecipe, LockedPiece> questLocks = new ConcurrentHashMap<>();

    public ArcaneForge(
            @Nonnull ItemGroup itemGroup,
            @Nonnull SlimefunItemStack item,
            @Nonnull RecipeType recipeType,
            @Nonnull ItemStack[] recipe,
            @Nonnull QuestService questService) {
        super(itemGroup, item, recipeType, recipe);
        this.questService = questService;
    }

    /** Makes a registered recipe require the given set's quest and optional slot limit. */
    public void lockBehindQuest(@Nonnull MachineRecipe recipe, @Nonnull ArmorSetDefinition set, @Nullable ArmorSlot slot) {
        questLocks.put(recipe, new LockedPiece(set, slot));
    }

    /** Backward compatibility: locks a recipe behind a set's quest without a specific slot limit. */
    public void lockBehindQuest(@Nonnull MachineRecipe recipe, @Nonnull ArmorSetDefinition set) {
        lockBehindQuest(recipe, set, null);
    }

    @Override
    public void preRegister() {
        super.preRegister();

        addItemHandler(new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(@Nonnull BlockPlaceEvent event) {
                BlockStorage.addBlockInfo(event.getBlock(), OWNER_KEY, event.getPlayer().getUniqueId().toString());
                BlockStorage.addBlockInfo(event.getBlock(), CRAFTER_KEY, event.getPlayer().getUniqueId().toString());
            }
        });
    }

    @Override
    protected void onMenuCreated(@Nonnull BlockMenu menu, @Nonnull Block block) {
        menu.addMenuOpeningHandler(player -> {
            BlockStorage.addBlockInfo(block, CRAFTER_KEY, player.getUniqueId().toString());
            if (BlockStorage.getLocationInfo(block.getLocation(), OWNER_KEY) == null) {
                BlockStorage.addBlockInfo(block, OWNER_KEY, player.getUniqueId().toString());
                player.sendMessage(Component.text(
                        "You claimed this Arcane Forge. It forges the armor your quests have unlocked.",
                        NamedTextColor.LIGHT_PURPLE));
            }
        });
    }

    @Override
    protected void tick(@Nonnull BlockMenu menu, @Nonnull Block block) {
        if (!questLocks.isEmpty() && getMachineProcessor().getOperation(block) == null) {
            ItemStack[] grid = readGrid(menu);
            MachineRecipe recipe = grid == null ? null : firstMatch(grid);
            LockedPiece locked = recipe == null ? null : questLocks.get(recipe);

            if (locked != null) {
                UUID crafter = getCrafter(menu, block);
                ArmorSetDefinition set = locked.set();
                ArmorSlot slot = locked.slot();

                // 1. Crafter must have completed the set's quest
                if (!isUnlockedFor(crafter, set)) {
                    showLocked(menu, block, set, crafter);
                    return;
                }

                // 2. Crafter can only craft one armor set per quest (1 piece of each slot)
                if (crafter != null && slot != null && questService.hasCraftedArmor(crafter, set.getId(), slot)) {
                    showLimitReached(menu, set, slot);
                    return;
                }
            }
        }

        super.tick(menu, block);
    }

    private boolean isUnlockedFor(@Nullable UUID crafter, @Nonnull ArmorSetDefinition set) {
        if (!questService.isEnabled() || questService.getQuest(set.getId()) == null) {
            return true;
        }
        return crafter != null && questService.isUnlocked(crafter, set.getId());
    }

    @Nullable
    private UUID getCrafter(@Nonnull BlockMenu menu, @Nonnull Block block) {
        List<HumanEntity> viewers = menu.toInventory().getViewers();
        if (!viewers.isEmpty() && viewers.get(0) instanceof Player player) {
            UUID id = player.getUniqueId();
            BlockStorage.addBlockInfo(block, CRAFTER_KEY, id.toString());
            return id;
        }

        String raw = BlockStorage.getLocationInfo(block.getLocation(), CRAFTER_KEY);
        if (raw != null) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {}
        }
        return ownerOf(block);
    }

    @Nullable
    private static UUID ownerOf(@Nonnull Block block) {
        String raw = BlockStorage.getLocationInfo(block.getLocation(), OWNER_KEY);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void showLocked(
            @Nonnull BlockMenu menu,
            @Nonnull Block block,
            @Nonnull ArmorSetDefinition set,
            @Nullable UUID crafter) {
        if (!menu.hasViewer()) {
            return;
        }

        String name = set.getColor() + set.getDisplayName();
        menu.replaceExistingItem(getProgressSlot(), CustomItemStack.create(
                Material.BARRIER,
                ChatColor.RED + "Locked: " + name + " Armor",
                "",
                ChatColor.GRAY + "You have not finished the",
                ChatColor.GRAY + name + ChatColor.GRAY + " armor quest.",
                "",
                ChatColor.DARK_GRAY + "See progress with " + ChatColor.LIGHT_PURPLE + "/arcane quests"));
    }

    private void showLimitReached(
            @Nonnull BlockMenu menu,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArmorSlot slot) {
        if (!menu.hasViewer()) {
            return;
        }

        String name = set.getColor() + set.getDisplayName() + " " + slot.getDisplayName();
        menu.replaceExistingItem(getProgressSlot(), CustomItemStack.create(
                Material.BARRIER,
                ChatColor.RED + "Limit Reached: " + name,
                "",
                ChatColor.GRAY + "You already forged this piece!",
                ChatColor.GRAY + "Each player may only create",
                ChatColor.GRAY + "one set of " + set.getColor() + set.getDisplayName() + ChatColor.GRAY + " armor.",
                "",
                ChatColor.DARK_GRAY + "Armor cannot be crafted for others."));
    }

    /**
     * Matches the grid against each recipe slot by slot.
     *
     * <p>A recipe entry of {@code null} requires that grid slot to be empty, which is what stops a
     * larger recipe's ingredients from satisfying a smaller one.
     */
    @Nullable
    @Override
    protected MachineRecipe findNextRecipe(@Nonnull BlockMenu menu) {
        ItemStack[] grid = readGrid(menu);
        if (grid == null) {
            return null;
        }

        MachineRecipe recipe = firstMatch(grid);
        if (recipe == null || !InvUtils.fitAll(menu.toInventory(), recipe.getOutput(), OUTPUT_SLOTS)) {
            return null;
        }

        LockedPiece locked = questLocks.get(recipe);
        if (locked != null) {
            Block block = menu.getLocation().getBlock();
            UUID crafter = getCrafter(menu, block);
            if (!isUnlockedFor(crafter, locked.set())) {
                return null;
            }
            if (crafter != null && locked.slot() != null && questService.hasCraftedArmor(crafter, locked.set().getId(), locked.slot())) {
                return null;
            }
            if (crafter != null && locked.slot() != null) {
                questService.recordArmorCrafted(crafter, locked.set().getId(), locked.slot());
            }
        }

        for (int index = 0; index < GRID_SLOTS.length; index++) {
            ItemStack required = index < recipe.getInput().length ? recipe.getInput()[index] : null;
            if (required != null) {
                menu.consumeItem(GRID_SLOTS[index], required.getAmount());
            }
        }

        return recipe;
    }

    /** The grid's contents by position, or {@code null} when it is empty. */
    @Nullable
    private static ItemStack[] readGrid(@Nonnull BlockMenu menu) {
        ItemStack[] grid = new ItemStack[GRID_SLOTS.length];
        boolean anyInput = false;

        for (int index = 0; index < GRID_SLOTS.length; index++) {
            ItemStack item = menu.getItemInSlot(GRID_SLOTS[index]);
            if (item != null && !item.getType().isAir()) {
                grid[index] = ItemStackWrapper.wrap(item);
                anyInput = true;
            }
        }

        return anyInput ? grid : null;
    }

    @Nullable
    private MachineRecipe firstMatch(@Nonnull ItemStack[] grid) {
        for (MachineRecipe recipe : getRecipes()) {
            if (matches(grid, recipe.getInput())) {
                return recipe;
            }
        }
        return null;
    }

    private static boolean matches(@Nonnull ItemStack[] grid, @Nonnull ItemStack[] pattern) {
        for (int index = 0; index < grid.length; index++) {
            ItemStack required = index < pattern.length ? pattern[index] : null;
            ItemStack present = grid[index];

            if (required == null) {
                if (present != null) {
                    return false;
                }
                continue;
            }

            if (present == null
                    || !SlimefunUtils.isItemSimilar(present, required, true)
                    || present.getAmount() < required.getAmount()) {
                return false;
            }
        }

        return true;
    }

    @Nonnull
    @Override
    protected int[] getInputSlots() {
        return GRID_SLOTS;
    }

    @Nonnull
    @Override
    protected int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }

    @Override
    protected int getProgressSlot() {
        return PROGRESS_SLOT;
    }

    @Nonnull
    @Override
    protected ItemStack getProgressBar() {
        return new ItemStack(Material.SMITHING_TABLE);
    }
}
