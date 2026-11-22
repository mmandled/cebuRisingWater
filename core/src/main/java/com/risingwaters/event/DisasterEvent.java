package com.risingwaters.event;

public abstract class DisasterEvent {
    protected boolean active;
    public abstract void update(float delta, boolean enabled);
    public boolean isActive() { return active; }
    public abstract String getMessage();
}
