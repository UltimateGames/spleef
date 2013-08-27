package me.ampayne2.spleef;

import java.util.HashMap;

import java.util.Map;

import org.bukkit.ChatColor;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.gson.Gson;
import me.ampayne2.ultimategames.scoreboards.ArenaScoreboard;
import me.ampayne2.ultimategames.webapi.WebHandler;

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
        Map<String, Integer> map = new HashMap<String, Integer>();

        for (ArenaScoreboard scoreBoard : ug.getScoreboardManager().getArenaScoreboards(arena)) {
            if (scoreBoard.getName().equals("Kills")) {
                map.put("Survivors", scoreBoard.getScore(ChatColor.GREEN + "Survivors"));
                break;
            }
        }
        return gson.toJson(map);
    }
}
