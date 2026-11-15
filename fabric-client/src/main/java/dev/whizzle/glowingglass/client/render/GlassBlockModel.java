package dev.whizzle.glowingglass.client.render;

import dev.whizzle.glowingglass.client.GlowingGlassClient;
import dev.whizzle.glowingglass.model.Colour;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Wraps a carrier block's model (sea lantern, froglights, shroomlight). At a position the server marked as glowing
 * glass it emits the glowing glass cube instead (a swap, not an overlay); everywhere else the carrier's own model.
 * The block is still the carrier to the game, so light, shaders' block-type lighting and culling are unchanged.
 */
public final class GlassBlockModel extends WrapperBlockStateModel {
    private final Function<Colour, BlockStateModel> glass;

    public GlassBlockModel(BlockStateModel carrier, Function<Colour, BlockStateModel> glass) {
        super(carrier);
        this.glass = glass;
    }

    /** The glass cube to draw instead of the carrier, or null. A block sliding under a piston carries its colour. */
    private BlockStateModel at(BlockAndTintGetter level, BlockPos pos) {
        Colour c = level instanceof MovingGlass moving ? moving.glowingglass$colour() : GlowingGlassClient.GLASS.at(pos.getX(), pos.getY(), pos.getZ());
        return c == null ? null : glass.apply(c);
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        BlockStateModel g = at(level, pos);
        if (g != null) g.emitQuads(emitter, level, pos, state, random, cullTest);
        else super.emitQuads(emitter, level, pos, state, random, cullTest);
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return at(level, pos) != null ? null : super.createGeometryKey(level, pos, state, random);
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        BlockStateModel g = at(level, pos);
        return g != null ? g.particleMaterial(level, pos, state) : super.particleMaterial(level, pos, state);
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        BlockStateModel g = at(level, pos);
        return g != null ? g.materialFlags(level, pos, state, random) : super.materialFlags(level, pos, state, random);
    }

    @Override
    public boolean hasMaterialFlag(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, int flag) {
        BlockStateModel g = at(level, pos);
        return g != null ? g.hasMaterialFlag(level, pos, state, random, flag) : super.hasMaterialFlag(level, pos, state, random, flag);
    }
}
