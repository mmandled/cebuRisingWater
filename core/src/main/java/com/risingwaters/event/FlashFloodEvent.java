package com.risingwaters.event;

public final class FlashFloodEvent extends DisasterEvent {
    private static final float FIRST_SURGE = 35f;
    private static final float SURGE_LENGTH = 6f;
    private static final float BETWEEN_SURGES = 55f;
    private float untilNext = FIRST_SURGE;
    private float remaining;

    @Override
    public void update(float delta, boolean enabled) {
        if (!enabled) { active = false; remaining = 0f; return; }
        if (active) {
            remaining -= delta;
            if (remaining <= 0f) {
                active = false;
                untilNext = BETWEEN_SURGES;
            }
        } else {
            untilNext -= delta;
            if (untilNext <= 0f) {
                active = true;
                remaining = SURGE_LENGTH;
            }
        }
    }

    public float getSurgeAmount() { return active ? 0.15f : 0f; }
    @Override public String getMessage() {
        return active ? "FLASH FLOOD! Stronger currents for " + (int)Math.ceil(remaining) + "s"
                : "Flash flood alert in " + (int)Math.ceil(untilNext) + "s";
    }
}
