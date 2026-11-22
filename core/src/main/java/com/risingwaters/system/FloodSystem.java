package com.risingwaters.system;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.risingwaters.model.CleanupTarget;
import com.risingwaters.model.Drainage;
import com.risingwaters.model.GarbagePile;
import com.risingwaters.model.FloodBarrier;

// Handles water levels, cleanup, and the moving currents.
public final class FloodSystem {
    public static final float BOAT_DEPTH = 0.18f;
    public static final float WALKABLE_DEPTH = 0.20f;
    private static final float CLEANUP_RANGE = 90f;
    private final float mapWidth;
    private final float mapHeight;
    private final Rectangle evacuation;
    private final Array<Drainage> drains = new Array<>();
    private final Array<GarbagePile> garbage = new Array<>();
    private final Array<FloodBarrier> barriers = new Array<>();
    private static final float BARRIER_RANGE = 118f;
    private static final float PROTECTION_RADIUS = 175f;
    private int barrierSupplies;
    private String barrierMessage = "P: place a barrier near a marked spot.";
    private float depth = 1f;
    private float targetDepth = 1f;
    private int cleaners;
    private Drainage cleaningDrain;
    private float cleaningProgress;
    private String message = "Hold C near a drain, or press C near garbage.";

    public FloodSystem(float mapWidth, float mapHeight, Rectangle evacuation) {
        this(mapWidth, mapHeight, evacuation, 4, 3);
    }

    public FloodSystem(float mapWidth, float mapHeight, Rectangle evacuation,
            int startingCleaners, int startingBarriers) {
        cleaners = Math.max(0, startingCleaners);
        barrierSupplies = Math.max(0, startingBarriers);
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.evacuation = new Rectangle(evacuation);
    }

    public void addDrain(Drainage drain) { drains.add(drain); }
    public void addGarbage(GarbagePile pile) { garbage.add(pile); }
    public void addBarrier(FloodBarrier barrier) { barriers.add(barrier); }
    public Array<FloodBarrier> getBarriers() { return barriers; }
    public int getBarrierSupplies() { return barrierSupplies; }
    public String getBarrierMessage() { return barrierMessage; }

    public boolean isProtected(float x, float y) {
        float r2 = PROTECTION_RADIUS * PROTECTION_RADIUS;
        for (FloodBarrier barrier : barriers) {
            if (barrier.isPlaced() && barrier.getPosition().dst2(x, y) <= r2) return true;
        }
        return false;
    }

    public void placeBarrier(float x, float y) {
        FloodBarrier closest = null;
        float best = BARRIER_RANGE * BARRIER_RANGE;
        for (FloodBarrier barrier : barriers) {
            if (barrier.isPlaced()) continue;
            float dist = barrier.getPosition().dst2(x, y);
            if (dist <= best) { closest = barrier; best = dist; }
        }
        if (closest == null) { barrierMessage = "Move closer to a barrier spot."; return; }
        if (barrierSupplies <= 0) { barrierMessage = "No barriers left."; return; }
        closest.place();
        barrierSupplies--;
        barrierMessage = closest.getId() + " placed. Current is weaker nearby.";
    }
    public Array<Drainage> getDrains() { return drains; }
    public Array<GarbagePile> getGarbage() { return garbage; }
    public Rectangle getEvacuation() { return evacuation; }
    public float getDepth() { return depth; }
    public float getTargetDepth() { return targetDepth; }
    public int getCleaners() { return cleaners; }
    public String getMessage() { return message; }
    public void setMessage(String text) { message = text; }
    public float getCleaningProgress() { return cleaningProgress; }
    public Drainage getCleaningDrain() { return cleaningDrain; }

    public int getTotalTasks() { return drains.size + garbage.size; }
    public int getFinishedTasks() {
        int done = 0;
        for (Drainage d : drains) if (d.isCleared()) done++;
        for (GarbagePile g : garbage) if (g.isCleared()) done++;
        return done;
    }
    public boolean allClean() { return getTotalTasks() > 0 && getFinishedTasks() == getTotalTasks(); }

