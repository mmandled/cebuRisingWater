package com.risingwaters.model;

public final class GarbagePile extends CleanupTarget {
    public GarbagePile(String id, float x, float y) {
        super(id, x, y, false);
    }

    @Override public String getLabel() { return "Garbage"; }
}
