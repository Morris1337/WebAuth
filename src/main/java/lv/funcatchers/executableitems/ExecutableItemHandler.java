package lv.funcatchers.executableitems;

import lv.funcatchers.WebAuth;
import lv.funcatchers.rpghealth.HealthManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ExecutableItemHandler implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("hp-add")) {
            return handleHpAdd(args);
        }

        if (label.equalsIgnoreCase("hp-temp")) {
            return handleHpTemp(args);
        }

        return false;
    }

    private boolean handleHpAdd(String[] args) {
        if (args.length != 2) return false;
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) return false;

        try {
            int amount = Integer.parseInt(args[1]);
            HealthManager.getInstance().heal(target, amount);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean handleHpTemp(String[] args) {
        if (args.length != 3) return false;
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) return false;

        try {
            int amount = Integer.parseInt(args[1]);
            int durationSeconds = Integer.parseInt(args[2]);

            int originalMax = (int) HealthManager.getInstance().getMaxHealth(target);

            int boostedMax = originalMax + amount;

            HealthManager.getInstance().setMaxHealth(target, boostedMax);
            HealthManager.getInstance().heal(target, amount);

            Bukkit.getScheduler().runTaskLater(WebAuth.getInstance(), () -> {
                HealthManager.getInstance().setMaxHealth(target, originalMax);
                int current = (int) HealthManager.getInstance().getHealth(target);

                if (current > originalMax) {
                    HealthManager.getInstance().setHealth(target, originalMax);
                }
            }, 20L * durationSeconds);

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
