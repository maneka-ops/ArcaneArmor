package me.arcane.armor.machine;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotHopperable;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Base for every Arcane machine: a powered, ticking container that runs recipes.
 *
 * <p>Slimefun 4.9 splits this behaviour across several interfaces; folding them into one base here
 * means a concrete machine only has to declare its layout and its recipes. Subclasses that need
 * something other than plain recipe matching override {@link #findNextRecipe(BlockMenu)}.
 *
 * <p>Ticks run off the main thread by default — Slimefun's ticker is async — so subclasses must not
 * touch entities or the world from {@code findNextRecipe}. Inventory access through
 * {@link BlockMenu} is safe.
 */
public abstract class AbstractArcaneMachine extends SlimefunItem
        implements MachineProcessHolder<CraftingOperation>, EnergyNetComponent, RecipeDisplayItem, NotHopperable {

    protected static final int[] INPUT_BORDER = {9, 10, 11, 12, 18, 21, 27, 28, 29, 30};
    protected static final int[] OUTPUT_BORDER = {14, 15, 16, 17, 23, 26, 32, 33, 34, 35};

    private static final int[] DEFAULT_INPUT_SLOTS = {19, 20};
    private static final int[] DEFAULT_OUTPUT_SLOTS = {24, 25};
    private static final int PROGRESS_SLOT = 22;
    private static final int MENU_SIZE = 54;

    /** Machine ticks per second; Slimefun runs its ticker twice a second. */
    private static final int TICKS_PER_SECOND = 2;
    private static final int BAR_LENGTH = 20;

    private final MachineProcessor<CraftingOperation> processor = new MachineProcessor<>(this);
    private final List<MachineRecipe> recipes = new ArrayList<>();

    private int energyCapacity;
    private int energyPerTick;
    private int speed = 1;

    protected AbstractArcaneMachine(
            @Nonnull ItemGroup itemGroup,
            @Nonnull SlimefunItemStack item,
            @Nonnull RecipeType recipeType,
            @Nonnull ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        processor.setProgressBar(getProgressBar());

        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                setupMenu(this);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block block) {
                AbstractArcaneMachine.this.onMenuCreated(menu, block);
            }

            @Override
            public boolean canOpen(Block block, Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager()
                                .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Nonnull
            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.INSERT ? getInputSlots() : getOutputSlots();
            }
        };
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return false;
            }

            @Override
            public void tick(Block block, SlimefunItem item, Config data) {
                BlockMenu menu = BlockStorage.getInventory(block);
                if (menu != null) {
                    AbstractArcaneMachine.this.tick(menu, block);
                }
            }
        });

        addItemHandler(new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(BlockBreakEvent event, ItemStack item, List<ItemStack> drops) {
                Location location = event.getBlock().getLocation();
                BlockMenu menu = BlockStorage.getInventory(event.getBlock());
                if (menu != null) {
                    menu.dropItems(location, getInputSlots());
                    menu.dropItems(location, getOutputSlots());
                }
                processor.endOperation(event.getBlock());
            }
        });
    }

    // ---- Machine wiring ----------------------------------------------------------------------

    @Nonnull
    @Override
    public MachineProcessor<CraftingOperation> getMachineProcessor() {
        return processor;
    }

    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return energyCapacity;
    }

    public int getEnergyConsumption() {
        return energyPerTick;
    }

    public int getSpeed() {
        return speed;
    }

    @Nonnull
    public AbstractArcaneMachine setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.energyCapacity = capacity;
        return this;
    }

    @Nonnull
    public AbstractArcaneMachine setConsumption(int consumption) {
        if (consumption <= 0 || consumption >= energyCapacity) {
            throw new IllegalArgumentException("consumption must be positive and below the capacity");
        }
        this.energyPerTick = consumption;
        return this;
    }

    @Nonnull
    public AbstractArcaneMachine setSpeed(int speed) {
        if (speed <= 0) {
            throw new IllegalArgumentException("speed must be positive");
        }
        this.speed = speed;
        return this;
    }

    // ---- Recipes -----------------------------------------------------------------------------

    @Nonnull
    public List<MachineRecipe> getRecipes() {
        return recipes;
    }

    @Nonnull
    public MachineRecipe registerRecipe(int seconds, @Nonnull ItemStack[] inputs, @Nonnull ItemStack[] outputs) {
        MachineRecipe recipe = new MachineRecipe(seconds, inputs, outputs);
        recipe.setTicks(Math.max(1, recipe.getTicks() / speed));
        recipes.add(recipe);
        return recipe;
    }

    public void registerRecipe(int seconds, @Nonnull ItemStack input, @Nonnull ItemStack output) {
        registerRecipe(seconds, new ItemStack[] {input}, new ItemStack[] {output});
    }

    /**
     * Guide listing for this machine, with the processing time written onto each result.
     *
     * <p>The time is the number that actually matters when deciding how many of a machine to build,
     * and it is not derivable from the recipe grid, so it belongs on the item rather than in the
     * wiki.
     */
    @Nonnull
    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> display = new ArrayList<>();

        for (MachineRecipe recipe : recipes) {
            ItemStack input = recipe.getInput()[0];
            ItemStack output = recipe.getOutput()[0];
            if (input == null || output == null) {
                continue;
            }

            display.add(withExtraInputsLore(input, recipe.getInput()));
            display.add(withTimeLore(output, recipe.getTicks()));
        }

        return display;
    }

    /**
     * Copies the first input and lists every other input in its lore.
     *
     * <p>The guide shows machine recipes as input/output pairs with room for one input, so without
     * this a two-ingredient recipe — an essence and its rune — would show only half of what it takes.
     */
    @Nonnull
    private static ItemStack withExtraInputsLore(@Nonnull ItemStack first, @Nonnull ItemStack[] inputs) {
        ItemStack copy = first.clone();
        if (inputs.length < 2) {
            return copy;
        }

        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            return copy;
        }

        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        lore.add("");
        lore.add(ChatColor.GRAY + "Also needs:");
        for (int index = 1; index < inputs.length; index++) {
            ItemStack extra = inputs[index];
            if (extra != null) {
                lore.add(ChatColor.WHITE + " • " + extra.getAmount() + "x " + displayName(extra));
            }
        }

        meta.setLore(lore);
        copy.setItemMeta(meta);
        return copy;
    }

    @Nonnull
    private static String displayName(@Nonnull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return ChatColor.stripColor(meta.getDisplayName());
        }
        String name = item.getType().name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** Copies an item and appends the processing time to its lore. */
    @Nonnull
    private static ItemStack withTimeLore(@Nonnull ItemStack output, int ticks) {
        ItemStack copy = output.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            return copy;
        }

        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        lore.add("");
        lore.add(ChatColor.GRAY + "Takes " + ChatColor.WHITE + formatSeconds(ticks) + ChatColor.GRAY + " to make");

        meta.setLore(lore);
        copy.setItemMeta(meta);
        return copy;
    }

    /**
     * Finds a runnable recipe and consumes its inputs.
     *
     * <p>Called from the async ticker, so it only touches the menu.
     */
    @Nullable
    protected MachineRecipe findNextRecipe(@Nonnull BlockMenu menu) {
        Map<Integer, ItemStack> inventory = new HashMap<>();
        for (int slot : getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            if (item != null) {
                inventory.put(slot, ItemStackWrapper.wrap(item));
            }
        }

        if (inventory.isEmpty()) {
            return null;
        }

        Map<Integer, Integer> matched = new HashMap<>();

        for (MachineRecipe recipe : recipes) {
            for (ItemStack input : recipe.getInput()) {
                for (int slot : getInputSlots()) {
                    ItemStack candidate = inventory.get(slot);
                    if (candidate == null || matched.containsKey(slot)) {
                        continue;
                    }
                    if (SlimefunUtils.isItemSimilar(candidate, input, true)
                            && candidate.getAmount() >= input.getAmount()) {
                        matched.put(slot, input.getAmount());
                        break;
                    }
                }
            }

            if (matched.size() == recipe.getInput().length) {
                if (!InvUtils.fitAll(menu.toInventory(), recipe.getOutput(), getOutputSlots())) {
                    return null;
                }

                for (Map.Entry<Integer, Integer> entry : matched.entrySet()) {
                    menu.consumeItem(entry.getKey(), entry.getValue());
                }
                return recipe;
            }

            matched.clear();
        }

        return null;
    }

    // ---- Ticking -----------------------------------------------------------------------------

    protected void tick(@Nonnull BlockMenu menu, @Nonnull Block block) {
        CraftingOperation operation = processor.getOperation(block);

        if (operation == null) {
            MachineRecipe next = findNextRecipe(menu);
            if (next != null) {
                CraftingOperation started = new CraftingOperation(next);
                processor.startOperation(block, started);
                showProgress(menu, started, true);
            } else {
                showIdle(menu);
            }
            return;
        }

        if (!takeCharge(block.getLocation())) {
            showStalled(menu, operation);
            return;
        }

        if (!operation.isFinished()) {
            operation.addProgress(1);
            showProgress(menu, operation, true);
            return;
        }

        for (ItemStack output : operation.getResults()) {
            if (output != null) {
                menu.pushItem(output.clone(), getOutputSlots());
            }
        }

        processor.endOperation(block);
        showIdle(menu);
    }

    // ---- Progress display --------------------------------------------------------------------

    /**
     * Draws the progress slot: percentage, a bar, and the time left.
     *
     * <p>Slimefun's own progress bar only cycles an icon, which tells a player that something is
     * happening but not how long it will take. Skipped entirely when nobody has the menu open, so
     * an idle machine farm costs nothing to display.
     */
    private void showProgress(@Nonnull BlockMenu menu, @Nonnull CraftingOperation operation, boolean powered) {
        if (!menu.hasViewer()) {
            return;
        }

        int total = Math.max(1, operation.getTotalTicks());
        int done = Math.min(total, operation.getProgress());
        int percent = (int) Math.round(done * 100.0D / total);
        int remaining = Math.max(0, total - done);

        int filled = Math.round(BAR_LENGTH * (float) done / total);
        String bar = ChatColor.GREEN + "|".repeat(filled)
                + ChatColor.DARK_GRAY + "|".repeat(BAR_LENGTH - filled);

        ItemStack icon = getProgressBar().clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "Progress: " + ChatColor.YELLOW + percent + "%");
            meta.setLore(List.of(
                    bar,
                    "",
                    ChatColor.GRAY + "Time left: " + ChatColor.WHITE + formatSeconds(remaining),
                    ChatColor.GRAY + "Total: " + ChatColor.WHITE + formatSeconds(total),
                    "",
                    powered
                            ? ChatColor.GREEN + "Running"
                            : ChatColor.RED + "Not enough power"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            icon.setItemMeta(meta);
        }

        menu.replaceExistingItem(getProgressSlot(), icon);
    }

    /** Same readout, but flagged as stalled so the cause of a stuck machine is obvious. */
    private void showStalled(@Nonnull BlockMenu menu, @Nonnull CraftingOperation operation) {
        showProgress(menu, operation, false);
    }

    private void showIdle(@Nonnull BlockMenu menu) {
        if (!menu.hasViewer()) {
            return;
        }

        menu.replaceExistingItem(getProgressSlot(), CustomItemStack.create(
                Material.BLACK_STAINED_GLASS_PANE,
                ChatColor.GRAY + "Idle",
                "",
                ChatColor.DARK_GRAY + "Insert ingredients to begin."));
    }

    /** Machine ticks to a human duration, e.g. {@code 90} to {@code 45s} and {@code 150} to 1m 15s. */
    @Nonnull
    protected static String formatSeconds(int ticks) {
        int seconds = Math.max(0, ticks) / TICKS_PER_SECOND;
        if (seconds < 60) {
            return seconds + "s";
        }
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }

    protected boolean takeCharge(@Nonnull Location location) {
        if (!isChargeable()) {
            return true;
        }

        int charge = getCharge(location);
        if (charge < energyPerTick) {
            return false;
        }

        setCharge(location, charge - energyPerTick);
        return true;
    }

    // ---- Menu --------------------------------------------------------------------------------

    /**
     * Lays the menu out by filling every slot first and then cutting the working slots back out.
     *
     * <p>Filling by subtraction rather than by listing the decorative slots means a layout change
     * can never leave a stray usable slot behind — the gap through which items get lost or dropped
     * on the floor when a machine is broken.
     */
    protected void setupMenu(@Nonnull BlockMenuPreset preset) {
        Set<Integer> working = new HashSet<>();
        for (int slot : getInputSlots()) {
            working.add(slot);
        }
        for (int slot : getOutputSlots()) {
            working.add(slot);
        }

        for (int slot = 0; slot < MENU_SIZE; slot++) {
            if (working.contains(slot) || slot == getProgressSlot()) {
                continue;
            }
            preset.addItem(slot, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }

        for (int slot : INPUT_BORDER) {
            if (!working.contains(slot)) {
                preset.addItem(slot, ChestMenuUtils.getInputSlotTexture(), ChestMenuUtils.getEmptyClickHandler());
            }
        }
        for (int slot : OUTPUT_BORDER) {
            if (!working.contains(slot)) {
                preset.addItem(slot, ChestMenuUtils.getOutputSlotTexture(), ChestMenuUtils.getEmptyClickHandler());
            }
        }

        preset.addItem(getProgressSlot(), CustomItemStack.create(
                        Material.BLACK_STAINED_GLASS_PANE,
                        ChatColor.GRAY + "Idle",
                        "",
                        ChatColor.DARK_GRAY + "Insert ingredients to begin."),
                ChestMenuUtils.getEmptyClickHandler());
    }

    @Nonnull
    protected int[] getInputSlots() {
        return DEFAULT_INPUT_SLOTS;
    }

    @Nonnull
    protected int[] getOutputSlots() {
        return DEFAULT_OUTPUT_SLOTS;
    }

    protected int getProgressSlot() {
        return PROGRESS_SLOT;
    }

    /**
     * Called on the main thread whenever a placed machine's menu is created or loaded. Subclasses
     * use it to attach opening handlers; the default does nothing.
     */
    protected void onMenuCreated(@Nonnull BlockMenu menu, @Nonnull Block block) {
    }

    /** Icon shown spinning in the progress slot. */
    @Nonnull
    protected abstract ItemStack getProgressBar();
}
