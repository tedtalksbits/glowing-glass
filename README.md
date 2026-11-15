# Glowing Glass

Coloured glass that gives off light, for decorative builds: glowing flames, signs, floors. One glowing glass for each
glass colour (15 stained colours and clear; no black yet). A Paper plugin (recipes, storage, works for everyone)
plus a Fabric client mod (draws it). Players without the mod see a sea lantern, froglight or shroomlight,
with light 15 either way.

By **tedtalksbits**. Minecraft / Paper 26.3.

## Using it

- **Recipe:** 8 glass of one colour around 1 glowstone dust gives 8 glowing glass of that colour (any crafting table,
  or a crafter). Plain glass gives clear glowing glass.
- **Placing and breaking:** like any block. It drops itself, also when broken without silk touch, blown up (with
  the explosion's usual drop chance) or knocked out by a command. Pistons, slime and honey blocks move it.
- **Middle-click** picks glowing glass of that colour (creative hands one out), never the block underneath.
- `/glowingglass info`: what the block you look at, or the item you hold, is.
- `/glowingglass demo` (creative): all 16 in a wall in front of you, and a kit; `/glowingglass demo kit` for the kit
  only.

**Light colour** is up to the player's shader. Vanilla Java light has no colour; coloured-light shaders colour light
by block type, so each colour sits on the vanilla light block closest to it:

| Glowing glass | Block underneath (what players without the mod see) |
|---|---|
| white, light gray, gray, clear, cyan, light blue, blue | Sea lantern |
| yellow, brown | Ochre froglight |
| lime, green | Verdant froglight |
| purple, magenta, pink | Pearlescent froglight |
| red, orange | Shroomlight |

## Status

Plugin 0.2.0 and client mod 0.2.2, protocol 1. Slice 1 (server side) and slice 2 (the mod) are done; the redstone version is slice 3. See `AGENTS.md` and `docs/TEST_PLAN.md`.

**With the mod** (`glowing-glass-client-<version>.jar` plus Fabric API in `.minecraft/mods`): placed glowing glass
and its items show the glowing glass textures. `/glowingglassmod` says whether it's connected and what you look at.

## Building

Plugin: `gradlew build` (Java 25). Jar: `glowing-plugin/build/libs/glowing-plugin-<version>.jar`. Mod:
`cd fabric-client && gradlew build`. Jar: `fabric-client/build/libs/glowing-glass-client-<version>.jar`. Self-test on a real Paper
server: `ci/run-selftest.sh` (docs/CI.md). Dev loop on Windows: `run\build-and-install.cmd`.
