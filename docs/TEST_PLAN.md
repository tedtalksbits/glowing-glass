# Test plan: Glowing Glass

**Slice 1 (server side, plugin 0.1.0) is new and hasn't been in a real game.** The self-test passed on Paper 26.3-211
(22 checks); what needs a player is below. There's no mod yet, so glowing glass looks like a sea lantern, froglight
or shroomlight with a name: that's expected. `/glowingglass info` tells you what a block or item really is.

## Setup (once, about 5 minutes)

1. Double-click `run\build-and-install.cmd`. It stops the test server (`..\blueprints-mc\run\local-paper`), builds
   and tests the plugin, installs `plugins\GlowingGlass.jar`, then offers to start the server. (It skips the client:
   there's no mod yet.)
2. Join the test server with your normal 26.3 game (no mod needed for this slice).
3. In creative, **`/glowingglass demo`** builds all 16 in a wall 3 blocks in front of you (warm colours on top) and
   gives you a kit: 16 of each, plus 8 blue glass and a glowstone dust. `/glowingglass demo kit` gives only the kit.

## Tests

| # | Do this | Expect |
|---|---|---|
| 1 | Look at a few blocks of the demo wall: `/glowingglass info` | "That is Glowing Red Glass (players without the mod see shroomlight)", and so on. Blue, cyan, grays and clear are sea lanterns; yellow and brown ochre froglights; lime and green verdant; purple, magenta and pink pearlescent; red and orange shroomlights. |
| 2 | Open the recipe book, search "glowing" | 16 recipes, grouped together. |
| 3 | Crafting table: 8 blue stained glass around 1 glowstone dust (dust in the middle) | 8 Glowing Blue Glass. Shift-click works. Plain glass gives Glowing Glass (clear). |
| 4 | Hover a glowing glass item | Its name, not "Sea Lantern". It doesn't stack with a plain sea lantern. |
| 5 | **Survival:** place a Glowing Blue Glass, break it with your hand | Drops 1 Glowing Blue Glass (no prismarine crystals). `/glowingglass info` on the item. |
| 6 | **Survival:** same with a yellow (froglight) and a red (shroomlight) | Each drops itself, same colour. |
| 7 | **Creative:** break a glowing glass | Nothing drops (as for any block in creative). Place a plain sea lantern in the same spot: `/glowingglass info` says it's not glowing glass. |
| 8 | **Creative:** middle-click a placed glowing glass, with and without Ctrl | Glowing glass of that colour in your hand, not a sea lantern. Again with one already in your inventory: it picks that one. |
| 9 | **Survival:** middle-click a glowing glass with none in your inventory but a plain sea lantern in your hotbar | Nothing is picked (never the plain block). With a glowing glass of that colour in your inventory: it picks it. |
| 10 | Rename a glowing glass in an anvil ("Lamp"), place it, break it in survival | Placed as glowing glass (`/glowingglass info`). The drop is a fresh Glowing ... Glass: the anvil name doesn't come back (as for most blocks). |
| 11 | A piston pushing a glowing glass, then a sticky piston pulling it back | `/glowingglass info` on the moved block: same colour at each spot. |
| 12 | Place a few glowing glass, `/stop` the server, start it, rejoin | Still glowing glass (`/glowingglass info`). |
| 13 | **Survival:** light TNT next to a few glowing glass | Each drops itself, never prismarine crystals or a plain froglight. |
| 14 | Right-click a composter with Glowing Red Glass | Nothing happens (a shroomlight would compost). |
| 15 | Put glowing glass in a crafting grid in any shape | No result, ever (no vanilla recipe may treat it as its sea lantern or froglight). |
| 16 | `/glowingglass demo` in survival | "Demos only work in creative mode." |

## ⚠️ Known limits and decisions for you

1. **It always drops itself**, also without silk touch (vanilla glass needs silk touch; the kickoff said to replace
   the carrier's drops with the glowing glass item). Say if you'd rather it need silk touch.
2. **Explosions use their usual drop chance**: TNT always drops it, a creeper sometimes doesn't, like any block.
3. **Survival middle-click with none in your inventory picks nothing**, rather than the plain sea lantern or
   froglight it is underneath (same rule as Logic Blocks).
4. **Blocks changed by commands or world editors:** a glowing glass replaced by something else stops being glowing
   glass (its record is dropped). One replaced by its own carrier (`/setblock ... sea_lantern` on a blue one) stays
   glowing glass.
5. **Sea lanterns count for conduits**, so the seven sea-lantern colours do too. Harmless.
6. Not checked yet: whether a **sulfur cube** (26.x) can swallow it (shroomlights are on its list; red and orange
   might come back as their carrier, or not at all). I couldn't test it without a player.
7. The **mod handshake and chunk queries** are only unit-tested: there's no mod yet (slice 2).
8. Not checked by the self-test, only by you: everything above that needs a player (placing, breaking by hand,
   middle-click, the crafting table and recipe book, the composter click, rejoining).
