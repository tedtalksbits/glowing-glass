package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Colour;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DemoTest {
    ServerMock server; WorldMock world;

    @BeforeEach void setUp() { server = MockBukkit.mock(); world = server.addSimpleWorld("demo"); }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    @Test void wallHasEveryColourOnce() {
        assertEquals(EnumSet.allOf(Colour.class), EnumSet.copyOf(Demo.WALL));
        assertEquals(Demo.WIDTH * Demo.HEIGHT, Demo.WALL.size());
    }

    @Test void footprintIsInFrontAndCentred() {
        for (BlockFace f : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
            Location feet = new Location(world, 0.5, 70, 0.5);
            List<Block> fp = Demo.footprint(Demo.origin(feet, f), f);
            assertEquals(16, new HashSet<>(fp).size());
            for (Block b : fp) {
                int ahead = (b.getX() * f.getModX()) + (b.getZ() * f.getModZ());
                assertEquals(Demo.DISTANCE, ahead, f + ": every block " + Demo.DISTANCE + " ahead");
                assertTrue(b.getY() == 70 || b.getY() == 71);
            }
        }
    }
}
