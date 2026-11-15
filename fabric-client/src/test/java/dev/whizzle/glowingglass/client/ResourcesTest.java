package dev.whizzle.glowingglass.client;

import dev.whizzle.glowingglass.model.Colour;
import dev.whizzle.glowingglass.model.Glass;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Every colour has its lit and unlit texture and block model, and every glass a case in its carrier's item definition. */
class ResourcesTest {
    static final Path ASSETS = Path.of(System.getProperty("glowingglass.client.dir", ""), "src/main/resources/assets");

    @Test void texturesAre16x16AndOpaque() throws IOException {
        for (Colour c : Colour.values()) for (String suffix : new String[]{"", "_off"}) {
            Path f = ASSETS.resolve("glowingglass/textures/block/glowing_" + c.key + "_glass" + suffix + ".png");
            assertTrue(Files.exists(f), "missing " + f);
            BufferedImage img = ImageIO.read(f.toFile());
            assertEquals(16, img.getWidth(), f.getFileName().toString());
            assertEquals(16, img.getHeight(), f.getFileName().toString());
            for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++)
                assertEquals(0xFF, img.getRGB(x, y) >>> 24, f.getFileName() + ": pixel " + x + "," + y + " isn't opaque (the carrier would show through)");
        }
    }

    @Test void blockModels() throws IOException {
        for (Colour c : Colour.values()) for (String suffix : new String[]{"", "_off"}) {
            String json = Files.readString(ASSETS.resolve("glowingglass/models/block/glowing_" + c.key + "_glass" + suffix + ".json"));
            assertTrue(json.contains("\"glowingglass:block/glowing_" + c.key + "_glass" + suffix + "\""), c.key + suffix);
            assertTrue(json.contains("\"minecraft:block/cube_all\""), c.key);
        }
    }

    /** Always-on glass shows its lit cube; redstone glass its unlit one, like a vanilla redstone lamp item. */
    @Test void itemDefinitionsSelectEveryGlassOnItsCarrier() throws IOException {
        java.util.Set<String> carriers = new java.util.TreeSet<>();
        for (Glass g : Glass.all()) carriers.add(g.carrierId().replace("minecraft:", ""));
        assertEquals(6, carriers.size());
        for (String name : carriers) {
            String json = Files.readString(ASSETS.resolve("minecraft/items/" + name + ".json"));
            assertTrue(json.contains("\"minecraft:custom_model_data\""), name);
            assertTrue(json.contains("\"model\": \"minecraft:block/" + name + "\""), name + ": falls back to the vanilla model");
            for (Glass g : Glass.all()) {
                boolean has = json.contains("\"when\": \"" + g.modelString() + "\"");
                assertEquals(g.carrierId().equals("minecraft:" + name), has, name + " / " + g.key());
                String model = "\"glowingglass:block/glowing_" + g.colour().key + "_glass" + (g.redstone() ? "_off" : "") + "\"";
                if (has) assertTrue(json.contains(model), name + " / " + g.key() + " -> " + model);
            }
        }
    }
}
