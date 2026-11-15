package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Colour;
import dev.whizzle.glowingglass.model.Glass;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /glowingglass demo} (creative only): a wall of all 16 glowing glass in front of the player, and a kit for
 * building and trying the recipe. {@code demo redstone}: the 16 redstone ones twice, the top copy powered by
 * redstone blocks behind it, the bottom copy not. Crafting test blocks every time is tedious.
 */
public final class Demo {
    private Demo() {}

    public static final int WIDTH = 8, HEIGHT = 2, DISTANCE = 3;

    /** Top row then bottom row, left to right as the player sees them: the warm colours, then the cool ones. */
    static final List<Colour> WALL = List.of(
        Colour.RED, Colour.ORANGE, Colour.YELLOW, Colour.LIME, Colour.GREEN, Colour.CYAN, Colour.LIGHT_BLUE, Colour.BLUE,
        Colour.PURPLE, Colour.MAGENTA, Colour.PINK, Colour.BROWN, Colour.WHITE, Colour.LIGHT_GRAY, Colour.GRAY, Colour.CLEAR);

    /** The wall's bottom-left block (as the player sees it), {@link #DISTANCE} blocks in front of the player's feet. */
    static Block origin(Location feet, BlockFace facing) {
        return feet.getBlock().getRelative(facing, DISTANCE).getRelative(leftOf(facing), WIDTH / 2);
    }

    static BlockFace leftOf(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.WEST; case SOUTH -> BlockFace.EAST; case EAST -> BlockFace.NORTH; default -> BlockFace.SOUTH;
        };
    }

    static List<Block> footprint(Block origin, BlockFace facing) {
        BlockFace right = leftOf(facing).getOppositeFace();
        List<Block> out = new ArrayList<>();
        for (int row = 0; row < HEIGHT; row++) for (int col = 0; col < WIDTH; col++) out.add(origin.getRelative(right, col).getRelative(BlockFace.UP, row));
        return out;
    }

    /** Builds the wall; null if done, or why not (nothing is changed then). */
    public static String build(Location feet, BlockFace facing, Records records) {
        Block origin = origin(feet, facing);
        for (Block b : footprint(origin, facing))
            if (!b.getType().isAir()) return "Not enough room: the demo needs an empty space " + WIDTH + " wide and " + HEIGHT + " high, " + DISTANCE + " blocks in front of you.";
        BlockFace right = leftOf(facing).getOppositeFace();
        for (int i = 0; i < WALL.size(); i++) {
            int row = HEIGHT - 1 - i / WIDTH, col = i % WIDTH;
            Block b = origin.getRelative(right, col).getRelative(BlockFace.UP, row);
            Glass g = Glass.of(WALL.get(i), false);
            b.setType(GlassItem.carrier(g));
            records.put(b, g);
        }
        return null;
    }

    /** The redstone demo's blocks: the wall (4 rows), and the redstone blocks behind its top 2 rows. */
    static List<Block> redstoneFootprint(Block origin, BlockFace facing) {
        BlockFace right = leftOf(facing).getOppositeFace();
        List<Block> out = new ArrayList<>();
        for (int row = 0; row < 2 * HEIGHT; row++) for (int col = 0; col < WIDTH; col++) {
            Block b = origin.getRelative(right, col).getRelative(BlockFace.UP, row);
            out.add(b);
            if (row >= HEIGHT) out.add(b.getRelative(facing));
        }
        return out;
    }

    /** Builds the redstone wall; null if done, or why not (nothing is changed then). */
    public static String buildRedstone(Location feet, BlockFace facing, Records records) {
        Block origin = origin(feet, facing);
        for (Block b : redstoneFootprint(origin, facing))
            if (!b.getType().isAir()) return "Not enough room: the redstone demo needs an empty space " + WIDTH + " wide, " + (2 * HEIGHT) + " high and 2 deep, " + DISTANCE + " blocks in front of you.";
        BlockFace right = leftOf(facing).getOppositeFace();
        records.batch(() -> {
            for (int copy = 0; copy < 2; copy++) for (int i = 0; i < WALL.size(); i++) {
                int row = copy * HEIGHT + HEIGHT - 1 - i / WIDTH, col = i % WIDTH;
                Block b = origin.getRelative(right, col).getRelative(BlockFace.UP, row);
                Glass g = Glass.of(WALL.get(i), true);
                b.setType(GlassItem.carrier(g));
                records.put(b, g);
            }
        });
        // Power last, so the lamps light up as they would in a build.
        for (int row = HEIGHT; row < 2 * HEIGHT; row++) for (int col = 0; col < WIDTH; col++)
            origin.getRelative(right, col).getRelative(BlockFace.UP, row).getRelative(facing).setType(Material.REDSTONE_BLOCK);
        return null;
    }

    /** 16 of each glowing glass, and blue glass and glowstone dust for one craft. */
    public static List<ItemStack> kit() { return kit(false); }

    /**
     * Always on: 16 of each, and blue glass and glowstone dust for one craft. Redstone: 16 of each redstone glowing
     * glass, levers and redstone dust, and 8 glowing blue glass with a redstone dust for one craft.
     */
    public static List<ItemStack> kit(boolean redstone) {
        List<ItemStack> out = new ArrayList<>();
        for (Colour c : WALL) out.add(GlassItem.create(Glass.of(c, redstone), 16));
        if (redstone) {
            out.add(new ItemStack(Material.LEVER, 8));
            out.add(new ItemStack(Material.REDSTONE, 32));
            out.add(GlassItem.create(Glass.of(Colour.BLUE, false), 8));
            out.add(new ItemStack(Material.REDSTONE, 1));
        } else {
            out.add(new ItemStack(Material.BLUE_STAINED_GLASS, 8));
            out.add(new ItemStack(Material.GLOWSTONE_DUST, 1));
        }
        return out;
    }

    /** Gives the kit; whatever doesn't fit drops at the player's feet. */
    public static void give(Player p, boolean redstone) {
        for (ItemStack overflow : p.getInventory().addItem(kit(redstone).toArray(ItemStack[]::new)).values())
            p.getWorld().dropItemNaturally(p.getLocation(), overflow);
    }
}
