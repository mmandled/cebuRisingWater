package com.risingwaters.ability;

public final class MoraleBoost {
    private static final float DURATION = 4f;
    private static final float COOLDOWN = 14f;
    private float remaining;
    private float cooldown;

    public boolean activate() {
        if (remaining > 0f || cooldown > 0f) return false;
        remaining = DURATION;
        cooldown = COOLDOWN;
        return true;
    }

    public void update(float delta) {
        remaining = Math.max(0f, remaining - delta);
        cooldown = Math.max(0f, cooldown - delta);
    }

    public boolean isActive() { return remaining > 0f; }
    public float getRemaining() { return remaining; }
    public float getCooldown() { return cooldown; }
    public float getSpeedMultiplier() { return isActive() ? 1.35f : 1f; }
}
