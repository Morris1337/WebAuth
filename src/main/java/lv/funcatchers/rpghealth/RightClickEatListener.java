package lv.funcatchers.rpghealth;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;

public class    RightClickEatListener implements Listener {

    private final FoodConfigManager foodConfig;

    public RightClickEatListener(FoodConfigManager foodConfig) {
        this.foodConfig = foodConfig;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        if (item == null || !item.getType().isEdible()) return;
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Material type = item.getType();
        FoodConfigManager.FoodData data = foodConfig.get(type);
        if (data == null || data.heal <= 0) return;

        // 💡 Если игрок кликает по блоку и держит предмет, который МОЖНО посадить — не мешаем
        if (action == Action.RIGHT_CLICK_BLOCK && type == Material.SWEET_BERRIES) {
            // Проверка: блок, на который кликают, имеет верхнюю грань
            if (event.getBlockFace() == BlockFace.UP) return; // разрешаем посадку
        }

        // 🍎 Если не было посадки — еда
        event.setCancelled(true);

        boolean fullHp = HealthManager.getInstance().getHealth(player) >= HealthManager.getInstance().getMaxHealth(player);
        boolean hasEffects = data.effects != null && !data.effects.isEmpty();
        if (fullHp && !hasEffects) {
            player.sendMessage(ChatColor.YELLOW + "Вы полностью здоровы и эта еда ничего не даёт.");
            return;
        }

        // Съедаем
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
        HealthManager.getInstance().heal(player, data.heal);
        for (PotionEffect effect : data.effects) {
            player.addPotionEffect(effect);
        }
        player.sendMessage(ChatColor.GREEN + data.lore);
        player.updateInventory();
    }
}
