package dev.whizzle.glowingglass.model;

import org.junit.jupiter.api.Test;

import java.util.*;

import static dev.whizzle.glowingglass.model.Colour.Carrier.*;
import static org.junit.jupiter.api.Assertions.*;

class ColourTest {
    @Test void sixteenColoursNoBlackUniqueIds() {
        assertEquals(16, Colour.values().length);
        assertTrue(Colour.byKey("black").isEmpty());
        assertEquals(16, Arrays.stream(Colour.values()).map(c -> c.id).distinct().count());
        assertTrue(Colour.byId(15).isEmpty(), "15 is black's dye id, kept free");
        for (Colour c : Colour.values()) {
            assertEquals(c, Colour.byId(c.id).orElseThrow());
            assertEquals(c, Colour.byKey(c.key.toUpperCase(Locale.ROOT)).orElseThrow());
        }
    }

    /** The owner's table (kickoff, confirmed 2026-11-14; brown on the ochre froglight). */
    @Test void carriersAsDecided() {
        Map<Colour.Carrier, Set<Colour>> want = Map.of(
            SEA_LANTERN, EnumSet.of(Colour.WHITE, Colour.LIGHT_GRAY, Colour.GRAY, Colour.CLEAR, Colour.CYAN, Colour.LIGHT_BLUE, Colour.BLUE),
            OCHRE_FROGLIGHT, EnumSet.of(Colour.YELLOW, Colour.BROWN),
            VERDANT_FROGLIGHT, EnumSet.of(Colour.LIME, Colour.GREEN),
            PEARLESCENT_FROGLIGHT, EnumSet.of(Colour.PURPLE, Colour.MAGENTA, Colour.PINK),
            SHROOMLIGHT, EnumSet.of(Colour.RED, Colour.ORANGE));
        for (Colour c : Colour.values()) assertTrue(want.get(c.carrier).contains(c), c + " on " + c.carrier);
    }

    @Test void namesAndIds() {
        assertEquals("Glowing Light Blue Glass", Colour.LIGHT_BLUE.itemName());
        assertEquals("Glowing Glass", Colour.CLEAR.itemName());
        assertEquals("minecraft:light_gray_stained_glass", Colour.LIGHT_GRAY.glassId());
        assertEquals("minecraft:glass", Colour.CLEAR.glassId());
        assertEquals("glowingglass:blue", Colour.BLUE.modelString());
    }
}
