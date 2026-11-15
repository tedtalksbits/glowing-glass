package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Glass;
import io.papermc.paper.event.player.PlayerPickBlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Keeps glowing glass glowing glass through every way a block appears, moves or turns back into an item:
 * <ul>
 * <li>placing: the item's colour is recorded for the block (a plain carrier placed there clears any old record);</li>
 * <li>a player breaking it: {@link BlockDropItemEvent}, the carrier's drops (prismarine crystals for a sea lantern)
 * become one glowing glass; creative breaks drop nothing, as usual;</li>
 * <li>explosions: glowing glass is taken out of the explosion's block list and broken here, dropping glowing glass
 * with the explosion's drop chance (vanilla merges identical drops from different blocks, so they can't be told
 * apart afterwards);</li>
 * <li>pistons (pushing, pulling, slime and honey contraptions, across chunk borders): the records move with the
 * blocks;</li>
 * <li>anything else that drops the block while it's still there ({@code breakNaturally}, withers, commands with
 * drops): the drop is matched to the block by position;</li>
 * <li>middle-click picks glowing glass of that colour, never the carrier.</li>
 * </ul>
 */
public final class Keeper implements Listener {
    private final Plugin plugin;
    private final Records records;
    /** Glowing glass an explosion is about to destroy, by event, between the HIGHEST and MONITOR handlers. */
    private final Map<Event, List<Block>> exploding = new IdentityHashMap<>();
    /** Positions whose carrier drop was already turned into glowing glass this tick (more drops get removed). */
    private final Set<Location> droppedThisTick = new HashSet<>();

    public Keeper(Plugin plugin, Records records) {
        this.plugin = plugin;
        this.records = records;
    }

    /** Called every tick. */
    public void tick() {
        exploding.clear();
        droppedThisTick.clear();
    }

    /** Drops the record at {@code b} next tick if the block turned out to be gone. */
    void verifyLater(Block b) { Bukkit.getScheduler().runTask(plugin, () -> records.verify(b)); }

    static Location centre(Block b) { return b.getLocation().add(0.5, 0.5, 0.5); }

