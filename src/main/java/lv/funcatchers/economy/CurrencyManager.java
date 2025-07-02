package economy;

import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CurrencyManager {

    private static final OkHttpClient client = new OkHttpClient();
    private static final String BASE_URL = "http://fc-server.zapto.org/minecraft";

    public static void tagAsCoin(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(Bukkit.getPluginManager().getPlugin("WebAuth"), "moneta"),
                    PersistentDataType.BYTE,
                    (byte) 1
            );
            item.setItemMeta(meta);
        }
    }


    public static void getBalance(String username, Consumer<Integer> onSuccess, Consumer<String> onError) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json"),
                "{\"username\":\"" + username + "\"}");

        Request request = new Request.Builder()
                .url(BASE_URL + "/balance")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                onError.accept("Ошибка запроса баланса: " + e.getMessage());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    onError.accept("HTTP ошибка: " + response.code());
                    return;
                }
                JSONObject obj = new JSONObject(response.body().string());
                int balance = obj.optInt("balance", 0);
                onSuccess.accept(balance);
            }
        });
    }

    public static void transfer(String from, String to, int amount, Consumer<Boolean> onComplete) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json"),
                String.format("{\"fromUsername\":\"%s\",\"toUsername\":\"%s\",\"amount\":\"%d\"}", from, to, amount));

        Request request = new Request.Builder()
                .url(BASE_URL + "/transfer")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                onComplete.accept(false);
            }

            @Override public void onResponse(Call call, Response response) {
                onComplete.accept(response.isSuccessful());
            }
        });
    }

    public static void syncBalanceToInventory(Player player) {
        String username = player.getName();
        getBalance(username, balance -> {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("WebAuth"), () -> {
                if (balance > 0) {
                    String command = "ei give " + player.getName() + " moneta " + balance;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                } else {
                    player.sendMessage("⚠️ Баланс на сайте 0 — ничего не выдано.");
                }
            });
        }, error -> player.sendMessage("❌ Ошибка синхронизации: " + error));
    }

    public static int getMonetCount(Player player) {
        NamespacedKey key = new NamespacedKey(Bukkit.getPluginManager().getPlugin("WebAuth"), "moneta");

        return Arrays.stream(player.getInventory().getContents())
                .filter(item -> {
                    if (item == null || item.getType() != Material.GOLD_NUGGET) return false;
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return false;

                    if (meta.hasDisplayName() && meta.getDisplayName().contains("Монета")) {
                        List<String> lore = meta.getLore();
                        if (lore != null && lore.stream().anyMatch(line -> line.contains("валюта FunCatchers"))) {

                            // 🛠️ Добавим PersistentData тег, если его нет
                            if (!meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                                tagAsCoin(item); // Добавляет тег "moneta: 1b"
                            }

                            return true;
                        }
                    }
                    return false;
                })
                .mapToInt(ItemStack::getAmount)
                .sum();
    }


    public static void removeMonets(Player player, int amountToRemove) {
        NamespacedKey key = new NamespacedKey(Bukkit.getPluginManager().getPlugin("WebAuth"), "moneta");

        for (ItemStack item : player.getInventory().getContents()) {
            if (amountToRemove <= 0) break;
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            if (!meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) continue;

            int amt = item.getAmount();
            if (amt <= amountToRemove) {
                amountToRemove -= amt;
                item.setAmount(0);
            } else {
                item.setAmount(amt - amountToRemove);
                amountToRemove = 0;
            }
        }
    }
}
