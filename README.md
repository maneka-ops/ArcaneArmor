# Arcane Armor

A magical armor progression expansion for Slimefun. Armor is not "protection, but higher": every set
has its own passives, its own active and ultimate ability, a tier ladder, module slots you fill
yourself, and permanent runes.

## Progression

```
Magic Dust ──► Arcane Compressor ──► Arcane Crystal
                                          │
Raw material + Slimefun rune ──► Essence Extractor ──► Essences
                                          │
Arcane Crystal + Catalyst ──► Arcane Infuser ──► Infused Crystal
                                          │
                     Arcane Forge ──► Runic Plate, Mana Core, Arcane Circuit
                                          │
                     Arcane Forge ──► Armor pieces, Modules
                                          │
               Rune Engraver ──► permanent runes   Upgrade Altar ──► next tier
```

Every machine is a normal Slimefun energy consumer and hooks up to any Slimefun power network.

Build the machines in this order: **Arcane Compressor → Arcane Infuser → Arcane Forge**. These
three are crafted from Magic Dust, Electro Magnets and the previous machine's output; every other
machine needs Arcane Crystals and an Arcane Circuit from the Forge.

## Essences

Made in the Essence Extractor, one per 40 seconds at 96 J per tick. Each takes three ingredients,
one per input slot: a stack of raw material, two Slimefun elemental runes from the Ancient Altar,
and a Slimefun catalyst.

| Essence | Raw material | Runes | Catalyst |
|---|---|---|---|
| Lightning | 48 Redstone | 2 Lightning Rune | 4 Magic Lump III |
| Fire | 12 Blaze Rods | 2 Fire Rune | 8 Sulfate |
| Water | 48 Prismarine Crystals | 2 Water Rune | 2 Synthetic Sapphire |
| Void | 16 Ender Pearls | 2 Ender Rune | 4 Ender Lump III |
| Nature | 24 Emeralds | 2 Earth Rune | 8 Magic Lump II |

## Playing

| Action | Input |
|---|---|
| Active ability | Swap-hand key (`F` by default) |
| Ultimate ability | Sneak + swap-hand, full set only |
| Install a module | Pick up a module, left-click it onto an armor piece in your inventory |
| Remove a module | Shift-right-click an armor piece with an empty cursor |
| Inspect your set | `/arcane modules` |

Energy regenerates slowly on its own, much faster while asleep, and fastest from Charged Crystals
and the Crystal Charger.

## Armor quests

Apprentice armor can always be forged. Every set above it has a quest, and the Arcane Forge will
not make that set's armor until the player who placed the Forge has finished it. Quests only count
while you wear the full set below: the Storm quest is done in Apprentice armor, the Infernal quest
in Storm armor, and so on. `/arcane quests` shows each quest, its objectives and progress.

| Set | Wear | Quest | Reward |
|---|---|---|---|
| Apprentice | - | none | - |
| Storm | Apprentice | 40 Creepers and 60 Zombies at night, 64 Redstone Ore, 30 Apprentice abilities, deliver 16 Lightning Essence | 15 levels |
| Infernal | Storm | 50 Blazes, 25 Wither Skeletons and 10 Ghasts in the Nether, 8 Ancient Debris, 50 Storm abilities, deliver 24 Fire Essence | 25 levels |
| Frost | Infernal | 40 Strays, 30 Guardians, the Elder Guardian, 128 Ice, 16 Diamond Ore, 60 Infernal abilities, deliver 32 Water Essence and 4 Charged Crystals | 35 levels |
| Void | Frost | 100 Endermen in the End, 30 Shulkers, the Ender Dragon, the Wither, 80 Frost abilities, deliver 48 Void Essence and 8 Pure Mana | 50 levels |

Objective types are `KILL`, `MINE`, `ABILITY` (cast Arcane abilities) and `DELIVER` (click the quest
in `/arcane quests` to hand in items). Any objective can be limited with `dimension:` or
`night: true`. Dying loses 25% of unfinished progress (`quests.death-penalty`); delivered items are
never lost. Rewards are experience levels plus optional console commands. All of it is set in
`config.yml` under `quests`; `quests.required-pieces` sets how much of the previous set must be worn.
Spawner mobs and Silk Touch breaks do not count. A Forge placed before quests existed belongs to
the first player to open it. Set `quests.enabled: false` to turn the gate off.

## Upgrading

Armor, weapons and tools all upgrade at the **Upgrade Altar**: the item in the left slot, Infused
Crystals in the right. Tiers run I → II → III → IV → V → Ascended and cost 4, 4, 8, 16, 24 and 40
crystals. Armor keeps its modules and runes; every tier raises protection, enchantment levels and
module slots. Weapons and tools gain enchantment levels and damage with each tier.

| Upgrade to | Infused Crystals | Total spent on one piece |
|---|---|---|
| Tier II | 4 | 4 |
| Tier III | 8 | 12 |
| Tier IV | 16 | 28 |
| Tier V | 24 | 52 |
| Ascended | 40 | 92 |

Each upgrade step also draws power from the Slimefun grid (192 J per tick) while the altar runs.
Set `balance.upgrade-consumes-materials: false` to make crystal cost free (creative/testing servers).

While the altar is running, its progress slot shows a live preview of the next step: the tier
being climbed, stat multiplier, cooldown cut, module/rune slots, and the exact crystal cost. A
successful upgrade plays a celebration effect on the crafter, and the first player ever to bring a
piece of a set to Ascended tier is announced server-wide
(`achievements.first-ascended-broadcast`; the claim is persisted in `ascension-data.yml`).

