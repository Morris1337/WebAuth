package lv.funcatchers.rpghealth;

import lv.funcatchers.WebAuth;
import lv.funcatchers.utils.CustomHungerManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HotbarEatListener implements Listener {

    private final FoodConfigManager foodConfig;
    private final Map<UUID, Integer> readyToEatSlot = new HashMap<>();

    public HotbarEatListener(FoodConfigManager foodConfig) {
        this.foodConfig = foodConfig;
    }

    @EventHandler
    public void onHotbarKey(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        int slot = event.getNewSlot();
        ItemStack item = player.getInventory().getItem(slot);

        if (item == null || !item.getType().isEdible()) return;

        if (isCustomExecutableItem(item)) return; // ⛔ исключаем кастомную еду

        Material type = item.getType();
        FoodConfigManager.FoodData data = foodConfig.get(type);
        if (data == null || data.heal <= 0) return;

        // Если это первый раз — активируем "подготовку"
        if (!readyToEatSlot.containsKey(uuid) || readyToEatSlot.get(uuid) != slot) {
            readyToEatSlot.put(uuid, slot);
//            player.sendMessage(ChatColor.GRAY + "Вы держите " + type.name() + ". Повторите, чтобы съесть.");

            // Сбросить "готовность" через 3 секунды
            new BukkitRunnable() {
                @Override
                public void run() {
                    readyToEatSlot.remove(uuid, slot); // удаляем только если слот тот же
                }
            }.runTaskLater(WebAuth.getInstance(), 60L); // 3 сек

            return;
        }

        // Второй раз — съедаем
        readyToEatSlot.remove(uuid);

        boolean fullHp = HealthManager.getInstance().getHealth(player) >= HealthManager.getInstance().getMaxHealth(player);
        boolean hasEffects = data.effects != null && !data.effects.isEmpty();

        if (fullHp && !hasEffects) {
            player.sendMessage(ChatColor.YELLOW + "Вы полностью здоровы и эта еда ничего не даёт.");
            return;
        }

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItem(slot, null);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
        HealthManager.getInstance().heal(player, data.heal);
        CustomHungerManager.add(player, data.food); // ✅ Восстанавливаем сытость

        for (PotionEffect effect : data.effects) {
            player.addPotionEffect(effect);
        }

        player.sendMessage(ChatColor.GREEN + data.lore);
        player.updateInventory();
    }

    private boolean isCustomExecutableItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        // 📛 Название
        if (meta.hasDisplayName()) {
            String name = ChatColor.stripColor(meta.getDisplayName());
            if (name.equalsIgnoreCase("Rare Bread")) return true;
//            List<String> ignoredNames = Arrays.asList("Rare Bread", "Speed Orb", "Magic Apple");
        }

        // 📜 Лор
        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                String stripped = ChatColor.stripColor(line).toLowerCase();
                if (stripped.contains("хлеб героев") || stripped.contains("временных hp")) return true;
            }
        }

        // 🧱 Тип предмета
        // Если это обычный хлеб — не считаем кастомным
        if (item.getType() == Material.BREAD && !meta.hasLore() && !meta.hasDisplayName()) {
            return false;
        }

        // 🧠 ExecutableItems ID
        NamespacedKey eiKey = new NamespacedKey("ExecutableItems", "id");
        if (meta.getPersistentDataContainer().has(eiKey)) {
            return true;
        }

        return false;
    }

}
