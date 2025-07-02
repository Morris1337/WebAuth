package lv.funcatchers.rpghealth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealthCommands implements CommandExecutor {

    private final HealthManager manager;

    public HealthCommands(HealthManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (args.length < 2) return false;

        String action = args[0];
        double value = Double.parseDouble(args[1]);

        switch (action.toLowerCase()) {
            case "set":
                manager.setHealth(player, value);
                player.sendMessage("✔ Установлено здоровье: " + value);
                break;
            case "heal":
                manager.heal(player, value);
//                player.sendMessage("✔ Вы вылечились на: " + value);
                break;
            case "damage":
                manager.damage(player, value);
//                player.sendMessage("✔ Вы получили урон: " + value);
                break;
            default:
                player.sendMessage("❌ Неизвестная команда.");
        }

        if (cmd.getName().equalsIgnoreCase("mc-refresh-health")) {
//            Player player = (Player) sender;
            String uuid = player.getUniqueId().toString();
            player.sendMessage("🔄 Обновляем здоровье и бонусы...");
            HealthManager.getInstance().loadAndApply(player, uuid, false);
            return true;
        }

        return true;
    }
}
