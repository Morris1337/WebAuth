package lv.funcatchers.rpgmana;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManaBarManager {

    private static final Map<UUID, BossBar> manaBars = new HashMap<>();

    public static void show(Player player) {
        UUID uuid = player.getUniqueId();
        hide(player);
        BossBar bar = Bukkit.createBossBar("§9SP", BarColor.BLUE, BarStyle.SEGMENTED_10);
        bar.setProgress(1.0);
        bar.setVisible(true);
        bar.addPlayer(player);
        manaBars.put(uuid, bar);
        update(player);
    }

    public static void update(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = manaBars.get(uuid);
        if (bar == null) return;

        int max = (int) ManaManager.getMaxMana(player);
        double current = ManaManager.getCurrentMana(player);

        double progress = max == 0 ? 0.0 : (double) current / max;
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        bar.setTitle("§9SP: " + current + " / " + max);
    }

    public static void hide(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = manaBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
    }
}
