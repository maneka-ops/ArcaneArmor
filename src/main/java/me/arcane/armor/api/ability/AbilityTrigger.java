package me.arcane.armor.api.ability;

/**
 * How an ability is fired.
 */
public enum AbilityTrigger {

    /**
     * Bound to the active key (swap-hand by default) and usable from a single worn piece.
     */
    ACTIVE(1),

    /**
     * Bound to the ultimate key (sneak + swap-hand by default) and gated behind the full set.
     */
    ULTIMATE(4);

    private final int requiredPieces;

    AbilityTrigger(int requiredPieces) {
        this.requiredPieces = requiredPieces;
    }

    /** How many pieces of the owning set must be worn. */
    public int getRequiredPieces() {
        return requiredPieces;
    }
}
