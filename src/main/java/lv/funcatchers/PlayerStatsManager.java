package lv.funcatchers;
import com.ssomar.score.api.executableitems.ExecutableItemsAPI;
import lv.funcatchers.EffectRegistry;
import lv.funcatchers.rpghealth.HealthManager;
import com.ssomar.score.api.executableitems.config.ExecutableItemInterface;
import com.ssomar.score.api.executableitems.config.ExecutableItemsManagerInterface;




import lv.funcatchers.rpgmana.ManaItemRegistry;
import lv.funcatchers.rpgmana.ManaManager;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Egg;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;
import org.bukkit.util.Vector; // ✅ Это нужный импорт

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;


import java.io.IOException;
import java.util.*;
import java.util.UUID;

import static lv.funcatchers.WebAuth.getUserId;

public class PlayerStatsManager implements CommandExecutor, Listener {
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("cb2b5e45-7b0b-4cb4-bbd6-bb1f3bcb1c9d");


    private final JavaPlugin plugin;
    private final OkHttpClient client = new OkHttpClient();

    // 🧠 Кэш эффектов по UUID игрока
    private static final Map<UUID, Map<String, Double>> cachedEffects = new HashMap<>();
    //private VillagerReplenishTradeEvent EffectRegistry;

    public PlayerStatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, Double> getCachedEffects(Player player) {
        return cachedEffects.getOrDefault(player.getUniqueId(), new HashMap<>());
    }

