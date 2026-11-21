package com.risingwaters.mission;

public final class MissionObjective {
    private final String description;
    private final int target;
    private int progress;

    public MissionObjective(String description, int target) {
        this.description = description;
        this.target = Math.max(1, target);
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(target, progress));
    }

    public String getDescription() { return description; }
    public int getProgress() { return progress; }
    public int getTarget() { return target; }
    public boolean isDone() { return progress >= target; }
    public String getText() { return description + ": " + progress + "/" + target; }
}
