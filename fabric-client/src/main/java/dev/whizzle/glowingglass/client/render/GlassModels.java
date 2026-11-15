package dev.whizzle.glowingglass.client.render;

import dev.whizzle.glowingglass.model.Colour;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads one cube model per colour ({@code glowingglass:block/glowing_<colour>_glass}) and wraps every block state
 * model of the five carrier blocks in a {@link GlassBlockModel}.
 */
public final class GlassModels {
    private GlassModels() {}

    static final Map<Colour, ExtraModelKey<BlockStateModel>> KEYS = new EnumMap<>(Colour.class);

    public static Identifier modelId(Colour c) { return Identifier.fromNamespaceAndPath("glowingglass", "block/glowing_" + c.key + "_glass"); }

    static Set<Block> carriers() {
        Set<Block> out = new HashSet<>();
        for (Colour.Carrier c : Colour.Carrier.values()) out.add(BuiltInRegistries.BLOCK.getValue(Identifier.parse(c.id)));
        return out;
    }

    /** The baked cube for a colour (null before the first resource load). */
    static BlockStateModel model(Colour c) {
        ExtraModelKey<BlockStateModel> key = KEYS.get(c);
        return key == null ? null : Minecraft.getInstance().getModelManager().getModel(key);
    }

    public static void register() {
        for (Colour c : Colour.values()) KEYS.put(c, ExtraModelKey.create(() -> "glowingglass:" + c.key));
        ModelLoadingPlugin.register(context -> {
            for (Colour c : Colour.values()) context.addModel(KEYS.get(c), SimpleUnbakedExtraModel.blockStateModel(modelId(c)));
            Set<Block> carriers = carriers();
            context.modifyBlockModelAfterBake().register((model, ctx) ->
                carriers.contains(ctx.state().getBlock()) ? new GlassBlockModel(model, GlassModels::model) : model);
        });
    }
}
