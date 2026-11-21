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
import com.risingwaters.model.Drainage;
import com.risingwaters.model.GarbagePile;
import com.risingwaters.system.FloodSystem;
import com.badlogic.gdx.graphics.GL20;
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
    private FloodSystem flood;
    private final Vector2 current = new Vector2();
    private float collisionCooldown;
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
        loadFloodCleanup();
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
        MapLayer boatLayer = map.getLayers().get("BoatSpawn");
        if (boatLayer == null || boatLayer.getObjects().getCount() == 0) {
            throw new IllegalStateException("BoatSpawn object layer missing");
        }
        MapObject spawn = boatLayer.getObjects().get(0);
        float x = ((Number) spawn.getProperties().get("x")).floatValue();
        float y = ((Number) spawn.getProperties().get("y")).floatValue();
        boat = new RescueBoat(x, y, 2, 175f, 100f);
    }

    private void loadFloodCleanup() {
        flood = new FloodSystem(mapWidth, mapHeight, rescue.getEvacuationZone());
        MapLayer drains = map.getLayers().get("Drainage");
        MapLayer garbage = map.getLayers().get("Garbage");
        if (drains == null || garbage == null) {
            throw new IllegalStateException("Missing Drainage or Garbage object layer");
        }
        for (MapObject obj : drains.getObjects()) {
            float x = number(obj, "x", 0f);
            float y = number(obj, "y", 0f);
            boolean clogged = Boolean.TRUE.equals(obj.getProperties().get("clogged"));
            float time = number(obj, "cleaningTime", 5f);
            float reduction = number(obj, "floodReduction", 0.2f);
            flood.addDrain(new Drainage(obj.getName(), x, y, clogged, time, reduction));
        }
        for (MapObject obj : garbage.getObjects()) {
            flood.addGarbage(new GarbagePile(obj.getName(), number(obj, "x", 0f), number(obj, "y", 0f)));
        }
    }

    private float number(MapObject obj, String key, float fallback) {
        Object value = obj.getProperties().get(key);
        return value instanceof Number ? ((Number)value).floatValue() : fallback;
    }

    private float centerX() { return player.x + player.width / 2f; }
    private float centerY() { return player.y + player.height / 2f; }

    private boolean nearBoat() {
        return boat.getPosition().dst2(centerX(), centerY()) <= BOAT_RANGE * BOAT_RANGE;
    }

    private boolean isOnDryGround(float x, float y) {
        Rectangle area = new Rectangle(x - PLAYER_SIZE / 2f, y - PLAYER_SIZE / 2f, PLAYER_SIZE, PLAYER_SIZE);
        if (flood.isWet(x, y) || x < 0 || y < 0 || x > mapWidth || y > mapHeight) return false;
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
            boatMessage = "Go to the playground edge or wait for water to drain.";
        } else {
            if (rescue.isEscorting()) { boatMessage = "Finish your foot escort first."; return; }
            if (!nearBoat()) { boatMessage = "Get closer to the boat to board."; return; }
            if (!boat.canMove()) { boatMessage = "The boat needs repairs."; return; }
            if (flood.getDepth() <= FloodSystem.BOAT_DEPTH) {
                boatMessage = "The flood is too shallow for the boat.";
                return;
            }
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
            if (input.len2() > 0) input.nor().scl(boat.getSpeed());
            flood.currentAt(boat.getPosition().x, boat.getPosition().y, current);
            // Hold C to anchor the boat while cleaning nearby drains.
            if (Gdx.input.isKeyPressed(Input.Keys.C)) current.scl(0.10f);
            // Even when the player lets go, the current can keep pushing.
            boolean hit = boat.sail((input.x + current.x) * delta,
                    (input.y + current.y) * delta, flood, walls);
            if (hit && current.len2() > 0f && collisionCooldown <= 0f) {
                boat.takeDamage(10f);
                collisionCooldown = 0.9f;
                boatMessage = "Boat hit something! -10 HP";
            }
            if (!boat.canMove()) {
                drivingBoat = false;
                boatMessage = "Boat damaged. You can still walk and clean.";
            }
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
        boolean cleaning = Gdx.input.isKeyPressed(Input.Keys.C);
        flood.clean(centerX(), centerY(), delta, cleaning,
                Gdx.input.isKeyJustPressed(Input.Keys.C));
        flood.update(delta);
        rescue.setAllowWalkingForAll(flood.getDepth() <= FloodSystem.WALKABLE_DEPTH);
        collisionCooldown = Math.max(0f, collisionCooldown - delta);
        if (flood.getDepth() <= FloodSystem.BOAT_DEPTH && drivingBoat) {
            // The boat stops once the water is too shallow.
            drivingBoat = false;
            boatMessage = "Water is shallow now. Press U to unload.";
        }
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            return;
        }
        if (!boat.canMove()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                game.setScreen(new TalambanGameScreen(game));
                return;
            }
        } else {
            handleBoatControls(Math.min(delta, 1f / 30f));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) collisionDebug = !collisionDebug;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) floodDebug = !floodDebug;

        viewport.apply();
        ScreenUtils.clear(0.10f, 0.12f, 0.14f, 1f);
        mapRenderer.setView(camera);
        mapRenderer.render();
        drawFlood();

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
            shapes.rect(0, 0, mapWidth, mapHeight);
            Rectangle safe = flood.getEvacuation();
            shapes.setColor(Color.GREEN);
            shapes.rect(safe.x, safe.y, safe.width, safe.height);
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
        hudViewport.apply()
        ;
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        float top = Gdx.graphics.getHeight() - 18f;
        font.draw(batch, "MISSION 1: TALAMBAN | WASD Move | B Boat | E Rescue | U Unload | Hold C Clean | F3/F4 Debug | ESC", 14f, top);
        font.draw(batch, "Evacuated: " + rescue.getEvacuatedCount() + "/" + rescue.getCivilianCount()
            + "  |  " + (rescue.isComplete() ? "All civilians safe!" : rescue.getMessage()), 14f, top - 22f);
        font.draw(batch, "Boat HP " + (int)boat.getHealth() + "/" + (int)boat.getMaxHealth()
                + "  |  Passengers " + boat.getPassengerCount() + "/" + boat.getCapacity()
                + "  |  " + (drivingBoat ? "IN BOAT" : "ON FOOT"), 14f, top - 44f);
        font.draw(batch, "Flood: " + (int)(flood.getDepth() * 100f) + "% | Cleanup: "
                + flood.getFinishedTasks() + "/" + flood.getTotalTasks()
                + " | Garbage cleaners: " + flood.getCleaners(), 14f, top - 66f);
        font.draw(batch, flood.getMessage(), 14f, top - 88f);
        font.draw(batch, boat.canMove() ? boatMessage : "BOAT DESTROYED - Press R to retry, ESC for menu",
                14f, top - 110f);
        if (flood.allClean() && flood.getDepth() <= 0.01f) {
            font.draw(batch, "All drains and garbage cleared! Flood gone.", 14f, top - 132f);
        }
        if (!drivingBoat && rescue.getEscorted() == null && nearBoat() && boat.getPassengerCount() > 0) {
            font.draw(batch, "Press U to help one passenger off the boat.", 14f, top - 154f);
        }
        batch.end();
    }

    private void drawFlood() {
        float depth = flood.getDepth();
        if (depth <= 0.005f) return;
        Rectangle safe = flood.getEvacuation();
        float left = MathUtils.clamp(safe.x, 0f, mapWidth);
        float right = MathUtils.clamp(safe.x + safe.width, 0f, mapWidth);
        float bottom = MathUtils.clamp(safe.y, 0f, mapHeight);
        float top = MathUtils.clamp(safe.y + safe.height, 0f, mapHeight);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.10f, 0.49f, 0.82f, 0.13f + 0.28f * depth);
        // Leave the playground dry.
        shapes.rect(0f, top, mapWidth, mapHeight - top);
        shapes.rect(0f, 0f, mapWidth, bottom);
        shapes.rect(0f, bottom, left, top - bottom);
        shapes.rect(right, bottom, mapWidth-right, top-bottom);
        shapes.end();
        // Show where the currents are flowing.
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.75f, 0.94f, 1f, 0.28f + 0.35f * depth);
        for (float y = 100f; y < mapHeight; y += 170f) {
            for (float x = 110f; x < mapWidth; x += 220f) {
                if (!flood.isWet(x, y)) continue;
                flood.currentAt(x, y, current);
                float endX = x + current.x * 0.37f;
                float endY = y + current.y * 0.37f;
                shapes.line(x, y, endX, endY);
                float d = current.x >= 0f ? -1f : 1f;
                shapes.line(endX, endY, endX + d * 9f, endY + 5f);
                shapes.line(endX, endY, endX + d * 9f, endY - 5f);
            }
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void updatePlayer(float delta) {
        input.set(0f, 0f);
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) input.x--;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) input.x++;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) input.y++;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) input.y--;
        // Walking in deep water is slower than on dry ground.
        float walkingSpeed = flood.isWet(centerX(), centerY()) ? SPEED * 0.65f : SPEED;
        if (input.len2() > 0) input.nor().scl(walkingSpeed * delta);
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
