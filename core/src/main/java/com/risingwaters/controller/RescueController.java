package com.risingwaters.controller;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.risingwaters.model.Civilian;
import com.risingwaters.model.RescueBoat;

// Handles civilians following the player and riding the boat.
public final class RescueController {
    public static final float INTERACTION_RANGE = 84f;
    private final Array<Civilian> civilians;
    private final Rectangle evacuationZone;
    private Civilian escorted;
    private boolean allowWalkingForAll;
    private String message = "Use the boat to rescue civilians during flooding.";

    public RescueController(Array<Civilian> civilians, Rectangle evacuationZone) {
        this.civilians = civilians;
        this.evacuationZone = new Rectangle(evacuationZone);
    }

    public void setAllowWalkingForAll(boolean allowed) { allowWalkingForAll = allowed; }

    public Civilian nearestAvailable(float x, float y) {
        Civilian nearest = null;
        float maxDistance2 = INTERACTION_RANGE * INTERACTION_RANGE;
        for (Civilian civilian : civilians) {
            if (!civilian.isWaiting() || !allowWalkingForAll) continue;
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
            message = allowWalkingForAll ? "No civilian nearby. Move closer." : "Too deep to walk-rescue. Use the boat.";
            return;
        }
        nearest.beginEscort(allowWalkingForAll);
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

    public Civilian nearestBoatRescue(float x, float y) {
        Civilian nearest = null;
        float best = INTERACTION_RANGE * INTERACTION_RANGE;
        for (Civilian c : civilians) {
            if (!c.isWaiting()) continue;
            float d = c.getPosition().dst2(x, y);
            if (d <= best) { nearest = c; best = d; }
        }
        return nearest;
    }

    public void pickUpByBoat(RescueBoat boat) {
        Civilian civilian = nearestBoatRescue(boat.getPosition().x, boat.getPosition().y);
        if (civilian == null) { message = "No stranded civilian close to the boat."; return; }
        if (!boat.hasRoom()) { message = "Boat is full. Return to shore."; return; }
        if (boat.board(civilian)) message = civilian.getId() + " boarded the boat.";
    }

    public void unloadFromBoat(RescueBoat boat, float x, float y) {
        if (escorted != null) { message = "Evacuate your current civilian first."; return; }
        Civilian civilian = boat.unloadOne();
        if (civilian == null) { message = "No passengers to unload."; return; }
        escorted = civilian;
        escorted.follow(x - 22f, y - 22f);
        message = "Escort " + civilian.getId() + " to the evacuation playground.";
    }

    public int evacuateBoatPassengers(RescueBoat boat) {
        int delivered = 0;
        // The playground staff receive passengers straight from the boat.
        Civilian passenger;
        while ((passenger = boat.unloadOne()) != null) {
            passenger.completeEvacuation();
            delivered++;
        }
        if (delivered > 0) {
            message = delivered + " passengers safe! " + getEvacuatedCount()
                    + "/" + civilians.size + " evacuated.";
        }
        return delivered;
    }

    public boolean isEscorting() { return escorted != null; }
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
