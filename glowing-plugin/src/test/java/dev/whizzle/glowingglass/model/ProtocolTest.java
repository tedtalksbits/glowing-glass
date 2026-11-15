package dev.whizzle.glowingglass.model;

import dev.whizzle.glowingglass.model.Protocol.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolTest {
    @Test void helloWelcome() {
        assertEquals(Protocol.VERSION, Protocol.readHello(Protocol.hello()));
        assertEquals(Protocol.VERSION, Protocol.readWelcome(Protocol.welcome()));
        assertEquals("glowingglass:chunk", Protocol.channel(Protocol.CHUNK));
    }

    @Test void queryRoundTripAndLimits() {
        List<ChunkPos> q = List.of(new ChunkPos(0, 0), new ChunkPos(-3, 12), new ChunkPos(Integer.MAX_VALUE, Integer.MIN_VALUE));
        assertEquals(q, Protocol.readQuery(Protocol.query(q)));
        List<ChunkPos> many = new ArrayList<>();
        for (int i = 0; i <= Protocol.MAX_QUERY; i++) many.add(new ChunkPos(i, i));
        assertThrows(IllegalArgumentException.class, () -> Protocol.query(many));
        assertThrows(IllegalArgumentException.class, () -> Protocol.query(List.of()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.readQuery(new Wire.Writer().varInt(0).bytes()));
        byte[] trailing = new Wire.Writer().varInt(1).int32(1).int32(2).bool(true).bytes();
        assertThrows(IllegalArgumentException.class, () -> Protocol.readQuery(trailing));
    }

    @Test void chunkRoundTrip() {
        ChunkGlass g = new ChunkGlass();
        g.put(ChunkGlass.pack(3, -64, 4), Colour.PINK);
        g.put(ChunkGlass.pack(3, -63, 4), Colour.CLEAR);
        Protocol.Chunk c = Protocol.readChunk(Protocol.chunk(new ChunkPos(-1, 5), g));
        assertEquals(new ChunkPos(-1, 5), c.pos());
        assertEquals(g, c.glass());
        assertEquals(new ChunkGlass(), Protocol.readChunk(Protocol.chunk(new ChunkPos(0, 0), new ChunkGlass())).glass());
    }

    @Test void chunkKeyMatchesBukkit() {
        for (int[] xz : new int[][]{{0, 0}, {-1, 1}, {123, -456}, {-30000000 >> 4, 30000000 >> 4}}) {
            long bukkit = ((long) xz[1] << 32) | (xz[0] & 0xFFFFFFFFL);   // org.bukkit.Chunk.getChunkKey(x, z)
            ChunkPos p = new ChunkPos(xz[0], xz[1]);
            assertEquals(bukkit, p.key());
            assertEquals(p, ChunkPos.of(p.key()));
        }
    }
}
