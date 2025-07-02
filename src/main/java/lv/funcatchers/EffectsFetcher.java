package lv.funcatchers;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EffectsFetcher {

    private static final OkHttpClient client = new OkHttpClient();

    public static Map<String, Double> fetchEffects(int userId) {
        Map<String, Double> effects = new HashMap<>();
        Request request = new Request.Builder()
                .url("http://fc-server.zapto.org/minecraft/player-id/" + userId) // ⚠️ userId теперь используется
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                System.out.println("📡 JSON от API: " + body);

                JSONObject json = new JSONObject(body);
                String[] keys = {"str", "agi", "vit", "dex", "int", "luk"};
                for (String key : keys) {
                    if (json.has(key)) {
                        effects.put(key, json.getDouble(key));
                    }
                }
            } else {
                System.out.println("❌ Ответ от API: " + response.code());
            }
        } catch (IOException e) {
            System.err.println("❌ Ошибка загрузки эффектов: " + e.getMessage());
        }

        return effects;
    }
}
