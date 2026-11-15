# Test plan: Glowing Glass

**Client 0.2.4: your hand-drawn light blue, recoloured to the other 15** (tests 18-21 for the look).
**Client 0.2.3: other players placing or walking on glowing glass sound like glass too** (test 33).
**Client 0.2.2: no carrier flash during a piston slide** (test 35; you saw it on 0.2.1). 0.2.1 brought your
redstone-recolor textures (tests 18-21).
**Plugin 0.2.0 + client 0.2.0 (tests 29-35):** glass sounds for every colour, and no glimpse of the carrier when
glowing glass appears (placed by you or someone else, or coming into view). Update both (the dev loop does).
Slice 2 (the mod, tests 17-28) ran in your game on 0.1.0, where you found the two things 0.2.0 fixes. Slice 1 (server side, tests 1-16)
passed the real-Paper self-test (23 checks now). `/glowingglass info` (plugin) and `/glowingglassmod` (mod) show what
each side knows.

## Setup (once, about 5 minutes)

1. Close Minecraft (Windows won't replace a mod jar the game has open).
2. Double-click `run\build-and-install.cmd`. It stops the test server (`..\blueprints-mc\run\local-paper`), builds
   and tests both parts, installs `plugins\GlowingGlass.jar` and the mod in `%APPDATA%\.minecraft\mods\`, then
   offers to start the server.
3. Launch **fabric-loader-26.3** (with your Iris/Sodium setup) and join. For tests 1-16 the mod makes no difference
   except how things look.
4. In creative, **`/glowingglass demo`** builds all 16 in a wall 3 blocks in front of you (warm colours on top) and
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
| 17 | `/glowingglassmod` | "Glowing Glass: connected (N glowing glass known nearby, ...)". |
| 18 | Look at the demo wall (shader **off**) | Every block shows its texture from your redstone-recolor set, in its colour. No sea lantern, froglight or shroomlight pattern anywhere. Screenshot please. |
| 19 | Same, shader **on** (Complementary via Iris) | Same textures; each block glows and lights its surroundings the way its carrier would (blues and grays like a sea lantern, and so on). Screenshot please. |
| 20 | Look at one block: `/glowingglassmod` | "Looking at: Glowing ... Glass", the right colour. |
| 21 | Your hotbar and inventory, the item in your hand, one dropped on the ground, one in an item frame | The glowing glass texture as a cube, not a sea lantern or froglight. |
| 22 | Place a glowing glass | It shows as glowing glass almost at once (a tick or two of sea lantern is possible). |
| 23 | Break one (survival) | The cracks and breaking particles use the glass texture. |
| 24 | Place a **plain** sea lantern, froglight and shroomlight next to the wall | They look like their normal selves. |
| 25 | A piston pushing a glowing glass back and forth | Glowing glass before, during and after the move. |
| 26 | Walk far away (beyond your view distance) and back, or relog | Still glowing glass everywhere. |
| 27 | Go to the Nether and back (or `/execute in minecraft:the_nether run tp ~ ~ ~` and back) | Still glowing glass on return. |
| 28 | A friend **without** the mod on the same server | Sees the carriers (sea lanterns etc.), no errors. |
| 29 | Place a yellow, green, pink and red glowing glass | Each makes the glass place sound (not a froglight's or shroomlight's). |
| 30 | Walk on them, jump onto them from a few blocks up | Glass footsteps and landing. |
| 31 | Mine one slowly in survival, then let it break | Glass hit sounds while mining, the glass break sound at the end. |
| 32 | Place glowing glass quickly, a dozen in a row, watching each | It shows as glowing glass from the first frame: no sea lantern or froglight flash. |
| 33 | A second player (or alt) places and breaks glowing glass near you | No flash on your screen either, and their placing sounds like glass (client 0.2.3). |
| 34 | Fly away until the demo wall unloads, then come back; relog next to it | It comes back as glowing glass, no carrier flash. |
| 35 | A piston (and a sticky piston pulling back) moving a row of glowing glass, and a slime contraption | Glowing glass the whole way, during the slide too (client 0.2.2). |


## ⚠️ Known limits and decisions for you

1. **It always drops itself**, also without silk touch (vanilla glass needs silk touch). Your call, 2026-11-14.
2. **Explosions use their usual drop chance**: TNT always drops it, a creeper sometimes doesn't, like any block.
3. **Survival middle-click with none in your inventory picks nothing**, rather than the plain sea lantern or
   froglight it is underneath (same rule as Logic Blocks).
4. **Blocks changed by commands or world editors:** a glowing glass replaced by something else stops being glowing
   glass (its record is dropped). One replaced by its own carrier (`/setblock ... sea_lantern` on a blue one) stays
   glowing glass.
5. **Sea lanterns count for conduits**, so the seven sea-lantern colours do too. Harmless.
6. Not checked yet: whether a **sulfur cube** (26.x) can swallow it (shroomlights are on its list; red and orange
   might come back as their carrier, or not at all). I couldn't test it without a player.
7. **Textures: your hand-drawn light blue** (Aseprite), recoloured to the other 15 by
   `Claude outputs\glowing-glass\recolor\recolor.py` (client 0.2.4; the redstone-recolor set before that is still in
   `Claude outputs\glowing-glass\textures-16px\redstone-recolor\` and in git). To change them again, replace the files in
   `fabric-client/src/main/resources/assets/glowingglass/textures/block/` (same names, 16 x 16, opaque; a unit test
   checks) and rebuild.
8. **Nothing on screen is checked here** (you check it): the cloud workspace can't run the 26.3 client. Checked: the mod compiles,
   loads in a real Fabric Loader with Fabric API (the audit loads every class), and its unit tests pass (resources:
   every colour has a texture, model and item case). Likely suspects if something looks wrong: Sodium/Iris not
   calling Fabric's model hook (everything stays a sea lantern), the shader treating our texture differently from
   the carrier's.
9. **A resource pack that replaces the sea lantern, froglight or shroomlight item** (`items/<block>.json`) wins over
   the mod: glowing glass items then show that pack's look. Placed blocks are unaffected.
10. **Players without the mod hear the carriers' sounds**: glass for the seven sea lantern colours, froglight and
   shroomlight sounds for the rest.
11. Not checked by the self-test, only by you: everything above that needs a player (placing, breaking by hand,
   middle-click, the crafting table and recipe book, the composter click, rejoining) and everything on screen.
