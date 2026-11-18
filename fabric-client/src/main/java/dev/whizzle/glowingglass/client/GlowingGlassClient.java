package dev.whizzle.glowingglass.client;

import dev.whizzle.glowingglass.client.net.Net;
import dev.whizzle.glowingglass.client.render.GlassModels;
import dev.whizzle.glowingglass.model.Glass;
import dev.whizzle.glowingglass.model.Protocol;
import dev.whizzle.glowingglass.model.Protocol.ChunkPos;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Glowing Glass client. Talks to the plugin (hello; from then on the server sends each chunk's glowing glass right
 * after the chunk, and pushes changes; chunks the client had before the hello are asked about), draws glowing glass
 * in place of its carrier block ({@code render/}), marks the player's own placements at once ({@link Prediction}) and
 * swaps the carrier's sounds for glass ({@link Sounds}, {@code mixin/}).
 * Items need no code: the mod's {@code assets/minecraft/items/<carrier>.json} pick the glowing glass model by the
 * item's custom model data. {@code /glowingglassmod} shows what the mod knows.
 */
public final class GlowingGlassClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("glowingglass");
    public static final GlassCache GLASS = new GlassCache();
    /** From the server's welcome; 0 until it answers (or when the server doesn't have the plugin). */
    static int serverProtocol;
    /** Chunks the client has (since joining or changing level), and those still to ask about (had before the welcome). */
    private static final Set<Long> LOADED = new HashSet<>();
    private static final LinkedHashSet<Long> TO_ASK = new LinkedHashSet<>();

    @Override
    public void onInitializeClient() {
        Net.register(Map.of(
            Protocol.WELCOME, data -> {
                serverProtocol = Protocol.readWelcome(data);
                if (serverProtocol != Protocol.VERSION) LOG.warn("Server speaks Glowing Glass protocol {}, this mod {}: glowing glass won't be drawn", serverProtocol, Protocol.VERSION);
                else TO_ASK.addAll(LOADED);
            },
            Protocol.CHUNK, data -> onChunk(Protocol.readChunk(data))));
        GlassModels.register();
        CreativeTab.register();
        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> reset());
        ServerboundPlayChannelEvents.REGISTER.register((listener, sender, client, channels) -> {
            if (channels.contains(Identifier.fromNamespaceAndPath(Protocol.NAMESPACE, Protocol.HELLO))) Net.send(Protocol.HELLO, Protocol.hello());
        });
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> { GLASS.clear(); LOADED.clear(); TO_ASK.clear(); });
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            long key = new ChunkPos(chunk.getPos().x(), chunk.getPos().z()).key();
            LOADED.add(key);   // once connected, the server sends the chunk's glowing glass by itself
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            ChunkPos pos = new ChunkPos(chunk.getPos().x(), chunk.getPos().z());
            LOADED.remove(pos.key());
            TO_ASK.remove(pos.key());
            GLASS.forget(pos);
        });
        ClientTickEvents.END_CLIENT_TICK.register(GlowingGlassClient::tick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
            dispatcher.register(ClientCommands.literal("glowingglassmod").executes(c -> { status(c.getSource().getClient()); return 1; })));
    }

    static void reset() { serverProtocol = 0; GLASS.clear(); LOADED.clear(); TO_ASK.clear(); }

    public static boolean active() { return serverProtocol == Protocol.VERSION; }

    /** One query (up to 64 chunks) per tick: well inside the server's limit, and a full view distance in a few seconds. */
    static void tick(Minecraft mc) {
        if (!active() || mc.level == null || TO_ASK.isEmpty()) return;
        List<ChunkPos> batch = new ArrayList<>(Protocol.MAX_QUERY);
        for (Iterator<Long> it = TO_ASK.iterator(); it.hasNext() && batch.size() < Protocol.MAX_QUERY; ) {
            batch.add(ChunkPos.of(it.next()));
            it.remove();
        }
        Net.send(Protocol.QUERY, Protocol.query(batch));
    }

    /** The server's list for a chunk: remember it and re-mesh the sections whose blocks changed. */
    static void onChunk(Protocol.Chunk chunk) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !LOADED.contains(chunk.pos().key())) return;   // gone meanwhile
        TO_ASK.remove(chunk.pos().key());
        for (int sy : GLASS.update(chunk.pos(), chunk.glass())) level.setSectionDirtyWithNeighbors(chunk.pos().x(), sy, chunk.pos().z());
    }

    static void status(Minecraft mc) {
        if (mc.player == null) return;
        if (serverProtocol == 0) { mc.player.sendSystemMessage(Component.literal("Glowing Glass: this server doesn't run the plugin (no answer).")); return; }
        if (!active()) { mc.player.sendSystemMessage(Component.literal("Glowing Glass: server protocol " + serverProtocol + ", mod " + Protocol.VERSION + ". Update the older one.")); return; }
        mc.player.sendSystemMessage(Component.literal("Glowing Glass: connected (" + GLASS.size() + " glowing glass known nearby, " + TO_ASK.size() + " chunks still to ask about)."));
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos p = hit.getBlockPos();
        Glass c = GLASS.at(p.getX(), p.getY(), p.getZ());
        String what = c == null ? "not glowing glass"
            : mc.level != null && !Carriers.holds(c, mc.level.getBlockState(p)) ? "not glowing glass (a leftover " + c.itemName() + " mark, ignored)"
            : c.itemName();
        mc.player.sendSystemMessage(Component.literal("  Looking at: " + what));
    }
}
