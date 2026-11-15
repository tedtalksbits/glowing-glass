package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.ChunkGlass;
import dev.whizzle.glowingglass.model.Colour;
import dev.whizzle.glowingglass.model.Protocol;
import dev.whizzle.glowingglass.model.Protocol.ChunkPos;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** The bridge against a fake world: {@code glass} and {@code sentChunks} stand in for the records and Paper. */
class ClientBridgeTest {
    ServerMock server; WorldMock world; PlayerMock player, other; ClientBridge bridge;
    final Map<ChunkPos, ChunkGlass> glass = new HashMap<>();
    final Set<ChunkPos> sentChunks = new HashSet<>();
    final List<String[]> sent = new ArrayList<>();
    final List<byte[]> bodies = new ArrayList<>();

    @BeforeEach void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("bridge");
        player = server.addPlayer();
        other = server.addPlayer();
        player.teleport(new Location(world, 0.5, 70, 0.5));
        other.teleport(new Location(world, 0.5, 70, 0.5));
        bridge = new ClientBridge((Player p, String channel, byte[] data) -> { sent.add(new String[]{p.getName(), channel}); bodies.add(data); },
            new ClientBridge.View() {
                @Override public boolean sent(Player p, ChunkPos c) { return sentChunks.contains(c); }
                @Override public ChunkGlass glass(World w, ChunkPos c) { return glass.get(c); }
            });
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            ChunkPos c = new ChunkPos(x, z);
            glass.put(c, new ChunkGlass());
            sentChunks.add(c);
        }
        glass.get(new ChunkPos(1, 2)).put(ChunkGlass.pack(4, 70, 5), Colour.BLUE);
    }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    void query(Player p, ChunkPos... cs) { bridge.onPluginMessageReceived(ClientBridge.QUERY, p, Protocol.query(List.of(cs))); }
    Protocol.Chunk lastChunk() { assertEquals(ClientBridge.CHUNK, sent.getLast()[1]); return Protocol.readChunk(bodies.getLast()); }

    @Test void helloGetsWelcome() {
        bridge.onPluginMessageReceived(ClientBridge.HELLO, player, Protocol.hello());
        assertEquals(ClientBridge.WELCOME, sent.getLast()[1]);
        assertEquals(Protocol.VERSION, Protocol.readWelcome(bodies.getLast()));
    }

    @Test void answersOnTheNextTick() {
        query(player, new ChunkPos(1, 2), new ChunkPos(0, 0));
        assertTrue(sent.isEmpty());
        bridge.tick();
        assertEquals(2, sent.size());
        assertEquals(new Protocol.Chunk(new ChunkPos(1, 2), glass.get(new ChunkPos(1, 2))), Protocol.readChunk(bodies.get(0)));
        assertEquals(2, bridge.watched(player));
    }

    @Test void skipsChunksThePlayerDoesNotHaveOrThatArentLoaded() {
        sentChunks.remove(new ChunkPos(0, 0));
        query(player, new ChunkPos(0, 0), new ChunkPos(9, 9));
        bridge.tick();
        assertTrue(sent.isEmpty());
        assertEquals(0, bridge.watched(player));
    }

    @Test void pushesChangesToWatchersOnlyAndAtOnce() {
        query(player, new ChunkPos(1, 2));
        bridge.tick();
        sent.clear(); bodies.clear();
        glass.get(new ChunkPos(1, 2)).put(ChunkGlass.pack(0, 71, 0), Colour.RED);
        bridge.changed(new Records.ChunkId(world.getUID(), new ChunkPos(1, 2).key()));
        assertEquals(1, sent.size(), "pushed straight away, not on the next tick");
        assertEquals(player.getName(), sent.getFirst()[0]);
        assertEquals(2, lastChunk().glass().size());
        bridge.changed(new Records.ChunkId(world.getUID(), new ChunkPos(2, 2).key()));   // nobody watches it
        bridge.tick();
        assertEquals(1, sent.size());
    }

    @Test void sendsEachChunkToModdedPlayersAsTheServerSendsIt() {
        bridge.sent(player, new ChunkPos(1, 2));
        assertTrue(sent.isEmpty(), "no hello yet: not modded");
        bridge.onPluginMessageReceived(ClientBridge.HELLO, player, Protocol.hello());
        sent.clear(); bodies.clear();
        bridge.sent(player, new ChunkPos(1, 2));
        bridge.sent(player, new ChunkPos(0, 0));
        assertEquals(2, sent.size());
        assertEquals(new Protocol.Chunk(new ChunkPos(1, 2), glass.get(new ChunkPos(1, 2))), Protocol.readChunk(bodies.getFirst()));
        assertEquals(0, Protocol.readChunk(bodies.getLast()).glass().size(), "empty chunks too, so the mod needn't ask");
        assertEquals(2, bridge.watched(player));
        bridge.sent(other, new ChunkPos(1, 2));
        assertEquals(2, sent.size(), "the other player has no mod");
    }

    @Test void anOldModIsNotSentChunksUnasked() {
        bridge.onPluginMessageReceived(ClientBridge.HELLO, player, new dev.whizzle.glowingglass.model.Wire.Writer().varInt(Protocol.VERSION + 1).bytes());
        sent.clear();
        bridge.sent(player, new ChunkPos(1, 2));
        assertTrue(sent.isEmpty());
    }

    @Test void answersAtMostSoManyPerTick() {
        List<ChunkPos> all = new ArrayList<>();
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) all.add(new ChunkPos(x, z));
        for (int i = 0; i < all.size(); i += Protocol.MAX_QUERY) query(player, all.subList(i, Math.min(all.size(), i + Protocol.MAX_QUERY)).toArray(ChunkPos[]::new));
        bridge.tick();
        assertEquals(Math.min(all.size(), ClientBridge.ANSWERS_PER_TICK), sent.size());
        bridge.tick();
        assertEquals(all.size(), sent.size());
    }

    @Test void rateLimitedAndMalformedIgnored() {
        bridge.clock = () -> 7;
        for (int i = 0; i < ClientBridge.MESSAGES_PER_SECOND + 5; i++) bridge.onPluginMessageReceived(ClientBridge.HELLO, player, Protocol.hello());
        assertEquals(ClientBridge.MESSAGES_PER_SECOND, sent.size());
        bridge.onPluginMessageReceived(ClientBridge.QUERY, other, new byte[]{5});
        bridge.tick();
        assertEquals(ClientBridge.MESSAGES_PER_SECOND, sent.size());
    }

    @Test void forgetsPlayersWhoLeave() {
        query(player, new ChunkPos(1, 2));
        bridge.tick();
        player.disconnect();
        bridge.tick();
        assertEquals(0, bridge.watched(player));
    }
}
