package me.arcane.armor.core.armor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorSlot;
import me.arcane.armor.api.armor.ArmorTier;
import me.arcane.armor.api.armor.SetTrait;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.api.registry.Registry;
import me.arcane.armor.api.rune.Rune;
import me.arcane.armor.core.data.ArmorData;
import me.arcane.armor.core.data.ArmorDataService;
import me.arcane.armor.core.quest.QuestService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Works out which set a player is wearing and caches the answer.
 *
 * <p>Resolving a state means reading four persistent data containers and folding in every module
 * and rune, which is too much to redo on each damage event. The cache is written on the main
 * thread by the tick task and read from anywhere, so it is backed by a concurrent map and holds
 * only immutable {@link ArmorState} values.
 */
public final class EquipmentService {

    private final ArmorDataService dataService;
    private final Registry<ArmorSetDefinition> sets;
    private final Registry<ArmorModule> modules;
    private final Registry<Rune> runes;
    private final Map<UUID, ArmorState> cache = new ConcurrentHashMap<>();
    private volatile QuestService questService;

    public EquipmentService(
            @Nonnull ArmorDataService dataService,
            @Nonnull Registry<ArmorSetDefinition> sets,
            @Nonnull Registry<ArmorModule> modules,
            @Nonnull Registry<Rune> runes) {
        this.dataService = dataService;
        this.sets = sets;
        this.modules = modules;
        this.runes = runes;
    }

    public void setQuestService(@Nonnull QuestService questService) {
        this.questService = questService;
    }

    /**
     * Last known state for a player. Never blocks and never touches the inventory, so it is safe
     * from async code; the value is at most one tick-task interval stale.
     */
    @Nonnull
    public ArmorState getCachedState(@Nonnull Player player) {
        return cache.getOrDefault(player.getUniqueId(), ArmorState.EMPTY);
    }

    /**
     * Recomputes the state from the player's armor slots and refreshes the cache.
     *
     * <p>Main thread only: it reads live inventory contents.
     */
    @Nonnull
    public ArmorState refresh(@Nonnull Player player) {
        checkLockedArmor(player);
        ArmorState state = compute(player);
        cache.put(player.getUniqueId(), state);
        return state;
    }

    /**
     * Checks if a player is permitted to equip or wear the given item.
     * Returns false if the item belongs to an Arcane armor set whose quest is not completed.
     */
    public boolean canEquip(@Nonnull Player player, @Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || questService == null || !questService.isEnabled() || player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        var dataOpt = dataService.read(item);
        if (dataOpt.isEmpty()) {
            return true;
        }
        String setId = dataOpt.get().setId();
        return questService.getQuest(setId) == null || questService.isUnlocked(player.getUniqueId(), setId);
    }

