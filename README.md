# Glowing Glass

Coloured glass that gives off light, for decorative builds: glowing flames, signs, floors. One glowing glass for each
glass colour (15 stained colours and clear; no black yet), always on or redstone (lit only while powered). A Paper plugin (recipes, storage, works for everyone)
plus a Fabric client mod (draws it). Players without the mod see a sea lantern, froglight or shroomlight,
with light 15 either way.

By **tedtalksbits**. Minecraft / Paper 26.3.

## Using it

- **Recipe:** 8 glass of one colour around 1 glowstone dust gives 8 glowing glass of that colour (any crafting table,
  or a crafter). Plain glass gives clear glowing glass.
- **Redstone glowing glass:** 8 glowing glass of one colour around 1 redstone dust gives 8 redstone glowing glass. It
  works like a redstone lamp: lit while powered, dark otherwise. Players without the mod see a redstone lamp.
- **Placing and breaking:** like any block. It drops itself, also when broken without silk touch, blown up (with
  the explosion's usual drop chance) or knocked out by a command. Pistons, slime and honey blocks move it.
- **Middle-click** picks glowing glass of that colour (creative hands one out), never the block underneath.
- `/glowingglass info`: what the block you look at, or the item you hold, is.
- `/glowingglass demo` (creative): all 16 in a wall in front of you, and a kit; `/glowingglass demo kit` for the kit
  only. `/glowingglass demo redstone [kit]`: the redstone ones, the top copy powered.

**Light colour** is up to the player's shader. Vanilla Java light has no colour; coloured-light shaders colour light
by block type, so each colour sits on the vanilla light block closest to it:

| Glowing glass | Block underneath (what players without the mod see) |
|---|---|
| white, light gray, gray, clear, cyan, light blue, blue | Sea lantern |
| yellow, brown | Ochre froglight |
| lime, green | Verdant froglight |
| purple, magenta, pink | Pearlescent froglight |
| red, orange | Shroomlight |
| every redstone glowing glass | Redstone lamp (all get the lamp's light from a shader) |

## Status

Plugin 0.3.0 and client mod 0.3.0, protocol 1. Slice 1 (server side), slice 2 (the mod) and slice 3 (redstone glowing
glass) are done; chaining (one powered block lighting its whole group) is a possible slice 4. See `AGENTS.md` and `docs/TEST_PLAN.md`.

**With the mod** (`glowing-glass-client-<version>.jar` plus Fabric API in `.minecraft/mods`): placed glowing glass
and its items show the glowing glass textures. `/glowingglassmod` says whether it's connected and what you look at.

## Building

Plugin: `gradlew build` (Java 25). Jar: `glowing-plugin/build/libs/glowing-plugin-<version>.jar`. Mod:
`cd fabric-client && gradlew build`. Jar: `fabric-client/build/libs/glowing-glass-client-<version>.jar`. Self-test on a real Paper
server: `ci/run-selftest.sh` (docs/CI.md). Dev loop on Windows: `run\build-and-install.cmd`.
