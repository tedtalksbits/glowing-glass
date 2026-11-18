package dev.whizzle.glowingglass.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Glowing glass sounds like glass: stepping, falling, mining, breaking and placing (the sea lantern colours already
 * do; froglights and shroomlights don't). Called from the {@code mixin/} sound hooks: {@link #at} where the game
 * picks a block's sounds on this client, {@link #fromServer} for sounds the server plays (another player placing or
 * walking). The server sends its glowing glass update before it plays the place sound (Paper plays it after the
 * place event), so the mod already knows.
 */
public final class Sounds {
    private Sounds() {}

    /** Glass's sounds for {@code state} at {@code pos} if it's glowing glass (the block is that glass's carrier). */
    public static SoundType at(BlockPos pos, BlockState state, SoundType original) {
        if (original == SoundType.GLASS || pos == null) return original;
        return Carriers.at(pos, state) != null ? SoundType.GLASS : original;
    }

    /** Froglight and shroomlight sounds, by id, to glass's matching sound. */
    private static Map<Identifier, SoundEvent> swaps;

    private static Map<Identifier, SoundEvent> swaps() {
        if (swaps == null) {
            Map<Identifier, SoundEvent> m = new HashMap<>();
            for (SoundType carrier : new SoundType[]{SoundType.FROGLIGHT, SoundType.SHROOMLIGHT})
                for (Function<SoundType, SoundEvent> kind : java.util.List.<Function<SoundType, SoundEvent>>of(
                        SoundType::getPlaceSound, SoundType::getBreakSound, SoundType::getStepSound, SoundType::getHitSound, SoundType::getFallSound))
                    m.put(kind.apply(carrier).location(), kind.apply(SoundType.GLASS));
            swaps = m;
        }
        return swaps;
    }

    /**
     * A sound the server plays at (x, y, z): a froglight or shroomlight sound at glowing glass becomes glass's. Block
     * sounds play at the block's centre, footsteps at the feet (the block is just below).
     */
    public static Holder<SoundEvent> fromServer(double x, double y, double z, Holder<SoundEvent> sound) {
        SoundEvent glass = swaps().get(sound.value().location());
        if (glass == null) return sound;
        int bx = Mth.floor(x), bz = Mth.floor(z);
        GlassCache cache = GlowingGlassClient.GLASS;
        if (cache.at(bx, Mth.floor(y), bz) == null && cache.at(bx, Mth.floor(y - 0.5), bz) == null) return sound;
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(glass);
    }
}
