package com.risingwaters;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.risingwaters.shop.Shop;
import com.risingwaters.view.screen.MainMenuScreen;

public class CebuRisingWatersGame extends Game {
    private Preferences save;
    private Shop shop;
    private int funds;
    private boolean talambanRewardClaimed;

    @Override
    public void create() {
        save = Gdx.app.getPreferences("cebu-rising-waters-progress");
        funds = save.getInteger("funds", 0);
        talambanRewardClaimed = save.getBoolean("talambanRewardClaimed", false);
        shop = new Shop(save);
        setScreen(new MainMenuScreen(this));
    }

    public int getFunds() { return funds; }
    public Shop getShop() { return shop; }
    public boolean isMissionUnlocked(int mission) {
        return mission == 1 || (mission == 2 && talambanRewardClaimed);
    }

    public boolean buyUpgrade(int type) {
        int cost = shop.getCost(type);
        if (!shop.buy(type, funds)) return false;
        funds -= cost;
        save.putInteger("funds", funds);
        save.flush();
        return true;
    }


    public void awardTalambanCompletion(int amount) {
        funds += Math.max(0, amount);
        talambanRewardClaimed = true;

        save.putInteger("funds", funds);
        save.putBoolean("talambanRewardClaimed", true);
        save.flush();
    }


    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        super.dispose();
    }
}
