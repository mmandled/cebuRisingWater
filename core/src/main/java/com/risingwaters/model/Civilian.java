package com.risingwaters.model;

import com.badlogic.gdx.math.Vector2;

/** A civilian's mission state is owned by this model, not the rendering screen. */
public final class Civilian {
    public enum State { WAITING, ESCORTED, EVACUATED }

    private final String id;
    private final Vector2 position;
    private final boolean requiresBoat;
    private State state;

    public Civilian(String id, float x, float y, boolean requiresBoat, boolean alreadyRescued) {
        this.id = id;
        this.position = new Vector2(x, y);
        this.requiresBoat = requiresBoat;
        this.state = alreadyRescued ? State.EVACUATED : State.WAITING;
    }

    public String getId() { return id; }
    public Vector2 getPosition() { return position; }
    public boolean requiresBoat() { return requiresBoat; }
    public State getState() { return state; }
    public boolean isWaiting() { return state == State.WAITING; }
    public boolean isEvacuated() { return state == State.EVACUATED; }

    public void beginEscort() {
        if (state != State.WAITING || requiresBoat) {
            throw new IllegalStateException("Civilian cannot be escorted on foot: " + id);
        }
        state = State.ESCORTED;
    }

    public void follow(float x, float y) {
        if (state == State.ESCORTED) position.set(x, y);
    }

    public void completeEvacuation() {
        if (state != State.ESCORTED) {
            throw new IllegalStateException("Civilian is not being escorted: " + id);
        }
        state = State.EVACUATED;
    }
}
