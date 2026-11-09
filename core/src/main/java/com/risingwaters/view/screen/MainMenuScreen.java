
package com.risingwaters.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MainMenuScreen extends ScreenAdapter {

    private static final float WIDTH = 1280;
    private static final float HEIGHT = 720;

    private static final float BUTTON_WIDTH = 290;
    private static final float BUTTON_HEIGHT = 52;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont buttonFont;

    private Texture background;

    private OrthographicCamera camera;
    private FitViewport viewport;

    private String[] menuOptions = {
        "Start Game",
        "Missions",
        "Settings",
        "Credits",
        "Exit"
    };

    private Rectangle[] buttons = new Rectangle[5];

    private int hoveredButton = -1;

    public MainMenuScreen() {

        camera = new OrthographicCamera();

        viewport = new FitViewport(WIDTH, HEIGHT, camera);

        viewport.update(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            true
        );

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Load the smooth font.
        FreeTypeFontGenerator generator =
            new FreeTypeFontGenerator(
                Gdx.files.internal("fonts/Roboto-Bold.ttf")
            );

        titleFont = createFont(generator, 58);
        subtitleFont = createFont(generator, 30);
        buttonFont = createFont(generator, 24);

        generator.dispose();

        // Center all buttons horizontally.
        float buttonX = (WIDTH - BUTTON_WIDTH) / 2;

        for (int i = 0; i < buttons.length; i++) {

            float buttonY = 350 - (i * 66);

            buttons[i] = new Rectangle(
                buttonX,
                buttonY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
            );
        }

        // Load the background.
        background = new Texture(
            Gdx.files.internal("images/main-menu-bg.png")
        );

        background.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );
    }

    private BitmapFont createFont(
        FreeTypeFontGenerator generator,
        int size
    ) {

        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.size = size;
        parameter.color = Color.WHITE;

        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;

        return generator.generateFont(parameter);
    }

    @Override
    public void show() {

        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean touchDown(
                int screenX,
                int screenY,
                int pointer,
                int button
            ) {

                if (button != Input.Buttons.LEFT) {
                    return false;
                }

                Vector2 position = new Vector2(screenX, screenY);

                viewport.unproject(position);

                int selected = getButtonIndex(
                    position.x,
                    position.y
                );

                if (selected != -1) {
                    handleButton(selected);
                }

                return true;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {

                Vector2 position = new Vector2(screenX, screenY);

                viewport.unproject(position);

                hoveredButton = getButtonIndex(
                    position.x,
                    position.y
                );

                return true;
            }
        });
    }

    @Override
    public void render(float delta) {

        ScreenUtils.clear(0.05f, 0.10f, 0.18f, 1f);

        viewport.apply();

        camera.update();

        batch.setProjectionMatrix(camera.combined);

        // Draw background.
        batch.begin();

        batch.draw(
            background,
            0,
            0,
            WIDTH,
            HEIGHT
        );

        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);

        Gdx.gl.glBlendFunc(
            GL20.GL_SRC_ALPHA,
            GL20.GL_ONE_MINUS_SRC_ALPHA
        );

        shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw buttons without the dark transparent panel.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < buttons.length; i++) {

            if (i == hoveredButton) {

                shapeRenderer.setColor(
                    0.25f, 0.48f, 0.78f, 1f
                );

            } else if (i == 0) {

                shapeRenderer.setColor(
                    0.08f, 0.32f, 0.85f, 1f
                );

            } else {

                shapeRenderer.setColor(
                    0.08f, 0.16f, 0.27f, 1f
                );
            }

            Rectangle button = buttons[i];

            shapeRenderer.rect(
                button.x,
                button.y,
                button.width,
                button.height
            );
        }

        shapeRenderer.end();

        // Draw button borders.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        shapeRenderer.setColor(
            0.55f, 0.75f, 1f, 1f
        );

        for (Rectangle button : buttons) {

            shapeRenderer.rect(
                button.x,
                button.y,
                button.width,
                button.height
            );
        }

        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Draw centered title.
        batch.begin();

        GlyphLayout title = new GlyphLayout(
            titleFont,
            "CEBU"
        );

        titleFont.draw(
            batch,
            title,
            (WIDTH - title.width) / 2,
            590
        );

        GlyphLayout subtitle = new GlyphLayout(
            subtitleFont,
            "RISING WATERS"
        );

        subtitleFont.draw(
            batch,
            subtitle,
            (WIDTH - subtitle.width) / 2,
            535
        );

        // Draw centered button labels.
        for (int i = 0; i < menuOptions.length; i++) {

            GlyphLayout text = new GlyphLayout(
                buttonFont,
                menuOptions[i]
            );

            Rectangle button = buttons[i];

            float textX = button.x
                + (button.width - text.width) / 2;

            float textY = button.y
                + (button.height + text.height) / 2;

            buttonFont.draw(
                batch,
                text,
                textX,
                textY
            );
        }

        batch.end();
    }

    private int getButtonIndex(float x, float y) {

        for (int i = 0; i < buttons.length; i++) {

            if (buttons[i].contains(x, y)) {
                return i;
            }
        }

        return -1;
    }

    private void handleButton(int index) {

        switch (index) {

            case 0:
                Gdx.app.log("MainMenu", "Start Game selected.");
                break;

            case 1:
                Gdx.app.log("MainMenu", "Missions selected.");
                break;

            case 2:
                Gdx.app.log("MainMenu", "Settings selected.");
                break;

            case 3:
                Gdx.app.log("MainMenu", "Credits selected.");
                break;

            case 4:
                Gdx.app.exit();
                break;
        }
    }

    @Override
    public void resize(int width, int height) {

        viewport.update(width, height, true);
    }

    @Override
    public void hide() {

        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {

        batch.dispose();
        shapeRenderer.dispose();

        titleFont.dispose();
        subtitleFont.dispose();
        buttonFont.dispose();

        background.dispose();
    }
}