### Tier unlock ladder

Each rung of the ladder grants something new, not just bigger numbers:

| Tier | Unlock |
|---|---|
| II | **Aqua Affinity** on every piece |
| III | **Swift Sneak II** on leggings |
| IV | **Every full-set passive +1 amplifier** (Speed I → II, Regen I → II, ...) |
| V | **Set signature trait** - Storm `THUNDER_STEP` (melee hits taken discharge lightning back), Infernal `MOLTEN_EDGE` (melee hits ignite), Frost `GLACIAL_SHELL` (stand still 3s to hard-freeze the next attacker) |
| Ascended | **Arcane Bond** - worn armor is kept on death instead of dropping |

Signature traits only activate once *every* worn piece of the set has reached Tier V, consistent
with how tiers already hold each other back. Sets added through the API can set their own
signature with `ArmorSetDefinition.Builder.signature(...)`.

### Custom enchantments

- **Rune of Fire** now also grants `MOLTEN_EDGE`: melee hits set the struck target on fire for
  three seconds, on any piece it is engraved into.
- Night Vision was removed from the shared baseline; every piece grants **Water Breathing** instead.
- Blast Protection is no longer applied to pieces, so explosions retain their bite.

## Broadcasts and effects

- Forging an armor piece announces it in chat (`messages.piece-forged`, toggle
  `achievements.piece-forged-broadcast`) and celebrates on the crafter with a firework/dust burst
  in the set's own color.
- The first player ever to forge a full set of one type is announced (`messages.world-first`) with
  a bigger celebration; claims persist in `quest-data.yml`.
- The first player to bring any piece of a set to Ascended tier is announced
  (`messages.first-ascended`); claims persist in `ascension-data.yml`.

## Sets

| Set | Theme | Active | Ultimate |
|---|---|---|---|
| Apprentice | Mana | Mana Wave | Mana Burst |
| Storm | Lightning | Lightning Strike | Thunderstorm |
| Infernal | Fire | Flame Spin | Inferno Explosion |
| Frost | Ice | Ice Nova | Blizzard |
| Void | Teleportation | Blink | Void Rift |

Tiers run I → II → III → IV → V → Ascended. Each tier adds a module slot, raises energy capacity and
defense, and cuts ability cooldowns; Ascended is roughly half the cooldown of tier I.

## Commands and permissions

```
/arcane modules              inspect what you are wearing        arcane.use
/arcane energy [player]      show stored energy                  arcane.use
/arcane quests               armor quests and progress           arcane.use
/arcane quests complete <player> <set>                           arcane.admin
/arcane quests reset <player> [set]                              arcane.admin
/arcane give <player> <set> <slot> [tier]                        arcane.give
/arcane upgrade              raise the held piece one tier       arcane.admin
/arcane reload               reload config.yml                   arcane.reload
/arcane debug                registry and cache counts           arcane.debug
```

`arcane.admin` implies `arcane.give`, `arcane.reload` and `arcane.debug`.

## Placeholders

Requires PlaceholderAPI.

```
%arcane_energy%          %arcane_energy_max%      %arcane_energy_percent%
%arcane_set%             %arcane_set_id%          %arcane_level%
%arcane_upgrade%         %arcane_pieces%          %arcane_full_set%
%arcane_modules%         %arcane_modules_list%    %arcane_runes%
%arcane_cooldown_<ability_id>%
```

## Developer API

Depend on ArcaneArmor in your `plugin.yml`, then load the service:

```java
ArcaneArmorAPI api = ArcaneArmorAPI.get();
```

Register your own content from `onEnable` — the registries freeze once ArcaneArmor finishes
starting, and registering afterwards throws rather than failing silently:

```java
api.getAbilities().register(new MyAbility());

api.getModules().register(ArmorModule.builder("diver", "Diver Module")
        .effect(PotionEffectType.WATER_BREATHING, 0)
        .energyDiscount(0.1D)
        .build());

api.getSets().register(ArmorSetDefinition.builder("tempest", "Tempest")
        .color(ChatColor.BLUE, Color.AQUA)
        .passive(PassiveEffect.fullSet(PotionEffectType.SPEED, 1))
        .trait(SetTrait.FALL_IMMUNE)
        .active("my_ability")
        .ultimate("thunderstorm")
        .build());
```

Reading state, from any thread:

```java
ArmorState state = api.getArmorState(player);
if (state.isFullSet() && state.hasTrait(SetTrait.FIRE_IMMUNE)) {
    ...
}
```

Events, all cancellable: `AbilityUseEvent`, `ArcaneEnergyChangeEvent`, `ArmorUpgradeEvent`,
`ModuleInstallEvent`.

## Configuration

Everything in `config.yml` is read live — `/arcane reload` applies changes on the next tick with no
restart. Per-ability energy costs and cooldowns, task intervals, regeneration rates, cosmetics,
global defense scaling and all messages are configurable.

Performance knobs, in the order worth turning:

1. `feedback.aura` — the per-player particle ring, the most expensive cosmetic
2. `tasks.tick-interval` — how often passives are reapplied
3. `feedback.particles` — all ability particles

## Threading

- Passive application, energy regeneration and item writes run on the main thread.
- Cooldown housekeeping runs async against concurrent maps only.
- Slimefun ticks machines off-thread, so machine code touches only its `BlockMenu`.
- Armor state is computed once per interval and cached, so combat events cost a map lookup rather
  than four persistent-data reads.
