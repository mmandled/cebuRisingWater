package com.risingwaters.mission;

import com.badlogic.gdx.utils.Array;

public abstract class Mission {
    public enum Status { ACTIVE, SUCCESS, FAILED }

    private final String name;
    protected final Array<MissionObjective> objectives = new Array<>();
    private Status status = Status.ACTIVE;
    private String failureReason = "";
    private float elapsed;

    protected Mission(String name) { this.name = name; }

    public void tick(float delta) {
        if (status == Status.ACTIVE) elapsed += Math.max(0f, delta);
    }

    protected void succeed() { status = Status.SUCCESS; }
    protected void fail(String reason) {
        status = Status.FAILED;
        failureReason = reason;
    }

    public String getName() { return name; }
    public Status getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public float getElapsed() { return elapsed; }
    public Array<MissionObjective> getObjectives() { return objectives; }
    public boolean isFinished() { return status != Status.ACTIVE; }
}
