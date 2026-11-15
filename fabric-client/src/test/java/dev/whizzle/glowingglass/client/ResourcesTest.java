package dev.whizzle.glowingglass.client;

import dev.whizzle.glowingglass.model.Colour;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Every colour has its texture, block model and a case in its carrier's item definition. */
class ResourcesTest {
    static final Path ASSETS = Path.of(System.getProperty("glowingglass.client.dir", ""), "src/main/resources/assets");

    @Test void texturesAre16x16AndOpaque() throws IOException {
        for (Colour c : Colour.values()) {
            Path f = ASSETS.resolve("glowingglass/textures/block/glowing_" + c.key + "_glass.png");
            assertTrue(Files.exists(f), "missing " + f);
            BufferedImage img = ImageIO.read(f.toFile());
            assertEquals(16, img.getWidth(), f.getFileName().toString());
            assertEquals(16, img.getHeight(), f.getFileName().toString());
            for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++)
                assertEquals(0xFF, img.getRGB(x, y) >>> 24, f.getFileName() + ": pixel " + x + "," + y + " isn't opaque (the carrier would show through)");
        }
    }

    @Test void blockModels() throws IOException {
        for (Colour c : Colour.values()) {
            String json = Files.readString(ASSETS.resolve("glowingglass/models/block/glowing_" + c.key + "_glass.json"));
            assertTrue(json.contains("\"glowingglass:block/glowing_" + c.key + "_glass\""), c.key);
            assertTrue(json.contains("\"minecraft:block/cube_all\""), c.key);
        }
    }

    @Test void itemDefinitionsSelectEveryColourOnItsCarrier() throws IOException {
        for (Colour.Carrier carrier : Colour.Carrier.values()) {
            String name = carrier.id.replace("minecraft:", "");
            String json = Files.readString(ASSETS.resolve("minecraft/items/" + name + ".json"));
            assertTrue(json.contains("\"minecraft:custom_model_data\""), name);
            assertTrue(json.contains("\"model\": \"minecraft:block/" + name + "\""), name + ": falls back to the vanilla model");
            for (Colour c : Colour.values()) {
                boolean has = json.contains("\"when\": \"" + c.modelString() + "\"");
                assertEquals(c.carrier == carrier, has, name + " / " + c.key);
                if (has) assertTrue(json.contains("\"glowingglass:block/glowing_" + c.key + "_glass\""), name + " / " + c.key);
            }
        }
    }
}
