# Cebu: Rising Waters — Mission 1 Gameplay Milestone 01

Uses your latest original `assets/maps/mission1/mission1_talamban.tmx`, unchanged.

## Changes
- `MainMenuScreen`: Start Game now opens `TalambanGameScreen`.
- `TalambanGameScreen`: loads and renders the real TMX; uses the `PlayerSpawn` object (`player-start`), draws a temporary blue/yellow square, follows it with a camera, and blocks movement at `BuildingCollisions` rectangles.
- `CebuRisingWatersGame`: passes the game reference into the menu for screen changes.

## Controls
- Start Game from the menu.
- WASD / arrow keys: move the temporary player.
- F3: display collision rectangles in red.
- ESC: return to the main menu.

## Run in IntelliJ (Windows)
1. Open this project folder (where `settings.gradle` is).
2. Let Gradle sync (Internet required if Gradle/dependencies are not cached).
3. Run `Lwjgl3Launcher` or `gradlew.bat lwjgl3:run` from the project root.
4. Click Start Game and test walking around buildings.

## Current scope and caveats
- No finished character sprite, gameplay interactions, flood simulation, barriers, mission timer, or rescue logic yet.
- Uses existing building collisions only. Your `Civilians`, `Drainage`, `Garbage`, `EvacuationZone` and `FloodZones` remain untouched, ready for future integration.
- Some of your map's civilian markers visually overlap buildings; we should revise these positions before rescue gameplay.
- Not tested by launching LibGDX in this environment; Gradle tried to download its distribution, but internet was unavailable. Test on Windows/IntelliJ before treating this as a verified working build.
