package lv.funcatchers.listeners;

import com.google.gson.*;
import net.luckperms.api.*;
import net.luckperms.api.model.user.*;
import net.luckperms.api.node.Node;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.*;
import java.util.UUID;
import java.util.stream.Collectors;

//import static com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Node;

public class PermissionSyncListener implements Listener {

    private final JavaPlugin plugin;

    public PermissionSyncListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Bukkit.getLogger().info("🔍 PlayerJoinEvent отработал: " + player.getName());
        Bukkit.getLogger().info("✅ EXP >= 60, пытаемся добавить use.magic_wand");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://fc-server.zapto.org/minecraft/permissions/" + uuid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = in.lines().collect(Collectors.joining());
                in.close();

                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonArray permissions = json.getAsJsonArray("permissions");

                LuckPerms api = LuckPermsProvider.get();
                User lpUser = api.getUserManager().loadUser(uuid).join();

                for (JsonElement el : permissions) {
                    String permission = el.getAsString();
                    lpUser.data().add(Node.builder(permission).build());
                }

                api.getUserManager().saveUser(lpUser);
                Bukkit.getLogger().info("✅ Разрешения по EXP синхронизированы для " + player.getName());

            } catch (Exception e) {
                Bukkit.getLogger().warning("❌ Ошибка синхронизации прав: " + e.getMessage());
            }
        });
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://fc-server.zapto.org/minecraft/exp/" + uuid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = in.lines().collect(Collectors.joining());
                in.close();

                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                int exp = json.get("exp").getAsInt();

                if (exp >= 60) {
                    LuckPerms api = LuckPermsProvider.get();
                    User lpUser = api.getUserManager().loadUser(uuid).join();

                    lpUser.data().add(Node.builder("use.magic_wand").build());
                    api.getUserManager().saveUser(lpUser);

                    Bukkit.getLogger().info("✅ Игроку " + player.getName() + " выдано право на magic_wand (exp=" + exp + ")");
                } else {
                    Bukkit.getLogger().info("ℹ️ У игрока " + player.getName() + " недостаточно опыта: " + exp);
                }

            } catch (Exception e) {
                Bukkit.getLogger().warning("❌ Ошибка при получении exp: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() == Material.STICK) { // допустим, это волшебная палочка
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    UUID uuid = player.getUniqueId();
                    URL url = new URL("https://fc-server.zapto.org/minecraft/class/" + uuid);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String response = in.lines().collect(Collectors.joining());
                    in.close();

                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    String playerClass = json.get("class").getAsString();

                    if (!playerClass.equalsIgnoreCase("E")) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(ChatColor.RED + "❌ Ваш класс не может использовать этот предмет!");
                            event.setCancelled(true);
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

}
