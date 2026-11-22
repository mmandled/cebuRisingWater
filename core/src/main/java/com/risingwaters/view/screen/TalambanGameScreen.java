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
import com.risingwaters.model.FloodBarrier;
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
import com.risingwaters.shop.Shop;
import com.risingwaters.mission.Mission;
import com.risingwaters.mission.MissionObjective;
import com.risingwaters.mission.MissionResult;
import com.risingwaters.mission.TalambanMission;

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
    private float safeSpawnX;
    private float safeSpawnY;
    private final Rectangle player = new Rectangle();
    private final Array<Rectangle> walls = new Array<>();
    private final Rectangle evacuationGate = new Rectangle();
    private final Rectangle unloadZone = new Rectangle();
    private final Vector2 input = new Vector2();
    private boolean collisionDebug = false;
    private boolean floodDebug = false;
    private RescueController rescue;
    private OrthographicCamera hudCamera;
    private ScreenViewport hudViewport;
    private static final float CIVILIAN_RADIUS = 14f;
    private static final float DOCK_RANGE = 112f;
    private static final float PLAYER_WATER_DAMAGE_INTERVAL = 2.0f;
    private RescueBoat boat;
    private FloodSystem flood;
    private final Vector2 current = new Vector2();
    private float collisionCooldown;
    private int playerHealth = 3;
    private float deepWaterTime;
    private int repairKits;
    private String healthMessage = "Stay in the boat when the water is deep.";
    private boolean drivingBoat;
    private boolean boatSummoned;
    private TalambanMission mission;
    private boolean cleaningLockedMessageShown;
    private float waterDamageCooldown;
    private String boatMessage = "Press B at the evacuation gate to launch your rescue boat.";


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
        loadGateAreas();
        loadPlayerSpawn();
        loadRescueObjects();
        loadBoat();
        loadFloodCleanup();
        mission = new TalambanMission(rescue, flood);
        // Start at the safe playground so the boat is available right away.
        movePlayerToSafeStart();
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

    private Rectangle readZone(String layerName) {
        MapLayer layer = map.getLayers().get(layerName);
        if (layer == null) throw new IllegalStateException(layerName + " layer missing");
        for (MapObject object : layer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                return new Rectangle(((RectangleMapObject)object).getRectangle());
            }
        }
        throw new IllegalStateException(layerName + " needs a rectangle");
    }

    private void loadGateAreas() {
        evacuationGate.set(readZone("EvacuationGate"));
        unloadZone.set(readZone("BoatUnloadZone"));
        MapLayer fences = map.getLayers().get("EvacuationFence");
        if (fences == null) throw new IllegalStateException("EvacuationFence missing");
        for (MapObject object : fences.getObjects()) {
            if (object instanceof RectangleMapObject) {
                walls.add(new Rectangle(((RectangleMapObject)object).getRectangle()));
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
        safeSpawnX = player.x;
        safeSpawnY = player.y;
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
        // The player carries the boat. It appears after pressing B.
        boat = new RescueBoat(0f, 0f,
                2 + game.getShop().getLevel(Shop.BOAT_CAPACITY),
                175f * (1f + 0.15f * game.getShop().getLevel(Shop.BOAT_SPEED)),
                100f + 25f * game.getShop().getLevel(Shop.BOAT_HEALTH));
        repairKits = 2 + game.getShop().getLevel(Shop.REPAIR_SUPPLIES);
        boatSummoned = false;
    }

    private void movePlayerToSafeStart() {
        // Use the spawn we placed in Tiled.
        player.setPosition(safeSpawnX, safeSpawnY);
    }

    private boolean closeToDock(float x, float y) {
        // Passengers can only be unloaded at the gate.
        return unloadZone.contains(x, y);
    }

    private boolean findBoatLaunch() {
        // Try the shoreline spawn from Tiled first.
        MapLayer spawnLayer = map.getLayers().get("BoatSpawn");
        if (spawnLayer != null) {
            for (MapObject object : spawnLayer.getObjects()) {
                float x = number(object, "x", -100f);
                float y = number(object, "y", -100f);
                if (!flood.canBoatOccupy(x, y, 18f)) continue;
                Rectangle hitbox = new Rectangle(x - 18f, y - 18f, 36f, 36f);
                boolean blocked = false;
                for (Rectangle wall : walls) if (wall.overlaps(hitbox)) { blocked = true; break; }
                if (!blocked) {
                    boat.getPosition().set(x, y);
                    return true;
                }
            }
        }
        Rectangle safe = rescue.getEvacuationZone();
        float right = safe.x + safe.width;
        float top = safe.y + safe.height;
        // Pick a shoreline spot without any building collisions.
        float[][] spots = {
            {right + 32f, safe.y + safe.height * 0.5f},
            {right + 44f, safe.y + safe.height * 0.7f},
            {right + 44f, safe.y + safe.height * 0.3f},
            {safe.x + safe.width * 0.75f, top + 35f},
            {safe.x + safe.width * 0.5f, top + 35f}
        };
        for (float[] spot : spots) {
            if (!flood.canBoatOccupy(spot[0], spot[1], 18f)) continue;
            Rectangle hitbox = new Rectangle(spot[0] - 18f, spot[1] - 18f, 36f, 36f);
            boolean blocked = false;
            for (Rectangle wall : walls) if (wall.overlaps(hitbox)) { blocked = true; break; }
            if (!blocked) {
                boat.getPosition().set(spot[0], spot[1]);
                return true;
            }
        }
        return false;
    }

    private void loadFloodCleanup() {
        flood = new FloodSystem(mapWidth, mapHeight, rescue.getEvacuationZone(),
                4 + game.getShop().getLevel(Shop.CLEANUP_TOOLS),
                3 + game.getShop().getLevel(Shop.BARRIER_SUPPLIES));
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
            float faster = 1f + 0.20f * game.getShop().getLevel(Shop.CLEANUP_TOOLS);
            flood.addDrain(new Drainage(obj.getName(), x, y, clogged, time / faster, reduction));
        }
        for (MapObject obj : garbage.getObjects()) {
            flood.addGarbage(new GarbagePile(obj.getName(), number(obj, "x", 0f), number(obj, "y", 0f)));
        }
        MapLayer barrierLayer = map.getLayers().get("BarrierSpots");
        if (barrierLayer != null) {
            for (MapObject obj : barrierLayer.getObjects()) {
                flood.addBarrier(new FloodBarrier(obj.getName(), number(obj, "x", 0f), number(obj, "y", 0f)));
            }
        }
    }

    private float number(MapObject obj, String key, float fallback) {
        Object value = obj.getProperties().get(key);
        return value instanceof Number ? ((Number)value).floatValue() : fallback;
    }

    private float centerX() { return player.x + player.width / 2f; }
    private float centerY() { return player.y + player.height / 2f; }

    private boolean nearBoat() {
        return boatSummoned && boat.getPosition().dst2(centerX(), centerY()) <= DOCK_RANGE * DOCK_RANGE;
    }

    private void toggleBoat() {
        if (drivingBoat) {
            if (!closeToDock(boat.getPosition().x, boat.getPosition().y)) {
                boatMessage = "Bring the boat to the evacuation gate to disembark.";
                return;
            }
            // Store the boat again when getting off at the playground.
            drivingBoat = false;
            boatSummoned = false;
            movePlayerToSafeStart();
            boatMessage = "Boat stored. Press B to launch it again.";
            return;
        }
        if (!evacuationGate.contains(centerX(), centerY())) {
            boatMessage = "Go to the evacuation gate to launch the boat.";
            return;
        }
        if (rescue.isEscorting()) {
            boatMessage = "Finish your escort first.";
            return;
        }
        if (!boat.canMove()) {
            boatMessage = "Repair your boat first.";
            return;
        }
        if (flood.getDepth() <= FloodSystem.BOAT_DEPTH) {
            boatMessage = "The water is too shallow for the boat.";
            return;
        }
        if (!findBoatLaunch()) {
            boatMessage = "No clear launch space outside the gate.";
            return;
        }
        boatSummoned = true;
        drivingBoat = true;
        player.setPosition(boat.getPosition().x - PLAYER_SIZE/2f, boat.getPosition().y - PLAYER_SIZE/2f);
        boatMessage = "Boat launched. Pick up civilians with E, unload at the gate with U.";
    }

    private void handleBoatControls(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) toggleBoat();
        if (drivingBoat) {
            deepWaterTime = 0f;
            input.set(0, 0);
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) input.x--;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) input.x++;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) input.y++;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) input.y--;
            if (input.len2() > 0) input.nor().scl(boat.getSpeed());
            flood.currentAt(boat.getPosition().x, boat.getPosition().y, current);
            // The current is weaker near the evacuation gate.
            if (closeToDock(boat.getPosition().x, boat.getPosition().y)) current.scl(0.18f);
            if (Gdx.input.isKeyPressed(Input.Keys.C)) current.scl(0.10f);
            boolean hit = boat.sail((input.x + current.x) * delta,
                    (input.y + current.y) * delta, flood, walls);
            if (hit && current.len() >= 30f
                    && !closeToDock(boat.getPosition().x, boat.getPosition().y)
                    && !flood.isProtected(boat.getPosition().x, boat.getPosition().y)
                    && collisionCooldown <= 0f) {
                boat.takeDamage(10f);
                collisionCooldown = 1.2f;
                boatMessage = "Strong current collision! -10 boat HP";
            }
            if (!boat.canMove()) {
                drivingBoat = false;
                boatMessage = "Boat destroyed. Press R to retry.";
            }
            player.setPosition(boat.getPosition().x - PLAYER_SIZE/2f, boat.getPosition().y - PLAYER_SIZE/2f);
        } else {
            updatePlayer(delta);
            if (flood.isWet(centerX(), centerY())) {
                // Walking in floodwater costs health right away and while staying there.
                waterDamageCooldown -= delta;
                if (waterDamageCooldown <= 0f) {
                    playerHealth--;
                    waterDamageCooldown = PLAYER_WATER_DAMAGE_INTERVAL;
                    healthMessage = "Deep water! Lost 1 HP. Get back to the playground.";
                }
            } else {
                waterDamageCooldown = 0f;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (drivingBoat) rescue.pickUpByBoat(boat);
            else rescue.interact(centerX(), centerY());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            if (!drivingBoat) {
                boatMessage = "Board the rescue boat first (B).";
            } else if (!closeToDock(boat.getPosition().x, boat.getPosition().y)) {
                boatMessage = "Dock at the evacuation gate to unload passengers.";
            } else if (boat.getPassengerCount() == 0) {
                boatMessage = "There are no passengers to unload.";
            } else {
                int delivered = rescue.evacuateBoatPassengers(boat);
                boatMessage = delivered + " civilians evacuated at the center!";
            }
        }
        if (!drivingBoat) rescue.update(centerX(), centerY());

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            float x = drivingBoat ? boat.getPosition().x : centerX();
            float y = drivingBoat ? boat.getPosition().y : centerY();
            flood.placeBarrier(x, y);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H) && repairKits > 0
                && boat.getHealth() < boat.getMaxHealth() && (drivingBoat || nearBoat()
                || rescue.isInEvacuationZone(centerX(), centerY()))) {
            boat.repair(35f);
            repairKits--;
            healthMessage = "Boat repaired (+35 HP).";
        }

        // Finish all rescues before the drains and garbage can be cleaned.
        boolean cleaning = Gdx.input.isKeyPressed(Input.Keys.C);
        if (rescue.isComplete()) {
            flood.clean(centerX(), centerY(), delta, cleaning,
                    Gdx.input.isKeyJustPressed(Input.Keys.C));
        } else {
            flood.cancelCleaning();
            if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                flood.setMessage("Rescue all civilians first (" + rescue.getEvacuatedCount()
                        + "/" + rescue.getCivilianCount() + ").");
            }
        }
        flood.update(delta);
        rescue.setAllowWalkingForAll(flood.getDepth() <= FloodSystem.WALKABLE_DEPTH);
        collisionCooldown = Math.max(0f, collisionCooldown - delta);

        if (flood.getDepth() <= FloodSystem.BOAT_DEPTH && drivingBoat) {
            drivingBoat = false;
            boatSummoned = false;
            movePlayerToSafeStart();
            boatMessage = "Flood cleared. Boat stored.";
        }
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            // Start the mission again from the beginning.
            game.setScreen(new TalambanGameScreen(game));
            dispose();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
            return;
        }
        float step = Math.min(delta, 1f / 30f);
        if (!mission.isFinished()) handleBoatControls(step);
        mission.update(step, rescue, flood, boat, playerHealth);
        if (mission.isFinished()) {
            MissionResult result = mission.makeResult(rescue, flood, boat, playerHealth, repairKits,
                    2 + game.getShop().getLevel(Shop.REPAIR_SUPPLIES),
                    4 + game.getShop().getLevel(Shop.CLEANUP_TOOLS));
            if (result.isSuccess()) game.awardTalambanOnce(result.getReward());
            game.setScreen(new MissionResultScreen(game, result));
            dispose();
            return;
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
        if (boatSummoned) {
            shapes.setColor(drivingBoat ? Color.GOLD : Color.ORANGE);
            shapes.ellipse(boat.getPosition().x - 24f, boat.getPosition().y - 17f, 48f, 34f);
            shapes.setColor(Color.DARK_GRAY);
            shapes.rect(boat.getPosition().x - 12f, boat.getPosition().y - 7f, 24f, 14f);
        }
        if (!drivingBoat) {
            shapes.setColor(Color.valueOf("F8DF73"));
            shapes.rect(player.x, player.y, player.width, player.height);
            shapes.setColor(Color.valueOf("2358AD"));
            shapes.rect(player.x + 5, player.y + 5, player.width - 10, player.height - 10);
        }
        shapes.end();

        // Mark the barrier spots before the mission art is added.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (FloodBarrier barrier : flood.getBarriers()) {
            shapes.setColor(barrier.isPlaced() ? Color.GREEN : Color.ORANGE);
            float x = barrier.getPosition().x;
            float y = barrier.getPosition().y;
            shapes.rect(x - 15f, y - 6f, 30f, 12f);
        }
        shapes.end();

        // White civilians are waiting, cyan is being escorted.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Civilian civilian : rescue.getCivilians()) {
            if (civilian.isEvacuated() || civilian.getState() == Civilian.State.BOARDED) continue;
            if (civilian.getState() == Civilian.State.ESCORTED) shapes.setColor(Color.CYAN);
            else shapes.setColor(Color.WHITE);
            shapes.circle(civilian.getPosition().x, civilian.getPosition().y, CIVILIAN_RADIUS);
            shapes.setColor(Color.DARK_GRAY);
            shapes.circle(civilian.getPosition().x, civilian.getPosition().y, CIVILIAN_RADIUS / 2f);
        }
        shapes.end();

        drawCleanupMarkers();

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
        hudViewport.apply();
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        float top = Gdx.graphics.getHeight() - 18f;
        font.draw(batch, "MISSION 1: TALAMBAN | B Boat | E Rescue | U Unload | C Clean | P Barrier | H Repair | R Retry | ESC Menu", 14f, top);
        font.draw(batch, "Evacuated: " + rescue.getEvacuatedCount() + "/" + rescue.getCivilianCount()
            + "  |  " + (rescue.isComplete() ? "All civilians safe!" : rescue.getMessage()), 14f, top - 22f);
        font.draw(batch, "Boat HP " + (int)boat.getHealth() + "/" + (int)boat.getMaxHealth()
                + "  |  Passengers " + boat.getPassengerCount() + "/" + boat.getCapacity()
                + "  |  " + (drivingBoat ? "IN BOAT" : "ON FOOT"), 14f, top - 44f);
        font.draw(batch, "Player HP: " + playerHealth + "/3 | Repair kits: " + repairKits
                + " | Barriers: " + flood.getBarrierSupplies() + " | " + healthMessage, 14f, top - 66f);
        font.draw(batch, "Stage: " + (rescue.isComplete() ? "CLEANUP" : "RESCUE FIRST")
                + " | Flood: " + (int)(flood.getDepth() * 100f) + "% | Cleanup: "
                + flood.getFinishedTasks() + "/" + flood.getTotalTasks()
                + " | Garbage cleaners: " + flood.getCleaners(), 14f, top - 88f);
        font.draw(batch, flood.getMessage() + "  |  Cyan grates = drains, orange circles = garbage", 14f, top - 110f);
        font.draw(batch, flood.getBarrierMessage(), 14f, top - 132f);
        font.draw(batch, boatMessage, 14f, top - 154f);
        if (flood.allClean() && flood.getDepth() <= 0.01f) {
            font.draw(batch, "All drains and garbage cleared! Flood gone.", 14f, top - 176f);
        }
        if (drivingBoat && boat.getPassengerCount() > 0) {
            font.draw(batch, "Passengers aboard: dock at the evacuation gate, then press U.", 14f, top - 198f);
        }
        // Mission progress stays visible while playing.
        float objectiveY = top - 226f;
        for (MissionObjective objective : mission.getObjectives()) {
            font.setColor(objective.isDone() ? Color.GREEN : Color.WHITE);
            String prefix = objective.isDone() ? "[DONE] " : "[ ] ";
            font.draw(batch, prefix + objective.getText(), 14f, objectiveY);
            objectiveY -= 20f;
        }
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawCleanupMarkers() {
        // Draw the cleanup spots so they are easy to find.
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Drainage drain : flood.getDrains()) {
            float x = drain.getPosition().x;
            float y = drain.getPosition().y;
            shapes.setColor(drain.isCleared() ? Color.FOREST : Color.CYAN);
            shapes.rect(x - 22f, y - 17f, 44f, 34f);
            shapes.setColor(Color.DARK_GRAY);
            shapes.rect(x - 17f, y - 12f, 34f, 24f);
            shapes.setColor(drain.isCleared() ? Color.GREEN : Color.LIGHT_GRAY);
            for (int i = -12; i <= 12; i += 8) {
                shapes.rect(x + i - 2f, y - 9f, 4f, 18f);
            }
        }
        for (GarbagePile pile : flood.getGarbage()) {
            float x = pile.getPosition().x;
            float y = pile.getPosition().y;
            shapes.setColor(pile.isCleared() ? Color.GREEN : Color.ORANGE);
            shapes.circle(x, y, 17f);
            shapes.setColor(Color.DARK_GRAY);
            shapes.rect(x - 9f, y - 7f, 18f, 14f);
            shapes.setColor(pile.isCleared() ? Color.GREEN : Color.LIGHT_GRAY);
            shapes.rect(x - 4f, y + 7f, 8f, 3f);
        }
        shapes.end();
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
        float oldX = player.x;
        float oldY = player.y;
        boolean wasInside = rescue.isInEvacuationZone(centerX(), centerY());
        if (xAxis) player.x = MathUtils.clamp(player.x + amount, 0f, mapWidth - player.width);
        else player.y = MathUtils.clamp(player.y + amount, 0f, mapHeight - player.height);
        boolean isInside = rescue.isInEvacuationZone(centerX(), centerY());
        if (wasInside != isInside && !evacuationGate.contains(centerX(), centerY())
                && !evacuationGate.contains(oldX + player.width / 2f, oldY + player.height / 2f)) {
            player.setPosition(oldX, oldY);
            return;
        }
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
