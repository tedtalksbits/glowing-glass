package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.ChunkGlass;
import dev.whizzle.glowingglass.model.Colour;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Which blocks are glowing glass, and which colour. The record lives in each chunk's persistent data
 * ({@code glowingglass:glass}, {@link ChunkGlass#save}), so it travels with the world save; loaded chunks are kept
 * in memory and every change is written straight back.
 * <p>
 * A record only counts while its block is still the colour's carrier (or a block being moved by a piston): anything
 * that replaces the block without an event the plugin sees (commands, world editors) leaves a stale record, which
 * reads as "not glowing glass" and is dropped by {@link #verify} or when the chunk next loads.
 */
public final class Records implements Listener {
    public static final NamespacedKey KEY = new NamespacedKey("glowingglass", "glass");

    public record ChunkId(UUID world, long key) {
        static ChunkId of(Chunk c) { return new ChunkId(c.getWorld().getUID(), c.getChunkKey()); }
        static ChunkId of(Block b) { return new ChunkId(b.getWorld().getUID(), Chunk.getChunkKey(b.getX() >> 4, b.getZ() >> 4)); }
        public int x() { return (int) key; }
        public int z() { return (int) (key >> 32); }
    }

    private final Map<ChunkId, ChunkGlass> chunks = new HashMap<>();
    private final Logger log;
    /** Called with the chunk after its glass changed (the client bridge pushes it to watching mods). */
    private final List<Consumer<ChunkId>> listeners = new ArrayList<>();
    /** Inside {@link #batch}: chunks changed so far, announced once at the end. */
    private Set<ChunkId> batched;

    public Records(Logger log) { this.log = log; }

    public void onChange(Consumer<ChunkId> listener) { listeners.add(listener); }

    public void removeListener(Consumer<ChunkId> listener) { listeners.remove(listener); }

    private void changed(ChunkId id) { for (Consumer<ChunkId> l : List.copyOf(listeners)) l.accept(id); }

    static boolean holds(Block b, Colour c) {
        Material t = b.getType();
        return t == GlassItem.carrier(c) || t == Material.MOVING_PISTON;
    }

    // ---- reading ----

    /** The colour of the glowing glass at {@code b}, or null (also for a stale record). Never changes anything. */
    public Colour at(Block b) {
        ChunkGlass g = glass(b);
        if (g == null) return null;
        Colour c = g.get(ChunkGlass.pack(b.getX() & 15, b.getY(), b.getZ() & 15));
        return c != null && holds(b, c) ? c : null;
    }

    /** The recorded colour at {@code b} whatever the block is now (for drops after the block is gone). */
    public Colour recorded(Block b) {
        ChunkGlass g = glass(b);
        return g == null ? null : g.get(ChunkGlass.pack(b.getX() & 15, b.getY(), b.getZ() & 15));
    }

    /** The glass of a loaded chunk, or null if it isn't loaded. */
    public ChunkGlass chunk(World w, int cx, int cz) {
        if (!w.isChunkLoaded(cx, cz)) return null;
        return chunks.computeIfAbsent(new ChunkId(w.getUID(), Chunk.getChunkKey(cx, cz)), id -> read(w.getChunkAt(cx, cz)));
    }

    private ChunkGlass glass(Block b) { return chunk(b.getWorld(), b.getX() >> 4, b.getZ() >> 4); }

    public int count() { int n = 0; for (ChunkGlass g : chunks.values()) n += g.size(); return n; }

    // ---- changing ----

    /**
     * Runs {@code changes} and announces each changed chunk once at the end, not after every block (a piston moving
     * a row of glass must not push a half-moved state to the mods).
     */
    public void batch(Runnable changes) {
        if (batched != null) { changes.run(); return; }
        batched = new LinkedHashSet<>();
        try { changes.run(); }
        finally {
            Set<ChunkId> done = batched;
            batched = null;
            for (ChunkId id : done) changed(id);
        }
    }

    public void put(Block b, Colour c) {
        ChunkGlass g = glass(b);
        if (g == null) return;
        Colour before = g.put(ChunkGlass.pack(b.getX() & 15, b.getY(), b.getZ() & 15), c);
        if (before != c) write(b);
    }

    /** Forgets the record at {@code b}; returns the colour it had (null if none). */
    public Colour remove(Block b) {
        ChunkGlass g = glass(b);
        if (g == null) return null;
        Colour before = g.remove(ChunkGlass.pack(b.getX() & 15, b.getY(), b.getZ() & 15));
        if (before != null) write(b);
        return before;
    }

    /** Drops the record at {@code b} if the block is no longer its carrier. */
    public void verify(Block b) {
        Colour c = recorded(b);
        if (c != null && !holds(b, c)) remove(b);
    }

    private void write(Block b) {
        Chunk chunk = b.getChunk();
        save(chunk, chunks.get(ChunkId.of(chunk)));
        if (batched != null) batched.add(ChunkId.of(chunk));
        else changed(ChunkId.of(chunk));
    }

    // ---- chunk data ----

    private ChunkGlass read(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        byte[] data = pdc.get(KEY, PersistentDataType.BYTE_ARRAY);
        if (data == null) return new ChunkGlass();
        ChunkGlass g;
        try { g = ChunkGlass.load(data); }
        catch (IllegalArgumentException e) {
            log.warning("Unreadable glowing glass record in chunk " + chunk.getX() + "," + chunk.getZ() + " of " + chunk.getWorld().getName() + " (" + e.getMessage() + "); ignored.");
            return new ChunkGlass();
        }
        // Blocks replaced while the plugin wasn't watching (world editors, the plugin removed for a while).
        int stale = 0;
        for (var e : List.copyOf(g.entries().entrySet())) {
            int p = e.getKey();
            Block b = chunk.getBlock(ChunkGlass.x(p), ChunkGlass.y(p), ChunkGlass.z(p));
            if (!holds(b, e.getValue())) { g.remove(p); stale++; }
        }
        if (stale > 0) {
            log.info("Forgot " + stale + " glowing glass record(s) in chunk " + chunk.getX() + "," + chunk.getZ() + " of " + chunk.getWorld().getName() + ": those blocks were replaced.");
            save(chunk, g);
        }
        return g;
    }

    private static void save(Chunk chunk, ChunkGlass g) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (g == null || g.isEmpty()) pdc.remove(KEY);
        else pdc.set(KEY, PersistentDataType.BYTE_ARRAY, g.save());
    }

    public void loadAll() {
        for (World w : Bukkit.getWorlds()) for (Chunk c : w.getLoadedChunks()) chunks.computeIfAbsent(ChunkId.of(c), id -> read(c));
    }

    public void saveAll() {
        for (World w : Bukkit.getWorlds()) for (Chunk c : w.getLoadedChunks()) {
            ChunkGlass g = chunks.get(ChunkId.of(c));
            if (g != null) save(c, g);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent e) { chunks.computeIfAbsent(ChunkId.of(e.getChunk()), id -> read(e.getChunk())); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent e) {
        ChunkGlass g = chunks.remove(ChunkId.of(e.getChunk()));
        if (g != null) save(e.getChunk(), g);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent e) {
        UUID w = e.getWorld().getUID();
        chunks.keySet().removeIf(id -> id.world().equals(w));
    }
}
