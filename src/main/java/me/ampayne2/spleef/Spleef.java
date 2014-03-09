package me.ampayne2.spleef;

import me.ampayne2.ultimategames.api.UltimateGames;
import me.ampayne2.ultimategames.api.arenas.Arena;
import me.ampayne2.ultimategames.api.arenas.ArenaStatus;
import me.ampayne2.ultimategames.api.arenas.scoreboards.Scoreboard;
import me.ampayne2.ultimategames.api.arenas.spawnpoints.PlayerSpawnPoint;
import me.ampayne2.ultimategames.api.games.Game;
import me.ampayne2.ultimategames.api.games.GamePlugin;
import me.ampayne2.ultimategames.api.games.items.GameItem;
import me.ampayne2.ultimategames.api.players.points.PointManager;
import me.ampayne2.ultimategames.api.utils.UGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;

public class Spleef extends GamePlugin {
    private UltimateGames ultimateGames;
    private Game game;
    private static final GameItem HASTE = new Haste();
    private static final ItemStack SHOVEL;
    private static final ItemStack SHOVEL1;
    private static final ItemStack SHOVEL2;
    private static final ItemStack SHOVEL3;
    private static final ItemStack SNOWBALLS = new ItemStack(Material.SNOW_BALL, 8);

    @Override
    public boolean loadGame(UltimateGames ultimateGames, Game game) {
        this.ultimateGames = ultimateGames;
        this.game = game;
        game.setMessages(SMessage.class);
        ultimateGames.getGameItemManager().registerGameItem(game, HASTE);
        return true;
    }

    @Override
    public void unloadGame() {
    }

    @Override
    public boolean reloadGame() {
        return true;
    }

    @Override
    public boolean stopGame() {
        return true;
    }

    @Override
    public boolean loadArena(Arena arena) {
        ultimateGames.addAPIHandler("/" + game.getName() + "/" + arena.getName(), new SpleefWebHandler(ultimateGames, arena));
        return true;
    }

    @Override
    public boolean unloadArena(Arena arena) {
        return true;
    }

    @Override
    public boolean isStartPossible(Arena arena) {
        return arena.getStatus() == ArenaStatus.OPEN;
    }

    @Override
    public boolean startArena(Arena arena) {
        return true;
    }

    @Override
    public boolean beginArena(Arena arena) {
        ultimateGames.getCountdownManager().createEndingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getInt("CustomValues.MaxGameTime"), false);

        Scoreboard scoreBoard = ultimateGames.getScoreboardManager().createScoreboard(arena, game.getName());
        for (String playerName : arena.getPlayers()) {
            scoreBoard.addPlayer(Bukkit.getPlayerExact(playerName));
        }
        for (PlayerSpawnPoint spawnPoint : ultimateGames.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
            spawnPoint.lock(false);
        }
        scoreBoard.setScore(ChatColor.GREEN + "Survivors", arena.getPlayers().size());
        scoreBoard.setVisible(true);

