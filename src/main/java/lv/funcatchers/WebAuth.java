package lv.funcatchers;
import lv.funcatchers.ActivitatesReact.*;
import lv.funcatchers.ActivitatesReact.ActivityMenuGUI;
import lv.funcatchers.executableitems.ExecutableItemHandler;
import lv.funcatchers.listeners.FoodListener;
import lv.funcatchers.listeners.VillagerTradeListener;
import lv.funcatchers.rpghealth.*;
import lv.funcatchers.rpgmana.ManaItemRegistry;
import lv.funcatchers.rpgmana.ManaManager;
import lv.funcatchers.utils.FoodHealthConfig;
import lv.funcatchers.utils.VillagerTradeConfig;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;
import org.json.JSONObject;

import lv.funcatchers.PlayerStatsManager;
import lv.funcatchers.rpghealth.HealthManager.*;

import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;

import org.json.JSONArray; // Это важно!


import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;




public class WebAuth extends JavaPlugin implements Listener {

    private static WebAuth instance;
    private VillagerTradeConfig tradeConfig;
    private final OkHttpClient client = new OkHttpClient();
    //private final Set<String> unauthenticatedPlayers = new HashSet<>();
    private static final Set<String> unauthenticatedPlayers = new HashSet<>();
    private static final Map<String, Integer> authenticatedIds = new HashMap<>();
    private PlayerStatsManager statsManager;

    public static int getUserId(Player player) {
        return authenticatedIds.getOrDefault(player.getName(), -1);
    }
    public static boolean isUnauthenticated(Player player) {
        return unauthenticatedPlayers.contains(player.getName());
    }
    public static void setUserId(Player player, int id) {
        authenticatedIds.put(player.getName(), id);
    }
    public PlayerStatsManager getStatsManager() {
        return this.statsManager;
    }
    public VillagerTradeConfig getTradeConfig() {
        return tradeConfig;
    }


    @Override
    public void onEnable() {
        this.statsManager = new PlayerStatsManager(this); // ✅ сохраняем в поле

        instance = this;

        // обязательно загрузи файл трейдов
        tradeConfig = new VillagerTradeConfig();

        getCommand("mc-upgrade").setExecutor(statsManager);
        getCommand("mc-stats").setExecutor(statsManager);
        getCommand("mc-reloadstats").setExecutor(statsManager); // 🔁 делегируем в PlayerStatsManager
        getCommand("coins").setExecutor(new commands.CoinsCommand());
        getCommand("updateactivities").setExecutor(new UpdateActivitiesCommand(this));
        getCommand("joinactivity").setExecutor(new OpenActivitiesLinkCommand(this));
        ActivityMenuGUI gui = new ActivityMenuGUI(this);
        getCommand("activities").setExecutor(new ActivitiesCommand(gui));


        getServer().getPluginManager().registerEvents(new VillagerTradeListener(), this);
//        tradeConfig = new VillagerTradeConfig();
        ManaItemRegistry.load(getDataFolder());

        FoodConfigManager foodConfig = new FoodConfigManager(getDataFolder());
        Bukkit.getPluginManager().registerEvents(new HotbarEatListener(foodConfig), this);
        Bukkit.getPluginManager().registerEvents(new RightClickEatListener(foodConfig), this);
        new ActivityMenuGUI(this);


        HealthManager healthManager = new HealthManager(this); // this = WebAuth
        getServer().getPluginManager().registerEvents(new HealthListener(this, healthManager), this);


        Bukkit.getPluginManager().registerEvents(statsManager, this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("✅ WebAuth plugin enabled");
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!unauthenticatedPlayers.contains(player.getName())) {
                    sendPlayerStatsToSite(player);
                }
            }
        }, 0L, 200L); // 200 тиков = 10 сек унд
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                int userId = getUserId(player);
                String uuid = player.getUniqueId().toString();
