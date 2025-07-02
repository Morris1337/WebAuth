package lv.funcatchers.utils;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CustomTrade {
    public int level;
    public List<ItemStack> buyItems;
    public ItemStack sellItem;
    public int price; // 💰 цена в валюте сайт

    public CustomTrade(int level, List<ItemStack> buyItems, ItemStack sellItem, int price) {
        this.level = level;
        this.buyItems = buyItems;
        this.sellItem = sellItem;
        this.price = price;
    }
}
