package lv.funcatchers.rpgmana;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ManaItemRegistry {

    private static final Map<String, Integer> manaCosts = new HashMap<>();

    public static void load(File dataFolder) {
        manaCosts.clear();
        File file = new File(dataFolder, "mana_items.yml");

        if (!file.exists()) {
            System.out.println("⚠ mana_items.yml не найден!");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            int cost = config.getInt(key + ".cost", 0);
            manaCosts.put(key.toLowerCase(), cost);
        }

        System.out.println("✅ Загружено " + manaCosts.size() + " магических предметов с манакостом.");
    }

    public static int getManaCost(String itemId) {
        return manaCosts.getOrDefault(itemId.toLowerCase(), 0);
    }

    public static boolean isMagicItem(String itemId) {
        return manaCosts.containsKey(itemId.toLowerCase());
    }
}
