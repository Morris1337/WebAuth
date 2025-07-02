package lv.funcatchers.rpgmana;

import okhttp3.*;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.IOException;

import static lv.funcatchers.WebAuth.getUserId;
import static lv.funcatchers.rpgmana.ManaManager.*;

public class ManaSyncService {

    public static void syncManaToDatabase(Player player) {
        int userId = getUserId(player);
        if (userId == -1) return;

        double current = getCurrentMana(player);
        double max = getMaxMana(player);

        JSONObject payload = new JSONObject();
        payload.put("user_id", userId);
        payload.put("mana", current);
        payload.put("max_mana", max);

        Request request = new Request.Builder()
                .url("http://fc-server.zapto.org/minecraft/update-mana")
                .post(RequestBody.create(payload.toString(), MediaType.parse("application/json")))
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("❌ Ошибка отправки маны: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    System.out.println("⚠ Не удалось обновить ману: " + response.code());
                }
            }
        });
    }
}
