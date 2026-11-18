package dev.whizzle.glowingglass.client;

import dev.whizzle.glowingglass.model.Glass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A mark only counts while its block is still that glass's carrier. A mark can outlive its glass: a placement the
 * mod predicted but the server refused, or a block the server replaced without telling the plugin (a command, a world
 * editor). Without this check any block put there later would sound like glass, and a carrier would look like it.
 */
public final class Carriers {
    private Carriers() {}

    private static final Map<Glass, Block> BLOCKS = new ConcurrentHashMap<>();

    public static Block of(Glass g) {
        return BLOCKS.computeIfAbsent(g, x -> BuiltInRegistries.BLOCK.getValue(Identifier.parse(x.carrierId())));
    }

    public static boolean holds(Glass g, BlockState state) { return state != null && state.is(of(g)); }

    /** The glowing glass at {@code pos} if {@code state} (the block there) is its carrier, else null. */
    public static Glass at(BlockPos pos, BlockState state) {
        Glass g = GlowingGlassClient.GLASS.at(pos.getX(), pos.getY(), pos.getZ());
        return g != null && holds(g, state) ? g : null;
    }

    /**
     * The server set the block at {@code pos} (a block update, or its answer to a placement we predicted): a mark
     * there whose carrier isn't the new block is dropped. A block a piston is moving keeps its mark.
     */
    public static void serverSet(BlockPos pos, BlockState state) {
        if (state.is(Blocks.MOVING_PISTON)) return;
        GlowingGlassClient.GLASS.verify(pos.getX(), pos.getY(), pos.getZ(), g -> holds(g, state));
    }
}
