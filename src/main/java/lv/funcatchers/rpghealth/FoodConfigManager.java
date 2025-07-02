package lv.funcatchers.rpghealth;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodConfigManager {
    private final Map<Material, FoodData> foodMap = new HashMap<>();


    public FoodConfigManager(File dataFolder) {
        File file = new File(dataFolder, "FoodHealth.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("foods")) return;

        for (String key : config.getConfigurationSection("foods").getKeys(false)) {
            try {
                Material mat = Material.valueOf(key);
                int heal = config.getInt("foods." + key + ".heal", 0);
                int food = config.getInt("foods." + key + ".food", 0); // 👈 добавлено
                String lore = config.getString("foods." + key + ".lore", "");

                List<PotionEffect> effects = new ArrayList<>();
                if (config.contains("foods." + key + ".effects")) {
                    for (Map<?, ?> raw : config.getMapList("foods." + key + ".effects")) {
                        try {
                            PotionEffectType type = PotionEffectType.getByName(String.valueOf(raw.get("type")));
                            Object durationObj = raw.containsKey("duration") ? raw.get("duration") : Integer.valueOf(100);
                            Object amplifierObj = raw.containsKey("amplifier") ? raw.get("amplifier") : Integer.valueOf(0);


                            int duration = durationObj instanceof Number ? ((Number) durationObj).intValue() : 100;
                            int amplifier = amplifierObj instanceof Number ? ((Number) amplifierObj).intValue() : 0;

                            if (type != null) {
                                effects.add(new PotionEffect(type, duration, amplifier));
                            }
                        } catch (Exception ignored) {}
                    }
                }

                foodMap.put(mat, new FoodData(heal, food, lore, effects, (float) config.getDouble("foods." + key + ".saturation", 0.0)));

            } catch (IllegalArgumentException e) {
                System.out.println("❌ Неверный тип еды в YML: " + key);
            }
        }


    }

    public FoodData get(Material type) {
        return foodMap.get(type);
    }

    public static class FoodData {
        public final int heal;
        public final int food; // 👈 добавляем
        public final String lore;
        public final List<PotionEffect> effects;
        public float saturation;


        public FoodData(int heal, int food, String lore, List<PotionEffect> effects, float saturation) {
            this.heal = heal;
            this.food = food;
            this.lore = lore;
            this.effects = effects;
            this.saturation = saturation;
        }
    }
}
