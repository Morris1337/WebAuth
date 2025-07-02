package lv.funcatchers.ActivitatesReact;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class HologramUpdater {

    private static final String API_URL = "https://fc-server.zapto.org/api/activities/upcoming-activities";
    private final JavaPlugin plugin;
    private static final String HOLOGRAM_NAME = "activities_display";
    private static final int ITEMS_PER_PAGE = 5;

    public HologramUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateHologram(Location location) {
        updateHologram(location, 1);
    }

    public void updateHologram(Location location, int page) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                JsonArray activities = JsonParser.parseReader(new InputStreamReader(connection.getInputStream())).getAsJsonArray();

                // 1. Отфильтровываем только будущие активности по времени Europe/Riga
                List<JsonObject> upcoming = new ArrayList<>();
                ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Riga"));

                for (var el : activities) {
                    JsonObject obj = el.getAsJsonObject();
                    try {
                        Instant utcTime = Instant.parse(obj.get("start_time").getAsString());
                        ZonedDateTime localTime = utcTime.atZone(ZoneId.of("Europe/Riga"));
                        if (localTime.isAfter(now)) {
                            obj.addProperty("local_time", localTime.toString());
                            upcoming.add(obj);
                        }
                    } catch (Exception ignore) {}
                }

                // 2. Пагинация и строки голограммы
                List<String> lines = new ArrayList<>();
                lines.add("&b&lБлижайшие активности:");

                int from = (page - 1) * ITEMS_PER_PAGE;
                int to = Math.min(from + ITEMS_PER_PAGE, upcoming.size());

                for (int i = from; i < to; i++) {
                    JsonObject obj = upcoming.get(i);
                    String title = obj.get("title").getAsString();
                    int id = obj.get("id").getAsInt();
                    ZonedDateTime startTime = ZonedDateTime.parse(obj.get("local_time").getAsString());
                    String date = startTime.format(DateTimeFormatter.ofPattern("dd.MM"));
                    String time = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));

                    lines.add("&e" + title + " &7" + date + " в " + time);
                    lines.add("/joinactivity " + id + "Чтобы записаться");
                }

                if (page > 1)
                    lines.add("-! &6< Предыдущая страница !command:/updateactivities " + (page - 1) + " !hover:Предыдущая страница");
                if (to < upcoming.size())
                    lines.add("-! &6Следующая страница > !command:/updateactivities " + (page + 1) + " !hover:Следующая страница");

                // 3. Обновляем голограмму в основном потоке
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi hologram new " + HOLOGRAM_NAME + " " + location.getWorld().getName() + " " + location.getX() + " " + (location.getY() + 1.5) + " " + location.getZ());

// Удалим старые строки
                    for (int i = 1; i <= 20; i++) { // допустим, максимум 20 строк
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi hologram deleteline " + HOLOGRAM_NAME + " " + i);
                    }


                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        String loc = String.format("%f %f %f", location.getX(), location.getY() + 1.5, location.getZ());
                        String world = location.getWorld().getName();

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                String.format("cmi hologram new %s %s %s", HOLOGRAM_NAME, world, loc));

                        for (String line : lines) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                    String.format("cmi hologram addline %s %s", HOLOGRAM_NAME, line));
                        }
                    }, 30L);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    public void startAutoUpdate(Location location) {
        // Обновлять каждые 5 минут (6000 тиков)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            updateHologram(location, 1); // Обновляем первую страницу
        }, 20L, 6000L); // 20L задержка старта, 6000L интервал (5 минут)
    }

}
