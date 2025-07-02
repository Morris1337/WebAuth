package lv.funcatchers;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EffectRegistry {

    private static final Map<String, Map<Integer, Double>> bonusesByStat = new HashMap<>();
    private static final OkHttpClient client = new OkHttpClient();

    public static void loadFromServer(int userId) {
        bonusesByStat.clear(); // очищаем всё, чтобы не дублировалось

        Request request = new Request.Builder()
                .url("http://fc-server.zapto.org/minecraft/effects/" + userId)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                JSONObject obj = new JSONObject(json);
                JSONObject stats = obj.getJSONObject("stats");

                for (String effectType : stats.keySet()) {
                    double value = stats.getDouble(effectType);

                    // если уже есть значение — суммируем
                    Map<Integer, Double> levelMap = bonusesByStat.getOrDefault(effectType, new HashMap<>());
                    double old = levelMap.getOrDefault(1, 0.0);
                    levelMap.put(1, old + value);
                    bonusesByStat.put(effectType, levelMap);
                }


                System.out.println("📦 Загружено бонусов из full-stats: " + bonusesByStat);
            } else {
                System.out.println("❌ Не удалось загрузить эффекты с сервера: " + response.code());
            }
        } catch (IOException e) {
            System.err.println("❌ Ошибка запроса бонусов: " + e.getMessage());
        }
    }

    public static double getBonus(String effectType) {
        return bonusesByStat.getOrDefault(effectType, Map.of())
                .getOrDefault(1, 0.0);
    }
}
