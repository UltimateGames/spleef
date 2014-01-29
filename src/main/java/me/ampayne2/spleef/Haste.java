package me.ampayne2.spleef;

import me.ampayne2.ultimategames.api.arenas.Arena;
import me.ampayne2.ultimategames.api.games.items.GameItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Haste GameItem.
 */
public class Haste extends GameItem {
    private static final ItemStack ITEM;

    public Haste() {
        super(ITEM, true);
    }

    @Override
    public boolean click(Arena arena, PlayerInteractEvent event) {
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 100, 1));
        return true;
    }

    static {
        ITEM = new ItemStack(Material.SUGAR);
        ItemMeta meta = ITEM.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "5s Haste");
        ITEM.setItemMeta(meta);
    }
}
