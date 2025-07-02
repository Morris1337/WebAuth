package lv.funcatchers.debug;

import lv.funcatchers.WebAuth;
import org.bukkit.GameMode;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MobDebugListener implements Listener {

    @EventHandler
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        boolean isAuth = !WebAuth.isUnauthenticated(player); // 👈 добавь такой метод в WebAuth
        System.out.println("🔍 Моб " + mob.getName() + " пытается нацелиться на " + player.getName());
        System.out.println("    🔐 Авторизован: " + isAuth);
        System.out.println("    🎮 GameMode: " + player.getGameMode());
        System.out.println("    👻 Invisible: " + player.isInvisible());
        System.out.println("    🛡 Invulnerable: " + player.isInvulnerable());
    }

    @EventHandler
    public void onMobAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Mob mob)) return;

        System.out.println("💥 " + mob.getName() + " атакует " + player.getName() + " на " + event.getFinalDamage());
    }
}