//                HealthManager.getInstance().loadAndApply(player, uuid, false);

            }
        }, 40L);
        Bukkit.getPluginManager().registerEvents(new lv.funcatchers.debug.MobDebugListener(), this);
        getServer().getPluginManager().registerEvents(new lv.funcatchers.listeners.PermissionSyncListener(this), this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            new HologramUpdater(this).updateHologram(new Location(Bukkit.getWorld("world"), 21, 112, -9));
        }, 0L, 20L * 300); // каждые 5 минут

        Location hologramLocation = new Location(
                Bukkit.getWorld("world"), // замените на нужный мир
                100.5, 65, 200.5           // координаты вашей голограммы
        );

        HologramUpdater updater = new HologramUpdater(this);
        updater.startAutoUpdate(hologramLocation); // ⏱️ запуск автообновлени

        ExecutableItemHandler eiHandler = new ExecutableItemHandler();
        getCommand("hp-add").setExecutor(eiHandler);
        getCommand("hp-temp").setExecutor(eiHandler);

    }
    public static WebAuth getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        unauthenticatedPlayers.clear();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
            Player player = event.getPlayer();
            player.setFoodLevel(20); // всегда 20
            player.setSaturation(20f);
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        unauthenticatedPlayers.add(player.getName());
        player.sendMessage("🔒 Введите /mc-login <пароль> для авторизации.");
        Bukkit.getScheduler().runTaskLater(this, () -> {
            int userId = getUserId(player);
            if (userId != -1) {
                HealthManager.getInstance().updateBossBar(player);
            }
        }, 40L);
        player.setInvisible(false);
        PlayerStatsManager.load(player);
