package dev.whizzle.glowingglass.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * One kind of glowing glass: a colour, always on or redstone (lit only while powered, on a redstone lamp). Pure Java,
 * compiled into the plugin and the mod. {@link #id} is on the wire and in saved chunks: always-on glass uses its
 * colour's id, redstone glass the colour's id + {@link #REDSTONE_OFFSET}, so a mod that only knows always-on glass
 * skips redstone entries (and shows the lamp). Compare with {@code equals}; {@link #of} returns shared instances.
 */
public record Glass(Colour colour, boolean redstone) {
    public static final int REDSTONE_OFFSET = 64;
    public static final String REDSTONE_LAMP = "minecraft:redstone_lamp";
    private static final List<Glass> ALL;

    static {
        List<Glass> all = new ArrayList<>();
        for (boolean r : new boolean[]{false, true}) for (Colour c : Colour.values()) all.add(new Glass(c, r));
        ALL = Collections.unmodifiableList(all);
    }

    /** All 32: the 16 always-on ones first, then the 16 redstone ones, each in {@link Colour} order. */
    public static List<Glass> all() { return ALL; }

    public static List<Glass> all(boolean redstone) { return ALL.subList(redstone ? 16 : 0, redstone ? 32 : 16); }

    public static Glass of(Colour c, boolean redstone) { return ALL.get((redstone ? 16 : 0) + c.ordinal()); }

    public int id() { return colour.id + (redstone ? REDSTONE_OFFSET : 0); }

    public static Optional<Glass> byId(int id) {
        boolean r = id >= REDSTONE_OFFSET;
        return Colour.byId(r ? id - REDSTONE_OFFSET : id).map(c -> of(c, r));
    }

    /** {@code blue}, {@code redstone_blue}: in commands, recipe keys and the item's model string. */
    public String key() { return redstone ? "redstone_" + colour.key : colour.key; }

    public static Optional<Glass> byKey(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        boolean r = k.startsWith("redstone_");
        return Colour.byKey(r ? k.substring("redstone_".length()) : k).map(c -> of(c, r));
    }

    /** "Glowing Blue Glass", "Redstone Glowing Blue Glass", "Glowing Glass", "Redstone Glowing Glass". */
    public String itemName() { return redstone ? "Redstone " + colour.itemName() : colour.itemName(); }

    /** The vanilla block it is on the server: the colour's light block, or a redstone lamp. */
    public String carrierId() { return redstone ? REDSTONE_LAMP : colour.carrier.id; }

    /** The custom model data string the mod's item definitions select on: {@code glowingglass:redstone_blue}. */
    public String modelString() { return Protocol.NAMESPACE + ":" + key(); }

    @Override public String toString() { return key(); }
}