    public boolean insideMap(float x, float y) {
        return x >= 0 && y >= 0 && x <= mapWidth && y <= mapHeight;
    }

    public boolean isWet(float x, float y) {
        return insideMap(x, y) && !evacuation.contains(x, y) && depth > BOAT_DEPTH;
    }

    public boolean canBoatOccupy(float x, float y, float radius) {
        if (depth <= BOAT_DEPTH) return false;
        Rectangle hitbox = new Rectangle(x-radius, y-radius, radius*2f, radius*2f);
        return hitbox.x >= 0 && hitbox.y >= 0 && hitbox.x + hitbox.width <= mapWidth
            && hitbox.y + hitbox.height <= mapHeight && !hitbox.overlaps(evacuation);
    }

    // Water flows in lanes, so different parts of town push different ways.
    public Vector2 currentAt(float x, float y, Vector2 out) {
        if (!isWet(x, y)) return out.setZero();
        int lane = (int)(y / 220f);
        float direction = lane % 2 == 0 ? -1f : 1f;
        float vertical = (float)Math.sin(x / 290f + lane) * 0.32f;
        float strength = isProtected(x, y) ? 0.25f : 1f;
        return out.set(direction, vertical).nor().scl(36f * depth * strength);
    }

    public void update(float delta) {
        // Water drains slowly after each cleanup.
        depth = MathUtils.lerp(depth, targetDepth, Math.min(1f, delta * 0.55f));
        if (Math.abs(depth-targetDepth) < 0.005f) depth = targetDepth;
    }

    public void cancelCleaning() { cleaningDrain = null; cleaningProgress = 0f; }

    public void clean(float x, float y, float delta, boolean held, boolean justPressed) {
        if (!held) { cancelCleaning(); return; }
        GarbagePile nearbyGarbage = null;
        float best = CLEANUP_RANGE * CLEANUP_RANGE;
        for (GarbagePile pile : garbage) {
            if (pile.isCleared()) continue;
            float distance = pile.getPosition().dst2(x, y);
            if (distance < best) { nearbyGarbage = pile; best = distance; }
        }
        if (nearbyGarbage != null && justPressed) {
            cancelCleaning();
            if (cleaners <= 0) { message = "No garbage cleaners left."; return; }
            nearbyGarbage.clear();
            cleaners--;
            message = nearbyGarbage.getId() + " cleared!";
            updateTarget();
            return;
        }
        Drainage nearbyDrain = null;
        best = CLEANUP_RANGE * CLEANUP_RANGE;
        for (Drainage drain : drains) {
            if (drain.isCleared()) continue;
            float distance = drain.getPosition().dst2(x, y);
            if (distance < best) { nearbyDrain = drain; best = distance; }
        }
        if (nearbyDrain == null) {
            cancelCleaning();
            if (justPressed) message = "Get closer to garbage or a drain.";
            return;
        }
        if (nearbyDrain != cleaningDrain) { cleaningDrain = nearbyDrain; cleaningProgress = 0f; }
        cleaningProgress += delta;
        message = "Cleaning " + nearbyDrain.getId() + " (hold C): "
            + (int)(100f * Math.min(1f, cleaningProgress / nearbyDrain.getCleaningTime())) + "%";
        if (cleaningProgress >= nearbyDrain.getCleaningTime()) {
            nearbyDrain.clear();
            message = nearbyDrain.getId() + " unclogged!";
            cancelCleaning();
            updateTarget();
        }
    }

    private void updateTarget() {
        // Every task helps; finishing all of them drains the rest.
        if (allClean()) { targetDepth = 0f; return; }
        float reduction = 0f;
        for (Drainage d : drains) if (d.isCleared()) reduction += d.getFloodReduction();
        for (GarbagePile g : garbage) if (g.isCleared()) reduction += 0.10f;
        targetDepth = Math.max(0.25f, 1f - reduction);
    }
}
