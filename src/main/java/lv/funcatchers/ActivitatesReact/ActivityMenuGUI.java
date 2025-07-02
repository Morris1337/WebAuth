package lv.funcatchers.ActivitatesReact;

import com.google.gson.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ActivityMenuGUI implements Listener {

    private final JavaPlugin plugin;
    private final String API_URL = "https://fc-server.zapto.org/api/activities/upcoming-activities";
    private final Map<UUID, Map<Integer, Integer>> playerActivityMap = new HashMap<>();

    public ActivityMenuGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                JsonArray activities = JsonParser.parseReader(new InputStreamReader(connection.getInputStream())).getAsJsonArray();

                Inventory gui = Bukkit.createInventory(null, 54, "Выбор активности");
                Map<Integer, Integer> slotToActivityId = new HashMap<>();

                int slot = 0;
                for (JsonElement el : activities) {
                    if (slot >= 54) break;
                    JsonObject obj = el.getAsJsonObject();
                    int id = obj.get("id").getAsInt();
                    String title = obj.get("title").getAsString();
                    String startTimeRaw = obj.get("start_time").getAsString();

                    // Перевод времени в рижское (GMT+3)
                    Instant utcTime = Instant.parse(startTimeRaw);
                    ZonedDateTime rigaTime = utcTime.atZone(ZoneId.of("Europe/Riga"));
                    String formatted = rigaTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

                    // Предмет GUI
                    ItemStack item = new ItemStack(Material.BOOK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§a[#" + id + "] " + title + " ➥");
                        meta.setLore(Collections.singletonList("§7Начало: " + formatted));
                        item.setItemMeta(meta);
                    }

                    gui.setItem(slot, item);
                    slotToActivityId.put(slot, id);
                    slot++;
                }

                playerActivityMap.put(player.getUniqueId(), slotToActivityId);
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(gui));

            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage("§cОшибка загрузки активностей.");
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Выбор активности")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < 54) {
                Map<Integer, Integer> map = playerActivityMap.get(player.getUniqueId());
                if (map != null && map.containsKey(slot)) {
                    int activityId = map.get(slot);
                    player.closeInventory();

                    // Кликабельное сообщение со ссылкой
                    String url = "https://funcatchers.lv/#/account/Activities/";
                    TextComponent message = new TextComponent("§a[Открыть активность #" + activityId + " на сайте]");
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                    player.spigot().sendMessage(message);
                }
            }
        }
    }
}
