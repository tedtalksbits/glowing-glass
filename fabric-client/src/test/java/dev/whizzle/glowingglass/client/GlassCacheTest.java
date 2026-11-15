package dev.whizzle.glowingglass.client;

import dev.whizzle.glowingglass.model.ChunkGlass;
import dev.whizzle.glowingglass.model.Colour;
import dev.whizzle.glowingglass.model.Protocol.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GlassCacheTest {
    static ChunkGlass glass(Object... posColour) {
        ChunkGlass g = new ChunkGlass();
        for (int i = 0; i < posColour.length; i += 2) g.put((Integer) posColour[i], (Colour) posColour[i + 1]);
        return g;
    }

    @Test void looksUpByWorldPosition() {
        GlassCache c = new GlassCache();
        c.update(new ChunkPos(-1, 2), glass(ChunkGlass.pack(15, -60, 0), Colour.BLUE));
        assertEquals(Colour.BLUE, c.at(-1, -60, 32));
        assertNull(c.at(-1, -59, 32));
        assertNull(c.at(15, -60, 32), "same local spot, other chunk");
    }

    @Test void reportsTheSectionsThatChanged() {
        GlassCache c = new GlassCache();
        ChunkPos p = new ChunkPos(0, 0);
        assertEquals(Set.of(-4, 4), c.update(p, glass(ChunkGlass.pack(1, -60, 1), Colour.RED, ChunkGlass.pack(1, 70, 1), Colour.RED)));
        assertEquals(Set.of(), c.update(p, glass(ChunkGlass.pack(1, -60, 1), Colour.RED, ChunkGlass.pack(1, 70, 1), Colour.RED)), "nothing new");
        assertEquals(Set.of(4), c.update(p, glass(ChunkGlass.pack(1, -60, 1), Colour.RED, ChunkGlass.pack(1, 70, 1), Colour.CYAN)), "colour changed");
        assertEquals(Set.of(-4, 4, 0), c.update(p, glass(ChunkGlass.pack(2, 3, 2), Colour.CLEAR)), "moved (piston): old and new");
        assertEquals(Set.of(0), c.update(p, new ChunkGlass()), "all gone");
        assertEquals(0, c.size());
    }

    @Test void forgetAndClear() {
        GlassCache c = new GlassCache();
        c.update(new ChunkPos(0, 0), glass(ChunkGlass.pack(0, 0, 0), Colour.LIME));
        c.update(new ChunkPos(1, 0), glass(ChunkGlass.pack(0, 0, 0), Colour.LIME));
        c.forget(new ChunkPos(0, 0));
        assertNull(c.at(0, 0, 0));
        assertEquals(Colour.LIME, c.at(16, 0, 0));
        c.clear();
        assertEquals(0, c.size());
    }

    @Test void markAndUnmarkTheOwnPlacement() {
        GlassCache c = new GlassCache();
        c.update(new ChunkPos(0, 0), glass(ChunkGlass.pack(0, 0, 0), Colour.LIME));
        c.mark(1, 0, 0, Colour.RED);
        assertEquals(Colour.RED, c.at(1, 0, 0));
        assertEquals(Colour.LIME, c.at(0, 0, 0), "the rest of the chunk stays");
        c.unmark(1, 0, 0, Colour.BLUE);
        assertEquals(Colour.RED, c.at(1, 0, 0), "only takes back its own mark");
        c.unmark(1, 0, 0, Colour.RED);
        assertNull(c.at(1, 0, 0));
        c.mark(-20, 5, -20, Colour.CLEAR);
        assertEquals(Set.of(), c.update(new ChunkPos(-2, -2), glass(ChunkGlass.pack(12, 5, 12), Colour.CLEAR)), "the server confirms the mark: nothing to re-mesh");
    }
}
