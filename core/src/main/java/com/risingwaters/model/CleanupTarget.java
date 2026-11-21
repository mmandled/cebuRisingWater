package com.risingwaters.model;

import com.badlogic.gdx.math.Vector2;

// A cleanup spot on the map.
public abstract class CleanupTarget {
    private final String id;
    private final Vector2 position;
    private boolean cleared;

    protected CleanupTarget(String id, float x, float y, boolean cleared) {
        this.id = id;
        position = new Vector2(x, y);
        this.cleared = cleared;
    }

    public String getId() { return id; }
    public Vector2 getPosition() { return position; }
    public boolean isCleared() { return cleared; }
    public void clear() { cleared = true; }
    public abstract String getLabel();
}
