package dev.whizzle.glowingglass.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GlassTest {
    @Test void thirtyTwoKindsWithUniqueIdsAndKeys() {
        assertEquals(32, Glass.all().size());
        Set<Integer> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (Glass g : Glass.all()) {
            assertTrue(ids.add(g.id()), "duplicate id " + g.id());
            assertTrue(keys.add(g.key()), "duplicate key " + g.key());
            assertSame(g, Glass.byId(g.id()).orElseThrow());
            assertSame(g, Glass.byKey(g.key()).orElseThrow());
            assertSame(g, Glass.of(g.colour(), g.redstone()));
        }
        assertTrue(Glass.all(false).stream().noneMatch(Glass::redstone));
        assertTrue(Glass.all(true).stream().allMatch(Glass::redstone));
    }

    @Test void alwaysOnIdsAreTheColourIdsSoOldSavesStillRead() {
        for (Colour c : Colour.values()) assertEquals(c.id, Glass.of(c, false).id());
        assertEquals(Glass.REDSTONE_OFFSET + Colour.BLUE.id, Glass.of(Colour.BLUE, true).id());
        assertTrue(Glass.byId(Glass.REDSTONE_OFFSET + 15).isEmpty(), "black stays free in both kinds");
    }

    @Test void namesCarriersAndModelStrings() {
        assertEquals("Redstone Glowing Light Blue Glass", Glass.of(Colour.LIGHT_BLUE, true).itemName());
        assertEquals("Redstone Glowing Glass", Glass.of(Colour.CLEAR, true).itemName());
        assertEquals("Glowing Glass", Glass.of(Colour.CLEAR, false).itemName());
        assertEquals("minecraft:redstone_lamp", Glass.of(Colour.RED, true).carrierId());
        assertEquals("minecraft:shroomlight", Glass.of(Colour.RED, false).carrierId());
        assertEquals("glowingglass:redstone_blue", Glass.of(Colour.BLUE, true).modelString());
        assertEquals("glowingglass:blue", Glass.of(Colour.BLUE, false).modelString());
        assertEquals("redstone_light_gray", Glass.of(Colour.LIGHT_GRAY, true).key());
    }

    @Test void anOldClientSkipsRedstoneEntries() {
        // What client 0.2.x does with ids it doesn't know: ChunkGlass.read skips them.
        ChunkGlass g = new ChunkGlass();
        g.put(ChunkGlass.pack(0, 0, 0), Glass.of(Colour.RED, true));
        g.put(ChunkGlass.pack(1, 0, 0), Glass.of(Colour.RED, false));
        Wire.Writer out = new Wire.Writer();
        g.write(out);
        assertEquals(g, ChunkGlass.read(new Wire.Reader(out.bytes())));
    }
}
