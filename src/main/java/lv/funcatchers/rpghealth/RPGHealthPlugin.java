package lv.funcatchers.rpghealth;

import lv.funcatchers.WebAuth;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RPGHealthPlugin extends JavaPlugin {

    private static RPGHealthPlugin instance;
    private HealthManager healthManager;

    @Override
    public void onEnable() {
        instance = this;
        this.healthManager = new HealthManager(this);

        getCommand("hp").setExecutor(new HealthCommands(healthManager));
        Bukkit.getPluginManager().registerEvents(new HealthListener(this, healthManager), this);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                int id = WebAuth.getUserId(player);
                if (id != -1) {
                    double hp = healthManager.getHealth(player);
                    double max = healthManager.getMaxHealth(player);
//                    healthManager.saveHealthToApi(id, hp, max);
                }
            }
        }, 0L, 20L * 60);

//        startCustomMobDamageLoop();

        getLogger().info("\u2705 RPGHealth \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d!");
    }

    @Override
    public void onDisable() {
        getLogger().info("\u274c RPGHealth \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d.");
    }

    public static RPGHealthPlugin getInstance() {
        return instance;
    }

    public HealthManager getHealthManager() {
        return healthManager;
    }

    private void startCustomMobDamageLoop() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                int id = WebAuth.getUserId(player);
                if (id == -1) continue;

                player.getNearbyEntities(3.0, 3.0, 3.0).forEach(entity -> {
                    if (entity instanceof Mob mob) {
                        double dmg = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null
                                ? mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue()
                                : 3.0;

                        healthManager.damage(player, dmg);
                        healthManager.updateBossBar(player);
                        healthManager.sendHealthToSite(player);

                        System.out.println("\u2694 [AI-\u0423\u0420\u041e\u041d] " + mob.getName() + " \u2794 " + player.getName() + " \u043d\u0430 " + dmg);
                    }
                });
            }
        }, 20L, 20L);
    }
}
