# Milestone 03 — Civilian escort prototype

This milestone preserves your Talamban map, fixed overview camera, movement, and building collisions.

New Java classes:
- `core/src/main/java/com/risingwaters/model/Civilian.java` — owns each civilian's state.
- `core/src/main/java/com/risingwaters/controller/RescueController.java` — manages escort and safe-zone delivery.

Updated:
- `core/src/main/java/com/risingwaters/view/screen/TalambanGameScreen.java` — loads Civilians and EvacuationZone TMX objects; draws placeholder civilian markers and HUD; handles E.

## Test in IntelliJ
1. Open the extracted project as a Gradle project. Run `lwjgl3:run` or `Lwjgl3Launcher` as before.
2. Main Menu > Start Game. Confirm the full map remains stationary.
3. Walk near a white civilian marker and press `E` (within 84 world pixels).
4. The civilian becomes cyan and follows your player marker; bring them to the bottom-left playground.
5. They disappear on delivery and the counter increases. Try all eight.
6. `F3` shows building collisions; `ESC` returns to menu.

## Scope and known limitations
- This is a **foot escort interaction prototype**, not yet the final rescue system. Boat handling, health, civilian pathfinding, multiple passengers, flood hazards, mission victory, and animation are later work.
- Each civilian is a simple circular placeholder; marker positions come from saved TMX objects, not hardcoded positions.
- The `requiresBoat` property is respected: civilians with `requiresBoat=true` cannot be escorted on foot until a later boat mechanic exists.
- The current TMX shows `requiresBoat=false` for all eight civilians.
- If an interaction point is behind a building collision, adjust the civilian point in Tiled before balancing the mission.
- This milestone does not write back rescue state to the TMX and does not save progress across restarts.
- Local compile/runtime was NOT verified in the sandbox: Gradle distribution download failed due to no network access.

The ZIP deliberately excludes `.git`, IDE caches, and generated build directories; keep your existing Git repository and move/merge the updated source/assets into it. Make a commit or backup first.
