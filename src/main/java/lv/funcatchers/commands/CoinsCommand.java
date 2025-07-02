package commands;

import economy.CurrencyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.*;

import java.util.List;

public class CoinsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("⛔ Только игрок может использовать эту команду.");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage("⚙️ Используй: /coins get <n> или /coins send <n>");
            return true;
        }

        if (args.length == 0) {
            showBalanceScoreboard(player);
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("❌ Введите число.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage("❌ Укажите положительное число.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "get" -> handleGet(player, amount);
            case "send" -> handleSend(player, amount);
            default -> player.sendMessage("⚙️ Используй: /coins get <n> или /coins send <n>");
        }

        return true;
    }

    public static ItemStack createCoin(int amount) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Монета");
        meta.setLore(List.of(
                ChatColor.GRAY + "Официальная валюта FunCatchers",
                ChatColor.WHITE + "Можно использовать для торговли и обмена"
        ));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(Bukkit.getPluginManager().getPlugin("WebAuth"), "moneta"),
                PersistentDataType.BYTE,
                (byte) 1
        );
        item.setItemMeta(meta);
        return item;
    }


    private void handleGet(Player player, int amount) {
        CurrencyManager.getBalance(player.getName(), siteBalance -> {
            if (siteBalance < amount) {
                player.sendMessage("❌ Недостаточно средств на сайте. Баланс: " + siteBalance);
                return;
            }

            // 🟢 ПРАВИЛЬНО: БАНК отдаёт игроку
            CurrencyManager.transfer(player.getName(), "BANK", amount, success -> {
                if (!success) {
                    player.sendMessage("❌ Не удалось перевести валюту с сайта.");
                    return;
                }

                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("WebAuth"), () -> {
                    int fullStacks = amount / 64;
                    int remainder = amount % 64;

                    for (int i = 0; i < fullStacks; i++) {
                        player.getInventory().addItem(CoinsCommand.createCoin(64));
                    }
                    if (remainder > 0) {
                        player.getInventory().addItem(CoinsCommand.createCoin(remainder));
                    }

                    player.sendMessage("💰 Получено " + amount + " монет с сайта.");
                });
            });
        }, error -> player.sendMessage("❌ Ошибка получения баланса: " + error));
    }

    private void handleSend(Player player, int amount) {
        int count = CurrencyManager.getMonetCount(player);
        if (count < amount) {
            player.sendMessage("❌ У тебя нет столько монет. В инвентаре: " + count);
            return;
        }

        // 🟢 ИГРОК отдаёт БАНКУ
        CurrencyManager.transfer("BANK", player.getName(), amount, success -> {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("WebAuth"), () -> {
                if (!success) {
                    player.sendMessage("❌ Не удалось зачислить валюту на сайт.");
                    return;
                }

                // ❗ Удаляем только при успехе
                CurrencyManager.removeMonets(player, amount);
                player.sendMessage("🏦 Отправлено " + amount + " монет на сайт.");
            });
        });
    }


    private void showBalanceScoreboard(Player player) {
        CurrencyManager.getBalance(player.getName(), balance -> {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("WebAuth"), () -> {
                ScoreboardManager manager = Bukkit.getScoreboardManager();
                Scoreboard board = manager.getNewScoreboard();

                Objective obj = board.registerNewObjective("coinView", "dummy", "💰 Баланс (банк)");
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);

                Score score = obj.getScore("Монет: " + balance);
                score.setScore(1);

                player.setScoreboard(board);

                // Очистим после 30 сек (если не заменили)
                Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("WebAuth"), () -> {
                    if (player.isOnline() && player.getScoreboard().getObjective("coinView") != null) {
                        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                    }
                }, 20L * 30);
            });
        }, error -> player.sendMessage("❌ Ошибка получения баланса: " + error));
    }

}
