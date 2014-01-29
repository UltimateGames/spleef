package me.ampayne2.spleef;

import me.ampayne2.ultimategames.api.UltimateGames;
import me.ampayne2.ultimategames.api.arenas.Arena;
import me.ampayne2.ultimategames.api.arenas.scoreboards.Scoreboard;
import me.ampayne2.ultimategames.api.webapi.WebHandler;
import me.ampayne2.ultimategames.gson.Gson;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

public class SpleefWebHandler implements WebHandler {
    private Arena arena;
    private UltimateGames ug;

    public SpleefWebHandler(UltimateGames ug, Arena arena) {
        this.arena = arena;
        this.ug = ug;
    }

    @Override
    public String sendResult() {
        Gson gson = new Gson();
        Map<String, Integer> map = new HashMap<>();

        Scoreboard scoreBoard = ug.getScoreboardManager().getScoreboard(arena);
        if (scoreBoard != null) {
            map.put("Survivors", scoreBoard.getScore(ChatColor.GREEN + "Survivors"));
        }
        return gson.toJson(map);
    }
}
