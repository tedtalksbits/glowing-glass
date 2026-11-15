package dev.whizzle.glowingglass.client.mixin;

import dev.whizzle.glowingglass.client.render.MovingGlass;
import dev.whizzle.glowingglass.model.Glass;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Lets a sliding block's render state carry its glowing glass ({@link MovingGlass}). */
@Mixin(MovingBlockRenderState.class)
abstract class MovingBlockRenderStateMixin implements MovingGlass {
    @Unique private Glass glowingglass$glass;

    @Override public Glass glowingglass$glass() { return glowingglass$glass; }

    @Override public void glowingglass$setGlass(Glass glass) { glowingglass$glass = glass; }
}