    /**
     * Sends a warning message and plays a rejection sound when an unauthorized armor piece is handled.
     */
    public void notifyLocked(@Nonnull Player player, @Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        dataService.read(item).ifPresent(data -> {
            ArmorSetDefinition set = sets.get(data.setId());
            String setName = set != null ? set.getDisplayName() : data.setId();
            player.sendMessage(Component.text(
                    "You cannot wear " + setName + " armor! You must complete its quest first (/arcane quests).",
                    NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
        });
    }

    /**
     * Prevents players who haven't completed a set's quest from wearing its armor.
     */
    public void checkLockedArmor(@Nonnull Player player) {
        if (questService == null || !questService.isEnabled() || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        for (ArmorSlot slot : ArmorSlot.values()) {
            ItemStack worn = slot.getWornItem(player);
            if (worn == null || worn.getType().isAir()) {
                continue;
            }

            if (!canEquip(player, worn)) {
                slot.setWornItem(player, null);
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(worn);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                notifyLocked(player, worn);
            }
        }
    }

    /** Drops a player's cache entry; called on quit so the map cannot grow without bound. */
    public void invalidate(@Nonnull UUID playerId) {
        cache.remove(playerId);
    }

    public void clear() {
        cache.clear();
    }

    /** Worn pieces of the player's dominant set, keyed by slot. Main thread only. */
    @Nonnull
    public Map<ArmorSlot, ItemStack> getWornPieces(@Nonnull Player player, @Nullable ArmorSetDefinition set) {
        Map<ArmorSlot, ItemStack> worn = new EnumMap<>(ArmorSlot.class);
        if (set == null) {
            return worn;
        }

        for (ArmorSlot slot : ArmorSlot.values()) {
            ItemStack item = slot.getWornItem(player);
            dataService.read(item).ifPresent(data -> {
                if (data.setId().equals(set.getId())) {
                    worn.put(slot, item);
                }
            });
        }
        return worn;
    }

    @Nonnull
    private ArmorState compute(@Nonnull Player player) {
        Map<String, List<ArmorData>> bySet = new HashMap<>();

        for (ArmorSlot slot : ArmorSlot.values()) {
            dataService.read(slot.getWornItem(player))
                    .ifPresent(data -> bySet.computeIfAbsent(data.setId(), key -> new ArrayList<>()).add(data));
        }

        if (bySet.isEmpty()) {
            return ArmorState.EMPTY;
        }

        // Mixing sets is allowed, but only the largest one grants bonuses.
        String dominantId = null;
        List<ArmorData> dominantPieces = List.of();
        for (Map.Entry<String, List<ArmorData>> entry : bySet.entrySet()) {
            if (entry.getValue().size() > dominantPieces.size()) {
                dominantId = entry.getKey();
                dominantPieces = entry.getValue();
            }
        }

        ArmorSetDefinition set = sets.get(dominantId);
        if (set == null) {
            // The item names a set this build no longer registers; treat it as inert rather than
            // throwing, so an addon that removed a set does not brick existing armor.
            return ArmorState.EMPTY;
        }

        if (questService != null && questService.isEnabled() && questService.getQuest(set.getId()) != null) {
            if (!questService.isUnlocked(player.getUniqueId(), set.getId())) {
                return ArmorState.EMPTY;
            }
        }

        ArmorTier lowestTier = ArmorTier.ASCENDED;
        int energy = 0;
        int capacity = 0;
        List<ArmorModule> installedModules = new ArrayList<>();
        List<Rune> engravedRunes = new ArrayList<>();
        Set<SetTrait> traits = EnumSet.copyOf(set.getTraits());
        double discount = 0.0D;
        double damageBonus = 0.0D;
        double damageReduction = 0.0D;

        for (ArmorData data : dominantPieces) {
            if (data.tier().ordinal() < lowestTier.ordinal()) {
                lowestTier = data.tier();
            }

            energy += data.energy();
            capacity += set.effectiveTier(data.tier()).getEnergyCapacity(set.getBaseEnergyCapacity());

            for (String moduleId : data.moduleIds()) {
                ArmorModule module = modules.get(moduleId);
                if (module == null) {
                    continue;
                }
                installedModules.add(module);
                traits.addAll(module.getTraits());
                discount += module.getEnergyDiscount();
                damageBonus += module.getDamageBonus();
            }

            for (String runeId : data.runeIds()) {
                Rune rune = runes.get(runeId);
                if (rune == null) {
                    continue;
                }
                engravedRunes.add(rune);
                traits.addAll(rune.getTraits());
                damageBonus += rune.getDamageBonus();
                damageReduction += rune.getDamageReduction();
                capacity += rune.getBonusEnergyCapacity();
            }
        }

        // Tier unlock ladder: a set's signature behavior arrives once every worn piece has
        // reached Tier V, and a fully-Ascended set keeps its armor on death.
        if (lowestTier.ordinal() >= ArmorTier.TIER_5.ordinal() && set.getSignatureTrait() != null) {
            traits.add(set.getSignatureTrait());
        }
        if (lowestTier == ArmorTier.ASCENDED) {
            traits.add(SetTrait.ARCANE_BOND);
        }

        return new ArmorState(
                set,
                dominantPieces.size(),
                lowestTier,
                List.copyOf(installedModules),
                List.copyOf(engravedRunes),
                Set.copyOf(traits),
                Math.min(energy, capacity),
                capacity,
                Math.min(0.9D, discount),
                damageBonus,
                Math.min(0.8D, damageReduction));
    }
}
