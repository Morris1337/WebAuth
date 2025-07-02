package lv.funcatchers.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomHungerManager {
    private static final Map<UUID, Integer> hunger = new HashMap<>();
    private static final int MAX_HUNGER = 100;

    public static int get(Player player) {
        return hunger.getOrDefault(player.getUniqueId(), MAX_HUNGER);
    }

    public static void set(Player player, int amount) {
        hunger.put(player.getUniqueId(), Math.max(0, Math.min(MAX_HUNGER, amount)));
        updateBossBar(player);
    }

    public static void add(Player player, int amount) {
        set(player, get(player) + amount);
    }

//    public static void consume(Player player, int amount) {
//        set(player, get(player) - amount);
//    }

    public static void consume(Player player, int amount) {
        int newHunger = Math.max(1, get(player) - amount); // 1 — это минимум, можешь изменить
        set(player, newHunger);
    }
    public static void updateBossBar(Player player) {
        int current = get(player);
        BossBar bar = Bukkit.createBossBar("🥩 Сытость: " + current + " / " + MAX_HUNGER, BarColor.GREEN, BarStyle.SEGMENTED_10);
        bar.setProgress(current / (double) MAX_HUNGER);
        bar.addPlayer(player);
        bar.setVisible(true);
    }

    public static void remove(Player player) {
        hunger.remove(player.getUniqueId());
    }


}


