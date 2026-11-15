package dev.whizzle.glowingglass.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkGlassTest {
    @Test void packsNegativeAndHighY() {
        for (int y : new int[]{-64, -1, 0, 70, 319, 2031, -2032})
            for (int x : new int[]{0, 7, 15}) for (int z : new int[]{0, 9, 15}) {
                int p = ChunkGlass.pack(x, y, z);
                assertEquals(x, ChunkGlass.x(p));
                assertEquals(y, ChunkGlass.y(p));
                assertEquals(z, ChunkGlass.z(p));
            }
    }

    @Test void saveLoadRoundTrip() {
        ChunkGlass g = new ChunkGlass();
        g.put(ChunkGlass.pack(1, -60, 2), Colour.BLUE);
        g.put(ChunkGlass.pack(15, 300, 15), Colour.CLEAR);
        assertEquals(g, ChunkGlass.load(g.save()));
        assertEquals(new ChunkGlass(), ChunkGlass.load(new ChunkGlass().save()));
    }

    @Test void unknownColourSkipped() {
        Wire.Writer out = new Wire.Writer().varInt(1).varInt(2).int32(ChunkGlass.pack(0, 0, 0)).varInt(15).int32(ChunkGlass.pack(1, 0, 0)).varInt(Colour.RED.id);
        ChunkGlass g = ChunkGlass.load(out.bytes());
        assertEquals(1, g.size());
        assertEquals(Colour.RED, g.get(ChunkGlass.pack(1, 0, 0)));
    }

    @Test void rejectsBadData() {
        assertThrows(IllegalArgumentException.class, () -> ChunkGlass.load(new Wire.Writer().varInt(2).varInt(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> ChunkGlass.load(new Wire.Writer().varInt(1).varInt(3).bytes()));
        assertThrows(IllegalArgumentException.class, () -> ChunkGlass.load(new Wire.Writer().varInt(1).varInt(-1).bytes()));
        assertThrows(IllegalArgumentException.class, () -> ChunkGlass.load(new Wire.Writer().varInt(1).varInt(0).varInt(7).bytes()));
    }
}
