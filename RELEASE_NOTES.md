# Arcane Armor 1.0.0 — Release Notes

First public release. A magical armor progression expansion for Slimefun on Paper 26.2 (Java 21):

**5 armor sets** (Apprentice → Storm → Infernal → Frost → Void), each with shared passives, unique actives/ultimates, a 6-tier upgrade ladder, installable modules, permanent runes and quest-gated forging.

```
    Release target: ArcaneArmor.jar (drop into plugins/, restart)
    Requires:       Paper 26.2 + Slimefun 4.9-UNOFFICIAL-fork
```

---

## ✨ New: Forging broadcasts & effects

- Forging any armor piece announces it server-wide in chat **and** celebrates on the crafter with a firework/colored-dust burst + level-up sound
- **World First**: the first player ever to forge a full set of one kind gets a server-wide announcement, toasts for everyone online, and the claim persists across restarts
- Toggleable under `achievements.piece-forged-broadcast` / `achievements.world-first-broadcast`

## 📈 Tier unlock ladder (the Altar now grants new things per tier)

| Upgrade step | Unlock |
|---|---|
| I → II | **Aqua Affinity** on every piece |
| II → III | **Swift Sneak II** on leggings |
| III → IV | **Every full-set passive +1 amplifier** (Speed I→II, Regen I→II, ...) |
| IV → V | **Set signature trait** (below) |
| V → Ascended | **Arcane Bond**: your Ascended armor survives death |

Crystal costs: 4 / 8 / 16 / 24 / 40 (92 total for a full climb, per piece).
Each step also draws 192 J/tick from your Slimefun grid.

## 🔥 Tier V signature traits — every set gets a new soul

| Set | Signature | Effect |
|---|---|---|
| Storm | `THUNDER_STEP` | Melee hits taken discharge lightning back at the attacker (2 hearts) |
| Infernal | `MOLTEN_EDGE` | Melee hits ignite the target for 3 s |
| Frost | `GLACIAL_SHELL` | Stand still 3s → hard-freeze (7s) + Slowness III the next attacker |
| Void | `PHASE_WALK` | Take a hit → 10s window of **free ender pearls** (no self-damage, no throw cooldown, momentum wiped, portal shimmer on landing) |

Active only when *every* worn piece of the set is Tier V+ — one lagging piece holds it back, consistent with how tiers already scale. Third-party addon sets can set their own signature via `ArmorSetDefinition.Builder.signature(...)`.

## ⛓️ Arcane Bond (Ascended)

When *all four* worn pieces reach Ascended, they are **kept on death** instead of dropping. Implemented via `PlayerDeathEvent#getItemsToKeep()` with per-piece matching against worn armor.

## 🔮 Upgrade Altar overhaul

- **Live preview panel**: while a piece sits in the slot, the altar shows exactly what the next tier grants — tier climb, stat multiplier, cooldown cut, module/rune slots, and the exact crystal cost
- **Celebration effects** on the crafter (totem burst + level-up sound) — honoring `feedback.particles` / `feedback.sounds` toggles
- **First-to-Ascend milestone**: the first player to bring any piece of a set to Ascended tier is announced server-wide; claims are atomic (two players in one tick can't both win) and persisted in `ascension-data.yml`
- Idle / "nothing to upgrade" statuses are now explainable instead of silent

## 🧪 Custom enchantments & rebalance

- **Rune of Fire** now also grants `MOLTEN_EDGE` on any piece it is engraved into (fire-damage pairing with its fire immunity)
- **Night Vision removed** from the shared baseline — every piece now grants **Water Breathing** instead (pairing with helmet Respiration for the full underwater kit)
- **Blast Protection removed** from all pieces — explosions (creepers, TNT, ghasts) retain their bite against Arcane wearers

---

## ⚙️ New configuration (all live-reload via `/arcane reload`)

```yaml
achievements:
  first-ascended-broadcast: true   # server-wide milestone, one per set, forever

messages:
  first-ascended: '&6&lMilestone! &e{player} &7is the first player to Ascend a {set} &7piece!'
```

`tasks.night-vision-duration` is no longer used and can be deleted from older configs.

## 📦 New files a server should know about

- `ascension-data.yml` — auto-created; records who was first to Ascend each set
- `quest-data.yml` — unchanged, still records world-firsts and quest progress

## 🧩 Developer API additions

- `ArmorSetDefinition.Builder.signature(SetTrait)` — tier-gated signature behavior for custom sets
- New `SetTrait` values available for addons: `MOLTEN_EDGE`, `THUNDER_STEP`, `GLACIAL_SHELL`, `PHASE_WALK`, `ARCANE_BOND`

## 🛠 Internal

- `CosmeticService`: `forgeBurst`, `worldFirstBurst` and previously-dead `upgrade` are all wired and toggle-respecting
- Tier unlock logic centralized in `EquipmentService`, `ArcaneTickTask`, `ArmorItemFactory` and `ArcaneCombatListener`
- All machine-adjacent effects scheduled back to the main thread, honoring the async Slimefun ticker

---

**Install:** drop `ArcaneArmor.jar` into `plugins/`, restart. Back up your existing `quest-data.yml` before upgrading from any pre-release jar.
