package lv.funcatchers.rpgmana;

import lv.funcatchers.WebAuth;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lv.funcatchers.PlayerStatsManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;

import static lv.funcatchers.rpgmana.ManaSyncService.syncManaToDatabase;

public class ManaManager {

    private static final Map<UUID, Double> currentMana = new HashMap<>();
    private static final Map<UUID, Double> maxMana = new HashMap<>();
//    private static final Map<UUID, Double> manaMap = new HashMap<>();
    private static final Map<UUID, BukkitTask> regenTasks = new HashMap<>();
    private static final Map<UUID, Integer> userIdCache = new HashMap<>();


    public static int getMaxMana(Player player) {
        double manaBonus = PlayerStatsManager.getEffect(player, "mana_bonus");
        return (int) PlayerStatsManager.getEffect(player, "mana_bonus");
    }


    public static double getCurrentMana(Player player) {
        return currentMana.getOrDefault(player.getUniqueId(), 30.0);
    }


    public static void set(Player player, double value) {
        double max = getMaxMana(player);
        currentMana.put(player.getUniqueId(), Math.min(value, max));
        ManaBarManager.update(player); // Обновляет 1 строку — как и надо
    }

    public static void subtract(Player player, double amount) {
        double current = getCurrentMana(player);
        currentMana.put(player.getUniqueId(), Math.max(0, current - amount)); // ✔ ПРАВИЛЬНО!
        ManaBarManager.update(player);
    }



    public static void regen(Player player, double amount) {
        set(player, getCurrentMana(player) + amount);
        ManaSyncService.syncManaToDatabase(player);
    }

    public static void init(Player player) {
        UUID uuid = player.getUniqueId();
        double max = getMaxMana(player);
        if (max <= 0) return; // ❌ Не показываем бар если нет маны

        currentMana.put(uuid, max);
        ManaBarManager.show(player);

        startManaRegen(WebAuth.getInstance(), player);
        ManaSyncService.syncManaToDatabase(player);

    }


    public static void remove(Player player) {
        UUID uuid = player.getUniqueId();
        currentMana.remove(uuid);
        maxMana.remove(uuid);
        stopManaRegen(player); // ✅ Остановка регена
        ManaBarManager.hide(player);
    }

    public static boolean consumeMana(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        double current = getCurrentMana(player);
        if (current < amount) return false;

        currentMana.put(uuid, current - amount);
        ManaBarManager.update(player);
        return true;
    }

    public static void startManaRegen(JavaPlugin plugin, Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                double regen = PlayerStatsManager.getEffect(player, "mana_regen");
                if (regen > 0) {
                    ManaManager.regen(player, regen);
                }
            }
        }.runTaskTimer(plugin, 20 * 10, 20 * 10); // каждые 10 секунд

        regenTasks.put(uuid, task);
    }

    public static void stopManaRegen(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = regenTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public static int getUserId(Player player) {
        UUID uuid = player.getUniqueId();

        // ✅ Используем кэш если уже есть
        if (userIdCache.containsKey(uuid)) {
            return userIdCache.get(uuid);
        }

        try {
            URL url = new URL("http://fc-server.zapto.org/minecraft/resolve-user/" + player.getName());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine = in.readLine();
            in.close();

            if (inputLine != null && inputLine.contains("userId")) {
                JSONObject response = new JSONObject(inputLine);
                int userId = response.getInt("userId");

                // ✅ Сохраняем в кэш
                userIdCache.put(uuid, userId);
                return userId;
            }

        } catch (Exception e) {
            System.out.println("❌ Ошибка получения userId: " + e.getMessage());
        }

        return -1;
    }


}
