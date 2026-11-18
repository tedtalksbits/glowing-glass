package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.ChunkGlass;
import dev.whizzle.glowingglass.model.Protocol;
import dev.whizzle.glowingglass.model.Protocol.ChunkPos;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.*;

/**
 * Server half of the client-mod protocol (docs/PROTOCOL.md). Once a player's mod has said hello, every chunk the
 * server sends that player is followed at once by the chunk's glowing glass, so the mod knows before it draws the
 * chunk; the mod can also ask about chunks (those it had before the hello). From then on the chunk is watched: when
 * its glowing glass changes (placed, broken, moved by a piston, blown up) the new list is pushed straight away, ahead
 * of the block changes themselves. Watching stops when the server stops sending the player that chunk, or the player
 * changes world.
 */
public final class ClientBridge implements PluginMessageListener, Listener {
    public static final List<String> INCOMING = Protocol.SERVERBOUND.stream().map(Protocol::channel).toList();
    public static final List<String> OUTGOING = Protocol.CLIENTBOUND.stream().map(Protocol::channel).toList();
    static final String HELLO = Protocol.channel(Protocol.HELLO), QUERY = Protocol.channel(Protocol.QUERY),
        WELCOME = Protocol.channel(Protocol.WELCOME), CHUNK = Protocol.channel(Protocol.CHUNK);
    static final int MESSAGES_PER_SECOND = 40;
    /** Chunks answered per player per tick; the rest wait their turn (joining with a large view distance). */
    static final int ANSWERS_PER_TICK = 64;
    /** Most chunks waiting for an answer per player (a 32-chunk view distance is 4225). */
    static final int MAX_QUEUED = 8192;

    public interface Outbox { void send(Player player, String channel, byte[] data); }

    /** The world as the bridge sees it; tests substitute it. */
    public interface View {
        /** Whether the server has sent this chunk to the player (only then may the mod ask about it). */
        boolean sent(Player player, ChunkPos chunk);
        /** The chunk's glowing glass, or null if the chunk isn't loaded. */
        ChunkGlass glass(World world, ChunkPos chunk);
        /** The same, leftovers dropped first: for sending a chunk as it is (not for pushing a change). */
        default ChunkGlass checked(World world, ChunkPos chunk) { return glass(world, chunk); }
    }

    /** What one player's mod asked about. */
    static final class Watch {
        final UUID world;
        final Set<Long> watched = new HashSet<>();
        final LinkedHashSet<Long> queued = new LinkedHashSet<>();
        /** The mod said hello with our protocol: send it chunks without being asked. */
        boolean modded;
        long second; int messages;
        Watch(UUID world) { this.world = world; }
    }

    private final Outbox outbox;
    private final View view;
    private final Map<UUID, Watch> watches = new HashMap<>();
    /** The current second, for the rate limit (tests fix it). */
    java.util.function.LongSupplier clock = () -> System.currentTimeMillis() / 1000;

    public ClientBridge(Outbox outbox, View view) {
        this.outbox = outbox;
        this.view = view;
    }

    public static View real(Records records) {
        return new View() {
            @Override public boolean sent(Player player, ChunkPos chunk) { return player.isChunkSent(chunk.key()); }
            @Override public ChunkGlass glass(World world, ChunkPos chunk) { return records.chunk(world, chunk.x(), chunk.z()); }
            @Override public ChunkGlass checked(World world, ChunkPos chunk) { return records.checked(world, chunk.x(), chunk.z()); }
        };
    }

