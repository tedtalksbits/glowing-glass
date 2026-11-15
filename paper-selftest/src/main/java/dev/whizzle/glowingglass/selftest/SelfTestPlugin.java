package dev.whizzle.glowingglass.selftest;

import dev.whizzle.glowingglass.Demo;
import dev.whizzle.glowingglass.GlassItem;
import dev.whizzle.glowingglass.GlowingGlassPlugin;
import dev.whizzle.glowingglass.Recipes;
import dev.whizzle.glowingglass.Records;
import dev.whizzle.glowingglass.model.ChunkGlass;
import dev.whizzle.glowingglass.model.Colour;
import dev.whizzle.glowingglass.model.Protocol;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;

/**
 * Development-only: checks Glowing Glass on a real Paper server: recipes (crafter), the vanilla-recipe guard, chunk
 * records surviving unload/reload, every drop path a test can trigger without a player (breakNaturally, a command
 * with drops, TNT, a sourceless explosion), pistons (push, pull, slime, across a chunk border), the composter guard,
 * dispensers and the demo. Writes report.txt ending "SELFTEST PASSED"; with -Dglowingglass.selftest.exit=true it
 * stops the server when done. A player breaking, placing and middle-clicking need a real client: docs/TEST_PLAN.md.
 */
public final class SelfTestPlugin extends JavaPlugin {
    record Step(String name, Runnable action, int ticks, Supplier<String> check) {}

    private final List<Step> steps = new ArrayList<>();
    private final List<String> report = new ArrayList<>();
    private World w;
    private Records records;
    private static final int Y = -60;
    private int failures;

    @Override public void onEnable() { getServer().getScheduler().runTaskLater(this, this::start, 40); }

    Block at(int x, int z) { return w.getBlockAt(x, Y, z); }

    /** Glowing glass at (x, z), as the plugin leaves it after a player places one. */
    Block glass(int x, int z, Colour c) {
        Block b = at(x, z);
        b.setType(GlassItem.carrier(c));
        records.put(b, c);
        return b;
    }

    List<Item> itemsNear(int x, int z, double r) {
        Location c = at(x, z).getLocation().add(0.5, 0.5, 0.5);
        return new ArrayList<>(w.getNearbyEntitiesByType(Item.class, c, r));
    }

    void clearItems(int x, int z) { for (Item i : itemsNear(x, z, 6)) i.remove(); }

    static String describe(List<Item> items) {
        return items.stream().map(i -> {
            ItemStack s = i.getItemStack();
            Colour c = GlassItem.colour(s);
            return s.getAmount() + " " + (c != null ? c.itemName() : s.getType().getKey().getKey());
        }).toList().toString();
    }

    /** Problem with the drops near (x, z), or null if they are exactly the given glowing glass (one each) and nothing else. */
    String dropped(int x, int z, double r, Colour... expected) {
        List<Item> items = itemsNear(x, z, r);
        List<Colour> got = new ArrayList<>();
        for (Item i : items) {
            Colour c = GlassItem.colour(i.getItemStack());
            if (c == null && !carrierDrop(i.getItemStack().getType())) continue;   // the floor, etc.
            if (c == null) return "dropped " + describe(items) + ", expected only " + Arrays.toString(expected);
            for (int n = 0; n < i.getItemStack().getAmount(); n++) got.add(c);
        }
        List<Colour> want = new ArrayList<>(List.of(expected));
        Collections.sort(got);
        Collections.sort(want);
        return got.equals(want) ? null : "dropped " + describe(items) + ", expected " + want;
    }

    static boolean carrierDrop(Material m) { return m == Material.PRISMARINE_CRYSTALS || GlassItem.carriers().contains(m); }

    String recordIs(Block b, Colour c) {
        Colour now = records.at(b);
        return now == c ? null : "record at " + b.getX() + " " + b.getY() + " " + b.getZ() + " is " + now + " (block " + b.getType() + "), expected " + c;
    }

    String noRecord(Block b) {
        Colour r = records.recorded(b);
        return r == null ? null : "a " + r + " record is still at " + b.getX() + " " + b.getY() + " " + b.getZ() + " (block " + b.getType() + ")";
    }

