package lv.funcatchers.listeners;

import lv.funcatchers.WebAuth;
import lv.funcatchers.rpghealth.FoodConfigManager;
import lv.funcatchers.rpghealth.HealthManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class FoodListener implements Listener {

    private final FoodConfigManager foodManager; // ✅ правильное имя переменной

    public FoodListener(FoodConfigManager foodManager) {
        this.foodManager = foodManager;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Action action = event.getAction();

        if (item == null || item.getType() == Material.AIR) return;
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Material type = item.getType();
        FoodConfigManager.FoodData foodData = foodManager.get(type);
        if (foodData == null) return;

        event.setCancelled(true);

        double current = HealthManager.getInstance().getHealth(player);
        double max = HealthManager.getInstance().getMaxHealth(player);
        double healed = Math.min(foodData.heal, max - current);

        if (healed > 0) {
            HealthManager.getInstance().heal(player, healed);
            player.sendMessage("🍗 " + foodData.lore);
        } else {
            player.sendMessage("§eВы полностью здоровы и эта еда ничего не даёт.");
        }

        // Эффекты
        for (PotionEffect effect : foodData.effects) {
            player.addPotionEffect(effect);
        }

        // Отнимаем предмет
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Воспроизводим звук
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);

        // 🔁 Устанавливаем сытость через 1 тик
        Bukkit.getScheduler().runTask(WebAuth.getInstance(), () -> {
            int newFood = Math.min(20, player.getFoodLevel() + foodData.food);
            player.setFoodLevel(newFood);
            player.setSaturation(foodData.saturation);
            player.updateInventory();
        });
    }

}
