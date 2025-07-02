package lv.funcatchers.rpghealth;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HealthDataFetcher {
    private static final OkHttpClient client = new OkHttpClient();

    public static OkHttpClient getClient() {
        return client;
    }

    public static Map<String, Double> fetchHealth(String uuid) {
        Map<String, Double> result = new HashMap<>();
        String url = "http://fc-server.zapto.org/minecraft/player-stats/" + uuid;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                JSONObject obj = new JSONObject(json);

                result.put("health", obj.optDouble("health", 20.0));
                result.put("max_health", obj.optDouble("max_health", 20.0));
                result.put("bonus_health", obj.optDouble("bonus_health", 0.0)); // бонусы тоже
            } else {
                System.err.println("❌ [FETCH] Ошибка получения HP: " + response.code());
            }
        } catch (IOException e) {
            System.err.println("❌ [FETCH] Ошибка запроса HP: " + e.getMessage());
        }
        return result;
    }
}
