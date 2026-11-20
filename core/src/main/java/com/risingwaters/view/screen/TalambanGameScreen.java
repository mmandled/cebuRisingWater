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
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.risingwaters.model.Civilian;
import com.risingwaters.model.RescueBoat;
import com.risingwaters.controller.RescueController;
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

// Talamban gameplay screen. The player and boat are placeholders for now.
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
    private boolean floodDebug = false;
    private RescueController rescue;
    private OrthographicCamera hudCamera;
    private ScreenViewport hudViewport;
    private static final float CIVILIAN_RADIUS = 14f;
    private static final float BOAT_RANGE = 112f;
    private RescueBoat boat;
    private Rectangle boatWater;
    private boolean drivingBoat;
    private String boatMessage = "B: board the boat when close to shore";


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
        loadRescueObjects();
        loadBoat();
        hudCamera = new OrthographicCamera();
        hudViewport = new ScreenViewport(hudCamera);

        viewport.update(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            true
        );

        centerCameraOnMap();
        hudViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
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
        // LibGDX already flips Tiled's Y coordinates when loading the map.
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

    private void loadRescueObjects() {
        MapLayer civilianLayer = map.getLayers().get("Civilians");
        MapLayer zoneLayer = map.getLayers().get("EvacuationZone");
        if (civilianLayer == null || zoneLayer == null) {
            throw new IllegalStateException("Civilians / EvacuationZone object layer missing");
        }
        Array<Civilian> civilianModels = new Array<>();
        for (MapObject object : civilianLayer.getObjects()) {
            Object px = object.getProperties().get("x");
            Object py = object.getProperties().get("y");
            if (!(px instanceof Number) || !(py instanceof Number)) continue;
            boolean requiresBoat = Boolean.TRUE.equals(object.getProperties().get("requiresBoat"));
            boolean rescued = Boolean.TRUE.equals(object.getProperties().get("rescued"));
            civilianModels.add(new Civilian(object.getName(),
                ((Number) px).floatValue(), ((Number) py).floatValue(), requiresBoat, rescued));
        }
        Rectangle evacuation = null;
        for (MapObject object : zoneLayer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                evacuation = ((RectangleMapObject) object).getRectangle();
                break;
            }
        }
        if (evacuation == null) throw new IllegalStateException("No rectangle in EvacuationZone");
        rescue = new RescueController(civilianModels, evacuation);
        Gdx.app.log("Talamban", "Loaded " + civilianModels.size + " civilians and evacuation zone");
    }

    private void loadBoat() {
        MapLayer floodLayer = map.getLayers().get("FloodZones");
        MapLayer boatLayer = map.getLayers().get("BoatSpawn");
        if (floodLayer == null || boatLayer == null || boatLayer.getObjects().getCount() == 0) {
            throw new IllegalStateException("Missing FloodZones or BoatSpawn layer");
        }
        for (MapObject obj : floodLayer.getObjects()) {
            if (obj instanceof RectangleMapObject) {
                boatWater = new Rectangle(((RectangleMapObject) obj).getRectangle());
                break;
            }
        }
        if (boatWater == null) throw new IllegalStateException("FloodZones needs a rectangle");
        MapObject spawn = boatLayer.getObjects().get(0);
        float x = ((Number) spawn.getProperties().get("x")).floatValue();
        float y = ((Number) spawn.getProperties().get("y")).floatValue();
        // These are starting values. We can connect them to the shop later.
        boat = new RescueBoat(x, y, 2, 175f, 100f);
    }

    private float centerX() { return player.x + player.width / 2f; }
    private float centerY() { return player.y + player.height / 2f; }

    private boolean nearBoat() {
        return boat.getPosition().dst2(centerX(), centerY()) <= BOAT_RANGE * BOAT_RANGE;
    }

    private boolean isOnDryGround(float x, float y) {
        Rectangle area = new Rectangle(x - PLAYER_SIZE / 2f, y - PLAYER_SIZE / 2f, PLAYER_SIZE, PLAYER_SIZE);
        if (boatWater.overlaps(area) || x < 0 || y < 0 || x > mapWidth || y > mapHeight) return false;
        for (Rectangle wall : walls) if (wall.overlaps(area)) return false;
        return true;
    }

    private void toggleBoat() {
        if (drivingBoat) {
            // Find a free spot on land beside the boat.
            float bx = boat.getPosition().x;
            float by = boat.getPosition().y;
            float[][] offsets = {{0,-65},{0,65},{-65,0},{65,0},{-90,0},{90,0},{0,-90},{0,90}};
            for (float[] offset : offsets) {
                float x = bx + offset[0];
                float y = by + offset[1];
                if (!isOnDryGround(x, y)) continue;
                player.setPosition(x - PLAYER_SIZE/2f, y - PLAYER_SIZE/2f);
                drivingBoat = false;
                boatMessage = "On shore. Press U near the boat to unload a passenger.";
                return;
            }
            boatMessage = "Move the boat close to a dry shore before leaving.";
        } else {
            if (rescue.isEscorting()) { boatMessage = "Finish your foot escort first."; return; }
            if (!nearBoat()) { boatMessage = "Get closer to the boat to board."; return; }
            if (!boat.canMove()) { boatMessage = "The boat needs repairs."; return; }
            drivingBoat = true;
            boatMessage = "Driving boat. E: rescue | B: go ashore";
        }
    }

    private void handleBoatControls(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) toggleBoat();
        if (drivingBoat) {
            input.set(0, 0);
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) input.x--;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) input.x++;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) input.y++;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) input.y--;
            if (input.len2() > 0) input.nor().scl(boat.getSpeed() * delta);
            boat.move(input.x, input.y, boatWater, walls);
            player.setPosition(boat.getPosition().x - PLAYER_SIZE/2f, boat.getPosition().y - PLAYER_SIZE/2f);
        } else {
            updatePlayer(delta);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (drivingBoat) rescue.pickUpByBoat(boat);
            else rescue.interact(centerX(), centerY());
        }
        if (!drivingBoat && nearBoat() && Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            rescue.unloadFromBoat(boat, centerX(), centerY());
        }
        if (!drivingBoat) rescue.update(centerX(), centerY());
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) collisionDebug = !collisionDebug;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) floodDebug = !floodDebug;

        handleBoatControls(Math.min(delta, 1f / 30f));
        viewport.apply();
        ScreenUtils.clear(0.10f, 0.12f, 0.14f, 1f);
        mapRenderer.setView(camera);
        mapRenderer.render();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Yellow boat marker until we add our real boat sprite.
        shapes.setColor(drivingBoat ? Color.GOLD : Color.ORANGE);
        shapes.ellipse(boat.getPosition().x - 24f, boat.getPosition().y - 17f, 48f, 34f);
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(boat.getPosition().x - 12f, boat.getPosition().y - 7f, 24f, 14f);
        if (!drivingBoat) {
            shapes.setColor(Color.valueOf("F8DF73"));
            shapes.rect(player.x, player.y, player.width, player.height);
            shapes.setColor(Color.valueOf("2358AD"));
            shapes.rect(player.x + 5, player.y + 5, player.width - 10, player.height - 10);
        }
        shapes.end();

        // White = waiting, cyan = following, orange = needs a boat.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Civilian civilian : rescue.getCivilians()) {
            if (civilian.isEvacuated() || civilian.getState() == Civilian.State.BOARDED) continue;
            if (civilian.getState() == Civilian.State.ESCORTED) shapes.setColor(Color.CYAN);
            else if (civilian.requiresBoat()) shapes.setColor(Color.ORANGE);
            else shapes.setColor(Color.WHITE);
            shapes.circle(civilian.getPosition().x, civilian.getPosition().y, CIVILIAN_RADIUS);
            shapes.setColor(Color.DARK_GRAY);
            shapes.circle(civilian.getPosition().x, civilian.getPosition().y, CIVILIAN_RADIUS / 2f);
        }
        shapes.end();

        if (floodDebug) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(Color.CYAN);
            shapes.rect(boatWater.x, boatWater.y, boatWater.width, boatWater.height);
            shapes.end();
        }
        if (collisionDebug) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(Color.RED);
            for (Rectangle wall : walls) shapes.rect(wall.x, wall.y, wall.width, wall.height);
            shapes.setColor(Color.YELLOW);
            shapes.rect(player.x, player.y, player.width, player.height);
            shapes.end();
        }
        // Draw the text after the map so it stays on screen.
        hudViewport.apply();
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        float top = Gdx.graphics.getHeight() - 18f;
        font.draw(batch, "MISSION 1: TALAMBAN | WASD Move | B Boat | E Rescue | U Unload | F3 Collisions | F4 Flood | ESC Menu", 14f, top);
        font.draw(batch, "Evacuated: " + rescue.getEvacuatedCount() + "/" + rescue.getCivilianCount()
            + "  |  " + (rescue.isComplete() ? "All civilians safe!" : rescue.getMessage()), 14f, top - 22f);
        font.draw(batch, "Boat HP " + (int)boat.getHealth() + "/" + (int)boat.getMaxHealth()
                + "  |  Passengers " + boat.getPassengerCount() + "/" + boat.getCapacity()
                + "  |  " + (drivingBoat ? "IN BOAT" : "ON FOOT"), 14f, top - 44f);
        font.draw(batch, boatMessage, 14f, top - 66f);
        if (!drivingBoat && rescue.getEscorted() == null && nearBoat() && boat.getPassengerCount() > 0) {
            font.draw(batch, "Press U to help one passenger off the boat.", 14f, top - 88f);
        }
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
        hudViewport.update(width, height, true);
    }

    @Override public void dispose() { font.dispose(); batch.dispose(); shapes.dispose(); mapRenderer.dispose(); map.dispose(); }
}
