package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Colour;
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
 * building and trying the recipe. Crafting test blocks every time is tedious.
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
            Colour c = WALL.get(i);
            b.setType(GlassItem.carrier(c));
            records.put(b, c);
        }
        return null;
    }

    /** 16 of each glowing glass, and blue glass and glowstone dust for one craft. */
    public static List<ItemStack> kit() {
        List<ItemStack> out = new ArrayList<>();
        for (Colour c : WALL) out.add(GlassItem.create(c, 16));
        out.add(new ItemStack(Material.BLUE_STAINED_GLASS, 8));
        out.add(new ItemStack(Material.GLOWSTONE_DUST, 1));
        return out;
    }

    /** Gives the kit; whatever doesn't fit drops at the player's feet. */
    public static void give(Player p) {
        for (ItemStack overflow : p.getInventory().addItem(kit().toArray(ItemStack[]::new)).values())
            p.getWorld().dropItemNaturally(p.getLocation(), overflow);
    }
}
