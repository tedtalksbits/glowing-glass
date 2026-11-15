package dev.whizzle.glowingglass.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * The glowing glass in one chunk: position inside the chunk to {@link Glass} (colour, always on or redstone). Positions are packed as
 * {@code (y << 8) | (z << 4) | x} with x and z 0-15 and y the world height (may be negative). The same bytes are the
 * chunk's saved record (plugin) and the body of the {@code chunk} message (docs/PROTOCOL.md).
 */
public final class ChunkGlass {
    /** More than a chunk can hold (16 x 16 x 4064); guards against corrupt data. */
    public static final int MAX_ENTRIES = 16 * 16 * 4064;
    private final TreeMap<Integer, Glass> entries = new TreeMap<>();

    public static int pack(int localX, int y, int localZ) { return (y << 8) | ((localZ & 15) << 4) | (localX & 15); }
    public static int x(int packed) { return packed & 15; }
    public static int z(int packed) { return (packed >> 4) & 15; }
    public static int y(int packed) { return packed >> 8; }

    public Glass get(int packed) { return entries.get(packed); }
    public Glass put(int packed, Glass glass) { return entries.put(packed, glass); }
    public Glass remove(int packed) { return entries.remove(packed); }
    public boolean isEmpty() { return entries.isEmpty(); }
    public int size() { return entries.size(); }
    public Map<Integer, Glass> entries() { return Collections.unmodifiableMap(entries); }

    /** VarInt n, then n x (Int32 packed position, VarInt glass id: {@link Glass#id}). */
    public void write(Wire.Writer out) {
        out.varInt(entries.size());
        for (var e : entries.entrySet()) out.int32(e.getKey()).varInt(e.getValue().id());
    }

    /** Reads {@link #write}; entries with an unknown id (a newer server) are skipped. */
    public static ChunkGlass read(Wire.Reader in) {
        int n = in.varInt();
        if (n < 0 || n > MAX_ENTRIES) throw new IllegalArgumentException("Bad entry count " + n);
        ChunkGlass g = new ChunkGlass();
        for (int i = 0; i < n; i++) {
            int packed = in.int32();
            int id = in.varInt();
            Glass.byId(id).ifPresent(gl -> g.entries.put(packed, gl));
        }
        return g;
    }

    /** Saved form: VarInt format (1), then {@link #write}. */
    public byte[] save() {
        Wire.Writer out = new Wire.Writer();
        out.varInt(1);
        write(out);
        return out.bytes();
    }

    public static ChunkGlass load(byte[] data) {
        Wire.Reader in = new Wire.Reader(data);
        int format = in.varInt();
        if (format != 1) throw new IllegalArgumentException("Unknown format " + format);
        ChunkGlass g = read(in);
        in.end();
        return g;
    }

    @Override public boolean equals(Object o) { return o instanceof ChunkGlass g && g.entries.equals(entries); }
    @Override public int hashCode() { return entries.hashCode(); }
    @Override public String toString() { return entries.toString(); }
}
