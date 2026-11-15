package dev.whizzle.glowingglass.client.mixin;

import dev.whizzle.glowingglass.client.render.MovingGlass;
import dev.whizzle.glowingglass.model.Colour;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Lets a sliding block's render state carry its glowing glass colour ({@link MovingGlass}). */
@Mixin(MovingBlockRenderState.class)
abstract class MovingBlockRenderStateMixin implements MovingGlass {
    @Unique private Colour glowingglass$colour;

    @Override public Colour glowingglass$colour() { return glowingglass$colour; }

    @Override public void glowingglass$setColour(Colour colour) { glowingglass$colour = colour; }
}
