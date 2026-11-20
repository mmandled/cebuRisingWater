package com.risingwaters.model;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class RescueBoat {
    private final Vector2 position;
    private final Array<Civilian> passengers = new Array<>();
    private final int capacity;
    private final float speed;
    private final float maxHealth;
    private float health;

    public RescueBoat(float x, float y, int capacity, float speed, float maxHealth) {
        position = new Vector2(x, y);
        this.capacity = capacity;
        this.speed = speed;
        this.maxHealth = maxHealth;
        health = maxHealth;
    }

    public Vector2 getPosition() { return position; }
    public int getCapacity() { return capacity; }
    public int getPassengerCount() { return passengers.size; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getSpeed() { return speed; }
    public boolean hasRoom() { return passengers.size < capacity; }
    public boolean canMove() { return health > 0; }

    public void move(float dx, float dy, Rectangle water, Array<Rectangle> walls) {
        if (!canMove()) return;
        float newX = position.x + dx;
        float newY = position.y + dy;
        Rectangle hitbox = new Rectangle(newX - 18, newY - 18, 36, 36);
        if (!water.contains(hitbox)) return;
        for (Rectangle wall : walls) if (wall.overlaps(hitbox)) return;
        position.set(newX, newY);
    }

    public boolean board(Civilian civilian) {
        if (!hasRoom() || !canMove() || !civilian.isWaiting()) return false;
        civilian.beginBoatRide();
        passengers.add(civilian);
        return true;
    }

    public Civilian unloadOne() {
        if (passengers.size == 0) return null;
        Civilian civilian = passengers.removeIndex(0);
        civilian.leaveBoat();
        return civilian;
    }

    public void takeDamage(float amount) {
        health = Math.max(0f, health - Math.max(0f, amount));
    }
}
