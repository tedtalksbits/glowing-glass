package dev.whizzle.glowingglass.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;

/**
 * Glowing glass sounds like glass: stepping, falling, mining, breaking and placing (the sea lantern colours already
 * do; froglights and shroomlights don't). Called from the {@code mixin/} sound hooks; works only where the mod knows
 * the block is glowing glass, so sounds the server plays for other players' actions stay the carrier's.
 */
public final class Sounds {
    private Sounds() {}

    public static SoundType at(BlockPos pos, SoundType original) {
        if (original == SoundType.GLASS || pos == null) return original;
        return GlowingGlassClient.GLASS.at(pos.getX(), pos.getY(), pos.getZ()) != null ? SoundType.GLASS : original;
    }
}
