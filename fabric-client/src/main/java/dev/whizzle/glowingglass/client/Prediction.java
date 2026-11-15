package dev.whizzle.glowingglass.client;

import dev.whizzle.glowingglass.model.Colour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * The player's own placements: the game places the block on screen before the server answers, so the mod marks the
 * position from the item itself (its custom model data names the colour) just before the block appears; otherwise
 * the carrier would show until the server's update arrives. The server's update then replaces the guess.
 */
public final class Prediction {
    private Prediction() {}

    /** The colour of a glowing glass item, from its custom model data string; null for anything else. */
    public static Colour colour(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        String s = data == null ? null : data.getString(0);
        if (s == null || !s.startsWith("glowingglass:")) return null;
        Colour c = Colour.byKey(s.substring("glowingglass:".length())).orElse(null);
        if (c == null) return null;
        String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return item.equals(c.carrier.id) ? c : null;
    }

    /** Before a client-side placement: marks the position if the item is glowing glass. Returns what was marked. */
    public static Colour before(BlockPlaceContext ctx) {
        if (!ctx.getLevel().isClientSide() || !GlowingGlassClient.active()) return null;
        Colour c = colour(ctx.getItemInHand());
        if (c == null) return null;
        BlockPos p = ctx.getClickedPos();
        GlowingGlassClient.GLASS.mark(p.getX(), p.getY(), p.getZ(), c);
        return c;
    }

    /** After it: if the placement didn't happen, takes the mark back. */
    public static void after(BlockPlaceContext ctx, Colour marked, boolean placed) {
        if (marked == null || placed) return;
        BlockPos p = ctx.getClickedPos();
        GlowingGlassClient.GLASS.unmark(p.getX(), p.getY(), p.getZ(), marked);
    }
}
