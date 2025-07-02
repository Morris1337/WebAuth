package lv.funcatchers.listeners;

import economy.CurrencyManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.List;

public class VillagerTradeListener implements Listener {

    @EventHandler
    public void onTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof MerchantInventory merchantInventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        MerchantRecipe recipe = merchantInventory.getSelectedRecipe();
        if (recipe == null) return;

        List<ItemStack> ingredients = recipe.getIngredients();
        boolean requiresCoin = ingredients.stream().anyMatch(item ->
                item != null && item.getType() == Material.GOLD_NUGGET &&
                        item.getItemMeta() != null &&
                        item.getItemMeta().getDisplayName().contains("Монета")
        );

        if (!requiresCoin) return;

        int requiredAmount = ingredients.stream()
                .filter(item -> item != null && item.getType() == Material.GOLD_NUGGET)
                .mapToInt(ItemStack::getAmount)
                .sum();

        int coins = CurrencyManager.getMonetCount(player);
        if (coins < requiredAmount) {
            player.sendMessage("❌ У тебя недостаточно монет для этой сделки.");
            event.setCancelled(true);
            return;
        }

        // Удаляем монеты после успешной сделки
        CurrencyManager.removeMonets(player, requiredAmount);
        player.sendMessage("💰 Списано " + requiredAmount + " монет за торговлю.");
    }
}
