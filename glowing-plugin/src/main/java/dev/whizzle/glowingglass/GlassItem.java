package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Colour;
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
 * The glowing glass item: its carrier block's item (a sea lantern for blue) with the colour in its persistent data
 * ({@code glowingglass:colour}), an item name ("Glowing Blue Glass") and a custom model data string
 * ({@code glowingglass:blue}) that the mod's item definitions select on. Players without the mod see a renamed
 * sea lantern. A renamed item keeps its colour (the anvil name goes over the item name).
 */
public final class GlassItem {
    private GlassItem() {}

    public static final NamespacedKey COLOUR_KEY = new NamespacedKey("glowingglass", "colour");

    public static Material carrier(Colour c) { return material(c.carrier.id); }

    public static Material glass(Colour c) { return material(c.glassId()); }

    private static Material material(String id) {
        Material m = Material.matchMaterial(id);
        if (m == null) throw new IllegalStateException("Unknown block " + id);
        return m;
    }

    /** The five carrier blocks. */
    public static Set<Material> carriers() {
        Set<Material> out = EnumSet.noneOf(Material.class);
        for (Colour c : Colour.values()) out.add(carrier(c));
        return out;
    }

    public static ItemStack create(Colour colour, int amount) {
        ItemStack item = new ItemStack(carrier(colour), amount);
        item.editPersistentDataContainer(pdc -> pdc.set(COLOUR_KEY, PersistentDataType.STRING, colour.key));
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(colour.itemName()));
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(colour.modelString()).build());
        return item;
    }

    /** The item's colour, or null if it isn't glowing glass (also when the colour doesn't belong on this item). */
    public static Colour colour(ItemStack item) {
        if (item == null || item.getType().isAir() || !carriers().contains(item.getType())) return null;
        String key = item.getPersistentDataContainer().get(COLOUR_KEY, PersistentDataType.STRING);
        if (key == null) return null;
        Colour c = Colour.byKey(key).orElse(null);
        return c != null && carrier(c) == item.getType() ? c : null;
    }

    public static boolean is(ItemStack item) { return colour(item) != null; }
}
