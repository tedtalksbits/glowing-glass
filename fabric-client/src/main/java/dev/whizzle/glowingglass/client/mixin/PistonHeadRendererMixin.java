package dev.whizzle.glowingglass.client.mixin;

import dev.whizzle.glowingglass.client.GlowingGlassClient;
import dev.whizzle.glowingglass.client.render.MovingGlass;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A block sliding under a piston: tag its render state with the glowing glass colour at the slide's destination
 * (the moving block entity's position), where the server already put the record.
 */
@Mixin(PistonHeadRenderer.class)
abstract class PistonHeadRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("RETURN"))
    private void glowingglass$tagSlidingGlass(PistonMovingBlockEntity be, PistonHeadRenderState state, float partialTicks, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling, CallbackInfo ci) {
        if (state.block == null || be.isSourcePiston()) return;
        BlockPos to = be.getBlockPos();
        ((MovingGlass) state.block).glowingglass$setColour(GlowingGlassClient.GLASS.at(to.getX(), to.getY(), to.getZ()));
    }
}
