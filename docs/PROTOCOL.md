# Client <-> server protocol (v1)

Plugin messages on `glowingglass:*` channels. Encoding is Minecraft's `FriendlyByteBuf` style: VarInt, big-endian
Int32, String (VarInt byte length + UTF-8). Both sides use the shared `dev.whizzle.glowingglass.model` classes
(`Wire`, `Protocol`, `ChunkGlass`, `Colour`), compiled into the plugin and (slice 2) the mod.

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
- The client asks about each chunk it receives (`query`). The server answers `chunk` for each chunk it has sent that
  player (an empty list where there's no glowing glass) and stays silent for the rest. Answers go out at up to 64
  chunks per player per tick, so joining with a long view distance catches up within a second or so.
- Every answered chunk stays **watched**: when its glowing glass changes (placed, broken, moved by a piston, blown
  up, a stale record dropped) the server sends `chunk` again, once per tick at most. Watching stops, silently, when
  the server stops sending that chunk to the player, or the player changes world or leaves; the client asks again
  when the chunk comes back.
- Limits per player: 40 messages per second; at most 8192 chunks waiting for an answer. Anyone may ask: which
  blocks are glowing glass is visible to every player anyway.
- Bump `Protocol.VERSION` when any message's bytes change (one constant, shared by both sides). Release the plugin
  and the mod together then.

## Stored data (not on the wire)

- Item: the carrier's item with persistent data `glowingglass:colour` (`blue`), the `item_name` component
  ("Glowing Blue Glass") and a `custom_model_data` string `glowingglass:blue` for the mod's item definitions.
- Placed blocks: the chunk's persistent data, `glowingglass:glass`, a byte array: VarInt format (1), then ChunkGlass.
