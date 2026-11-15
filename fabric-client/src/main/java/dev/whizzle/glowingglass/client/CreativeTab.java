package dev.whizzle.glowingglass.client;

import dev.whizzle.glowingglass.model.Colour;
import dev.whizzle.glowingglass.model.Glass;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/**
 * A "Glowing Glass" creative tab with all 32 (always-on, then redstone), so creative search finds them. Glowing
 * glass isn't an item type of its own (the plugin makes it from a carrier item plus data), so the stacks here are
 * built with exactly the components the plugin gives a crafted one ({@code GlassItem.create}): the server then
 * treats a stack taken from the tab as real glowing glass, and it stacks with crafted ones. The self-test checks
 * that the two match ("creative tab").
 */
public final class CreativeTab {
    private CreativeTab() {}

    public static ItemStack stack(Glass g) {
        ItemStack s = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(g.carrierId())));
        CompoundTag values = new CompoundTag();   // Bukkit's persistent data lives under PublicBukkitValues
        values.putString("glowingglass:colour", g.colour().key);
        if (g.redstone()) values.putByte("glowingglass:redstone", (byte) 1);
        CompoundTag root = new CompoundTag();
        root.put("PublicBukkitValues", values);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        s.set(DataComponents.ITEM_NAME, Component.literal(g.itemName()));
        s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(g.modelString()), List.of()));
        return s;
    }

    public static void register() {
        CreativeModeTab tab = FabricCreativeModeTab.builder()
            .title(Component.literal("Glowing Glass"))
            .icon(() -> stack(Glass.of(Colour.LIGHT_BLUE, false)))
            .displayItems((params, output) -> { for (Glass g : Glass.all()) output.accept(stack(g)); })
            .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("glowingglass", "glowing_glass"), tab);
    }
}
