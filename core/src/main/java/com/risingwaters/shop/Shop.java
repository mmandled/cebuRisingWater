package com.risingwaters.shop;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;

public final class Shop {
    public static final int BOAT_CAPACITY = 0;
    public static final int BOAT_SPEED = 1;
    public static final int BOAT_HEALTH = 2;
    public static final int CLEANUP_TOOLS = 3;
    public static final int REPAIR_SUPPLIES = 4;
    public static final int BARRIER_SUPPLIES = 5;
    public static final int VOLUNTEERS = 6;

    private final Preferences save;
    private final Array<Upgrade> upgrades = new Array<>();
    private String message = "Choose an upgrade. Purchased upgrades stay saved.";

    public Shop(Preferences save) {
        this.save = save;
        upgrades.add(new Upgrade("Boat capacity", "Carry one extra civilian per trip", 350, 3));
        upgrades.add(new Upgrade("Boat speed", "Boat moves 15% faster per level", 250, 3));
        upgrades.add(new Upgrade("Boat health", "Boat starts with 25 more HP", 250, 3));
        upgrades.add(new Upgrade("Cleanup tools", "Faster drains and one extra cleaner", 200, 3));
        upgrades.add(new Upgrade("Repair supplies", "One extra repair kit each mission", 180, 2));
        upgrades.add(new Upgrade("Barrier supplies", "One extra barrier each mission", 180, 2));
        upgrades.add(new Upgrade("Volunteers", "One helper to clear drainage or garbage", 400, 3));
    }

    public Array<Upgrade> getUpgrades() { return upgrades; }
    public String getMessage() { return message; }
    public int getLevel(int type) { return save.getInteger("upgrade_" + type, 0); }
    public int getCost(int type) {
        Upgrade upgrade = upgrades.get(type);
        return upgrade.getBaseCost() * (getLevel(type) + 1);
    }

    public boolean buy(int type, int funds) {
        if (type < 0 || type >= upgrades.size) return false;
        Upgrade upgrade = upgrades.get(type);
        if (getLevel(type) >= upgrade.getMaxLevel()) {
            message = upgrade.getName() + " is already maxed out.";
            return false;
        }
        if (funds < getCost(type)) {
            message = "Not enough funds for " + upgrade.getName() + ".";
            return false;
        }
        save.putInteger("upgrade_" + type, getLevel(type) + 1);
        save.flush();
        message = upgrade.getName() + " upgraded!";
        return true;
    }

    public static final class Upgrade {
        private final String name;
        private final String description;
        private final int baseCost;
        private final int maxLevel;

        private Upgrade(String name, String description, int baseCost, int maxLevel) {
            this.name = name;
            this.description = description;
            this.baseCost = baseCost;
            this.maxLevel = maxLevel;
        }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getBaseCost() { return baseCost; }
        public int getMaxLevel() { return maxLevel; }
    }
}
