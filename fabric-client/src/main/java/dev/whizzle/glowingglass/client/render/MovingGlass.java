package dev.whizzle.glowingglass.client.render;

import dev.whizzle.glowingglass.model.Glass;

/**
 * Added to vanilla's moving-block render state (mixin {@code MovingBlockRenderStateMixin}): the glass of a block a
 * piston is sliding. The game draws a sliding block at its starting position, but the server has already moved the
 * record to the destination, so the glass is looked up there when the slide is drawn. Not in the mixin package:
 * Mixin refuses to load plain interfaces from it.
 */
public interface MovingGlass {
    Glass glowingglass$glass();

    void glowingglass$setGlass(Glass glass);
}
