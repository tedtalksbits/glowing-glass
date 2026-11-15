package dev.whizzle.glowingglass.client.render;

import dev.whizzle.glowingglass.model.Colour;
import dev.whizzle.glowingglass.model.Glass;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads two cube models per colour, lit ({@code glowingglass:block/glowing_<colour>_glass}) and unlit
 * ({@code ..._glass_off}, for redstone glowing glass without power), and wraps every block state model of the six
 * carrier blocks (five light blocks and the redstone lamp) in a {@link GlassBlockModel}.
 */
public final class GlassModels {
    private GlassModels() {}

    static final Map<Colour, ExtraModelKey<BlockStateModel>> LIT = new EnumMap<>(Colour.class), UNLIT = new EnumMap<>(Colour.class);

    public static Identifier modelId(Colour c, boolean lit) {
        return Identifier.fromNamespaceAndPath("glowingglass", "block/glowing_" + c.key + "_glass" + (lit ? "" : "_off"));
    }

    static Set<Block> carriers() {
        Set<Block> out = new HashSet<>();
        for (Glass g : Glass.all()) out.add(BuiltInRegistries.BLOCK.getValue(Identifier.parse(g.carrierId())));
        return out;
    }

    /**
     * The baked cube to draw for {@code glass} on a block in {@code state} (null before the first resource load).
     * Redstone glass follows its lamp: the lit cube while the lamp is lit, the unlit one otherwise.
     */
    static BlockStateModel model(Glass glass, BlockState state) {
        boolean lit = !glass.redstone() || (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT));
        ExtraModelKey<BlockStateModel> key = (lit ? LIT : UNLIT).get(glass.colour());
        return key == null ? null : Minecraft.getInstance().getModelManager().getModel(key);
    }

    public static void register() {
        for (Colour c : Colour.values()) {
            LIT.put(c, ExtraModelKey.create(() -> "glowingglass:" + c.key));
            UNLIT.put(c, ExtraModelKey.create(() -> "glowingglass:" + c.key + "_off"));
        }
        ModelLoadingPlugin.register(context -> {
            for (Colour c : Colour.values()) {
                context.addModel(LIT.get(c), SimpleUnbakedExtraModel.blockStateModel(modelId(c, true)));
                context.addModel(UNLIT.get(c), SimpleUnbakedExtraModel.blockStateModel(modelId(c, false)));
            }
            Set<Block> carriers = carriers();
            context.modifyBlockModelAfterBake().register((model, ctx) ->
                carriers.contains(ctx.state().getBlock()) ? new GlassBlockModel(model, GlassModels::model) : model);
        });
    }
}
