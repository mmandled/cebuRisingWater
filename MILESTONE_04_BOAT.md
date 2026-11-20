# Milestone 04 — Boat rescue prototype

This builds on Milestone 03. Keep using the existing Talamban map and fixed camera.

## Controls

- WASD / arrow keys: walk or steer the boat
- B: board the boat nearby, or leave it when beside dry ground
- E: escort a civilian on foot, or pick up a stranded civilian while driving the boat
- U: after leaving the boat, unload one passenger for a walk to the evacuation playground
- F3: show building collisions
- F4: show the boat's flood-zone boundary
- ESC: main menu

## Try this

1. Launch the game and walk north toward the flooded upper road's right-hand side.
2. Find the orange boat around the right road (roughly x=1500 on the map).
3. Stand near it and press B. The boat stays inside the FloodZones rectangle and avoids buildings.
4. Drive up and across the upper road. Find orange civilians `civilian_01` and `civilian_05` and press E nearby to pick them up.
5. The boat can carry 2 people. Go back toward the right-hand shore and press B when near dry land.
6. While close to the boat, press U to unload one passenger. Bring that civilian to the bottom-left evacuation area. Return and unload the next passenger.
7. Other civilians can still be escorted on foot as in Milestone 03.

## What's changed

- New `model/RescueBoat.java` class: boat position, speed, health, passenger capacity
- Updated `model/Civilian.java`: added a boarded state
- Updated `controller/RescueController.java`: boat pickup and passenger unloading
- Updated `view/screen/TalambanGameScreen.java`: boat controls and display
- Updated `assets/maps/mission1/mission1_talamban.tmx`: a `BoatSpawn` point and two civilians with `requiresBoat=true`

## Still to do

This is a testable boat-rescue prototype, not the complete flood system. FloodZones currently limit boat navigation; we have **not** added rising water, foot damage, boat damage from hazards, multiple flood areas, or barriers. Boat HP is tracked but no hazard damages it yet. There are no character or boat sprites yet.

The flood zone currently overlaps part of the city, so foot movement isn't restricted by flood water in this milestone; we should refine the flood layout before adding that rule. For now use F4 to see the boundary.

## Testing

Run `Lwjgl3Launcher` in IntelliJ. The original developer environment could not run Gradle here, so compile and run this locally before merging into your main branch. If errors appear, keep your working Milestone 03 version as backup.
