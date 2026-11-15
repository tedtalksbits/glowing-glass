package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Glass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.block.data.Directional;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.Keyed;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.Arrays;
import java.util.List;

/**
 * The 32 recipes: 8 glass of one colour around 1 glowstone dust gives 8 glowing glass (like dyeing glass); 8 glowing
 * glass of one colour around 1 redstone dust gives 8 redstone glowing glass. And the guards that keep glowing glass
 * from being used as its carrier: no crafting recipe but ours takes it (grids and crafters),
 * composters refuse it (a shroomlight, red and orange's carrier, can be composted), and sulfur cubes don't keep it
 * (every carrier is on their list; the cube would show the carrier inside it, breaking the illusion): feeding by hand
 * and by dispenser is refused, and anything a cube picks up itself is spat back out.
 */
public final class Recipes implements Listener {
    public static NamespacedKey key(Glass g) { return new NamespacedKey("glowingglass", g.key()); }

    public static List<NamespacedKey> keys() { return Glass.all().stream().map(Recipes::key).toList(); }

    public static ShapedRecipe recipe(Glass g) {
        ShapedRecipe r = new ShapedRecipe(key(g), GlassItem.create(g, 8));
        r.shape("GGG", "GDG", "GGG");
        if (g.redstone()) {
            // Any glowing glass of that colour, renamed ones too (an exact-item choice would need the exact name).
            Glass from = Glass.of(g.colour(), false);
            r.setIngredient('G', RecipeChoice.predicateChoice(i -> from.equals(GlassItem.glass(i)), GlassItem.create(from, 1)));
            r.setIngredient('D', Material.REDSTONE);
        } else {
            r.setIngredient('G', GlassItem.stainedGlass(g.colour()));
            r.setIngredient('D', Material.GLOWSTONE_DUST);
        }
        r.setGroup(g.redstone() ? "glowingglass_redstone" : "glowingglass");
        r.setCategory(g.redstone() ? CraftingBookCategory.REDSTONE : CraftingBookCategory.BUILDING);
        return r;
    }

    public static void register() {
        for (Glass g : Glass.all()) {
            Bukkit.removeRecipe(key(g));
            Bukkit.addRecipe(recipe(g));
        }
    }

    public static void unregister() { for (NamespacedKey k : keys()) Bukkit.removeRecipe(k); }

    public static void discover(Player p) { p.discoverRecipes(keys()); }

    static boolean anyGlowing(ItemStack[] items) {
        for (ItemStack i : items) if (GlassItem.is(i)) return true;
        return false;
    }

    static boolean ours(Recipe r) { return r instanceof Keyed k && k.getKey().getNamespace().equals("glowingglass"); }

    /** No crafting recipe but ours takes glowing glass (a vanilla one would treat it as its carrier). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent e) {
        if (e.getInventory().getResult() != null && !ours(e.getRecipe()) && anyGlowing(e.getInventory().getMatrix())) e.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent e) {
        if (!ours(e.getRecipe()) && e.getBlock().getState(false) instanceof Crafter crafter && anyGlowing(crafter.getInventory().getContents())) e.setCancelled(true);
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

    // ---- sulfur cubes ----

    /** A player feeding a sulfur cube glowing glass: refused (the item stays in the hand). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFeedCube(PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getType() == EntityType.SULFUR_CUBE && GlassItem.is(e.getPlayer().getInventory().getItem(e.getHand()))) e.setCancelled(true);
    }

    /**
     * Any other way glowing glass ends up inside a sulfur cube (it picks items up off the ground with its own code,
     * which fires no pickup event on Paper 26.3): it's spat straight back out, as an item mobs can't pick up again.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCubeSwallowed(EntityEquipmentChangedEvent e) {
        if (e.getEntity().getType() != EntityType.SULFUR_CUBE) return;
        EntityEquipmentChangedEvent.EquipmentChange body = e.getEquipmentChanges().get(EquipmentSlot.BODY);
        if (body != null && GlassItem.is(body.newItem())) spitOut(e.getEntity());
    }

    static void spitOut(LivingEntity cube) {
        EntityEquipment eq = cube.getEquipment();
        if (eq == null) return;
        ItemStack inside = eq.getItem(EquipmentSlot.BODY);
        if (!GlassItem.is(inside)) return;
        eq.setItem(EquipmentSlot.BODY, null);
        Item dropped = cube.getWorld().dropItem(cube.getLocation(), inside);
        dropped.setCanMobPickup(false);
    }

    /** A dispenser feeding glowing glass to a sulfur cube in front of it: nothing is dispensed. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispenseIntoCube(BlockDispenseEvent e) {
        if (!GlassItem.is(e.getItem()) || !(e.getBlock().getBlockData() instanceof Directional d)) return;
        Block front = e.getBlock().getRelative(d.getFacing());
        for (Entity n : front.getWorld().getNearbyEntities(BoundingBox.of(front)))
            if (n.getType() == EntityType.SULFUR_CUBE) { e.setCancelled(true); return; }
    }
}
