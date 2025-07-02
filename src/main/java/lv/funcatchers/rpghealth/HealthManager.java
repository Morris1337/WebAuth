//Система хранения хп:
//realHealth, maxHealth
//Методы: setHealth, heal, damage, reset, updateBossBar

package lv.funcatchers.rpghealth;

import lv.funcatchers.WebAuth;
import lv.funcatchers.rpghealth.*;
import lv.funcatchers.EffectRegistry;

import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class HealthManager implements Listener {
    private static HealthManager instance;
    private final JavaPlugin plugin;

    public JavaPlugin getPlugin() {
        return plugin;
    }


    public HealthManager(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static HealthManager getInstance() {
        return instance;
    }

    private final Map<UUID, Double> realHealth = new HashMap<>();
    private final Map<UUID, Double> maxHealth = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public void setHealth(Player player, double amount) {
        UUID id = player.getUniqueId();
        double max = maxHealth.getOrDefault(id, 60.0);
        realHealth.put(id, Math.max(0, Math.min(amount, max)));
        updateBossBar(player);
    }

    public void damage(Player player, double amount) {
        setHealth(player, getHealth(player) - amount);
    }

    public void heal(Player player, double amount) {
        setHealth(player, getHealth(player) + amount);
    }

    public void setMaxHealth(Player player, double amount) {
        maxHealth.put(player.getUniqueId(), amount);
        updateBossBar(player);
    }

    public void applyHealth(Player player, double real, double max) {
        UUID id = player.getUniqueId();
        realHealth.put(id, real);
        maxHealth.put(id, max);
        updateBossBar(player);
    }


    public double getHealth(Player player) {
        return realHealth.getOrDefault(player.getUniqueId(), 60.0);
    }

    public double getMaxHealth(Player player) {
        return maxHealth.getOrDefault(player.getUniqueId(), 60.0);
    }

    public void updateBossBar(Player player) {
        UUID id = player.getUniqueId();
        BossBar bar = bossBars.computeIfAbsent(id, k -> {
            BossBar newBar = Bukkit.createBossBar("❤ Здоровье", BarColor.RED, BarStyle.SEGMENTED_20);
            newBar.addPlayer(player);
            newBar.setVisible(true);
            return newBar;
        });


        double health = getHealth(player);
        double max = getMaxHealth(player);
        bar.setProgress(Math.max(0, Math.min(1, health / max)));
        bar.setTitle("❤ " + (int) health + " / " + (int) max);
        bar.addPlayer(player);
        player.setHealthScale(1.0); // практически скрывает
        player.setHealthScaled(true); // активировать масштабирование
        player.setInvulnerable(false);
        player.setGlowing(false); // отключает анимации
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
    }

    public void reset(Player player) {
        UUID id = player.getUniqueId();
        realHealth.put(id, 60.0);
        maxHealth.put(id, 60.0);
        updateBossBar(player);
    }

    public void loadAndApply(Player player, String uuid, boolean fullRestore) {
        Map<String, Double> data = HealthDataFetcher.fetchHealth(uuid);

        double savedHealth = data.getOrDefault("health", 60.0);       // фактическое сохранённое HP
        double baseMax = data.getOrDefault("max_health", 60.0);       // базовый max HP без бонуса

        // 💡 Подтягиваем бонус (например, vit)
        double bonus = EffectRegistry.getBonus("vit_hp_bonus");
        double finalMax = baseMax + bonus;

        double appliedHealth = fullRestore ? finalMax : savedHealth;

        // 💾 Обновляем кэш
        realHealth.put(player.getUniqueId(), appliedHealth);
        maxHealth.put(player.getUniqueId(), finalMax);

        // ⚠ Без тиков: сразу в main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            setMaxHealth(player, finalMax);
            setHealth(player, Math.min(appliedHealth, finalMax));
            updateBossBar(player);

            System.out.println("✅ [Instant] Загружено: baseMax=" + baseMax + ", bonus=" + bonus +
                    ", finalMax=" + finalMax + ", savedHealth=" + savedHealth + ", appliedHealth=" + appliedHealth);
        });
    }




    public void sendHealthToSite(Player player) {
        int userId = WebAuth.getUserId(player);
        if (userId == -1) return;

        double real = getHealth(player);
        double finalMax = getMaxHealth(player);

        // 💡 извлекаем бонус
        double bonus = HealthDataFetcher.fetchHealth(player.getUniqueId().toString()).getOrDefault("bonus_health", 0.0);
        double baseMax = finalMax - bonus;
        System.out.println("💾 Сохраняем в БД: health=" + real + ", max=" + baseMax + ", bonus=" + bonus);


        JSONObject json = new JSONObject();
        json.put("uuid", player.getUniqueId().toString());
        json.put("health", real);
        json.put("max_health", baseMax); // 💾 только базу

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url("http://fc-server.zapto.org/minecraft/player-stats")
                .post(body)
                .build();

        HealthDataFetcher.getClient().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                System.err.println("❌ Ошибка при отправке ХП: " + e.getMessage());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println("📡 Отправлено ХП игрока " + player.getName() + ": " + real + "/" + baseMax);
                } else {
                    System.err.println("❌ Сервер вернул код " + response.code());
                }
                response.close();
            }
        });
    }
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) return;
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        if (item.getType() == Material.COOKED_BEEF) {
            event.setCancelled(true);

            if (getHealth(player) >= getMaxHealth(player)) {
                player.sendMessage(ChatColor.YELLOW + "Вы полностью здоровы!");
                return;
            }

            // Уменьшаем предмет
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1, 1);
            heal(player, 10);
            player.sendMessage(ChatColor.GREEN + "Вы съели мясо и восстановили здоровье!");
            player.updateInventory();
        }
    }
}