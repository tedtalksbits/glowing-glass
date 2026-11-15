package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Colour;
import dev.whizzle.glowingglass.model.Glass;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.Set;

/**
 * The glowing glass item: its carrier block's item (a sea lantern for blue, a redstone lamp for any redstone glass)
 * with the colour in its persistent data ({@code glowingglass:colour}, plus {@code glowingglass:redstone} for the
 * redstone kind), an item name ("Glowing Blue Glass") and a custom model data string ({@code glowingglass:blue},
 * {@code glowingglass:redstone_blue}) that the mod's item definitions select on. Players without the mod see a renamed
 * carrier. A renamed item keeps its glass (the anvil name goes over the item name).
 */
public final class GlassItem {
    private GlassItem() {}

    public static final NamespacedKey COLOUR_KEY = new NamespacedKey("glowingglass", "colour");
    public static final NamespacedKey REDSTONE_KEY = new NamespacedKey("glowingglass", "redstone");

    public static Material carrier(Glass g) { return material(g.carrierId()); }

    /** The always-on carrier of a colour. */
    public static Material carrier(Colour c) { return material(c.carrier.id); }

    public static Material stainedGlass(Colour c) { return material(c.glassId()); }

    private static Material material(String id) {
        Material m = Material.matchMaterial(id);
        if (m == null) throw new IllegalStateException("Unknown block " + id);
        return m;
    }

    /** The six carrier blocks: five light blocks and the redstone lamp. */
    public static Set<Material> carriers() {
        Set<Material> out = EnumSet.noneOf(Material.class);
        for (Glass g : Glass.all()) out.add(carrier(g));
        return out;
    }

    public static ItemStack create(Glass glass, int amount) {
        ItemStack item = new ItemStack(carrier(glass), amount);
        item.editPersistentDataContainer(pdc -> {
            pdc.set(COLOUR_KEY, PersistentDataType.STRING, glass.colour().key);
            if (glass.redstone()) pdc.set(REDSTONE_KEY, PersistentDataType.BYTE, (byte) 1);
        });
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(glass.itemName()));
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(glass.modelString()).build());
        return item;
    }

    /** The item's glass, or null if it isn't glowing glass (also when the glass doesn't belong on this item). */
    public static Glass glass(ItemStack item) {
        if (item == null || item.getType().isAir() || !carriers().contains(item.getType())) return null;
        var pdc = item.getPersistentDataContainer();
        String key = pdc.get(COLOUR_KEY, PersistentDataType.STRING);
        if (key == null) return null;
        Colour c = Colour.byKey(key).orElse(null);
        if (c == null) return null;
        Glass g = Glass.of(c, pdc.has(REDSTONE_KEY, PersistentDataType.BYTE));
        return carrier(g) == item.getType() ? g : null;
    }

    public static boolean is(ItemStack item) { return glass(item) != null; }
}
