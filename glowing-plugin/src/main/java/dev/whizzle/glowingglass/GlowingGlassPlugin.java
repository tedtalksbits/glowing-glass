package dev.whizzle.glowingglass;

import dev.whizzle.glowingglass.model.Colour;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;

/** Glowing Glass: coloured glass that gives off light, drawn by the client mod; a carrier block for everyone else. */
public class GlowingGlassPlugin extends JavaPlugin implements Listener, TabExecutor {
    private Records records;
    private Keeper keeper;
    private ClientBridge bridge;
    private BukkitTask tick;

    @Override
    public void onEnable() {
        records = new Records(getLogger());
        keeper = new Keeper(this, records);
        bridge = new ClientBridge((p, channel, data) -> p.sendPluginMessage(this, channel, data), ClientBridge.real(records));
        records.onChange(bridge::changed);
        var pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(records, this);
        pm.registerEvents(keeper, this);
        pm.registerEvents(new Recipes(), this);
        pm.registerEvents(bridge, this);
        for (String c : ClientBridge.INCOMING) getServer().getMessenger().registerIncomingPluginChannel(this, c, bridge);
        for (String c : ClientBridge.OUTGOING) getServer().getMessenger().registerOutgoingPluginChannel(this, c);
        records.loadAll();
        Recipes.register();
        for (Player p : Bukkit.getOnlinePlayers()) Recipes.discover(p);
        PluginCommand command = getCommand("glowingglass");
        if (command == null) throw new IllegalStateException("glowingglass command missing from plugin.yml");
        command.setExecutor(this);
        command.setTabCompleter(this);
        tick = getServer().getScheduler().runTaskTimer(this, () -> { keeper.tick(); bridge.tick(); }, 1, 1);
    }

    @Override
    public void onDisable() {
        if (tick != null) tick.cancel();
        if (records != null) records.saveAll();
        Recipes.unregister();
    }

    public Records records() { return records; }
    public ClientBridge bridge() { return bridge; }

    @EventHandler public void onJoin(PlayerJoinEvent e) { Recipes.discover(e.getPlayer()); }

    @EventHandler public void onReloaded(ServerResourcesReloadedEvent e) {
        if (Bukkit.getRecipe(Recipes.key(Colour.CLEAR)) != null) return;
        Recipes.register();
        for (Player p : Bukkit.getOnlinePlayers()) Recipes.discover(p);
    }

    // ---- /glowingglass ----

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "info" -> info(p);
            case "demo" -> demo(p, args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "");
            default -> {
                p.sendMessage(Component.text("Glowing Glass", NamedTextColor.AQUA));
                p.sendMessage(Component.text("  Craft 8 glass of one colour around 1 glowstone dust: 8 glowing glass.", NamedTextColor.GRAY));
                p.sendMessage(Component.text("  Without the Glowing Glass mod it looks like a sea lantern, froglight or shroomlight.", NamedTextColor.GRAY));
                p.sendMessage(Component.text("  /" + label + " info: what the block you look at, or the item you hold, is.", NamedTextColor.GRAY));
                p.sendMessage(Component.text("  /" + label + " demo [kit]: all 16 in front of you, and a kit (creative only).", NamedTextColor.GRAY));
            }
        }
        return true;
    }

    private void info(Player p) {
        Colour held = GlassItem.colour(p.getInventory().getItemInMainHand());
        if (held != null) {
            p.sendMessage(Component.text("You hold " + held.itemName() + " (players without the mod see " + carrierName(held) + ").", NamedTextColor.AQUA));
            return;
        }
        Block b = p.getTargetBlockExact(8);
        Colour c = b == null ? null : records.at(b);
        if (c == null) { p.sendMessage(Component.text("Look at glowing glass (within 8 blocks) or hold some.", NamedTextColor.RED)); return; }
        p.sendMessage(Component.text("That is " + c.itemName() + " (players without the mod see " + carrierName(c) + ").", NamedTextColor.AQUA));
    }

    static String carrierName(Colour c) { return c.carrier.id.replace("minecraft:", "").replace('_', ' '); }

    /** {@code demo}: the wall and the kit; {@code demo kit}: the kit only. Creative only (free items). */
    private void demo(Player p, String what) {
        if (p.getGameMode() != GameMode.CREATIVE) {
            p.sendMessage(Component.text("Demos only work in creative mode.", NamedTextColor.RED));
            return;
        }
        if (!what.equals("kit")) {
            String problem = Demo.build(p.getLocation(), p.getFacing(), records);
            if (problem != null) { p.sendMessage(Component.text(problem, NamedTextColor.RED)); return; }
            p.sendMessage(Component.text("All 16 glowing glass in front of you.", NamedTextColor.GREEN));
        }
        Demo.give(p);
        p.sendMessage(Component.text("Kit: 16 of each, and blue glass and glowstone dust to try the recipe.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("info", "demo").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("demo")) return "kit".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("kit") : List.of();
        return List.of();
    }
}
