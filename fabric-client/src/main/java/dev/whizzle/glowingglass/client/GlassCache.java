package dev.whizzle.glowingglass.client;

import dev.whizzle.glowingglass.model.ChunkGlass;
import dev.whizzle.glowingglass.model.Colour;
import dev.whizzle.glowingglass.model.Protocol.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the server said is glowing glass, by chunk, for the current level. Read from chunk-meshing threads
 * ({@link #at}), written on the client thread: each chunk's map is replaced whole, never changed in place. Pure Java.
 */
public final class GlassCache {
    private final Map<Long, Map<Integer, Colour>> chunks = new ConcurrentHashMap<>();

    /** The colour of glowing glass at a block, or null. */
    public Colour at(int x, int y, int z) {
        Map<Integer, Colour> c = chunks.get(new ChunkPos(x >> 4, z >> 4).key());
        return c == null ? null : c.get(ChunkGlass.pack(x & 15, y, z & 15));
    }

    /**
     * Replaces what's known about a chunk; returns the section y indices (block y >> 4) whose blocks changed, which
     * need meshing again.
     */
    public Set<Integer> update(ChunkPos pos, ChunkGlass glass) {
        Map<Integer, Colour> now = Map.copyOf(glass.entries());
        Map<Integer, Colour> before = now.isEmpty() ? chunks.remove(pos.key()) : chunks.put(pos.key(), now);
        if (before == null) before = Map.of();
        Set<Integer> sections = new TreeSet<>();
        for (var e : now.entrySet()) if (e.getValue() != before.get(e.getKey())) sections.add(ChunkGlass.y(e.getKey()) >> 4);
        for (int p : before.keySet()) if (!now.containsKey(p)) sections.add(ChunkGlass.y(p) >> 4);
        return sections;
    }

    /** Marks one block (the player's own placement, before the server confirms it). */
    public void mark(int x, int y, int z, Colour c) {
        chunks.compute(new ChunkPos(x >> 4, z >> 4).key(), (k, old) -> {
            Map<Integer, Colour> m = old == null ? new HashMap<>() : new HashMap<>(old);
            m.put(ChunkGlass.pack(x & 15, y, z & 15), c);
            return Map.copyOf(m);
        });
    }

    /** Takes a {@link #mark} back if it's still there. */
    public void unmark(int x, int y, int z, Colour c) {
        chunks.computeIfPresent(new ChunkPos(x >> 4, z >> 4).key(), (k, old) -> {
            int p = ChunkGlass.pack(x & 15, y, z & 15);
            if (old.get(p) != c) return old;
            Map<Integer, Colour> m = new HashMap<>(old);
            m.remove(p);
            return m.isEmpty() ? null : Map.copyOf(m);
        });
    }

    public void forget(ChunkPos pos) { chunks.remove(pos.key()); }

    public void clear() { chunks.clear(); }

    /** Glowing glass blocks known. */
    public int size() { int n = 0; for (var c : chunks.values()) n += c.size(); return n; }
}
