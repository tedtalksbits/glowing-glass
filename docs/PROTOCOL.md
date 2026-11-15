# Client <-> server protocol (v1)

Plugin messages on `glowingglass:*` channels. Encoding is Minecraft's `FriendlyByteBuf` style: VarInt, big-endian
Int32, String (VarInt byte length + UTF-8). Both sides use the shared `dev.whizzle.glowingglass.model` classes
(`Wire`, `Protocol`, `ChunkGlass`, `Colour`), compiled into the plugin and the mod.

| Channel | Direction | Body |
|---|---|---|
| `glowingglass:hello` | C->S | VarInt protocol |
| `glowingglass:welcome` | S->C | VarInt protocol |
| `glowingglass:query` | C->S | VarInt n (1-64), n x (Int32 chunk x, Int32 chunk z) |
| `glowingglass:chunk` | S->C | Int32 chunk x, Int32 chunk z, then **ChunkGlass** |

**ChunkGlass**: VarInt n, then n x (Int32 position, VarInt colour id). Position inside the chunk:
`(y << 8) | (z << 4) | x`, x and z 0-15, y the world height (negative below 0; read it back with an arithmetic
shift). Colour ids: 0 white, 1 orange, 2 magenta, 3 light_blue, 4 yellow, 5 lime, 6 pink, 7 gray, 8 light_gray,
9 cyan, 10 purple, 11 blue, 12 brown, 13 green, 14 red, 16 clear (15, black, is reserved). A client skips ids it
doesn't know. The list is the whole chunk: it replaces whatever the client knew about that chunk.

## Behaviour

- The client sends `hello` once the server announces the channels. The server answers `welcome` whatever version
  the client sent; a client whose protocol differs stays quiet and draws nothing.
- **Unasked (plugin 0.2.0):** once a client has said `hello` with the server's protocol, every chunk the server sends
  that player is followed at once by `chunk` for it (an empty list where there's no glowing glass), so the client
  knows before it draws the chunk.
- The client asks (`query`) only about chunks it already had when the welcome arrived. The server answers `chunk` for
  each chunk it has sent that player and stays silent for the rest, at up to 64 chunks per player per tick.
- Every sent chunk stays **watched**: when its glowing glass changes (placed, broken, moved by a piston, blown up, a
  stale record dropped) the server sends `chunk` again **straight away**, from inside the event, so it reaches the
  client ahead of the block change itself. A piston move or an explosion sends each chunk once, after all its
  blocks changed. Watching stops, silently, when the server stops sending that chunk to the player, or the player
  changes world or leaves.
- Limits per player: 40 messages per second; at most 8192 chunks waiting for an answer. Anyone may ask: which
  blocks are glowing glass is visible to every player anyway.
- History: v1 (plugin 0.1.0, client 0.1.0): the client asked about every chunk; changes pushed on the next tick.
  Plugin 0.2.0 and client 0.2.0 keep v1's bytes and add the unasked sends and immediate pushes; any mix of the
  versions still works.
- Bump `Protocol.VERSION` when any message's bytes change (one constant, shared by both sides). Release the plugin
  and the mod together then.

## Stored data (not on the wire)

- Item: the carrier's item with persistent data `glowingglass:colour` (`blue`), the `item_name` component
  ("Glowing Blue Glass") and a `custom_model_data` string `glowingglass:blue` for the mod's item definitions.
- Placed blocks: the chunk's persistent data, `glowingglass:glass`, a byte array: VarInt format (1), then ChunkGlass.
