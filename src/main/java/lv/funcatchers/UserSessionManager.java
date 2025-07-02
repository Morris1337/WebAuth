package lv.funcatchers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserSessionManager {
    private static final Map<UUID, Integer> userSessions = new HashMap<>();

    public static void setUserId(Player player, int userId) {
        userSessions.put(player.getUniqueId(), userId);
    }

    public static int getUserId(Player player) {
        return userSessions.getOrDefault(player.getUniqueId(), -1);
    }

    public static void clear(Player player) {
        userSessions.remove(player.getUniqueId());
    }
}
