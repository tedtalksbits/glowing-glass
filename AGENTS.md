# Glowing Glass: notes for agents

Start here, then `README.md` (what it does) and `docs/PROTOCOL.md`. `docs/TEST_PLAN.md` and `docs/CI.md` cover
the rest. Kickoff brief, mockups and draft textures: `C:\Users\tedan\Documents\software\Claude outputs\glowing-glass\`
(the owner's central folder; never put such files inside a project repo, copy in what the project needs).

## What this is

Coloured glass that gives off light, for decorative builds, on a small Paper server for friends and family (owner:
tedtalksbits, MC name TedTalks2). Standalone: its own repo and versions, unrelated to Shulker Styles, Logic Blocks,
Blueprints or the frog plugin (their patterns were copied, their files never edited).

Paper can't add blocks, so each glowing glass is a vanilla light block (the **carrier**: sea lantern, a froglight or
a shroomlight) plus a record saying which colour it is. The mod (slice 2) will swap the carrier's model for marked
positions; everyone else sees the carrier.

- `glowing-plugin/`: Paper plugin (Java 25). Items, recipes, records, every drop and move path, the protocol.
  - `GlassItem`: the item (carrier item + PDC colour + item name + custom model data string).
  - `Recipes`: the 16 recipes; the guard (no crafting recipe takes glowing glass; composters refuse it).
  - `Records`: chunk persistent data `glowingglass:glass`, loaded chunks in memory, written through on every change.
  - `Keeper`: placing, breaking, explosions, pistons, drop fallback, middle-click.
  - `ClientBridge`: the protocol (query per chunk, then pushes).
  - `Demo`: `/glowingglass demo [kit]`.
- `glowing-plugin/src/main/java/dev/whizzle/glowingglass/model/`: pure Java (`Colour`, `ChunkGlass`, `Protocol`,
  `Wire`), to be **compiled into both** the plugin and the mod (slice 2, as Shulker Styles does). Change it once.
- `paper-selftest/`: development-only plugin that checks the plugin on a real Paper server.

## Status (2026-11-14)

- **Slice 1 (server side) done, not yet in a real game.** Plugin 0.1.0, protocol 1, no mod yet.
  - Checked here: 23 unit tests (MockBukkit for unit tests only); self-test on Paper 26.3-211, 22 checks: the 16
    recipes, a crafter crafting 8 blue, the vanilla-recipe guard (a probe recipe taking a sea lantern works with a
    plain one, not with glowing glass), records surviving a real chunk unload/load (and a stale one dropped on load),
    breakNaturally (sea lantern and froglight), `setblock ... destroy`, TNT with three colours, a sourceless
    explosion, sticky piston push and pull, a push across a chunk border, a slime contraption, the hopper-into-composter
    guard, a dispenser, the demo, a chunk answer from real records.
  - **Not seen in a real game:** a player placing, breaking (survival/creative), middle-click, the crafting table and
    recipe book, the composter click. `docs/TEST_PLAN.md` tests 1-16.
  - CI: workflows written (plugin + self-test; release publishes the plugin jar), **not yet run** (the owner pushes).
- Next: slice 2, the mod (textures from the owner's drafts, placed blocks by model swap, items by item definition
  override). Copy Shulker Styles' `fabric-client/` setup, `ci/mixin-audit/` and the client CI job; compile `model/`
  into the mod; add the client to the release workflow and `run/DevInstall.java` (it already skips a missing
  `fabric-client/`).

## Open items

1. Slice 1 needs the owner's in-game check (`docs/TEST_PLAN.md`).
2. Owner to say: drop without silk touch (current) or silk touch only (`docs/TEST_PLAN.md` limit 1).
3. Unchecked: sulfur cubes (26.x) swallowing glowing glass (shroomlight is on their list).
4. Slice 2: before writing the renderer, read how 26.3's Fabric renderer API / model loading API wraps block models
   (`ModelModifier`, block state model wrapping in the Fabric API jars) and how Sodium handles it. During a piston's
   2-tick slide the moving block renders as the carrier (vanilla's moving-piston renderer): check whether that shows.
5. Slice 3: redstone version on a redstone lamp; ask the owner for its recipe (kickoff suggests 8 glass around
   1 redstone dust). Chaining is its own slice after that, if still wanted.

## Decisions already made (don't reopen without asking)

| Decision | Why |
|---|---|
| Name "Glowing Glass", folder `software\glowing-glass`, namespace `glowingglass` | Owner, 2026-11-14 |
| Glass, not mineral blocks | Owner (one recipe pattern, a full palette) |
| 16 colours: 15 stained + clear, no black in v1. Colour ids = dye ids, clear 16, 15 reserved for black | Owner; ids never renumbered (saved in chunks, on the wire) |
| Always on in v1; the redstone version (on a redstone lamp) is a later slice, chaining after that | Owner |
| Carriers: white, light gray, gray, clear, cyan, light blue, blue on sea lantern; yellow and brown on ochre froglight; lime and green on verdant froglight; purple, magenta, pink on pearlescent froglight; red and orange on shroomlight | Owner, 2026-11-14 (brown on ochre froglight, not glowstone). Light colour is up to the player's shader, by block type |
| Recipe: 8 glass of one colour around 1 glowstone dust -> 8; shaped; group `glowingglass`, building tab | Owner |
| Look: frosted, lit from inside; textures hand-drawn by the owner (16 x 16, opaque, `glowing_<colour>_glass.png`), drafts until his arrive | Owner (slice 2) |
| Item: carrier item, PDC `glowingglass:colour`, `item_name` "Glowing Blue Glass" (clear: "Glowing Glass"), custom model data string `glowingglass:blue` | Players without the mod see a named carrier; the mod's item definitions select on the string; an anvil name doesn't remove the colour |
| Placed blocks: one record per chunk (PDC `glowingglass:glass`, all positions and colours), in memory while loaded, written on every change | Logic Blocks keeps per-block keys; one key per chunk keeps big floors compact |
| A record counts only while its block is its carrier (or a moving piston block); stale records are dropped on chunk load or by `verify` | Commands and world editors change blocks without events |
| Breaking drops one glowing glass whatever the tool (no silk touch); creative drops nothing | Kickoff (replace the carrier's drops). Flagged to the owner |
| Explosions: glowing glass is taken out of the block list (HIGHEST) and broken by the plugin (MONITOR) with the explosion's yield | Vanilla merges identical drops from different blocks, so sea lantern crystals can't be told apart afterwards |
| Pistons: records move at the piston event (MONITOR) by the piston's facing (retract: the opposite) | Covers slime/honey and chunk borders; the self-test proves them |
| Other drops (breakNaturally, withers, `setblock destroy`): the first carrier drop at a glowing glass block becomes glowing glass, more are removed | Those drops spawn while the block is still there |
| Middle-click: glowing glass of that colour; creative hands one out; survival with none picks nothing | Same rule as Logic Blocks |
| No crafting recipe takes glowing glass (grids: `PrepareItemCraftEvent`; crafters: `CrafterCraftEvent`); composters refuse it | Kickoff. No 26.3 recipe takes a carrier today (the self-test lists any), the guard covers future ones |
| Protocol: query per chunk, answered once, then the whole chunk pushed on change; answered only for chunks the server sent that player (`isChunkSent`), 64 per tick | Kickoff shape (Shulker Styles' protocol) |
| Demos only in creative | Same rule as the other projects: they hand out free items |
| Release tag = plugin version; the client keeps its own version | Same as the other projects |

## Working rules

- Commit locally; **the owner pushes** to GitHub. Agents commit as `Claude <noreply@anthropic.com>`, per commit:
  `git -c user.name=Claude -c user.email=noreply@anthropic.com commit ...`. Never change the repo's git config or
  rewrite the owner's commits.
- Reports: short, structured, anything needing the owner's decision at the **top, marked ⚠️**. No "•" separators.
- Verify on a real Paper server before claiming something works; MockBukkit is for unit tests only. Say plainly what
  was and wasn't seen in a real game.
- End each slice with `docs/TEST_PLAN.md` (Do this / Expect table, then "⚠️ Known limits and decisions for you") and
  update this file.

## Building and testing

- Plugin: `gradlew build` at the root (Java 25, Gradle 9.8.0). Jar: `glowing-plugin/build/libs/glowing-plugin-<v>.jar`.
- Self-test: `ci/run-selftest.sh <paper.jar> <new-folder>` (Linux, Git Bash, CI): a throwaway flat-world server
  with `-Dglowingglass.selftest.exit=true`; fails unless `plugins/GlowingGlassSelfTest/report.txt` ends
  `SELFTEST PASSED`. Offline: `PAPER_SEED_DIR=<existing server folder>`. `SELFTEST_ONLY=piston,TNT` runs only steps
  whose names start that way. About 25 seconds.
- Dev loop: `run\build-and-install.cmd` (Windows) or `run/build-and-install.sh` runs `run/DevInstall.java`: stops
  the local test server (RCON if enabled, else waits for `stop`), builds and tests, installs the plugin as
  `plugins/GlowingGlass.jar` (and, from slice 2, the client in Minecraft's mods folder), offers to start the server.
  Defaults: the test server `..\blueprints-mc\run\local-paper`, blueprints-mc's JDK and Gradle cache. Overrides:
  `GLOWINGGLASS_SERVER_DIR`, `GLOWINGGLASS_MODS_DIR`, `GLOWINGGLASS_GRADLE_ARGS`, `JAVA_HOME`, `GRADLE_USER_HOME`.
- CI on every push to `main`: plugin and self-test (`docs/CI.md`). Releases: tag `v<plugin version>`.

## Gotchas

- Pistons move blocks over 2 ticks; meanwhile both spots hold `MOVING_PISTON`. Records move at the event, and a
  record on a `MOVING_PISTON` block still counts, so nothing drops it mid-move.
- An explosion's drops spawn after the blocks are gone and identical drops are merged, hence the take-and-break
  handling in `Keeper`.
- A freshly loaded chunk won't unload at once (`unloadChunk` then succeeds a couple of seconds later, as the
  self-test does).
- Self-test: slime blocks grab the stone floor (then the push is too long and fails): build slime contraptions off
  the ground. Explosions also drop the floor's cobblestone: ignore drops that aren't carrier drops.
- Server-side copies of a shaped recipe (`Bukkit.getRecipe`) rename the shape's letters: test by shape position.
- `Records.ChunkId.key` is Bukkit's `Chunk.getChunkKey` (x low, z high); `Protocol.ChunkPos.key()` matches it
  (unit-tested).
- MockBukkit: no real chunk persistence or block entities; test placed-block code in the self-test.
- From Shulker Styles, for the mod in slice 2: only mixins in the mixin package (else a black screen; run the mixin
  audit); mouse buttons are SDL-numbered (`InputConstants.MOUSE_BUTTON_*`); screen overlays from
  `ScreenEvents.afterForeground`; the GUI font has no ✓ ✕ ≠ ≤ ≥ →.

## Cloud workspace pitfalls

- The desktop-linked shell (device VM) has Java 11 and **no network**; the cloud container has no Maven/Paper/Fabric
  access either, but `apt-get install openjdk-25-jdk-headless` works there. What worked (2026-11-14): on the device,
  tar `~/.gradle/caches/modules-2/files-2.1`, the Gradle 9.8.0 distribution and the Paper server bits (`paper-26.3-211.jar`,
  `cache/`, `versions/`, `libraries/` from `blueprints-mc\run\local-paper`) into `glowing-glass\.tools\stage\`
  (gitignored), split into parts under 350 MB, stage them (500 MB per call), unpack in the container, hard-link
  `files-2.1/<group>/<artifact>/<version>/<hash>/<file>` into a Maven layout, and build with `--offline` and an init
  script that replaces every repository with that folder. Delete `.tools\stage\` afterwards.
- Slice 2 also needs `caches/fabric-loom/26.3` (+ `minecraftMaven`, `mojang_versions_manifest.json`); keep Loom's
  own `file:` repositories in the init script.
- Getting commits to the owner: `git bundle create X.bundle main` (later `<his HEAD>..main`), copy it to
  `.tools\transfer\`, `git pull --ff-only .tools/transfer/X.bundle main` in the repo, delete `.tools/transfer`. Git
  on the device needs delete permission (its lock files).
