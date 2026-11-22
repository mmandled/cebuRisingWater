package com.risingwaters.model;

import com.badlogic.gdx.math.Vector2;

public final class Volunteer {
    private final String name;
    private final Vector2 position = new Vector2();
    private CleanupTarget task;
    private float timeLeft;

    public Volunteer(String name) { this.name = name; }
    public String getName() { return name; }
    public Vector2 getPosition() { return position; }
    public CleanupTarget getTask() { return task; }
    public boolean isBusy() { return task != null; }

    public void assign(CleanupTarget target, float duration) {
        task = target;
        position.set(target.getPosition());
        timeLeft = duration;
    }

    public boolean update(float delta) {
        if (task == null) return false;
        timeLeft = Math.max(0f, timeLeft - delta);
        return timeLeft == 0f;
    }

    public void finish() { task = null; }
}
