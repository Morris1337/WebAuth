package lv.funcatchers.utils;

import lv.funcatchers.WebAuth;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomTradeLoader {

    public static List<CustomTrade> getTradesForVillager(Villager villager) {
        FileConfiguration config = WebAuth.getInstance().getTradeConfig().getRawConfig();

        List<CustomTrade> result = new ArrayList<>();

        List<Map<?, ?>> villagerConfigs = config.getMapList("villagers");

        for (Map<?, ?> entry : villagerConfigs) {
            String name = (String) entry.get("name");
            String profession = (String) entry.get("profession");

            boolean matchesName = name != null && villager.getCustomName() != null && villager.getCustomName().equals(name);
            boolean matchesProfession = profession != null && villager.getProfession().name().equalsIgnoreCase(profession);

            if (matchesName || matchesProfession) {
                List<Map<?, ?>> trades = (List<Map<?, ?>>) entry.get("trades");
                if (trades == null) continue;

                for (Map<?, ?> trade : trades) {
                    Object levelObj = trade.containsKey("level") ? trade.get("level") : 1;
                    int level = Integer.parseInt(levelObj.toString());

                    Object priceObj = trade.containsKey("price") ? trade.get("price") : 0;
                    int price = Integer.parseInt(priceObj.toString());

                    Map<?, ?> sell = (Map<?, ?>) trade.get("sell");
                    List<Map<?, ?>> buyList = (List<Map<?, ?>>) trade.get("buy");

                    ItemStack sellItem = parseItem(sell);
                    List<ItemStack> buyItems = new ArrayList<>();
                    for (Map<?, ?> itemMap : buyList) {
                        buyItems.add(parseItem(itemMap));
                    }

                    result.add(new CustomTrade(level, buyItems, sellItem, price));
                }
            }
        }

        return result;
    }

    private static ItemStack parseItem(Map<?, ?> map) {
        String itemName = ((String) map.get("item")).toUpperCase().replace("MINECRAFT:", "");
        Object amountObj = map.containsKey("amount") ? map.get("amount") : 1;
        int amount = Integer.parseInt(amountObj.toString());
        return new ItemStack(Material.getMaterial(itemName), amount);
    }
}
