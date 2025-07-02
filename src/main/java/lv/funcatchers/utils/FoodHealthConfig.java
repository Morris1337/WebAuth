package lv.funcatchers.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FoodHealthConfig {

    private final Map<String, Integer> foodHealMap = new HashMap<>();

    public FoodHealthConfig(File dataFolder) {
        File file = new File(dataFolder, "FoodHealth.yml");
        if (!file.exists()) {
            // можешь сохранить дефолтный вариант отсюда, если хочешь
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getConfigurationSection("food_heal").getKeys(false)) {
            int value = config.getInt("food_heal." + key);
            foodHealMap.put(key.toUpperCase(), value);
        }
    }

    public int getHealAmount(String materialName) {
        return foodHealMap.getOrDefault(materialName.toUpperCase(), 0);
    }
}
