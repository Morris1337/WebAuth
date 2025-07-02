package lv.funcatchers.ActivitatesReact;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

public class UpdateActivitiesCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final String dbUrl = "jdbc:postgresql://142.93.135.140:5432/funcatcherslv";
    private final String dbUser = "funcatcherslv";
    private final String dbPassword = "newpassword";

    public UpdateActivitiesCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭту команду может использовать только игрок.");
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                player.sendMessage("§cНеверный формат страницы. Использование: /updateactivities [страница]");
                return true;
            }
        }

        if (args.length != 1) {
            player.sendMessage("§cИспользование: /joinactivity <activity_id>");
            return true;
        }


        new HologramUpdater(plugin).updateHologram(player.getLocation(), page);
        player.sendMessage("§aГолограмма обновлена. Страница: " + page);
        return true;
    }
}