    static String all(String... problems) { for (String p : problems) if (p != null) return p; return null; }

    Block piston(int x, int z, Material type, BlockFace facing) {
        Block p = at(x, z);
        p.setType(type);
        Directional d = (Directional) p.getBlockData();
        d.setFacing(facing);
        p.setBlockData(d);
        return p;
    }

    Crafter crafter(int x, int z) {
        Block c = at(x, z);
        c.setType(Material.CRAFTER);
        org.bukkit.block.data.type.Crafter data = (org.bukkit.block.data.type.Crafter) c.getBlockData();
        data.setOrientation(org.bukkit.block.Orientation.EAST_UP);
        c.setBlockData(data);
        return (Crafter) c.getState(false);
    }

    void start() {
        w = getServer().getWorlds().getFirst();
        records = ((GlowingGlassPlugin) Objects.requireNonNull(getServer().getPluginManager().getPlugin("GlowingGlass"))).records();
        for (int cx = -2; cx <= 16; cx++) for (int cz = -2; cz <= 2; cz++) w.setChunkForceLoaded(cx, cz, true);
        for (int x = -10; x <= 260; x++) for (int z = -10; z <= 10; z++) {
            w.getBlockAt(x, Y - 1, z).setType(Material.STONE);
            for (int y = Y; y < Y + 6; y++) w.getBlockAt(x, y, z).setType(Material.AIR);
        }

        // 1. Recipes and items.
        steps.add(new Step("recipes: 16, each 8 glass of one colour around 1 glowstone dust -> 8 glowing glass", () -> { }, 1, () -> {
            for (Colour c : Colour.values()) {
                if (!(Bukkit.getRecipe(Recipes.key(c)) instanceof ShapedRecipe r)) return "no recipe for " + c;
                if (r.getResult().getAmount() != 8 || GlassItem.colour(r.getResult()) != c) return c + " recipe makes " + r.getResult();
                String shape = String.join("", r.getShape());
                if (shape.length() != 9) return c + " recipe shape " + Arrays.toString(r.getShape());
                for (int i = 0; i < 9; i++) {
                    RecipeChoice ch = r.getChoiceMap().get(shape.charAt(i));
                    ItemStack want = new ItemStack(i == 4 ? Material.GLOWSTONE_DUST : GlassItem.glass(c));
                    if (ch == null || !ch.test(want)) return c + " recipe slot " + i + " doesn't take " + want.getType();
                    if (i != 4 && shape.charAt(i) == shape.charAt(4)) return c + " recipe: glass and dust share a slot";
                }
            }
            return null;
        }));
        steps.add(new Step("item: name, colour, model string; a renamed item keeps its colour", () -> { }, 1, () -> {
            ItemStack i = GlassItem.create(Colour.LIGHT_BLUE, 1);
            String name = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(i.getData(DataComponentTypes.ITEM_NAME)));
            CustomModelData cmd = i.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
            ItemStack renamed = i.clone();
            renamed.setData(DataComponentTypes.CUSTOM_NAME, Component.text("Lamp"));
            return all(i.getType() == Material.SEA_LANTERN ? null : "carrier " + i.getType(),
                name.equals("Glowing Light Blue Glass") ? null : "name " + name,
                cmd != null && cmd.strings().equals(List.of("glowingglass:light_blue")) ? null : "model data " + (cmd == null ? null : cmd.strings()),
                GlassItem.colour(renamed) == Colour.LIGHT_BLUE ? null : "renamed item lost its colour",
                GlassItem.colour(new ItemStack(Material.SEA_LANTERN)) == null ? null : "a plain sea lantern reads as glowing glass",
                GlassItem.create(Colour.CLEAR, 1).getType() == Material.SEA_LANTERN ? null : "clear isn't on a sea lantern");
        }));
        steps.add(new Step("vanilla: no crafting recipe takes a carrier block (INFO if one does: the guard covers it)", () -> { }, 1, () -> {
            Set<Material> carriers = GlassItem.carriers();
            List<String> using = new ArrayList<>();
            for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
                Recipe r = it.next();
                Collection<RecipeChoice> choices = r instanceof ShapedRecipe s ? s.getChoiceMap().values()
                    : r instanceof ShapelessRecipe s ? s.getChoiceList() : List.of();
                for (RecipeChoice ch : choices) if (ch != null && carriers.stream().anyMatch(m -> ch.test(new ItemStack(m)))) using.add(r instanceof Keyed k ? k.getKey().toString() : r.toString());
            }
            if (!using.isEmpty()) report.add("INFO crafting recipes taking a carrier: " + using);
            return null;
        }));

        int a = 0;
        steps.add(new Step("crafter: 8 blue glass + glowstone dust -> 8 glowing blue glass", () -> {
            Crafter crafter = crafter(a, 0);
            for (int i = 0; i < 9; i++) crafter.getInventory().setItem(i, new ItemStack(i == 4 ? Material.GLOWSTONE_DUST : Material.BLUE_STAINED_GLASS));
            at(a, -1).setType(Material.REDSTONE_BLOCK);
        }, 12, () -> all(dropped(a + 1, 0, 2.5, Collections.nCopies(8, Colour.BLUE).toArray(Colour[]::new)),
            ((Crafter) at(a, 0).getState(false)).getInventory().isEmpty() ? null : "the crafter still holds items")));

        int g = 10;
        NamespacedKey probe = new NamespacedKey(this, "probe");
        steps.add(new Step("guard: glowing glass doesn't work as its carrier in a recipe (crafter)", () -> {
            ShapelessRecipe r = new ShapelessRecipe(probe, new ItemStack(Material.STICK));
            r.addIngredient(Material.SEA_LANTERN);
            Bukkit.addRecipe(r);
            crafter(g, 0).getInventory().setItem(0, GlassItem.create(Colour.BLUE, 1));
            crafter(g + 6, 0).getInventory().setItem(0, new ItemStack(Material.SEA_LANTERN));
            at(g, -1).setType(Material.REDSTONE_BLOCK);
            at(g + 6, -1).setType(Material.REDSTONE_BLOCK);
        }, 12, () -> {
            boolean plainWorked = itemsNear(g + 7, 0, 2.5).stream().anyMatch(i -> i.getItemStack().getType() == Material.STICK);
            List<Item> out = itemsNear(g + 1, 0, 2.5);
            Bukkit.removeRecipe(probe);
            ItemStack kept = ((Crafter) at(g, 0).getState(false)).getInventory().getItem(0);
            return all(plainWorked ? null : "the probe recipe didn't work with a plain sea lantern (test broken)",
                out.isEmpty() ? null : "glowing glass crafted " + describe(out),
                GlassItem.colour(kept) == Colour.BLUE ? null : "the crafter lost the glowing glass: " + kept);
        }));

        // 2. Records: saved in the chunk, stale records ignored.
        int cx = 40, px = cx * 16 + 3;
        steps.add(new Step("records: (setting up a chunk to unload)", () -> {
            Block b = w.getBlockAt(px, 70, 5);
            b.setType(Material.OCHRE_FROGLIGHT);
            records.put(b, Colour.BROWN);
            Block stale = w.getBlockAt(px + 1, 70, 5);
            stale.setType(Material.SHROOMLIGHT);
            records.put(stale, Colour.RED);
            stale.setType(Material.STONE);   // replaced without an event (like a world editor)
        }, 40, () -> null));
        steps.add(new Step("records: survive the chunk unloading and loading again; a stale one is dropped on load", () -> {
            boolean ok = w.unloadChunk(cx, 0, true);
            report.add("INFO unloadChunk -> " + ok + ", tickets " + w.getPluginChunkTickets(cx, 0) + ", forced " + w.isChunkForceLoaded(cx, 0));
            if (!ok) w.unloadChunkRequest(cx, 0);
        }, 60, () -> {
            boolean wasLoaded = w.isChunkLoaded(cx, 0);
            Block b = w.getBlockAt(px, 70, 5), stale = w.getBlockAt(px + 1, 70, 5);
            byte[] saved = w.getChunkAt(cx, 0).getPersistentDataContainer().get(Records.KEY, PersistentDataType.BYTE_ARRAY);
            ChunkGlass g2 = saved == null ? null : ChunkGlass.load(saved);
            return all(wasLoaded ? "the chunk never unloaded" : null, recordIs(b, Colour.BROWN), noRecord(stale),
                g2 != null && g2.size() == 1 ? null : "saved record " + g2);
        }));
        int s = 22;
        steps.add(new Step("records: a block replaced without an event isn't glowing glass; verify drops the record", () -> {
            glass(s, 0, Colour.CYAN).setType(Material.SEA_LANTERN);   // still the carrier: still glowing glass
            glass(s + 1, 0, Colour.CYAN).setType(Material.GLASS);
        }, 1, () -> {
            String before = all(recordIs(at(s, 0), Colour.CYAN), records.at(at(s + 1, 0)) == null ? null : "a glass block reads as glowing glass");
            records.verify(at(s + 1, 0));
            return all(before, noRecord(at(s + 1, 0)), recordIs(at(s, 0), Colour.CYAN));
        }));

        // 3. Drops.
        int b1 = 30;
        steps.add(new Step("breakNaturally (sea lantern): one glowing glass, no prismarine crystals, record gone", () -> {
            clearItems(b1, 0);
            glass(b1, 0, Colour.BLUE).breakNaturally();
        }, 3, () -> all(dropped(b1, 0, 2, Colour.BLUE), noRecord(at(b1, 0)))));
        int b2 = 36;
        steps.add(new Step("breakNaturally (froglight): one glowing glass of that colour", () -> {
            clearItems(b2, 0);
            glass(b2, 0, Colour.YELLOW).breakNaturally();
        }, 3, () -> all(dropped(b2, 0, 2, Colour.YELLOW), noRecord(at(b2, 0)))));
        int b3 = 42;
        steps.add(new Step("command: setblock air destroy drops one glowing glass", () -> {
            clearItems(b3, 0);
            glass(b3, 0, Colour.MAGENTA);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock " + b3 + " " + Y + " 0 air destroy");
        }, 3, () -> all(dropped(b3, 0, 2, Colour.MAGENTA), noRecord(at(b3, 0)))));
        int t = 60;
        steps.add(new Step("TNT: three glowing glass -> three matching items, no carrier drops", () -> {
            clearItems(t, 0);
            glass(t - 1, 0, Colour.WHITE);
            glass(t, 0, Colour.LIGHT_BLUE);
            glass(t + 1, 0, Colour.ORANGE);
            w.spawn(at(t, 1).getLocation().add(0.5, 0, 0.5), TNTPrimed.class).setFuseTicks(1);
        }, 8, () -> all(at(t, 0).getType().isAir() ? null : "block still there: " + at(t, 0).getType(),
            dropped(t, 0, 5, Colour.WHITE, Colour.LIGHT_BLUE, Colour.ORANGE), noRecord(at(t - 1, 0)), noRecord(at(t, 0)), noRecord(at(t + 1, 0)))));
        int x = 80;
        steps.add(new Step("explosion (no entity): glass gone; drops are glowing glass or nothing (drop chance), never carrier drops", () -> {
            clearItems(x, 0);
            glass(x, 0, Colour.GREEN);
            glass(x + 1, 0, Colour.GREEN);
            w.createExplosion(at(x, 1).getLocation().add(0.5, 0, 0.5), 4f, false, true);
        }, 4, () -> {
            List<Item> items = itemsNear(x, 0, 5);
            items.removeIf(i -> !carrierDrop(i.getItemStack().getType()));   // the floor
            boolean onlyGlass = items.stream().allMatch(i -> GlassItem.colour(i.getItemStack()) == Colour.GREEN);
            long n = items.stream().mapToLong(i -> i.getItemStack().getAmount()).sum();
            return all(at(x, 0).getType().isAir() && at(x + 1, 0).getType().isAir() ? null : "glass still there",
                onlyGlass && n <= 2 ? null : "dropped " + describe(items), noRecord(at(x, 0)), noRecord(at(x + 1, 0)));
        }));

        // 4. Pistons.
        int p = 100;
        steps.add(new Step("sticky piston push: the record moves with the block", () -> {
            piston(p, 0, Material.STICKY_PISTON, BlockFace.EAST);
            glass(p + 1, 0, Colour.PURPLE);
            at(p - 1, 0).setType(Material.REDSTONE_BLOCK);
        }, 6, () -> all(at(p + 2, 0).getType() == Material.PEARLESCENT_FROGLIGHT ? null : "block at the new spot is " + at(p + 2, 0).getType(),
            recordIs(at(p + 2, 0), Colour.PURPLE), noRecord(at(p + 1, 0)))));
        steps.add(new Step("sticky piston pull: the record moves back", () -> at(p - 1, 0).setType(Material.AIR), 6,
            () -> all(recordIs(at(p + 1, 0), Colour.PURPLE), noRecord(at(p + 2, 0)))));
        int row = 200;
        List<Records.ChunkId> pushes = new ArrayList<>();
        java.util.function.Consumer<Records.ChunkId> counter = pushes::add;
        steps.add(new Step("piston pushing a row of 3: one change announced (one push to mods), not one per block", () -> {
            piston(row, 0, Material.PISTON, BlockFace.EAST);
            glass(row + 1, 0, Colour.BLUE);
            glass(row + 2, 0, Colour.RED);
            glass(row + 3, 0, Colour.LIME);
            records.onChange(counter);
            at(row - 1, 0).setType(Material.REDSTONE_BLOCK);
        }, 6, () -> {
            records.removeListener(counter);
            return all(pushes.size() == 1 ? null : pushes.size() + " announcements", recordIs(at(row + 4, 0), Colour.LIME), recordIs(at(row + 2, 0), Colour.BLUE));
        }));
        int border = 13 * 16 + 14;   // the block starts at x = 223 (chunk 13), ends at 224 (chunk 14)
        steps.add(new Step("piston push across a chunk border", () -> {
            piston(border, 0, Material.PISTON, BlockFace.EAST);
            glass(border + 1, 0, Colour.LIME);
            at(border - 1, 0).setType(Material.REDSTONE_BLOCK);
        }, 6, () -> all(recordIs(at(border + 2, 0), Colour.LIME), noRecord(at(border + 1, 0)))));
        int sl = 120, up = Y + 2;   // off the ground: the slime would grab the floor and the push would be too long
        steps.add(new Step("piston + slime block: glowing glass stuck to the slime moves with it", () -> {
            Block piston = w.getBlockAt(sl, up, 0);
            piston.setType(Material.PISTON);
            Directional dir = (Directional) piston.getBlockData();
            dir.setFacing(BlockFace.EAST);
            piston.setBlockData(dir);
            w.getBlockAt(sl + 1, up, 0).setType(Material.SLIME_BLOCK);
            for (Block b : List.of(w.getBlockAt(sl + 1, up, 1), w.getBlockAt(sl + 2, up, 0))) b.setType(GlassItem.carrier(b.getZ() == 1 ? Colour.PINK : Colour.GRAY));
            records.put(w.getBlockAt(sl + 1, up, 1), Colour.PINK);
            records.put(w.getBlockAt(sl + 2, up, 0), Colour.GRAY);
            w.getBlockAt(sl - 1, up, 0).setType(Material.REDSTONE_BLOCK);
        }, 6, () -> all(recordIs(w.getBlockAt(sl + 2, up, 1), Colour.PINK), recordIs(w.getBlockAt(sl + 3, up, 0), Colour.GRAY), noRecord(w.getBlockAt(sl + 1, up, 1)))));

        // 5. Composter, dispenser, demo, protocol.
        int k = 140;
        steps.add(new Step("composter: a hopper won't feed glowing glass (a shroomlight) into it", () -> {
            at(k, 0).setType(Material.COMPOSTER);
            Block hopper = w.getBlockAt(k, Y + 1, 0);
            hopper.setType(Material.HOPPER);
            ((Hopper) hopper.getState(false)).getInventory().setItem(0, GlassItem.create(Colour.RED, 4));
        }, 40, () -> {
            ItemStack still = ((Hopper) w.getBlockAt(k, Y + 1, 0).getState(false)).getInventory().getItem(0);
            int level = at(k, 0).getBlockData() instanceof Levelled l ? l.getLevel() : -1;
            return all(still != null && still.getAmount() == 4 && GlassItem.colour(still) == Colour.RED ? null : "hopper now holds " + still, level == 0 ? null : "composter level " + level);
        }));
        int d = 150;
        steps.add(new Step("dispenser: dispenses glowing glass as an item that keeps its colour", () -> {
            clearItems(d + 1, 0);
            Block disp = piston(d, 0, Material.DISPENSER, BlockFace.EAST);
            ((Dispenser) disp.getState(false)).getInventory().addItem(GlassItem.create(Colour.LIGHT_GRAY, 1));
            ((Dispenser) disp.getState(false)).dispense();
        }, 4, () -> all(at(d + 1, 0).getType().isAir() ? null : "it placed a block: " + at(d + 1, 0).getType(), dropped(d + 1, 0, 3, Colour.LIGHT_GRAY))));
        // Sulfur cubes (26.x) swallow every carrier block; glowing glass must not go in. Monsters need a difficulty.
        int cu = 240;
        List<org.bukkit.entity.LivingEntity> cubes = new ArrayList<>();
        steps.add(new Step("sulfur cube: a dispenser won't feed it glowing glass (a plain sea lantern goes in)", () -> {
            w.setDifficulty(Difficulty.EASY);
            for (int i = 0; i < 2; i++) {
                int z = i == 0 ? -4 : 4;
                Block disp = w.getBlockAt(cu, Y, z);
                disp.setType(Material.DISPENSER);
                Directional dir = (Directional) disp.getBlockData();
                dir.setFacing(BlockFace.EAST);
                disp.setBlockData(dir);
                var cube = (org.bukkit.entity.LivingEntity) w.spawnEntity(w.getBlockAt(cu + 1, Y, z).getLocation().add(0.5, 0, 0.5), EntityType.SULFUR_CUBE);
                if (cube instanceof org.bukkit.entity.Mob mob) mob.setAI(false);
                cube.setPersistent(true);
                cubes.add(cube);
                ((Dispenser) disp.getState(false)).getInventory().addItem(i == 0 ? GlassItem.create(Colour.BLUE, 1) : new ItemStack(Material.SEA_LANTERN));
                ((Dispenser) disp.getState(false)).dispense();
            }
        }, 4, () -> {
            ItemStack stillIn = ((Dispenser) w.getBlockAt(cu, Y, -4).getState(false)).getInventory().getItem(0);
            ItemStack glassBody = cubes.get(0).getEquipment().getItem(org.bukkit.inventory.EquipmentSlot.BODY);
            ItemStack plainBody = cubes.get(1).getEquipment().getItem(org.bukkit.inventory.EquipmentSlot.BODY);
            String control = plainBody.getType() == Material.SEA_LANTERN ? null : "the plain sea lantern didn't go in either (" + plainBody.getType() + "): test can't tell";
            return all(control, glassBody.getType().isAir() ? null : "the cube swallowed " + glassBody.getType(),
                GlassItem.colour(stillIn) == Colour.BLUE ? null : "the dispenser now holds " + stillIn);
        }));
        steps.add(new Step("sulfur cube: glowing glass it picks up off the ground is spat back out (a plain shroomlight stays in)", () -> {
            for (var cube : cubes) {
                cube.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.BODY, null);
                if (cube instanceof org.bukkit.entity.Mob mob) mob.setAI(true);
                cube.setCanPickupItems(true);
            }
            clearItems(cu + 1, -4);
            clearItems(cu + 1, 4);
            w.dropItem(cubes.get(0).getLocation(), GlassItem.create(Colour.RED, 1)).setPickupDelay(0);
            w.dropItem(cubes.get(1).getLocation(), new ItemStack(Material.SHROOMLIGHT)).setPickupDelay(0);
        }, 60, () -> {
            ItemStack glassBody = cubes.get(0).getEquipment().getItem(org.bukkit.inventory.EquipmentSlot.BODY);
            ItemStack plainBody = cubes.get(1).getEquipment().getItem(org.bukkit.inventory.EquipmentSlot.BODY);
            List<Item> loose = new ArrayList<>(w.getNearbyEntitiesByType(Item.class, cubes.get(0).getLocation(), 4));
            Item back = loose.stream().filter(i -> GlassItem.colour(i.getItemStack()) == Colour.RED).findFirst().orElse(null);
            for (var cube : cubes) cube.remove();
            w.setDifficulty(Difficulty.PEACEFUL);
            return all(plainBody.getType() == Material.SHROOMLIGHT ? null : "the control cube didn't take a plain shroomlight (" + plainBody.getType() + "): test can't tell",
                glassBody.getType().isAir() ? null : "the cube still holds " + glassBody.getType(),
                back != null ? null : "the glowing glass isn't on the ground near the cube: " + describe(loose),
                back == null || !back.canMobPickup() ? null : "the spat-out item can be picked up by mobs again");
        }));

        int m = 170;
        steps.add(new Step("demo: a wall of all 16, each recorded; refused when the space is taken", () -> { }, 1, () -> {
            Location feet = at(m, 0).getLocation();
            String problem = Demo.build(feet, BlockFace.SOUTH, records);
            if (problem != null) return "demo refused: " + problem;
            Set<Colour> seen = EnumSet.noneOf(Colour.class);
            for (int dx = -6; dx <= 6; dx++) for (int dy = 0; dy < 3; dy++) {
                Colour c = records.at(w.getBlockAt(m + dx, Y + dy, 3));
                if (c != null) seen.add(c);
            }
            return all(seen.size() == 16 ? null : "wall has " + seen, Demo.build(feet, BlockFace.SOUTH, records) != null ? null : "built a second wall over the first");
        }));
        steps.add(new Step("protocol: a chunk answer lists that chunk's glowing glass", () -> { }, 1, () -> {
            Block red = w.getBlockAt(m + 4, Y + 1, 3);   // the wall's top-left as seen facing south
            Chunk c = red.getChunk();
            ChunkGlass glass = records.chunk(w, c.getX(), c.getZ());
            Protocol.Chunk read = Protocol.readChunk(Protocol.chunk(new Protocol.ChunkPos(c.getX(), c.getZ()), glass));
            Colour got = read.glass().get(ChunkGlass.pack(red.getX() & 15, red.getY(), red.getZ() & 15));
            return all(read.glass().equals(glass) ? null : "round trip changed it", read.pos().x() == c.getX() && read.pos().z() == c.getZ() ? null : "position " + read.pos(),
                got == Colour.RED ? null : "the wall's top-left reads " + got);
        }));

        String only = System.getProperty("glowingglass.selftest.only", "");
        if (!only.isBlank()) {
            List<String> prefixes = List.of(only.split(","));
            steps.removeIf(st -> prefixes.stream().noneMatch(st.name()::startsWith));
            report.add("INFO only steps starting with " + prefixes + " (" + steps.size() + " steps)");
        }
        run(0);
    }

    void run(int i) {
        if (i >= steps.size()) { finish(); return; }
        Step s = steps.get(i);
        try { s.action().run(); }
        catch (RuntimeException ex) { record(s.name(), "action threw " + ex); run(i + 1); return; }
        getServer().getScheduler().runTaskLater(this, () -> {
            String problem;
            try { problem = s.check().get(); } catch (RuntimeException ex) { problem = "check threw " + ex; }
            record(s.name(), problem);
            run(i + 1);
        }, Math.max(1, s.ticks()));
    }

    void record(String name, String problem) {
        if (problem == null) report.add("PASS " + name);
        else { failures++; report.add("FAIL " + name + ": " + problem); }
    }

    void finish() {
        report.add(failures == 0 ? "SELFTEST PASSED on " + getServer().getVersion() : "SELFTEST FAILED (" + failures + ")");
        try { getDataFolder().mkdirs(); Files.write(getDataFolder().toPath().resolve("report.txt"), report); }
        catch (IOException e) { getLogger().severe("Could not write report: " + e); }
        getLogger().info(String.join("\n", report));
        if (Boolean.getBoolean("glowingglass.selftest.exit")) getServer().shutdown();
    }
}
