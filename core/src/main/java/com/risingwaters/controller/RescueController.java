package com.risingwaters.controller;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.risingwaters.model.Civilian;

/** Coordinates foot escorts. Boat rescues will be implemented separately. */
public final class RescueController {
    public static final float INTERACTION_RANGE = 84f;
    private final Array<Civilian> civilians;
    private final Rectangle evacuationZone;
    private Civilian escorted;
    private String message = "Approach a civilian and press E to escort.";

    public RescueController(Array<Civilian> civilians, Rectangle evacuationZone) {
        this.civilians = civilians;
        this.evacuationZone = new Rectangle(evacuationZone);
    }

    public Civilian nearestAvailable(float x, float y) {
        Civilian nearest = null;
        float maxDistance2 = INTERACTION_RANGE * INTERACTION_RANGE;
        for (Civilian civilian : civilians) {
            if (!civilian.isWaiting() || civilian.requiresBoat()) continue;
            float d2 = civilian.getPosition().dst2(x, y);
            if (d2 <= maxDistance2) {
                nearest = civilian;
                maxDistance2 = d2;
            }
        }
        return nearest;
    }

    public void interact(float x, float y) {
        if (escorted != null) {
            message = "Bring your civilian to the bottom-left evacuation playground.";
            return;
        }
        Civilian nearest = nearestAvailable(x, y);
        if (nearest == null) {
            message = "No walkable civilian nearby. Move closer.";
            return;
        }
        nearest.beginEscort();
        escorted = nearest;
        message = "Escorting " + nearest.getId() + ". Go to the evacuation playground!";
    }

    public void update(float x, float y) {
        if (escorted == null) return;
        escorted.follow(x - 22f, y - 22f);
        if (evacuationZone.contains(x, y)) {
            escorted.completeEvacuation();
            message = escorted.getId() + " evacuated! " + getEvacuatedCount() + "/" + civilians.size + " safe.";
            escorted = null;
        }
    }

    public Civilian getEscorted() { return escorted; }
    public String getMessage() { return message; }
    public int getEvacuatedCount() {
        int n = 0;
        for (Civilian civilian : civilians) if (civilian.isEvacuated()) n++;
        return n;
    }
    public int getCivilianCount() { return civilians.size; }
    public boolean isComplete() { return getEvacuatedCount() == civilians.size && civilians.size > 0; }
    public boolean isInEvacuationZone(float x, float y) { return evacuationZone.contains(x, y); }
    public Rectangle getEvacuationZone() { return evacuationZone; }
    public Array<Civilian> getCivilians() { return civilians; }
}
