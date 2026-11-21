# Milestone 07 - Mission objectives and results

This version keeps the Mission 06.1 rescue-first gameplay. No map layouts or assets were replaced.

## Test in IntelliJ

1. Run `Lwjgl3Launcher`, then select Start Game.
2. Verify the objective tracker shows 8 civilians, 4 garbage piles, 2 drains, and flood recovery.
3. Press C before rescuing everyone. Cleanup should stay locked.
4. Press B in the playground, pick up civilians with E and unload near the playground using U.
5. After evacuating all eight, clear garbage and drains with C.
6. When the water has receded, the results screen should show the correct rescue/cleanup counts, health, supplies used, elapsed time, and funds earned.
7. Press ENTER to return to the menu. Replay via Start Game and verify funds are not awarded a second time in the same game session.
8. For failure testing, deliberately lose all boat HP or player HP. The results screen should explain the failure and award zero funds. Press R to retry.

## Notes

- The rewards are provisional: 500 base + boat health x 2 + remaining player HP x 75 + unused repair kits x 30 + unused garbage cleaners x 15.
- Rewards remain in memory during the current game session; saving/loading and the shop come later.
- The results screen returns to the main menu because the shop is planned for Milestone 08.
- Gradle compilation was not possible in this environment because the wrapper distribution is not cached and network access is unavailable. Please test locally before committing.
