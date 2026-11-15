package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Colour;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class GlassItemTest {
    @BeforeEach void setUp() { MockBukkit.mock(); }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    @Test void createdItemsReadBack() {
        for (Colour c : Colour.values()) {
            ItemStack i = GlassItem.create(c, 3);
            assertEquals(GlassItem.carrier(c), i.getType());
            assertEquals(3, i.getAmount());
            assertEquals(c, GlassItem.colour(i));
        }
    }

    @Test void plainOrMismatchedItemsAreNotGlowingGlass() {
        assertNull(GlassItem.colour(new ItemStack(Material.SEA_LANTERN)));
        assertNull(GlassItem.colour(null));
        ItemStack wrong = new ItemStack(Material.SHROOMLIGHT);   // blue belongs on a sea lantern
        wrong.editPersistentDataContainer(p -> p.set(GlassItem.COLOUR_KEY, PersistentDataType.STRING, "blue"));
        assertNull(GlassItem.colour(wrong));
        ItemStack stone = new ItemStack(Material.STONE);
        stone.editPersistentDataContainer(p -> p.set(GlassItem.COLOUR_KEY, PersistentDataType.STRING, "blue"));
        assertNull(GlassItem.colour(stone));
    }

    @Test void recipeIsEightGlassAroundDust() {
        ShapedRecipe r = Recipes.recipe(Colour.BROWN);
        assertArrayEquals(new String[]{"GGG", "GDG", "GGG"}, r.getShape());
        assertEquals(new RecipeChoice.MaterialChoice(Material.BROWN_STAINED_GLASS), r.getChoiceMap().get('G'));
        assertEquals(new RecipeChoice.MaterialChoice(Material.GLOWSTONE_DUST), r.getChoiceMap().get('D'));
        assertEquals(8, r.getResult().getAmount());
        assertEquals(Colour.BROWN, GlassItem.colour(r.getResult()));
        assertEquals(Material.GLASS, GlassItem.glass(Colour.CLEAR));
        assertEquals(16, Recipes.keys().stream().distinct().count());
    }
}
