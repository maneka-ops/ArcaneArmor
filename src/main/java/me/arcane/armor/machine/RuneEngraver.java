package me.arcane.armor.machine;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.registry.Registry;
import me.arcane.armor.api.rune.Rune;
import me.arcane.armor.core.data.ArmorData;
import me.arcane.armor.core.data.ArmorDataService;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * Engraves a rune onto a piece of armor.
 *
 * <p>Unlike the recipe machines this transforms the input in place: the piece keeps its tier, its
 * energy and its installed modules, and gains one entry in its rune list. The piece is only moved
 * to the output once the engraving succeeds, so a full output never eats a rune.
 */
public class RuneEngraver extends AbstractArcaneMachine {

    private static final int ARMOR_SLOT = 19;
    private static final int RUNE_SLOT = 20;
    private static final int[] INPUT_SLOTS = {ARMOR_SLOT, RUNE_SLOT};
    private static final int[] OUTPUT_SLOTS = {24, 25};

    private final ArmorDataService dataService;
    private final Registry<Rune> runes;

    public RuneEngraver(
            @Nonnull ItemGroup itemGroup,
            @Nonnull SlimefunItemStack item,
            @Nonnull RecipeType recipeType,
            @Nonnull ItemStack[] recipe,
            @Nonnull ArmorDataService dataService,
            @Nonnull Registry<Rune> runes) {
        super(itemGroup, item, recipeType, recipe);
        this.dataService = dataService;
        this.runes = runes;
    }

    @Override
    protected void tick(@Nonnull BlockMenu menu, @Nonnull Block block) {
        ItemStack armor = menu.getItemInSlot(ARMOR_SLOT);
        ItemStack runeItem = menu.getItemInSlot(RUNE_SLOT);

        if (armor == null || runeItem == null) {
            return;
        }

        ArmorData data = dataService.read(armor).orElse(null);
        if (data == null) {
            return;
        }

        Rune rune = resolveRune(runeItem);
        if (rune == null) {
            return;
        }

        if (!data.hasFreeRuneSlot() || data.runeIds().contains(rune.getId())) {
            return;
        }

        if (!takeCharge(block.getLocation())) {
            return;
        }

        ItemStack engraved = armor.clone();
        engraved.setAmount(1);

        List<String> updated = new ArrayList<>(data.runeIds());
        updated.add(rune.getId());
        dataService.write(engraved, data.withRunes(updated));

        if (!InvUtils.fitAll(menu.toInventory(), new ItemStack[] {engraved}, OUTPUT_SLOTS)) {
            return;
        }

        menu.consumeItem(ARMOR_SLOT, 1);
        menu.consumeItem(RUNE_SLOT, 1);
        menu.pushItem(engraved, OUTPUT_SLOTS);
    }

    /** Maps the physical rune item back to its registered rune. */
    private Rune resolveRune(@Nonnull ItemStack item) {
        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
        if (slimefunItem == null) {
            return null;
        }

        for (Rune rune : runes.values()) {
            if (rune.getItemId().equals(slimefunItem.getId())) {
                return rune;
            }
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
        return new ItemStack(Material.ECHO_SHARD);
    }
}
