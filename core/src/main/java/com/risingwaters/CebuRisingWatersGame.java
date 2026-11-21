
package com.risingwaters;

import com.badlogic.gdx.Game;
import com.risingwaters.view.screen.MainMenuScreen;

public class CebuRisingWatersGame extends Game {
    private int funds;
    private boolean talambanRewardClaimed;

    public int getFunds() { return funds; }

    public void awardTalambanOnce(int amount) {
        if (talambanRewardClaimed) return;
        funds += Math.max(0, amount);
        talambanRewardClaimed = true;
    }


    @Override
    public void create() {

        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {

        if (getScreen() != null) {
            getScreen().dispose();
        }

        super.dispose();
    }
}
