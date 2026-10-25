package me.arcane.armor;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import javax.annotation.Nonnull;
import me.arcane.armor.api.ArcaneArmorAPI;
import me.arcane.armor.api.ability.Ability;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.api.registry.Registry;
import me.arcane.armor.api.rune.Rune;
import me.arcane.armor.command.ArcaneCommand;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.config.Messages;
import me.arcane.armor.content.armor.ArmorItemFactory;
import me.arcane.armor.content.gear.ArcaneGearFactory;
import me.arcane.armor.core.ability.AbilityService;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.cooldown.CooldownService;
import me.arcane.armor.core.cosmetic.CosmeticService;
import me.arcane.armor.core.cosmetic.FxPreferences;
import me.arcane.armor.core.data.ArmorDataService;
import me.arcane.armor.core.data.AscensionRecords;
import me.arcane.armor.core.energy.EnergyService;
import me.arcane.armor.core.quest.QuestService;
import me.arcane.armor.core.task.ArcaneTickTask;
import me.arcane.armor.core.task.EnergyRegenTask;
import me.arcane.armor.gui.ArmorInfoGui;
import me.arcane.armor.gui.GuiListener;
import me.arcane.armor.gui.LeaderboardGui;
import me.arcane.armor.gui.QuestGui;
import me.arcane.armor.integration.Integrations;
import me.arcane.armor.integration.PlaceholderHook;
import me.arcane.armor.listener.AbilityInputListener;
import me.arcane.armor.listener.ArcaneCombatListener;
import me.arcane.armor.listener.ModuleInstallListener;
import me.arcane.armor.listener.PlayerSessionListener;
import me.arcane.armor.listener.QuestProgressListener;
import me.arcane.armor.registry.ArcaneRegistries;
import me.arcane.armor.setup.ArcaneSetup;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin entry point and composition root.
 *
 * <p>Every service is constructed here and handed to whatever needs it, so there are no static
 * singletons to unwind and the wiring is readable in one pass. {@code onDisable} tears down in
 * reverse: tasks first, then caches, then the service registration.
 */
public final class ArcaneArmor extends JavaPlugin implements SlimefunAddon, ArcaneArmorAPI {

    private static final long COOLDOWN_PURGE_INTERVAL_TICKS = 20L * 300L;
    private static final long QUEST_SAVE_INTERVAL_TICKS = 20L * 300L;

    private ArcaneConfig config;
    private Messages messages;
    private ArcaneRegistries registries;
    private ArmorDataService armorDataService;
    private EquipmentService equipmentService;
    private EnergyService energyService;
    private CooldownService cooldownService;
    private CosmeticService cosmeticService;
    private FxPreferences fxPreferences;
    private AbilityService abilityService;
    private QuestService questService;
    private Integrations integrations;

    @Override
    public void onEnable() {
        config = new ArcaneConfig(this);
        messages = new Messages(config);

        registries = new ArcaneRegistries();
        registries.registerDefaults();

        armorDataService = new ArmorDataService(this);
        equipmentService = new EquipmentService(
                armorDataService, registries.sets(), registries.modules(), registries.runes());
        energyService = new EnergyService(armorDataService, equipmentService);
        cooldownService = new CooldownService();
        fxPreferences = new FxPreferences(this);
        cosmeticService = new CosmeticService(config, fxPreferences);
        abilityService = new AbilityService(this, config, messages, registries.abilities(),
                equipmentService, energyService, cooldownService, cosmeticService);

        questService = new QuestService(
                this, config, messages, registries.sets(), equipmentService, cosmeticService);
        questService.load();
        equipmentService.setQuestService(questService);
        AscensionRecords ascensionRecords = new AscensionRecords(this);

        ArmorItemFactory itemFactory =
                new ArmorItemFactory(this, armorDataService, registries.abilities(), config);
        ArcaneGearFactory gearFactory = new ArcaneGearFactory(this);

        // Content registration has to happen before the registries seal, and before any listener
        // can ask for an item that does not exist yet.
        new ArcaneSetup(this, registries, armorDataService, itemFactory, gearFactory, config,
                questService, cosmeticService, messages, ascensionRecords).run();
        registries.freeze();

        // Parsed after the freeze so quests can target sets that other addons registered.
        questService.reloadDefinitions();

        registerListeners(itemFactory);
        registerCommands(itemFactory, ascensionRecords);
        scheduleTasks();

        integrations = new Integrations(getLogger());
        if (integrations.hasPlaceholderApi()) {
            new PlaceholderHook(this, equipmentService, cooldownService, registries).register();
        }
        integrations.logSummary();

        Bukkit.getServicesManager().register(ArcaneArmorAPI.class, this, this, ServicePriority.Normal);

        getLogger().info("Arcane Armor enabled with "
                + registries.sets().size() + " sets, "
                + registries.abilities().size() + " abilities, "
                + registries.modules().size() + " modules and "
                + registries.runes().size() + " runes.");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);

