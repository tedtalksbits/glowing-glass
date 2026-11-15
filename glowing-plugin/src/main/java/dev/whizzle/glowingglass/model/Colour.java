package dev.whizzle.glowingglass.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The 16 glowing glass colours (15 stained colours and clear; no black in v1) and the real block each one is on the
 * server ("carrier"). Pure Java, compiled into the plugin and the mod. {@link #id} is on the wire and in saved
 * chunks: never renumber. Stained colours use their dye id (black, 15, is kept free); clear is 16.
 */
public enum Colour {
    WHITE(0, "white", "White", Carrier.SEA_LANTERN),
    ORANGE(1, "orange", "Orange", Carrier.SHROOMLIGHT),
    MAGENTA(2, "magenta", "Magenta", Carrier.PEARLESCENT_FROGLIGHT),
    LIGHT_BLUE(3, "light_blue", "Light Blue", Carrier.SEA_LANTERN),
    YELLOW(4, "yellow", "Yellow", Carrier.OCHRE_FROGLIGHT),
    LIME(5, "lime", "Lime", Carrier.VERDANT_FROGLIGHT),
    PINK(6, "pink", "Pink", Carrier.PEARLESCENT_FROGLIGHT),
    GRAY(7, "gray", "Gray", Carrier.SEA_LANTERN),
    LIGHT_GRAY(8, "light_gray", "Light Gray", Carrier.SEA_LANTERN),
    CYAN(9, "cyan", "Cyan", Carrier.SEA_LANTERN),
    PURPLE(10, "purple", "Purple", Carrier.PEARLESCENT_FROGLIGHT),
    BLUE(11, "blue", "Blue", Carrier.SEA_LANTERN),
    BROWN(12, "brown", "Brown", Carrier.OCHRE_FROGLIGHT),
    GREEN(13, "green", "Green", Carrier.VERDANT_FROGLIGHT),
    RED(14, "red", "Red", Carrier.SHROOMLIGHT),
    CLEAR(16, "clear", "", Carrier.SEA_LANTERN);

    /** The vanilla blocks glowing glass sits on: light 15 each, coloured light from shaders by block type. */
    public enum Carrier {
        SEA_LANTERN("minecraft:sea_lantern"),
        OCHRE_FROGLIGHT("minecraft:ochre_froglight"),
        VERDANT_FROGLIGHT("minecraft:verdant_froglight"),
        PEARLESCENT_FROGLIGHT("minecraft:pearlescent_froglight"),
        SHROOMLIGHT("minecraft:shroomlight");

        public final String id;
        Carrier(String id) { this.id = id; }
    }

    public final int id;
    /** {@code light_blue}; {@code clear} for plain glass. In textures ({@code glowing_<key>_glass}) and commands. */
    public final String key;
    /** Words before "Glass" in the item name ("" for clear: "Glowing Glass"). */
    public final String words;
    public final Carrier carrier;

    Colour(int id, String key, String words, Carrier carrier) {
        this.id = id;
        this.key = key;
        this.words = words;
        this.carrier = carrier;
    }

    /** The item name: "Glowing Light Blue Glass", "Glowing Glass". */
    public String itemName() { return words.isEmpty() ? "Glowing Glass" : "Glowing " + words + " Glass"; }

    /** The glass the recipe takes: {@code minecraft:blue_stained_glass}, {@code minecraft:glass}. */
    public String glassId() { return this == CLEAR ? "minecraft:glass" : "minecraft:" + key + "_stained_glass"; }

    /** The custom model data string the mod's item definitions select on: {@code glowingglass:blue}. */
    public String modelString() { return Protocol.NAMESPACE + ":" + key; }

    public static Optional<Colour> byId(int id) { return Arrays.stream(values()).filter(c -> c.id == id).findFirst(); }

    public static Optional<Colour> byKey(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(c -> c.key.equals(k)).findFirst();
    }
}
