package com.risingwaters.model;

import com.badlogic.gdx.math.Vector2;

public final class FloodBarrier {
    private final String id;
    private final Vector2 position;
    private boolean placed;

    public FloodBarrier(String id, float x, float y) {
        this.id = id;
        position = new Vector2(x, y);
    }

    public String getId() { return id; }
    public Vector2 getPosition() { return position; }
    public boolean isPlaced() { return placed; }
    public void place() { placed = true; }
}
