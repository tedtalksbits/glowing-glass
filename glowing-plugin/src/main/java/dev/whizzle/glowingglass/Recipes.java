package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Colour;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.Arrays;
import java.util.List;

/**
 * The 16 recipes (8 glass of one colour around 1 glowstone dust gives 8 glowing glass, like dyeing glass), and the
 * guard that keeps glowing glass from working as its carrier: no crafting recipe takes it (grids and crafters), and
 * composters refuse it (a shroomlight, red and orange's carrier, can be composted).
 */
public final class Recipes implements Listener {
    public static NamespacedKey key(Colour c) { return new NamespacedKey("glowingglass", c.key); }

    public static List<NamespacedKey> keys() { return Arrays.stream(Colour.values()).map(Recipes::key).toList(); }

    public static ShapedRecipe recipe(Colour c) {
        ShapedRecipe r = new ShapedRecipe(key(c), GlassItem.create(c, 8));
        r.shape("GGG", "GDG", "GGG");
        r.setIngredient('G', GlassItem.glass(c));
        r.setIngredient('D', Material.GLOWSTONE_DUST);
        r.setGroup("glowingglass");
        r.setCategory(CraftingBookCategory.BUILDING);
        return r;
    }

    public static void register() {
        for (Colour c : Colour.values()) {
            Bukkit.removeRecipe(key(c));
            Bukkit.addRecipe(recipe(c));
        }
    }

    public static void unregister() { for (NamespacedKey k : keys()) Bukkit.removeRecipe(k); }

    public static void discover(Player p) { p.discoverRecipes(keys()); }

    static boolean anyGlowing(ItemStack[] items) {
        for (ItemStack i : items) if (GlassItem.is(i)) return true;
        return false;
    }

    /** No crafting recipe takes glowing glass (none of ours does; a vanilla one would treat it as its carrier). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent e) {
        if (e.getInventory().getResult() != null && anyGlowing(e.getInventory().getMatrix())) e.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent e) {
        if (e.getBlock().getState(false) instanceof Crafter crafter && anyGlowing(crafter.getInventory().getContents())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onComposterClick(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && b != null && b.getType() == Material.COMPOSTER && GlassItem.is(e.getItem())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperIntoComposter(InventoryMoveItemEvent e) {
        if (e.getDestination().getType() == InventoryType.COMPOSTER && GlassItem.is(e.getItem())) e.setCancelled(true);
    }
}
