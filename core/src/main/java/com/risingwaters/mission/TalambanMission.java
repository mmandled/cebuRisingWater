package com.risingwaters.mission;

import com.risingwaters.controller.RescueController;
import com.risingwaters.model.Drainage;
import com.risingwaters.model.FloodBarrier;
import com.risingwaters.model.GarbagePile;
import com.risingwaters.model.RescueBoat;
import com.risingwaters.system.FloodSystem;

public final class TalambanMission extends Mission {
    private final MissionObjective rescueObjective;
    private final MissionObjective garbageObjective;
    private final MissionObjective drainageObjective;
    private final MissionObjective recoveryObjective;

    public TalambanMission(RescueController rescue, FloodSystem flood) {
        super("The First Warning - Talamban");
        rescueObjective = new MissionObjective("Evacuate civilians", rescue.getCivilianCount());
        garbageObjective = new MissionObjective("Remove garbage", flood.getGarbage().size);
        drainageObjective = new MissionObjective("Clear drainage", flood.getDrains().size);
        recoveryObjective = new MissionObjective("Water receded", 1);
        objectives.add(rescueObjective);
        objectives.add(garbageObjective);
        objectives.add(drainageObjective);
        objectives.add(recoveryObjective);
    }

    public void update(float delta, RescueController rescue, FloodSystem flood,
            RescueBoat boat, int playerHealth) {
        if (isFinished()) return;
        tick(delta);
        rescueObjective.setProgress(rescue.getEvacuatedCount());
        int garbage = 0;
        for (GarbagePile pile : flood.getGarbage()) if (pile.isCleared()) garbage++;
        garbageObjective.setProgress(garbage);
        int drains = 0;
        for (Drainage drain : flood.getDrains()) if (drain.isCleared()) drains++;
        drainageObjective.setProgress(drains);
        recoveryObjective.setProgress(flood.getDepth() <= 0.01f ? 1 : 0);

        if (playerHealth <= 0) fail("The responder ran out of health in floodwater.");
        else if (!boat.canMove()) fail("The rescue boat was destroyed.");
        else if (rescueObjective.isDone() && garbageObjective.isDone()
                && drainageObjective.isDone() && recoveryObjective.isDone()) succeed();
    }

    public String getPhase() {
        if (!rescueObjective.isDone()) return "RESCUE";
        if (!garbageObjective.isDone() || !drainageObjective.isDone()) return "CLEANUP";
        return "RECOVERY";
    }

    public MissionResult makeResult(RescueController rescue, FloodSystem flood,
            RescueBoat boat, int playerHealth, int repairKits,
            int startingKits, int startingCleaners) {
        if (!isFinished()) throw new IllegalStateException("Mission is still active");
        int barriers = 0;
        for (FloodBarrier b : flood.getBarriers()) if (b.isPlaced()) barriers++;
        int usedKits = Math.max(0, startingKits - repairKits);
        int usedCleaners = Math.max(0, startingCleaners - flood.getCleaners());
        // The reward is based on what the player managed to save.
        int reward = getStatus() == Status.SUCCESS
                ? 500 + Math.round(boat.getHealth()) * 2 + Math.max(0, playerHealth) * 75
                    + repairKits * 30 + Math.max(0, flood.getCleaners()) * 15
                : 0;
        return new MissionResult(getStatus() == Status.SUCCESS,
                getStatus() == Status.SUCCESS ? "All civilians safe and flood cleared." : getFailureReason(),
                rescueObjective.getProgress(), rescueObjective.getTarget(),
                garbageObjective.getProgress(), garbageObjective.getTarget(),
                drainageObjective.getProgress(), drainageObjective.getTarget(),
                barriers, usedKits, usedCleaners, playerHealth,
                Math.round(boat.getHealth()), getElapsed(), reward);
    }
}
