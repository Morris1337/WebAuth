package lv.funcatchers.utils;

import lv.funcatchers.WebAuth;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;

public class VillagerTradeConfig {
    private final FileConfiguration config;

    public VillagerTradeConfig() {
        File file = new File(WebAuth.getInstance().getDataFolder(), "villager_trading.yml");
        if (!file.exists()) {
            WebAuth.getInstance().saveResource("villager_trading.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isTradingEnabled() {
        return config.getBoolean("trading_enabled", true);
    }

    public boolean isDefaultTradesAllowed() {
        return config.getBoolean("default_trades_allowed", false);
    }

    public List<Map<?, ?>> getVillagerConfigs() {
        return config.getMapList("villagers");
    }

    public FileConfiguration getRawConfig() {
        return config;
    }
}
