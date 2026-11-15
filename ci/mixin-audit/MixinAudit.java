// Dev-only (ci/mixin-audit/run.sh): never part of the mod.
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Dev-only: loads the mod's classes, every mixin target and their Minecraft and Fabric API parents (applying every
 * mixin and interface injection on the way; a mixin that can't apply fails here) before the game starts, then exits.
 */
public class MixinAudit implements PreLaunchEntrypoint {
    @Override public void onPreLaunch() {
        int code = 0;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String[] targets = {"net.minecraft.world.item.BlockItem", "net.minecraft.client.renderer.LevelEventHandler",
            "net.minecraft.client.multiplayer.ClientLevel", "net.minecraft.world.entity.Entity",
            "net.minecraft.world.entity.LivingEntity", "net.minecraft.world.entity.animal.equine.AbstractHorse",
            "net.minecraft.client.renderer.blockentity.PistonHeadRenderer", "net.minecraft.client.multiplayer.ClientPacketListener", "net.minecraft.client.renderer.block.MovingBlockRenderState",
            "dev.whizzle.glowingglass.client.Sounds", "dev.whizzle.glowingglass.client.Prediction",
            "net.minecraft.client.renderer.block.dispatch.BlockStateModel",
            "net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel",
            "dev.whizzle.glowingglass.client.GlowingGlassClient", "dev.whizzle.glowingglass.client.GlassCache",
            "dev.whizzle.glowingglass.client.render.GlassModels", "dev.whizzle.glowingglass.client.render.GlassBlockModel",
            "dev.whizzle.glowingglass.client.net.Net", "dev.whizzle.glowingglass.client.net.RawPayload"};
        for (String t : targets) {
            try { Class.forName(t, false, cl); System.out.println("AUDIT loaded " + t); }   // loading applies the mixins; initialising would need the game's registries
            catch (Throwable e) { code = 1; System.out.println("AUDIT FAILED " + t + ": " + e); e.printStackTrace(System.out); }
        }
        try {
            Class<?> model = Class.forName("net.minecraft.client.renderer.block.dispatch.BlockStateModel", false, cl);
            Class<?> fabric = Class.forName("net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel", false, cl);
            System.out.println("AUDIT BlockStateModel has Fabric's emitQuads: " + fabric.isAssignableFrom(model));
            if (!fabric.isAssignableFrom(model)) code = 1;
            Class<?> ours = Class.forName("dev.whizzle.glowingglass.client.render.GlassBlockModel", false, cl);
            for (String m : new String[]{"emitQuads", "particleMaterial", "createGeometryKey", "materialFlags", "hasMaterialFlag"}) {
                boolean found = java.util.Arrays.stream(ours.getDeclaredMethods()).anyMatch(x -> x.getName().equals(m));
                System.out.println("AUDIT GlassBlockModel overrides " + m + ": " + found);
                if (!found) code = 1;
            }
            Class<?> moving = Class.forName("net.minecraft.client.renderer.block.MovingBlockRenderState", false, cl);
            Class<?> tag = Class.forName("dev.whizzle.glowingglass.client.render.MovingGlass", false, cl);
            System.out.println("AUDIT moving block render state carries a colour: " + tag.isAssignableFrom(moving));
            if (!tag.isAssignableFrom(moving)) code = 1;
        } catch (Throwable e) { code = 1; e.printStackTrace(System.out); }
        try { MixinEnvironment.getCurrentEnvironment().audit(); System.out.println("AUDIT mixin audit done"); }
        catch (Throwable e) { code = 1; System.out.println("AUDIT mixin audit FAILED: " + e); e.printStackTrace(System.out); }
        System.out.println("AUDIT RESULT " + (code == 0 ? "OK" : "FAILED"));
        Runtime.getRuntime().halt(code);
    }
}