    public int watched(Player p) { Watch w = watches.get(p.getUniqueId()); return w == null ? 0 : w.watched.size(); }
    public int queued(Player p) { Watch w = watches.get(p.getUniqueId()); return w == null ? 0 : w.queued.size(); }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        Watch w = watch(player);
        long now = clock.getAsLong();
        if (w.second != now) { w.second = now; w.messages = 0; }
        if (++w.messages > MESSAGES_PER_SECOND) return;
        try {
            if (channel.equals(HELLO)) {
                int version = Protocol.readHello(message);   // any version: the welcome tells the mod ours, and it decides
                w.watched.clear();   // a fresh start (the mod reconnected or reloaded); the rate limit carries on
                w.queued.clear();
                w.modded = version == Protocol.VERSION;
                outbox.send(player, WELCOME, Protocol.welcome());
            } else if (channel.equals(QUERY)) {
                for (ChunkPos c : Protocol.readQuery(message)) if (w.queued.size() < MAX_QUEUED) w.queued.add(c.key());
            }
        } catch (IllegalArgumentException ignored) {
            // A malformed message from a modified client: ignore it.
        }
    }

    private Watch watch(Player p) {
        Watch w = watches.get(p.getUniqueId());
        if (w == null || !w.world.equals(p.getWorld().getUID())) watches.put(p.getUniqueId(), w = new Watch(p.getWorld().getUID()));
        return w;
    }

    /** A chunk's glowing glass changed: its watchers get the new list now. */
    public void changed(Records.ChunkId id) {
        ChunkPos pos = ChunkPos.of(id.key());
        for (Map.Entry<UUID, Watch> e : watches.entrySet()) {
            Watch w = e.getValue();
            if (!w.world.equals(id.world()) || !w.watched.contains(id.key())) continue;
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null || !p.getWorld().getUID().equals(id.world())) continue;
            ChunkGlass glass = view.glass(p.getWorld(), pos);
            if (glass != null) outbox.send(p, CHUNK, Protocol.chunk(pos, glass));
        }
    }

    /** Runs every tick: answers waiting queries and forgets players who left or changed world. */
    public void tick() {
        for (Iterator<Map.Entry<UUID, Watch>> it = watches.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Watch> e = it.next();
            Player p = Bukkit.getPlayer(e.getKey());
            Watch w = e.getValue();
            if (p == null || !w.world.equals(p.getWorld().getUID())) { it.remove(); continue; }
            answer(p, w);
        }
    }

    /** The server just sent the player a chunk: a modded player gets its glowing glass right behind it. */
    @EventHandler
    public void onChunkSent(PlayerChunkLoadEvent e) { sent(e.getPlayer(), new ChunkPos(e.getChunk().getX(), e.getChunk().getZ())); }

    void sent(Player p, ChunkPos pos) {
        Watch w = watches.get(p.getUniqueId());
        if (w == null || !w.modded || !w.world.equals(p.getWorld().getUID())) return;
        ChunkGlass glass = view.checked(p.getWorld(), pos);
        if (glass == null) return;
        w.queued.remove(pos.key());
        w.watched.add(pos.key());
        outbox.send(p, CHUNK, Protocol.chunk(pos, glass));
    }

    /** Answers up to {@link #ANSWERS_PER_TICK} queued chunks; a chunk the player doesn't have is dropped silently. */
    void answer(Player p, Watch w) {
        int n = 0;
        for (Iterator<Long> it = w.queued.iterator(); it.hasNext() && n < ANSWERS_PER_TICK; ) {
            long key = it.next();
            it.remove();
            ChunkPos pos = ChunkPos.of(key);
            if (!view.sent(p, pos)) continue;
            ChunkGlass glass = view.checked(p.getWorld(), pos);
            if (glass == null) continue;
            w.watched.add(key);
            outbox.send(p, CHUNK, Protocol.chunk(pos, glass));
            n++;
        }
    }

    @EventHandler
    public void onChunkUnsent(PlayerChunkUnloadEvent e) {
        Watch w = watches.get(e.getPlayer().getUniqueId());
        if (w == null) return;
        long key = e.getChunk().getChunkKey();
        w.watched.remove(key);
        w.queued.remove(key);
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { watches.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent e) { watches.remove(e.getPlayer().getUniqueId()); }
}
