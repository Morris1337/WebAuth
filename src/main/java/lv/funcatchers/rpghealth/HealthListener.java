//@EventHandler на EntityDamageEvent
//Перехватываем урон
//Применяем через HealthManager
//Смерть, регенерация, дисплей в BossBar

package lv.funcatchers.rpghealth;

import lv.funcatchers.rpghealth.RPGHealthPlugin;
import lv.funcatchers.WebAuth;
import lv.funcatchers.PlayerStatsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.attribute.Attribute;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;


public class HealthListener implements Listener {

    private final JavaPlugin plugin; // ✅ not RPGHealthPlugin
    private final HealthManager healthManager;
    private final Set<UUID> ignoreNextDamage = new HashSet<>();



    public HealthListener(JavaPlugin plugin, HealthManager healthManager) {
        this.plugin = plugin;
        this.healthManager = healthManager;
    }



    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        System.out.println("🚀 Игрок авторизован: " + player.getName());
        System.out.println("   → Вызов loadAndApply() с UUID: " + player.getUniqueId());

        if (WebAuth.getUserId(player) != -1) {
            String uuid = player.getUniqueId().toString(); // получаем строковый UUID

            HealthManager.getInstance().loadAndApply(player, uuid, false);

            // Обновим BossBar чуть позже
            Bukkit.getScheduler().runTaskLater(HealthManager.getInstance().getPlugin(), () -> {
                HealthManager.getInstance().updateBossBar(player);
            }, 20L);
        }
    }



    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        int userId = WebAuth.getUserId(player);
        if (userId != -1) {
            HealthManager.getInstance().sendHealthToSite(player);
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (event.isCancelled()) return; // ⛔ Не обрабатывать, если другой слушатель (например, dodge) уже отменил урон

        double damage = event.getFinalDamage();
        double currentHealth = healthManager.getHealth(player);

        double armor = player.getAttribute(Attribute.GENERIC_ARMOR).getValue();
        double reducedDamage = damage * (1 - (armor / (armor + 100)));
        double newHealth = Math.max(0.0, currentHealth - reducedDamage);

        System.out.println("⚔ " + player.getName() + ": " + damage + " → " + reducedDamage + " [armor=" + armor + "]");

        healthManager.setHealth(player, newHealth);
        healthManager.updateBossBar(player);
        healthManager.sendHealthToSite(player);

        if (newHealth <= 1.0) {
            event.setDamage(1000);
            player.setHealth(0.0);
        } else {
            player.setHealth(20.0);
            event.setDamage(0.1);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            healthManager.updateBossBar(player);
        }, 2L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        int userId = WebAuth.getUserId(player);
        if (userId == -1) return;

        String uuid = player.getUniqueId().toString();
        HealthManager.getInstance().loadAndApply(player, uuid, true);
    }


}