    // ---- placing ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        Glass c = GlassItem.glass(e.getItemInHand());
        if (c != null && b.getType() == GlassItem.carrier(c)) records.put(b, c);
        else if (records.recorded(b) != null) records.remove(b);
    }

    // ---- breaking ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (records.recorded(e.getBlock()) != null) verifyLater(e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerBreakDrops(BlockDropItemEvent e) {
        Block b = e.getBlock();
        Glass c = records.recorded(b);
        if (c == null || e.getBlockState().getType() != GlassItem.carrier(c)) return;
        records.remove(b);
        if (e.getItems().isEmpty()) return;
        Item first = e.getItems().getFirst();
        first.setItemStack(GlassItem.create(c, 1));
        e.getItems().retainAll(List.of(first));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent e) {
        if (records.recorded(e.getBlock()) != null) verifyLater(e.getBlock());
    }

    /**
     * Drops that spawn while their block is still there: {@code breakNaturally}, withers, {@code /setblock ...
     * destroy}. The first carrier drop at a glowing glass block becomes one glowing glass; any more are removed.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        ItemStack stack = e.getEntity().getItemStack();
        if (!isCarrierDrop(stack.getType()) || GlassItem.is(stack)) return;
        Block at = e.getLocation().getBlock();
        Glass c = records.at(at);
        if (c == null || at.getType() == Material.MOVING_PISTON || !dropsAs(c, stack.getType())) return;
        if (droppedThisTick.add(at.getLocation())) {
            e.getEntity().setItemStack(GlassItem.create(c, 1));
            verifyLater(at);
        } else {
            e.setCancelled(true);
        }
    }

    /** What a carrier block can drop: itself (redstone lamps too), or prismarine crystals for a sea lantern. */
    static boolean dropsAs(Glass c, Material drop) {
        Material carrier = GlassItem.carrier(c);
        return drop == carrier || (carrier == Material.SEA_LANTERN && drop == Material.PRISMARINE_CRYSTALS);
    }

    static boolean isCarrierDrop(Material m) { return m == Material.PRISMARINE_CRYSTALS || GlassItem.carriers().contains(m); }

    // ---- explosions ----

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplodeTake(EntityExplodeEvent e) { take(e, e.blockList()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplodeTake(BlockExplodeEvent e) { take(e, e.blockList()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) { blowUp(e, e.getYield()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) { blowUp(e, e.getYield()); }

    private void take(Event e, List<Block> blocks) {
        List<Block> glass = null;
        for (Iterator<Block> it = blocks.iterator(); it.hasNext(); ) {
            Block b = it.next();
            if (records.at(b) == null || b.getType() == Material.MOVING_PISTON) continue;
            it.remove();
            (glass == null ? glass = new ArrayList<>() : glass).add(b);
        }
        if (glass != null) exploding.put(e, glass);
    }

    private void blowUp(Event e, float yield) {
        List<Block> glass = exploding.remove(e);
        if (glass == null) return;
        records.batch(() -> {
            for (Block b : glass) {
                Glass c = records.at(b);
                if (c == null) continue;
                records.remove(b);
                b.setType(Material.AIR);
                if (ThreadLocalRandom.current().nextFloat() < yield) b.getWorld().dropItemNaturally(centre(b), GlassItem.create(c, 1));
            }
        });
    }

    // ---- pistons ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) { move(e.getBlock(), e.getBlocks(), true); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) { move(e.getBlock(), e.getBlocks(), false); }

    /** Moves the records of the glowing glass among {@code blocks} one block along the piston's movement. */
    void move(Block piston, List<Block> blocks, boolean extend) {
        if (!(piston.getBlockData() instanceof Directional d)) return;
        BlockFace dir = extend ? d.getFacing() : d.getFacing().getOppositeFace();
        Map<Block, Glass> moving = new LinkedHashMap<>();
        for (Block b : blocks) {
            Glass c = records.at(b);
            if (c != null) moving.put(b, c);
        }
        if (moving.isEmpty()) return;
        records.batch(() -> {
            for (Block b : moving.keySet()) records.remove(b);
            for (var m : moving.entrySet()) records.put(m.getKey().getRelative(dir), m.getValue());
        });
    }

    // ---- middle-click ----

    /**
     * Middle-click on glowing glass picks glowing glass of that kind and colour, never the carrier (which is what the game would
     * pick). In creative it hands one out if there's none in the inventory (with or without Ctrl: there's no extra
     * data to copy); in survival with none, nothing is picked.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPick(PlayerPickBlockEvent e) {
        Glass c = records.at(e.getBlock());
        if (c == null) return;
        Player p = e.getPlayer();
        PlayerInventory inv = p.getInventory();
        int slot = slotWith(inv, c);
        if (slot >= 0) {
            e.setSourceSlot(slot);
            // Paper selects the target slot when the source is already on the hotbar, and otherwise swaps the source
            // into the target. The target it chose was for the carrier, so choose it again.
            e.setTargetSlot(slot < 9 ? slot : hotbarSlotFor(inv));
            return;
        }
        e.setCancelled(true);
        if (p.getGameMode() != GameMode.CREATIVE) return;
        int target = hotbarSlotFor(inv);
        inv.setItem(target, GlassItem.create(c, 1));
        inv.setHeldItemSlot(target);
    }

    static int slotWith(PlayerInventory inv, Glass c) {
        for (int i = 0; i < 36; i++) if (c.equals(GlassItem.glass(inv.getItem(i)))) return i;
        return -1;
    }

    /** Like vanilla: the selected slot if it's empty, else the first empty hotbar slot, else the selected slot. */
    static int hotbarSlotFor(PlayerInventory inv) {
        int selected = inv.getHeldItemSlot();
        if (empty(inv.getItem(selected))) return selected;
        for (int i = 0; i < 9; i++) if (empty(inv.getItem(i))) return i;
        return selected;
    }

    private static boolean empty(ItemStack item) { return item == null || item.getType().isAir(); }
}
