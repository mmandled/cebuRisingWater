package com.risingwaters.controller;

import com.badlogic.gdx.utils.Array;
import com.risingwaters.model.CleanupTarget;
import com.risingwaters.model.Drainage;
import com.risingwaters.model.GarbagePile;
import com.risingwaters.model.Volunteer;
import com.risingwaters.system.FloodSystem;

public final class VolunteerController {
    private static final float ASSIGN_RANGE = 130f;
    private final Array<Volunteer> volunteers = new Array<>();
    private final FloodSystem flood;
    private String message = "V: assign a volunteer near an uncleared cleanup spot.";

    public VolunteerController(int recruited, FloodSystem flood) {
        this.flood = flood;
        for (int i = 0; i < recruited; i++) volunteers.add(new Volunteer("Volunteer " + (i + 1)));
    }

    public Array<Volunteer> getVolunteers() { return volunteers; }
    public String getMessage() { return message; }

    public void assign(float x, float y, boolean rescueFinished) {
        if (!rescueFinished) { message = "Rescue civilians before assigning cleanup."; return; }
        Volunteer helper = null;
        for (Volunteer v : volunteers) if (!v.isBusy()) { helper = v; break; }
        if (helper == null) {
            message = volunteers.size == 0 ? "Recruit volunteers in the shop first." : "All volunteers are busy.";
            return;
        }
        CleanupTarget target = null;
        float nearest = ASSIGN_RANGE * ASSIGN_RANGE;
        for (Drainage drain : flood.getDrains()) {
            if (drain.isCleared() || isAssigned(drain)) continue;
            float d = drain.getPosition().dst2(x, y);
            if (d < nearest) { target = drain; nearest = d; }
        }
        for (GarbagePile pile : flood.getGarbage()) {
            if (pile.isCleared() || isAssigned(pile)) continue;
            float d = pile.getPosition().dst2(x, y);
            if (d < nearest) { target = pile; nearest = d; }
        }
        if (target == null) { message = "Get closer to an uncleared cleanup spot."; return; }
        helper.assign(target, target instanceof Drainage ? 6f : 4f);
        message = helper.getName() + " assigned to " + target.getId();
    }

    private boolean isAssigned(CleanupTarget target) {
        for (Volunteer v : volunteers) if (v.getTask() == target) return true;
        return false;
    }

    public void update(float delta) {
        for (Volunteer volunteer : volunteers) {
            if (!volunteer.isBusy()) continue;
            CleanupTarget target = volunteer.getTask();
            if (target.isCleared()) { volunteer.finish(); continue; }
            if (volunteer.update(delta)) {
                flood.finishVolunteerCleanup(target);
                message = volunteer.getName() + " finished " + target.getId();
                volunteer.finish();
            }
        }
    }
}
