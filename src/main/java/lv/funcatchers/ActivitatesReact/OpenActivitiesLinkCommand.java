package lv.funcatchers.ActivitatesReact;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class OpenActivitiesLinkCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public OpenActivitiesLinkCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("⛔ Только игрок может использовать эту команду.");
            return true;
        }

        String url = "https://funcatchers.lv/#/account/Activities";
        TextComponent message = new TextComponent("§a[НАЖМИ, чтобы открыть страницу активностей]");
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

        player.spigot().sendMessage(message);

        return true;
    }
}