        return true;
    }

    @Override
    public void endArena(Arena arena) {
        List<String> players = arena.getPlayers();
        if (players.size() == 1) {
            ultimateGames.getMessenger().sendGameMessage(Bukkit.getServer(), game, SMessage.GAME_END, players.get(0), game.getName(), arena.getName());
            ultimateGames.getPointManager().addPoint(game, players.get(0), "store", 10);
            ultimateGames.getPointManager().addPoint(game, players.get(0), "win", 1);
        }
    }

    public boolean resetArena(Arena arena) {
        return true;
    }

    @Override
    public boolean openArena(Arena arena) {
        return true;
    }

    @Override
    public boolean stopArena(Arena arena) {
        return true;
    }

    @Override
    public boolean addPlayer(Player player, Arena arena) {
        if (arena.getStatus() == ArenaStatus.OPEN && arena.getPlayers().size() >= arena.getMinPlayers() && !ultimateGames.getCountdownManager().hasStartingCountdown(arena)) {
            ultimateGames.getCountdownManager().createStartingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getInt("CustomValues.StartWaitTime"));
        }
        for (PlayerSpawnPoint spawnPoint : ultimateGames.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
            spawnPoint.lock(false);
        }
        List<PlayerSpawnPoint> spawnPoints = ultimateGames.getSpawnpointManager().getDistributedSpawnPoints(arena, arena.getPlayers().size());
        for (int i = 0; i < arena.getPlayers().size(); i++) {
            PlayerSpawnPoint spawnPoint = spawnPoints.get(i);
            spawnPoint.lock(true);
            spawnPoint.teleportPlayer(Bukkit.getPlayerExact(arena.getPlayers().get(i)));
        }
        resetInventory(player);
        return true;
    }

    @Override
    public void removePlayer(Player player, Arena arena) {
        if (arena.getPlayers().size() <= 1) {
            ultimateGames.getArenaManager().endArena(arena);
        }
    }

    @Override
    public boolean addSpectator(Player player, Arena arena) {
        ultimateGames.getSpawnpointManager().getSpectatorSpawnPoint(arena).teleportPlayer(player);
        resetInventory(player);
        return true;
    }

    @Override
    public void makePlayerSpectator(Player player, Arena arena) {
        ultimateGames.getSpawnpointManager().getSpectatorSpawnPoint(arena).teleportPlayer(player);
        resetInventory(player);
        if (arena.getPlayers().size() <= 1) {
            ultimateGames.getArenaManager().endArena(arena);
        }
    }

    @Override
    public void removeSpectator(Player player, Arena arena) {
    }

    @Override
    public void onPlayerDeath(Arena arena, PlayerDeathEvent event) {
        Player player = event.getEntity();
        UGUtils.autoRespawn(ultimateGames.getPlugin(), player);
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            ultimateGames.getPlayerManager().makePlayerSpectator(player);
            Scoreboard scoreBoard = ultimateGames.getScoreboardManager().getScoreboard(arena);
            if (scoreBoard != null) {
                scoreBoard.setScore(ChatColor.GREEN + "Survivors", arena.getPlayers().size());
            }
            ultimateGames.getMessenger().sendGameMessage(arena, game, SMessage.DEATH, player.getName());
            ultimateGames.getPointManager().addPoint(game, player.getName(), "loss", 1);
            ultimateGames.getPlayerManager().makePlayerSpectator(player);
        }
        event.getDrops().clear();
        for (String playerName : arena.getPlayers()) {
            ultimateGames.getPointManager().addPoint(game, playerName, "store", 1);
        }
    }

    @Override
    public void onPlayerRespawn(Arena arena, PlayerRespawnEvent event) {
        event.setRespawnLocation(ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena).getLocation());
        resetInventory(event.getPlayer());
    }

    @Override
    public void onEntityDamage(Arena arena, EntityDamageEvent event) {
        if (event.getCause() != DamageCause.LAVA && event.getCause() != DamageCause.ENTITY_ATTACK) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEntityDamageByEntity(Arena arena, EntityDamageByEntityEvent event) {
        event.setDamage(0);
    }

    @Override
    public void onPlayerFoodLevelChange(Arena arena, FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onItemPickup(Arena arena, PlayerPickupItemEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onItemDrop(Arena arena, PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBlockBreak(Arena arena, BlockBreakEvent event) {
        event.setCancelled(true);
        final Block block = event.getBlock();
        Bukkit.getScheduler().scheduleSyncDelayedTask(ultimateGames.getPlugin(), new Runnable() {
            @Override
            public void run() {
                block.setType(Material.AIR);
            }
        }, 0L);
        event.getPlayer().updateInventory();
    }

    @Override
    public void onBlockFade(Arena arena, BlockFadeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerIgnite(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    private void resetInventory(Player player) {
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().addItem(UGUtils.createInstructionBook(game));
        String playerName = player.getName();
        PointManager pointManager = ultimateGames.getPointManager();
        if (pointManager.hasPerk(game, playerName, "shovel3")) {
            player.getInventory().addItem(SHOVEL3);
        } else if (pointManager.hasPerk(game, playerName, "shovel2")) {
            player.getInventory().addItem(SHOVEL2);
        } else if (pointManager.hasPerk(game, playerName, "shovel1")) {
            player.getInventory().addItem(SHOVEL1);
        } else {
            player.getInventory().addItem(SHOVEL);
        }

        if (pointManager.hasPerk(game, playerName, "haste")) {
            player.getInventory().addItem(HASTE.getItem());
        }

        if (pointManager.hasPerk(game, playerName, "snowballs")) {
            player.getInventory().addItem(SNOWBALLS);
        }

        player.getInventory().setArmorContents(null);
        player.updateInventory();
    }

    static {
        SHOVEL = new ItemStack(Material.STONE_SPADE);
        SHOVEL.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
        SHOVEL.addEnchantment(Enchantment.DIG_SPEED, 2);

        SHOVEL1 = new ItemStack(Material.IRON_SPADE);
        SHOVEL1.addUnsafeEnchantment(Enchantment.DURABILITY, 10);

        SHOVEL2 = new ItemStack(Material.GOLD_SPADE);
        SHOVEL2.addUnsafeEnchantment(Enchantment.DURABILITY, 10);

        SHOVEL3 = new ItemStack(Material.DIAMOND_SPADE);
        SHOVEL3.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
    }
}
