# Test plan: Glowing Glass

**New: plugin 0.3.1 + client 0.3.2, leftover glowing glass marks (test 48).** A spot that once held glowing glass
could keep sounding like glass after something else replaced it (found with the Item Beacon: a placement the server
refused). Update both.
**Client 0.3.1, a "Glowing Glass" creative tab (test 47)**, released as v0.3.0. Slice 3 (redstone glowing glass, plugin
0.3.0, tests 37-46) worked in your game on client 0.3.0.
**Slice 3, redstone glowing glass (plugin 0.3.0 + client 0.3.0, tests 37-46).** Update both. The self-test
passed on a real Paper server (33 checks, 8 of them new for redstone glass); nothing about it has been seen in a game
yet. v0.2.1 (plugin 0.2.1 + client 0.2.4) is released; tests 1-36 cover it. `/glowingglass info` (plugin) and
`/glowingglassmod` (mod) show what each side knows.

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
| 36 | Right-click a sulfur cube holding glowing glass; then drop one on the ground next to a cube | Feeding by hand does nothing (it stays in your hand). The dropped one may vanish into the cube for a moment, then pops back out and stays on the ground; the cube never shows a sea lantern or froglight inside. |
| 37 | `/glowingglass demo redstone` (creative) | A wall of 32 redstone glowing glass 3 blocks ahead: the top 16 lit (redstone blocks behind them), the bottom 16 dark. Each in its colour; the dark ones a dimmed version of the same texture. Screenshot please, with and without the shader. |
| 38 | Recipe book, search "glowing" | 16 more recipes, in the redstone tab, grouped. |
| 39 | Crafting table: 8 Glowing Blue Glass around 1 redstone dust | 8 Redstone Glowing Blue Glass. A renamed Glowing Blue Glass works too; mixing colours, or plain sea lanterns, gives nothing. |
| 40 | Hover the item; look at it in your hotbar | "Redstone Glowing Blue Glass"; the dark (unlit) texture, like a vanilla redstone lamp item. |
| 41 | Place one, put a lever on it, flip the lever on and off | Lit texture while powered, dark texture when not (vanilla's short delay before it goes dark). `/glowingglass info`: "Redstone Glowing Blue Glass". |
| 42 | Power a row of them with redstone dust, a repeater, a button | Each lights and goes dark like a redstone lamp would. |
| 43 | **Survival:** break a lit one and a dark one | Each drops 1 Redstone Glowing ... Glass of its colour. |
| 44 | Middle-click a placed one (creative, then survival with one in your inventory) | Redstone glowing glass of that colour, not a plain lamp and not the always-on kind. |
| 45 | A piston pushing one onto a spot next to a redstone block | Moves as glowing glass, lights up where it lands, no lamp flash during the slide. |
| 46 | A friend **without** the mod (or the 0.2.4 mod) | Sees a plain redstone lamp that lights with power; no errors. |
| 47 | Creative inventory: the "Glowing Glass" tab, then search "glow" | The tab has all 32 (always-on, then redstone). Search finds them. Take some out, place them: `/glowingglass info` says glowing glass; they stack with crafted ones. |
| 48 | Go back to the spot where the crafting table sounded like glass; break and replace it a couple of times. Then place a glowing glass and break it normally; place a plain sea lantern and an ochre froglight there and look at them | Crafting table: normal wood sounds. Glowing glass still sounds like glass while it's there. The plain sea lantern and froglight look and sound like themselves. `/glowingglassmod` on a plain block never says glowing glass. |


## ⚠️ Known limits and decisions for you

1. **It always drops itself**, also without silk touch (vanilla glass needs silk touch). Your call, 2026-11-14.
2. **Explosions use their usual drop chance**: TNT always drops it, a creeper sometimes doesn't, like any block.
3. **Survival middle-click with none in your inventory picks nothing**, rather than the plain sea lantern or
   froglight it is underneath (same rule as Logic Blocks).
4. **Blocks changed by commands or world editors:** a glowing glass replaced by something else stops being glowing
   glass (its record is dropped). One replaced by its own carrier (`/setblock ... sea_lantern` on a blue one) stays
   glowing glass.
5. **Sea lanterns count for conduits**, so the seven sea-lantern colours do too. Harmless.
6. **Sulfur cubes don't keep glowing glass** (they'd show the carrier inside). Feeding by hand and by dispenser is
   refused; one a cube picks up off the ground is spat back out the next moment (Paper fires no event before that
   pickup), and mobs can't pick that item up again until a player does.
7. **Redstone glass looks the same in every shader light**: it sits on a redstone lamp, so coloured-light shaders
   give all 16 the lamp's warm light (your call). Players without the mod see a redstone lamp.
   **Unlit textures** (`glowing_<colour>_glass_off.png`) are generated from the lit ones by
   `recolor.py --off` (darker, less saturated); replace them like the others if you draw your own.
   **Textures: your hand-drawn light blue** (Aseprite), recoloured to the other 15 by
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
