package dev.whizzle.glowingglass.client.render;

import dev.whizzle.glowingglass.model.Colour;

/**
 * Added to vanilla's moving-block render state (mixin {@code MovingBlockRenderStateMixin}): the colour of a block a
 * piston is sliding. The game draws a sliding block at its starting position, but the server has already moved the
 * record to the destination, so the colour is looked up there when the slide is drawn. Not in the mixin package:
 * Mixin refuses to load plain interfaces from it.
 */
public interface MovingGlass {
    Colour glowingglass$colour();

    void glowingglass$setColour(Colour colour);
}
