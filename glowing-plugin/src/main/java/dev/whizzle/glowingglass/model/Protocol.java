package dev.whizzle.glowingglass.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The client-mod protocol (docs/PROTOCOL.md): channel names and every message's bytes, shared by the plugin and the
 * mod so both encode and decode with the same code. Bump {@link #VERSION} when any message's bytes change.
 */
public final class Protocol {
    private Protocol() {}

    public static final int VERSION = 1;
    public static final String NAMESPACE = "glowingglass";
    public static final String HELLO = "hello", WELCOME = "welcome", QUERY = "query", CHUNK = "chunk";
    public static final List<String> SERVERBOUND = List.of(HELLO, QUERY);
    public static final List<String> CLIENTBOUND = List.of(WELCOME, CHUNK);
    /** Most chunks in one query. */
    public static final int MAX_QUERY = 64;

    public static String channel(String name) { return NAMESPACE + ":" + name; }

    public record ChunkPos(int x, int z) {
        public long key() { return ((long) z << 32) | (x & 0xFFFFFFFFL); }   // Bukkit's Chunk.getChunkKey
        public static ChunkPos of(long key) { return new ChunkPos((int) key, (int) (key >> 32)); }
    }

    /** A chunk's glowing glass, as the server sends it. */
    public record Chunk(ChunkPos pos, ChunkGlass glass) {}

    // ---- hello (C->S): VarInt protocol ----
    public static byte[] hello() { return new Wire.Writer().varInt(VERSION).bytes(); }
    public static int readHello(byte[] data) { Wire.Reader in = new Wire.Reader(data); int v = in.varInt(); in.end(); return v; }

    // ---- welcome (S->C): VarInt protocol ----
    public static byte[] welcome() { return new Wire.Writer().varInt(VERSION).bytes(); }
    public static int readWelcome(byte[] data) { Wire.Reader in = new Wire.Reader(data); int v = in.varInt(); in.end(); return v; }

    // ---- query (C->S): VarInt n (1-64), n x (Int32 chunk x, Int32 chunk z) ----
    public static byte[] query(List<ChunkPos> chunks) {
        if (chunks.isEmpty() || chunks.size() > MAX_QUERY) throw new IllegalArgumentException("1-" + MAX_QUERY + " chunks per query");
        Wire.Writer out = new Wire.Writer().varInt(chunks.size());
        for (ChunkPos c : chunks) out.int32(c.x()).int32(c.z());
        return out.bytes();
    }
    public static List<ChunkPos> readQuery(byte[] data) {
        Wire.Reader in = new Wire.Reader(data);
        int n = in.varInt();
        if (n < 1 || n > MAX_QUERY) throw new IllegalArgumentException("Bad query size " + n);
        List<ChunkPos> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(new ChunkPos(in.int32(), in.int32()));
        in.end();
        return out;
    }

    // ---- chunk (S->C): Int32 chunk x, Int32 chunk z, then the chunk's glass (ChunkGlass.write) ----
    public static byte[] chunk(ChunkPos pos, ChunkGlass glass) {
        Wire.Writer out = new Wire.Writer().int32(pos.x()).int32(pos.z());
        glass.write(out);
        return out.bytes();
    }
    public static Chunk readChunk(byte[] data) {
        Wire.Reader in = new Wire.Reader(data);
        ChunkPos pos = new ChunkPos(in.int32(), in.int32());
        ChunkGlass glass = ChunkGlass.read(in);
        in.end();
        return new Chunk(pos, glass);
    }
}
