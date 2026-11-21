package com.risingwaters.mission;

public final class MissionResult {
    private final boolean success;
    private final String reason;
    private final int rescued;
    private final int civilians;
    private final int garbageCleared;
    private final int totalGarbage;
    private final int drainsCleared;
    private final int totalDrains;
    private final int barriersPlaced;
    private final int kitsUsed;
    private final int cleanersUsed;
    private final int playerHealth;
    private final int boatHealth;
    private final float seconds;
    private final int reward;

    public MissionResult(boolean success, String reason, int rescued, int civilians,
            int garbageCleared, int totalGarbage, int drainsCleared, int totalDrains,
            int barriersPlaced, int kitsUsed, int cleanersUsed, int playerHealth,
            int boatHealth, float seconds, int reward) {
        this.success = success;
        this.reason = reason;
        this.rescued = rescued;
        this.civilians = civilians;
        this.garbageCleared = garbageCleared;
        this.totalGarbage = totalGarbage;
        this.drainsCleared = drainsCleared;
        this.totalDrains = totalDrains;
        this.barriersPlaced = barriersPlaced;
        this.kitsUsed = kitsUsed;
        this.cleanersUsed = cleanersUsed;
        this.playerHealth = playerHealth;
        this.boatHealth = boatHealth;
        this.seconds = seconds;
        this.reward = reward;
    }

    public boolean isSuccess() { return success; }
    public String getReason() { return reason; }
    public int getRescued() { return rescued; }
    public int getCivilians() { return civilians; }
    public int getGarbageCleared() { return garbageCleared; }
    public int getTotalGarbage() { return totalGarbage; }
    public int getDrainsCleared() { return drainsCleared; }
    public int getTotalDrains() { return totalDrains; }
    public int getBarriersPlaced() { return barriersPlaced; }
    public int getKitsUsed() { return kitsUsed; }
    public int getCleanersUsed() { return cleanersUsed; }
    public int getPlayerHealth() { return playerHealth; }
    public int getBoatHealth() { return boatHealth; }
    public float getSeconds() { return seconds; }
    public int getReward() { return reward; }
}
