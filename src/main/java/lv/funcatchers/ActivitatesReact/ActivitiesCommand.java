package lv.funcatchers.ActivitatesReact;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ActivitiesCommand implements CommandExecutor {

    private final ActivityMenuGUI gui;

    public ActivitiesCommand(ActivityMenuGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько игрок может использовать эту команду.");
            return true;
        }

        gui.openMenu(player);
        return true;
    }
}