        if (questService != null) {
            questService.saveNow();
        }
        if (equipmentService != null) {
            equipmentService.clear();
        }
        if (cooldownService != null) {
            cooldownService.clearAll();
        }

        Bukkit.getServicesManager().unregisterAll(this);
    }

    private void registerListeners(@Nonnull ArmorItemFactory itemFactory) {
        Bukkit.getPluginManager().registerEvents(
                new AbilityInputListener(abilityService, equipmentService), this);
        Bukkit.getPluginManager().registerEvents(
                new ArcaneCombatListener(this, equipmentService, config), this);
        Bukkit.getPluginManager().registerEvents(
                new PlayerSessionListener(equipmentService, cooldownService), this);
        Bukkit.getPluginManager().registerEvents(
                new ModuleInstallListener(
                        registries, armorDataService, equipmentService, itemFactory, config, messages), this);
        Bukkit.getPluginManager().registerEvents(new QuestProgressListener(questService, config), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(), this);
    }

    private void registerCommands(@Nonnull ArmorItemFactory itemFactory, @Nonnull AscensionRecords ascensionRecords) {
        ArmorInfoGui armorInfoGui = new ArmorInfoGui(
                equipmentService, abilityService, cooldownService, registries.abilities());

        QuestGui questGui = new QuestGui(questService);
        Bukkit.getPluginManager().registerEvents(questGui, this);

        LeaderboardGui leaderboardGui = new LeaderboardGui(questService, ascensionRecords);

        ArcaneCommand command = new ArcaneCommand(
                registries, armorDataService, equipmentService, energyService, armorInfoGui,
                questService, questGui, config, messages, itemFactory, ascensionRecords,
                fxPreferences, leaderboardGui);

        var registered = getCommand("arcane");
        if (registered != null) {
            registered.setExecutor(command);
            registered.setTabCompleter(command);
        } else {
            getLogger().warning("The /arcane command is missing from plugin.yml; commands are unavailable.");
        }
    }

    private void scheduleTasks() {
        // Passives and the state cache: main thread, because both touch live players.
        Bukkit.getScheduler().runTaskTimer(this,
                new ArcaneTickTask(config, equipmentService, cosmeticService),
                20L, config.getTickInterval());

        // Energy regeneration: main thread, because it writes to worn items.
        Bukkit.getScheduler().runTaskTimer(this,
                new EnergyRegenTask(config, equipmentService, energyService),
                40L, config.getEnergyRegenInterval());

        // Cooldown housekeeping touches nothing but concurrent maps, so it runs off-thread.
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                cooldownService::purgeExpired,
                COOLDOWN_PURGE_INTERVAL_TICKS, COOLDOWN_PURGE_INTERVAL_TICKS);

        // Quest progress: snapshotted on the main thread, written to disk off it.
        Bukkit.getScheduler().runTaskTimer(this, questService::saveAsync,
                QUEST_SAVE_INTERVAL_TICKS, QUEST_SAVE_INTERVAL_TICKS);
    }

    // ---- SlimefunAddon -----------------------------------------------------------------------

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/arcane/ArcaneArmor/issues";
    }

    // ---- ArcaneArmorAPI ----------------------------------------------------------------------

    @Nonnull
    @Override
    public Registry<Ability> getAbilities() {
        return registries.abilities();
    }

    @Nonnull
    @Override
    public Registry<ArmorModule> getModules() {
        return registries.modules();
    }

    @Nonnull
    @Override
    public Registry<Rune> getRunes() {
        return registries.runes();
    }

    @Nonnull
    @Override
    public Registry<ArmorSetDefinition> getSets() {
        return registries.sets();
    }

    @Nonnull
    @Override
    public ArmorState getArmorState(@Nonnull Player player) {
        return equipmentService.getCachedState(player);
    }

    @Nonnull
    @Override
    public AbilityService getAbilityService() {
        return abilityService;
    }

    @Nonnull
    @Override
    public EnergyService getEnergyService() {
        return energyService;
    }

    @Nonnull
    @Override
    public CooldownService getCooldownService() {
        return cooldownService;
    }

    @Nonnull
    @Override
    public ArmorDataService getArmorDataService() {
        return armorDataService;
    }

    /** Armor quests and each player's progress through them. */
    @Nonnull
    public QuestService getQuestService() {
        return questService;
    }

    /** Optional third-party support detected at startup. */
    @Nonnull
    public Integrations getIntegrations() {
        return integrations;
    }
}
