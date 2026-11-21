package com.risingwaters.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.risingwaters.CebuRisingWatersGame;

public final class MissionSelectScreen extends ScreenAdapter {
    private final CebuRisingWatersGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(960, 600, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();

    public MissionSelectScreen(CebuRisingWatersGame game) {
        this.game = game;
        font.getData().setScale(1.6f);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            game.setScreen(new TalambanGameScreen(game));
            dispose();
            return;
        }
        viewport.apply();
        ScreenUtils.clear(0.055f, 0.085f, 0.13f, 1f);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.valueOf("152C40"));
        shapes.rect(85, 70, 790, 460);
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.valueOf("8DDFD5"));
        font.draw(batch, "MISSIONS", 112, 495);
        font.setColor(Color.WHITE);
        font.draw(batch, "1. Talamban - The First Warning  [PLAY: press 1]", 112, 431);
        font.draw(batch, "2. Banilad / Colon - When the Roads Disappear", 112, 362);
        font.draw(batch, "3. A.S. Fortuna - Rising Pressure", 112, 293);
        font.draw(batch, "4. Guadalupe - The Storm", 112, 224);
        font.setColor(Color.valueOf("F4DD92"));
        font.draw(batch, game.isMissionUnlocked(2)
                ? "Mission 2 unlocked - gameplay coming in Milestone 10"
                : "Mission 2 locked - finish Talamban to unlock", 112, 323);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Missions 3 and 4 are locked until their levels are added.", 112, 170);
        font.draw(batch, "ESC: Main menu", 112, 107);
        batch.end();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void dispose() { shapes.dispose(); batch.dispose(); font.dispose(); }
}
