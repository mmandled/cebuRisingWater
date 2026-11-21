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
import com.risingwaters.mission.MissionResult;

public final class MissionResultScreen extends ScreenAdapter {
    private final CebuRisingWatersGame game;
    private final MissionResult result;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(960, 600, camera);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();

    public MissionResultScreen(CebuRisingWatersGame game, MissionResult result) {
        this.game = game;
        this.result = result;
        font.getData().setScale(1.6f);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            game.setScreen(new TalambanGameScreen(game));
            dispose();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
            return;
        }

        viewport.apply();
        ScreenUtils.clear(0.055f, 0.085f, 0.13f, 1f);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.valueOf("152C40"));
        shapes.rect(90, 65, 780, 480);
        shapes.setColor(result.isSuccess() ? Color.valueOf("65D6A0") : Color.valueOf("E88F85"));
        shapes.rect(90, 520, 780, 7);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(result.isSuccess() ? Color.valueOf("83E4B1") : Color.valueOf("F7A7A1"));
        draw(result.isSuccess() ? "MISSION COMPLETE" : "MISSION FAILED", 120, 488);
        font.setColor(Color.WHITE);
        draw("MISSION 1 - TALAMBAN", 120, 450);
        draw(result.getReason(), 120, 416);
        draw("Civilians evacuated: " + result.getRescued() + "/" + result.getCivilians(), 120, 370);
        draw("Garbage cleared: " + result.getGarbageCleared() + "/" + result.getTotalGarbage(), 120, 340);
        draw("Drainage cleared: " + result.getDrainsCleared() + "/" + result.getTotalDrains(), 120, 310);
        draw("Barriers placed: " + result.getBarriersPlaced(), 120, 280);
        draw("Boat HP: " + result.getBoatHealth() + "  |  Player HP: " + result.getPlayerHealth(), 120, 250);
        draw("Repair kits used: " + result.getKitsUsed() + "  |  Cleaners used: " + result.getCleanersUsed(), 120, 220);
        int minutes = (int) result.getSeconds() / 60;
        int seconds = (int) result.getSeconds() % 60;
        draw(String.format("Time: %02d:%02d", minutes, seconds), 120, 190);
        font.setColor(Color.valueOf("F4DD92"));
        draw("Funds earned: " + result.getReward() + "   |   Total funds: " + game.getFunds(), 120, 150);
        font.setColor(Color.LIGHT_GRAY);
        draw("ENTER: Menu (shop coming in Milestone 08)   R: Retry   ESC: Menu", 120, 103);
        batch.end();
    }

    private void draw(String value, float x, float y) { font.draw(batch, value, x, y); }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void dispose() { font.dispose(); shapes.dispose(); batch.dispose(); }
}
