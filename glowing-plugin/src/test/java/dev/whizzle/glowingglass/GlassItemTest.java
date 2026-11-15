package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Colour;
import dev.whizzle.glowingglass.model.Glass;
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
        for (Glass g : Glass.all()) {
            ItemStack i = GlassItem.create(g, 3);
            assertEquals(GlassItem.carrier(g), i.getType());
            assertEquals(3, i.getAmount());
            assertEquals(g, GlassItem.glass(i));
        }
        assertEquals(Material.REDSTONE_LAMP, GlassItem.create(Glass.of(Colour.BLUE, true), 1).getType());
    }

    @Test void plainOrMismatchedItemsAreNotGlowingGlass() {
        assertNull(GlassItem.glass(new ItemStack(Material.SEA_LANTERN)));
        assertNull(GlassItem.glass(new ItemStack(Material.REDSTONE_LAMP)));
        assertNull(GlassItem.glass(null));
        ItemStack wrong = new ItemStack(Material.SHROOMLIGHT);   // blue belongs on a sea lantern
        wrong.editPersistentDataContainer(p -> p.set(GlassItem.COLOUR_KEY, PersistentDataType.STRING, "blue"));
        assertNull(GlassItem.glass(wrong));
        ItemStack lampNoFlag = new ItemStack(Material.REDSTONE_LAMP);   // a colour but not marked redstone
        lampNoFlag.editPersistentDataContainer(p -> p.set(GlassItem.COLOUR_KEY, PersistentDataType.STRING, "blue"));
        assertNull(GlassItem.glass(lampNoFlag));
        ItemStack stone = new ItemStack(Material.STONE);
        stone.editPersistentDataContainer(p -> p.set(GlassItem.COLOUR_KEY, PersistentDataType.STRING, "blue"));
        assertNull(GlassItem.glass(stone));
    }

    @Test void recipeIsEightGlassAroundDust() {
        ShapedRecipe r = Recipes.recipe(Glass.of(Colour.BROWN, false));
        assertArrayEquals(new String[]{"GGG", "GDG", "GGG"}, r.getShape());
        assertEquals(new RecipeChoice.MaterialChoice(Material.BROWN_STAINED_GLASS), r.getChoiceMap().get('G'));
        assertEquals(new RecipeChoice.MaterialChoice(Material.GLOWSTONE_DUST), r.getChoiceMap().get('D'));
        assertEquals(8, r.getResult().getAmount());
        assertEquals(Glass.of(Colour.BROWN, false), GlassItem.glass(r.getResult()));
        assertEquals(Material.GLASS, GlassItem.stainedGlass(Colour.CLEAR));
        assertEquals(32, Recipes.keys().stream().distinct().count());
    }

    @Test void redstoneRecipeIsEightGlowingGlassAroundRedstone() {
        ShapedRecipe r = Recipes.recipe(Glass.of(Colour.CYAN, true));
        assertArrayEquals(new String[]{"GGG", "GDG", "GGG"}, r.getShape());
        assertEquals(new RecipeChoice.MaterialChoice(Material.REDSTONE), r.getChoiceMap().get('D'));
        RecipeChoice g = r.getChoiceMap().get('G');
        assertTrue(g.test(GlassItem.create(Glass.of(Colour.CYAN, false), 5)));
        assertFalse(g.test(GlassItem.create(Glass.of(Colour.BLUE, false), 1)));
        assertFalse(g.test(new ItemStack(Material.SEA_LANTERN)));
        assertEquals(Glass.of(Colour.CYAN, true), GlassItem.glass(r.getResult()));
    }
}