//        ManaManager.init(player);
        player.setFoodLevel(20); // всегда 20
        player.setSaturation(20f);

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        unauthenticatedPlayers.remove(player.getName());
        int userId = getUserId(player);
        if (userId != -1) {
            double currentHealth = HealthManager.getInstance().getHealth(player);
            double maxHealth = HealthManager.getInstance().getMaxHealth(player);
            HealthManager.getInstance().sendHealthToSite(player);
            PlayerStatsManager.unload(player);
            ManaManager.remove(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (unauthenticatedPlayers.contains(event.getPlayer().getName())) {
            event.setCancelled(true);
        }
        Player player = event.getPlayer();
        player.setFoodLevel(20); // всегда 20
        player.setSaturation(20f);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (unauthenticatedPlayers.contains(event.getPlayer().getName())) {
            event.setCancelled(true);
        }
        Player player = event.getPlayer();
        player.setFoodLevel(20); // всегда 20
        player.setSaturation(20f);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (unauthenticatedPlayers.contains(event.getPlayer().getName())) {
            event.setCancelled(true);
        }
        Player player = event.getPlayer();
        player.setFoodLevel(20); // всегда 20
        player.setSaturation(20f);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && unauthenticatedPlayers.contains(p.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (unauthenticatedPlayers.contains(event.getPlayer().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (unauthenticatedPlayers.contains(event.getPlayer().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTakeDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player &&
                unauthenticatedPlayers.contains(player.getName())) {
            player.setFoodLevel(20); // всегда 20
            player.setSaturation(20f);
        }
    }


    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (unauthenticatedPlayers.contains(event.getPlayer().getName())) {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("❌ Только игрок может использовать эту команду.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("mc-login")) {
            if (args.length != 1) {
                player.sendMessage("⚠️ Используйте: /mc-login <пароль>");
                return true;
            }

            String username = player.getName(); // авто-логин по никам Minecraft
            String password = args[0];

            RequestBody body = RequestBody.create(MediaType.parse("application/json"),
                    "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");

            Request request = new Request.Builder()
                    .url("http://fc-server.zapto.org/api/users/login-minecraft")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            JavaPlugin plugin = WebAuth.this; // 👈 правильно получить ссылку на плагин

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    player.sendMessage("❌ Ошибка соединения с API");
                    getLogger().warning("❌ Ошибка подключения: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();
                    getLogger().info("📡 Код ответа: " + response.code());
                    getLogger().info("📡 Ответ от API: " + json);
                    getLogger().info("📛 Логин отправлен как: " + username + ", пароль: " + password);
                    if (response.isSuccessful()) {
                        JSONObject obj = new JSONObject(json);
                        int userId = obj.getInt("id"); // правильно!

                        authenticatedIds.put(player.getName(), userId);
                        WebAuth.setUserId(player, userId);
                        EffectRegistry.loadFromServer(userId);

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            getLogger().info("✅ Авторизация прошла для: " + player.getName() + ", userId=" + userId);

                            // Загрузить здоровье
                            HealthManager.getInstance().loadAndApply(player, player.getUniqueId().toString(), false);

                            // Через 2 секунды сохранить здоровье
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
//                                HealthManager.getInstance().saveHealthToApi(player);
                                HealthManager.getInstance().updateBossBar(player);
                            }, 40L);

                            unauthenticatedPlayers.remove(player.getName());
                            player.sendMessage("✅ Добро пожаловать на сервер!");
                            PlayerStatsManager.markAuthenticated(player);
                            statsManager.applyEffects(player);

                            // Инициализация BossBar
                            HealthManager.getInstance().loadAndApply(player, player.getUniqueId().toString(), false);
                        });

                    } else {
                        player.sendMessage("❌ Неверный логин или пароль.");
                    }
                }
            });
            return true;
        }
        if (command.getName().equalsIgnoreCase("mc-recalc")) {
            String uuid = player.getUniqueId().toString();

            Request request = new Request.Builder()
                    .url("http://fc-server.zapto.org/minecraft/recalc-bonus/" + uuid)
                    .post(RequestBody.create(new byte[0], null)) // пустое тело
                    .build();

            HealthDataFetcher.getClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    player.sendMessage("❌ Ошибка соединения при обновлении бонуса");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();

                    JSONObject obj = new JSONObject(json);
                    boolean updated = obj.optBoolean("updated", false);
                    String message = obj.optString("message", "✅ Бонус обновлён!");

                    player.sendMessage(message);

                    if (updated) {
                        Bukkit.getScheduler().runTask(WebAuth.this, () ->
                                HealthManager.getInstance().loadAndApply(player, uuid, true)
                        );
                    }

                    response.close();
                }
            });

            return true;
        }



        if (command.getName().equalsIgnoreCase("mc-balance")) {
            String username = player.getName();

            RequestBody body = RequestBody.create(MediaType.parse("application/json"),
                    "{\"username\":\"" + username + "\"}");

            Request request = new Request.Builder()
                    .url("http://fc-server.zapto.org/minecraft/balance")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            JavaPlugin plugin = WebAuth.this;
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    player.sendMessage("❌ Ошибка при запросе баланса.");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();
                    try {
                        JSONObject obj = new JSONObject(json);
                        String balance = obj.get("balance").toString();
                        player.sendMessage("💰 Ваш текущий баланс: " + balance + "F");
                    } catch (Exception e) {
                        player.sendMessage("❌ Ошибка разбора ответа сервера.");
                        getLogger().warning("❌ Ошибка JSON: " + e.getMessage());
                    }

                }
            });
            return true;
        }
        if (command.getName().equalsIgnoreCase("mc-pay")) {
            if (args.length != 2) {
                player.sendMessage("⚠️ Используйте: /mc-pay <ник> <сумма>");
                return true;
            }

            String from = player.getName();
            String to = args[0];
            String amount = args[1];

            RequestBody body = RequestBody.create(MediaType.parse("application/json"),
                    "{\"fromUsername\":\"" + from + "\",\"toUsername\":\"" + to + "\",\"amount\":\"" + amount + "\"}");

            Request request = new Request.Builder()
                    .url("http://fc-server.zapto.org/minecraft/transfer")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    player.sendMessage("❌ Ошибка при переводе валюты.");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();
                    if (response.isSuccessful()) {
                        player.sendMessage("✅ Перевод выполнен!");
                    } else {
                        player.sendMessage("❌ Ошибка перевода: " + json);
                    }
                }
            });
            return true;
        }

        return false;
    }

    // Отправка статистики пользователя на сайт





    private void sendPlayerStatsToSite(Player player) {
        JSONObject payload = new JSONObject();

        // Основные данные
        payload.put("username", player.getName());
        payload.put("uuid", player.getUniqueId().toString());
        payload.put("health", HealthManager.getInstance().getHealth(player));
        payload.put("maxHealth", HealthManager.getInstance().getMaxHealth(player));
        payload.put("level", player.getLevel());
        payload.put("armor", player.getInventory().getArmorContents().length);
        payload.put("mainHand", player.getInventory().getItemInMainHand().getType().toString());

        // Эффекты
        JSONArray effects = new JSONArray();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            effects.put(effect.getType().getName().toLowerCase());
        }
        payload.put("effects", effects);

        // Инвентарь
        JSONArray inventory = new JSONArray();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                JSONObject itemObj = new JSONObject();
                itemObj.put("item", item.getType().toString().toLowerCase());
                itemObj.put("amount", item.getAmount());
                inventory.put(itemObj);
            }
        }
        payload.put("inventory", inventory);

        // Отправка на сайт
        Request request = new Request.Builder()
                .url("http://fc-server.zapto.org/minecraft/player-stats")  // заменишь на нужный URL
                .post(RequestBody.create(payload.toString(), MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getLogger().warning("❌ Ошибка отправки статуса игрока: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                getLogger().info("📡 Отправлены данные игрока " + player.getName() + ": " + response.code());
            }
        });
    }

    // WebAuth.java
    private static final Map<String, String> playerTokens = new HashMap<>();

    public static void setToken(Player player, String token) {
        playerTokens.put(player.getName(), token);
    }

    public static String getToken(Player player) {
        return playerTokens.get(player.getName());
    }


}
