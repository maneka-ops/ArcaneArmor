package me.arcane.armor.gui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.armor.ArmorSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks an inventory as one of ours.
 *
 * <p>Identifying menus by holder rather than by title is what keeps the click listener correct:
 * titles are player-visible, translatable and easy to spoof with an anvil, while the holder is an
 * object identity the client can never forge.
 */
public final class ArcaneMenuHolder implements InventoryHolder {

    /** Which menu this inventory is. */
    public enum Type {
        ARMOR_INFO,
        MODULE_MANAGER,
        QUESTS
    }

    private final Type type;
    private final ArmorSlot slot;
    private Inventory inventory;

    public ArcaneMenuHolder(@Nonnull Type type, @Nullable ArmorSlot slot) {
        this.type = type;
        this.slot = slot;
    }

    @Nonnull
    public Type getType() {
        return type;
    }

    /** The armor slot the menu is operating on, for menus that edit one piece. */
    @Nullable
    public ArmorSlot getSlot() {
        return slot;
    }

    void setInventory(@Nonnull Inventory inventory) {
        this.inventory = inventory;
    }

    @Nonnull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
