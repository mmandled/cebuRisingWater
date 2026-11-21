package com.risingwaters.model;

public final class Drainage extends CleanupTarget {
    private final float cleaningTime;
    private final float floodReduction;

    public Drainage(String id, float x, float y, boolean clogged, float cleaningTime, float floodReduction) {
        super(id, x, y, !clogged);
        this.cleaningTime = Math.max(0.5f, cleaningTime);
        this.floodReduction = Math.max(0f, floodReduction);
    }

    public float getCleaningTime() { return cleaningTime; }
    public float getFloodReduction() { return floodReduction; }
    @Override public String getLabel() { return "Drain"; }
}