    public static Map<String, Double> getEffects(Player player) {
        return cachedEffects.getOrDefault(player.getUniqueId(), new HashMap<>());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("❌ Только игрок может использовать команду.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("mc-reloadstats")) {
            int userId = getUserId(player);
            if (userId == -1) {
                player.sendMessage("🔒 Вы не авторизованы.");
                return true;
            }

            applyEffects(player); // это вызывает всё, включая загрузку эффектов и бонусов
            player.sendMessage("🔁 Эффекты обновлены с API.");
            return true;
        }



        if (command.getName().equalsIgnoreCase("mc-upgrade")) {
            if (args.length != 1) {
                player.sendMessage("⚠ Используй: /mc-upgrade <str|agi|vit|dex|int|luk>");
                return true;
            }

            String stat = args[0].toLowerCase();
            JSONObject payload = new JSONObject();
            payload.put("username", player.getName());
            payload.put("stat", stat);

            Request request = new Request.Builder()
                    .url("http://fc-server.zapto.org/minecraft/distribute-stat")
                    .post(RequestBody.create(payload.toString(), MediaType.parse("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    player.sendMessage("❌ Ошибка подключения к API");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();
                    JSONObject obj = new JSONObject(json);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (response.isSuccessful()) {
                                player.sendMessage("✅ Прокачано: " + obj.getString("stat") + " → " + obj.getInt("newValue"));
                                applyEffects(player);
                            } else {
                                player.sendMessage("❌ Ошибка: " + obj.optString("message", "Не удалось прокачать"));
                            }
                        }
                    }.runTask(plugin);
                }
            });
            return true;
        }

        return false;
    }

    // 🧠 Загружает эффекты из API, сохраняет в кэш, применяет на игрока
    public void applyEffects(Player player) {
        int userId = getUserId(player);
        if (userId == -1) {
            System.out.println("❌ userId не найден для " + player.getName());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            EffectRegistry.loadFromServer(userId);
            Map<String, Double> effects = EffectsFetcher.fetchEffects(userId);

// 👇 добавляем все бонусы из EffectRegistry в общую карту
            String[] bonusTypes = {
                    "attack_speed_bonus", "speed_bonus",
                    "dodge_chance", "hunger_penalty",
                    "vit_hp_bonus", "vit_armor_bonus",
                    "attack_bonus", "luck_drop_bonus",
                    "accuracy_bonus", "ranged_damage_bonus",
                    "int", "mana_bonus", "mana_regen"
            };

            for (String type : bonusTypes) {
                double bonus = EffectRegistry.getBonus(type);
                effects.put(type, bonus); // 👈 объединение вручную
            }

            cachedEffects.put(player.getUniqueId(), effects);


            Bukkit.getScheduler().runTask(plugin, () -> {
                applyBonuses(player, effects);
                ManaManager.init(player); // ✅ теперь max и current будут равны загруженному mana_bonus
            });

        });
    }

    private void applyBonuses(Player player, Map<String, Double> effects) {
        AttributeInstance armorAttr = player.getAttribute(Attribute.GENERIC_ARMOR);
        if (armorAttr == null) return;

        double baseArmor = 1.0;
        double vit = effects.getOrDefault("vit", 0.0);
        double vitBonus = vit; // каждое очко вит = 1 броня

        double finalArmor = baseArmor + vitBonus;

        // Удаляем предыдущий модификатор (если есть)
        UUID modUuid = UUID.nameUUIDFromBytes(("vit-armor-bonus-" + player.getUniqueId()).getBytes());
        armorAttr.getModifiers().stream()
                .filter(mod -> mod.getUniqueId().equals(modUuid))
                .forEach(armorAttr::removeModifier);

        AttributeModifier vitModifier = new AttributeModifier(
                modUuid,
                "VitArmorBonus",
                finalArmor,
                AttributeModifier.Operation.ADD_NUMBER
        );

        // Сброс и повторная установка базовой брони
        armorAttr.setBaseValue(0.0);
        armorAttr.addModifier(vitModifier);

        player.sendMessage("🛡 ВИТ броня применена: +" + vitBonus + " (итого: " + finalArmor + ")");
        player.sendMessage("🧱 Реальная броня: " + armorAttr.getValue());

        //Настройка АГИ
        System.out.println("▶ AGI RAW EFFECTS: " + effects);
        AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            double bonus = effects.getOrDefault("attack_speed_bonus", 0.0);
            AttributeModifier attackSpeedMod = new AttributeModifier(
                    UUID.nameUUIDFromBytes(("agi-attack-speed-" + player.getUniqueId()).getBytes()),
                    "AgiAttackSpeed",
                    bonus,
                    AttributeModifier.Operation.ADD_SCALAR
            );

            attackSpeedAttr.getModifiers().stream()
                    .filter(m -> m.getName().equals("AgiAttackSpeed"))
                    .forEach(attackSpeedAttr::removeModifier);

            attackSpeedAttr.addModifier(attackSpeedMod);
        }
        AttributeInstance moveSpeedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (moveSpeedAttr != null) {
            double speedBonus = effects.getOrDefault("speed_bonus", 0.0);
            AttributeModifier moveSpeedMod = new AttributeModifier(
                    UUID.nameUUIDFromBytes(("agi-speed-bonus-" + player.getUniqueId()).getBytes()),
                    "AgiSpeedBonus",
                    speedBonus,
                    AttributeModifier.Operation.ADD_SCALAR
            );

            moveSpeedAttr.getModifiers().stream()
                    .filter(m -> m.getName().equals("AgiSpeedBonus"))
                    .forEach(moveSpeedAttr::removeModifier);

            moveSpeedAttr.addModifier(moveSpeedMod);
        }
