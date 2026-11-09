
package com.risingwaters;

import com.badlogic.gdx.Game;
import com.risingwaters.view.screen.MainMenuScreen;

public class CebuRisingWatersGame extends Game {

    @Override
    public void create() {

        setScreen(new MainMenuScreen());
    }

    @Override
    public void dispose() {

        if (getScreen() != null) {
            getScreen().dispose();
        }

        super.dispose();
    }
}
