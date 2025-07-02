package lv.funcatchers.ActivitatesReact;

import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import lv.funcatchers.WebAuth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JoinActivityCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public JoinActivityCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько игрок может использовать эту команду.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cИспользование: /joinactivity <activity_id>");
            return true;
        }

        String token = WebAuth.getToken(player); // реализуй это — чтобы вернуть JWT-токен, полученный при логине


        int userId = WebAuth.getUserId(player);
        if (token == null || token.isEmpty()) {
            player.sendMessage("§cВы не авторизованы. Используйте /mc-login");
            return true;
        }

        int activityId;
        try {
            activityId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cID должен быть числом.");
            return true;
        }

        OkHttpClient client = new OkHttpClient();
        String jsonBody = "{\"user_id\":" + userId + ",\"activity_id\":" + activityId + "}";
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBody);

        Request request = new Request.Builder()
                .url("http://fc-server.zapto.org/api/activities/participate")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + token) // ⬅️ КРИТИЧЕСКИЙ момент
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage("§cОшибка соединения с сервером.")
                );
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (response.isSuccessful()) {
                        player.sendMessage("§aВы успешно записались на активность #" + activityId);
                    } else {
                        player.sendMessage("§cОшибка при записи: " + response.code());
                    }
                });
            }
        });

        return true;
    }
}
