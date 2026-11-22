package com.risingwaters.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.risingwaters.CebuRisingWatersGame;
import com.risingwaters.shop.Shop;

public final class ShopScreen extends ScreenAdapter {
    private final CebuRisingWatersGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(960, 600, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final Vector2 click = new Vector2();

    public ShopScreen(CebuRisingWatersGame game) {
        this.game = game;
        font.getData().setScale(1.3f);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
            return;
        }
        for (int i = 0; i < game.getShop().getUpgrades().size; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + i)) game.buyUpgrade(i);
        }
        if (Gdx.input.justTouched()) {
            click.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(click);
            if (click.x >= 90 && click.x <= 870) {
                for (int i = 0; i < game.getShop().getUpgrades().size; i++) {
                    float bottom = 440 - i * 53;
                    if (click.y >= bottom && click.y <= bottom + 49) game.buyUpgrade(i);
                }
            }
        }

        viewport.apply();
        ScreenUtils.clear(0.055f, 0.085f, 0.13f, 1f);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.valueOf("152C40"));
        shapes.rect(65, 45, 830, 520);
        for (int i = 0; i < game.getShop().getUpgrades().size; i++) {
            shapes.setColor(i % 2 == 0 ? Color.valueOf("244358") : Color.valueOf("1D384E"));
            shapes.rect(90, 440 - i * 53, 780, 49);
        }
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.valueOf("8DDFD5"));
        font.draw(batch, "RESPONSE EQUIPMENT SHOP", 92, 536);
        font.setColor(Color.WHITE);
        font.draw(batch, "Funds: " + game.getFunds() + "   |   Click an upgrade or press 1-7", 92, 506);
        Shop shop = game.getShop();
        for (int i = 0; i < shop.getUpgrades().size; i++) {
            Shop.Upgrade upgrade = shop.getUpgrades().get(i);
            int level = shop.getLevel(i);
            int max = upgrade.getMaxLevel();
            float top = 474 - i * 53;
            font.setColor(Color.WHITE);
            font.draw(batch, (i + 1) + ". " + upgrade.getName() + "  Lv " + level + "/" + max, 105, top);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, upgrade.getDescription(), 105, top - 21);
            font.setColor(level == max ? Color.GREEN : Color.valueOf("F4DD92"));
            font.draw(batch, level == max ? "MAX" : "Cost " + shop.getCost(i), 725, top - 10);
        }
        font.setColor(Color.valueOf("8DDFD5"));
        font.draw(batch, shop.getMessage(), 95, 79);
        font.setColor(Color.WHITE);
        font.draw(batch, "ESC / ENTER: Main menu   |   Mission 2 is coming later", 95, 56);
        batch.end();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void dispose() { shapes.dispose(); batch.dispose(); font.dispose(); }
}
