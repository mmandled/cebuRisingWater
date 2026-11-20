package com.risingwaters.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.risingwaters.CebuRisingWatersGame;

/**
 * Initial playable Talamban map: rendering, camera, WASD, and solid building collisions.
 * This deliberately uses a placeholder marker until character artwork is ready.
 * Tiled object coordinates are converted from top-left origin to LibGDX bottom-left origin.
 */
public class TalambanGameScreen extends ScreenAdapter {
    private static final String MAP_PATH = "maps/mission1/mission1_talamban.tmx";
    private static final float VIEW_WIDTH = 960f;
    private static final float VIEW_HEIGHT = 540f;
    private static final float SPEED = 215f;
    private static final float PLAYER_SIZE = 27f;

    private final CebuRisingWatersGame game;
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private float mapWidth;
    private float mapHeight;
    private final Rectangle player = new Rectangle();
    private final Array<Rectangle> walls = new Array<>();
    private final Vector2 input = new Vector2();
    private boolean collisionDebug = false;

    public TalambanGameScreen(CebuRisingWatersGame game) {
        this.game = game;
        map = new TmxMapLoader().load(MAP_PATH);
        mapWidth = ((Number) map.getProperties().get("width")).floatValue()
                * ((Number) map.getProperties().get("tilewidth")).floatValue();
        mapHeight = ((Number) map.getProperties().get("height")).floatValue()
                * ((Number) map.getProperties().get("tileheight")).floatValue();
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f);
        camera = new OrthographicCamera();
        viewport = new FitViewport(mapWidth, mapHeight, camera);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        loadBuildingCollisions();
        loadPlayerSpawn();

        viewport.update(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            true
        );

        centerCameraOnMap();

    }

    private void loadBuildingCollisions() {
        MapLayer layer = map.getLayers().get("BuildingCollisions");
        if (layer == null) {
            Gdx.app.error("Talamban", "BuildingCollisions layer missing");
            return;
        }
        for (MapObject object : layer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle r = ((RectangleMapObject) object).getRectangle();
                walls.add(new Rectangle(r));
            }
        }
    }

    private void loadPlayerSpawn() {
        // Tiled point coordinates are top-down; libGDX's map loader converts them
        // into y-up coordinates, with y=0 at the bottom of the map.
        MapLayer layer = map.getLayers().get("PlayerSpawn");
        if (layer == null || layer.getObjects().getCount() == 0) {
            throw new IllegalStateException("PlayerSpawn layer is missing or empty");
        }
        MapObject spawn = layer.getObjects().get("player-start");
        if (spawn == null) spawn = layer.getObjects().get(0);
        float x = ((Number) spawn.getProperties().get("x")).floatValue();
        float y = ((Number) spawn.getProperties().get("y")).floatValue();
        player.set(x - PLAYER_SIZE / 2f, y - PLAYER_SIZE / 2f, PLAYER_SIZE, PLAYER_SIZE);
        player.x = MathUtils.clamp(player.x, 0, mapWidth - player.width);
        player.y = MathUtils.clamp(player.y, 0, mapHeight - player.height);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) collisionDebug = !collisionDebug;

        updatePlayer(Math.min(delta, 1f / 30f));
        viewport.apply();
        ScreenUtils.clear(0.10f, 0.12f, 0.14f, 1f);
        mapRenderer.setView(camera);
        mapRenderer.render();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.valueOf("F8DF73"));
        shapes.rect(player.x, player.y, player.width, player.height);
        shapes.setColor(Color.valueOf("2358AD"));
        shapes.rect(player.x + 5, player.y + 5, player.width - 10, player.height - 10);
        shapes.end();

        if (collisionDebug) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(Color.RED);
            for (Rectangle wall : walls) shapes.rect(wall.x, wall.y, wall.width, wall.height);
            shapes.setColor(Color.YELLOW);
            shapes.rect(player.x, player.y, player.width, player.height);
            shapes.end();
        }
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(
            batch,
            "MISSION 1 - TALAMBAN | WASD: Move | F3: Collisions | ESC: Menu",
            20f,
            mapHeight - 20f
        );
        batch.end();
    }

    private void updatePlayer(float delta) {
        input.set(0f, 0f);
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) input.x--;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) input.x++;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) input.y++;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) input.y--;
        if (input.len2() > 0) input.nor().scl(SPEED * delta);
        moveAxis(input.x, true);
        moveAxis(input.y, false);
    }

    private void moveAxis(float amount, boolean xAxis) {
        if (amount == 0f) return;
        if (xAxis) player.x = MathUtils.clamp(player.x + amount, 0f, mapWidth - player.width);
        else player.y = MathUtils.clamp(player.y + amount, 0f, mapHeight - player.height);
        for (Rectangle wall : walls) {
            if (player.overlaps(wall)) {
                if (xAxis) player.x = amount > 0 ? wall.x - player.width : wall.x + wall.width;
                else player.y = amount > 0 ? wall.y - player.height : wall.y + wall.height;
            }
        }
    }

    private void centerCameraOnMap() {
        camera.position.set(
            mapWidth / 2f,
            mapHeight / 2f,
            0f
        );

        camera.update();
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        centerCameraOnMap();
    }

    @Override public void dispose() { font.dispose(); batch.dispose(); shapes.dispose(); mapRenderer.dispose(); map.dispose(); }
}
