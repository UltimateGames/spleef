package me.ampayne2.spleef;

import java.util.List;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.api.GamePlugin;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.arenas.ArenaStatus;
import me.ampayne2.ultimategames.arenas.scoreboards.ArenaScoreboard;
import me.ampayne2.ultimategames.arenas.spawnpoints.PlayerSpawnPoint;
import me.ampayne2.ultimategames.games.Game;

import me.ampayne2.ultimategames.utils.UGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Spleef extends GamePlugin {

    private UltimateGames ultimateGames;
    private Game game;

    @Override
    public boolean loadGame(UltimateGames ultimateGames, Game game) {
        this.ultimateGames = ultimateGames;
        this.game = game;
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
        ultimateGames.getCountdownManager().createEndingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getConfig().getInt("CustomValues.MaxGameTime"), false);

        ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().createArenaScoreboard(arena, game.getName());
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
            ultimateGames.getMessageManager().sendGameMessage(ultimateGames.getServer(), game, "GameEnd", players.get(0), game.getName(), arena.getName());
            ultimateGames.getPointManager().addPoint(game, players.get(0), "store", 10);
            ultimateGames.getPointManager().addPoint(game, players.get(0), "win", 1);
        }
    }

    @Override
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
            ultimateGames.getCountdownManager().createStartingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getConfig().getInt("CustomValues.StartWaitTime"));
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
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
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
    }

    @Override
    public void removeSpectator(Player player, Arena arena) {

    }

    @Override
    public void onPlayerDeath(Arena arena, PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            ultimateGames.getPlayerManager().makePlayerSpectator(player);
            ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().getArenaScoreboard(arena);
            if (scoreBoard != null) {
                scoreBoard.setScore(ChatColor.GREEN + "Survivors", arena.getPlayers().size());
            }
            ultimateGames.getMessageManager().sendGameMessage(arena, game, "Death", player.getName());
            ultimateGames.getPointManager().addPoint(game, player.getName(), "loss", 1);
            ultimateGames.getPlayerManager().makePlayerSpectator(player);
        }
        event.getDrops().clear();
        UGUtils.autoRespawn(player);
        for (String playerName : arena.getPlayers()) {
            ultimateGames.getPointManager().addPoint(game, playerName, "store", 1);
        }
        if (arena.getPlayers().size() <= 1) {
            ultimateGames.getArenaManager().endArena(arena);
        }
    }

    @Override
    public void onPlayerRespawn(Arena arena, PlayerRespawnEvent event) {
        event.setRespawnLocation(ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena).getLocation());
        resetInventory(event.getPlayer());
    }

    @Override
    public void onEntityDamage(Arena arena, EntityDamageEvent event) {
        if (event.getCause() != DamageCause.LAVA) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEntityDamageByEntity(Arena arena, EntityDamageByEntityEvent event) {
        event.setCancelled(true);
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

    @Override
    public void onBlockBreak(Arena arena, BlockBreakEvent event) {
        event.setCancelled(true);
        final Block block = event.getBlock();
        Bukkit.getScheduler().scheduleSyncDelayedTask(ultimateGames, new Runnable() {
            @Override
            public void run() {
                block.setType(Material.AIR);
            }
        }, 0L);
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
        if (ultimateGames.getPointManager().hasPerk(game, player.getName(), "diamondshovel")) {
            player.getInventory().addItem(new ItemStack(Material.DIAMOND_SPADE));
        } else if (ultimateGames.getPointManager().hasPerk(game, player.getName(), "ironshovel")) {
            player.getInventory().addItem(new ItemStack(Material.IRON_SPADE));
        } else if (ultimateGames.getPointManager().hasPerk(game, player.getName(), "goldshovel")) {
            player.getInventory().addItem(new ItemStack(Material.GOLD_SPADE));
        } else if (ultimateGames.getPointManager().hasPerk(game, player.getName(), "stoneshovel")) {
            player.getInventory().addItem(new ItemStack(Material.STONE_SPADE));
        } else {
            player.getInventory().addItem(new ItemStack(Material.WOOD_SPADE));
        }

        if (ultimateGames.getPointManager().hasPerk(game, player.getName(), "haste")) {
            ItemStack sugar = new ItemStack(Material.SUGAR);
            ItemMeta meta = sugar.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "5s Haste");
            sugar.setItemMeta(meta);
            player.getInventory().addItem(sugar);
        }

        player.getInventory().addItem(UGUtils.createInstructionBook(game));
        player.getInventory().setArmorContents(null);
        player.updateInventory();
    }

    @Override
    public void onPlayerInteract(final Arena arena, PlayerInteractEvent event) {
        if (event.getMaterial().equals(Material.SUGAR)) {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 100, 1));
            event.getPlayer().getInventory().remove(event.getItem());
        }
    }
}