// 📦 Увеличение разрушения блоков через эффект HASTE
        double breakSpeedBonus = effects.getOrDefault("block_break_speed_bonus", 0.0);
        if (breakSpeedBonus > 0.0) {
            // Ограничим уровень Haste
            int amplifier = Math.min((int) Math.round(breakSpeedBonus) - 1, 4);

            player.removePotionEffect(PotionEffectType.FAST_DIGGING);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.FAST_DIGGING,
                    20 * 60 * 60, // 1 час в тиках
                    amplifier,
                    false,
                    false
            ));
            player.sendMessage("⛏ Скорость копания увеличена (Haste " + (amplifier + 1) + ")");
        } else {
            player.removePotionEffect(PotionEffectType.FAST_DIGGING);
        }
        AttributeInstance knockbackAttr = player.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK);
        if (knockbackAttr != null) {
            double knockbackBonus = effects.getOrDefault("knockback_bonus", 0.0);

            UUID knockbackUUID = UUID.nameUUIDFromBytes(("str-knockback-bonus-" + player.getUniqueId()).getBytes());

            // Удалим старый модификатор
            knockbackAttr.getModifiers().stream()
                    .filter(mod -> mod.getUniqueId().equals(knockbackUUID))
                    .forEach(knockbackAttr::removeModifier);

            // Применим новый модификатор
            AttributeModifier knockMod = new AttributeModifier(
                    knockbackUUID,
                    "STR_KnockbackBonus",
                    knockbackBonus,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            knockbackAttr.addModifier(knockMod);

            player.sendMessage("💥 Сила отталкивания STR: +" + knockbackBonus);
        }


    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        // === 🎯 Дальний бой: Projectile (лук, арбалет, трезубец, снежки, яйца и т.д.) ===
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile &&
                projectile.getShooter() instanceof Player shooter &&
                event.getEntity() instanceof LivingEntity target) {

            Map<String, Double> shooterEffects = cachedEffects.getOrDefault(shooter.getUniqueId(), new HashMap<>());
            Map<String, Double> targetEffects = cachedEffects.getOrDefault(target.getUniqueId(), new HashMap<>());

            double accuracy = shooterEffects.getOrDefault("accuracy_bonus", 0.0);
            double dodge = targetEffects.getOrDefault("dodge_chance", 0.0);
            double finalHitChance = 100.0 - Math.max(0, dodge - accuracy);

//            if (Math.random() * 100.0 > finalHitChance) {
//                event.setCancelled(true);
//                shooter.sendMessage("🎯 Промах! Цель уклонилась.");
//                return;
//            }
            if (Math.random() * 100.0 > finalHitChance) {
                event.setCancelled(true);

                String[] missMessages = { "❌ MISS", "🎯 Промах", "🌀 Уклонение" };
                String msg = missMessages[new Random().nextInt(missMessages.length)];

                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                return;
            }


            double rangedBonus = shooterEffects.getOrDefault("ranged_damage_bonus", 0.0);
            double damageWithBonus = event.getDamage() * (1.0 + rangedBonus);
            event.setDamage(damageWithBonus);

            shooter.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("🏹 Урон с бонусом (DEX): " + String.format("%.1f", event.getFinalDamage()))
            );
            if (projectile instanceof Snowball) {
                double snowballBonus = shooterEffects.getOrDefault("snowball_damage_bonus", 0.0);
                event.setDamage(snowballBonus);
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("❄ Урон снежком: " + snowballBonus));
                if (snowballBonus > 0.0) {
                    Bukkit.getScheduler().runTask(plugin, () -> target.damage(snowballBonus, shooter));
                }
            } else if (projectile instanceof Egg) {
                double eggBonus = shooterEffects.getOrDefault("egg_damage_bonus", 0.0);
                event.setDamage(eggBonus);
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("🥚 Урон яйцом: " + eggBonus));
                if (eggBonus > 0.0) {
                    Bukkit.getScheduler().runTask(plugin, () -> target.damage(eggBonus, shooter));
                }
            }


            return; // ⚠️ Не обрабатываем ниже как ближний бой!
        }

        // === 💥 Ближний бой: Игрок как атакующий ===
        if (event.getDamager() instanceof Player player) {
            if (event.getEntity() instanceof LivingEntity target) {
                double distance = player.getLocation().distance(target.getLocation());
                if (distance > 2.25) {
                    event.setCancelled(true);
//                    player.sendMessage("⛔ Слишком далеко для ближнего боя! (<2 блоков)");
                    return;
                }
            }
            Map<String, Double> effects = cachedEffects.getOrDefault(player.getUniqueId(), new HashMap<>());
            double bonus = EffectRegistry.getBonus("attack_bonus");

            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attribute == null) return;

            attribute.getModifiers().stream()
                    .filter(mod -> mod.getUniqueId().equals(ATTACK_MODIFIER_UUID))
                    .forEach(attribute::removeModifier);

            AttributeModifier modifier = new AttributeModifier(
                    ATTACK_MODIFIER_UUID,
                    "attack_bonus_from_stats",
                    bonus,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            attribute.addModifier(modifier);

            double knockbackBonus = effects.getOrDefault("knockback_bonus", 0.0);
            if (knockbackBonus > 0.0 && event.getEntity() instanceof LivingEntity target) {
                double knockPower = Math.min(knockbackBonus, 3.0);
                Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                direction.setY(0.25);
                target.setVelocity(direction.multiply(knockPower));
                player.sendMessage("💥 Сработал бонус STR: откидывание!");
            }

            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("⚔ Урон с бонусом (STR): " + String.format("%.1f", event.getFinalDamage()))
            );

            System.out.println("⚔ STR модификатор урона применён: +" + bonus + " → " + player.getName());
        }

    }




    // 🎯 Применение эффектов при входе
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyEffects(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Map<String, Double> effects = cachedEffects.getOrDefault(player.getUniqueId(), new HashMap<>());
        double dodgeChance = effects.getOrDefault("dodge_chance", 0.0);

//        if (Math.random() < dodgeChance) {
//            event.setCancelled(true);
//            player.sendMessage("🌀 Вы увернулись от атаки!");
//            System.out.println("✅ Уворот от атаки! Урон не нанесён.");
//        }
        if (Math.random() < dodgeChance) {
            event.setCancelled(true);

            String[] dodgeMessages = { "🌀 Уворот!", "💨 MISS", "🛡 Вы увернулись!" };
            String msg = dodgeMessages[new Random().nextInt(dodgeMessages.length)];

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            System.out.println("✅ Уворот от атаки! Урон не нанесён.");
        }else {
            System.out.println("❌ Не увернулся. Урон: " + event.getFinalDamage());
        }
    }



    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Map<String, Double> effects = cachedEffects.getOrDefault(player.getUniqueId(), new HashMap<>());
        double hungerPenalty = effects.getOrDefault("hunger_penalty", 0.0);

        if (hungerPenalty > 0 && Math.random() < hungerPenalty) {
            int newLevel = event.getFoodLevel() - 1;
            event.setFoodLevel(Math.max(newLevel, 0));
            player.sendMessage("🍖 Быстрее проголодались из-за AGI!");
        }
    }

    public static void markAuthenticated(Player player) {
        // здесь можно что-то сделать, если потребуется
    }

    public static void load(Player player) {
        int userId = getUserId(player); // обязательно добавь или вызови корректно

        if (userId == -1) {
            System.out.println("⚠ Не удалось найти userId для " + player.getName());
            return;
        }

        Map<String, Double> effects = EffectsFetcher.fetchEffects(userId);
        cachedEffects.put(player.getUniqueId(), effects);
        System.out.println("✅ Эффекты загружены и закешированы для " + player.getName());
    }


    public static void unload(Player player) {
        cachedEffects.remove(player.getUniqueId());
    }

    public static double getEffect(Player player, String key) {
        Map<String, Double> map = cachedEffects.getOrDefault(player.getUniqueId(), new HashMap<>());
        return map.getOrDefault(key, 0.0);
    }

    @EventHandler
    public void onUseMagicItem(PlayerInteractEvent event) {
        ExecutableItemsManagerInterface manager = ExecutableItemsAPI.getExecutableItemsManager();
        Player player = event.getPlayer();

        // Используем ExecutableItems API
        Optional<ExecutableItemInterface> eiOpt = manager.getExecutableItem(event.getItem());
        if (eiOpt.isEmpty()) return;

        ExecutableItemInterface ei = eiOpt.get();
        String itemId = ei.getId();
        if (ei == null) return;

        String id = ei.getId(); // Например: magic_wand

        if (!ManaItemRegistry.isMagicItem(id)) return;

        int cost = ManaItemRegistry.getManaCost(id);
        double currentMana = ManaManager.getCurrentMana(player);

        if (currentMana < cost) {
            event.setCancelled(true);
            player.sendMessage("❌ Недостаточно маны для использования!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        ManaManager.subtract(player, cost);
        player.sendMessage("✨ Потрачено " + cost + " SP для использования " + id);
    }

}
