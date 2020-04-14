package tk.t11e.chestshop.listener;
// Created by booky10 in ChestShop (21:26 06.04.20)

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tk.t11e.api.util.OtherUtils;
import tk.t11e.chestshop.main.Main;
import tk.t11e.chestshop.manager.ItemSorter;
import tk.t11e.chestshop.manager.SignShopManager;

import java.util.HashMap;
import java.util.UUID;

public class SignListener implements Listener {

    public static HashMap<UUID, Block> confirmationUserSigns = new HashMap<>();
    public static HashMap<UUID, Block> confirmationUserChests = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignPlace(SignChangeEvent event) {
        Player player = event.getPlayer();
        Sign sign = (Sign) event.getBlock().getState();
        if (!Tag.WALL_SIGNS.isTagged(sign.getType())) return;
        BlockFace chestDirection = ((WallSign) sign.getBlockData()).getFacing().getOppositeFace();
        Block chestBlock = sign.getLocation().add(chestDirection.getDirection()).getBlock();
        if (!(chestBlock.getState() instanceof Chest)) return;
        Chest chest = (Chest) chestBlock.getState();

        if (chest.getInventory().getContents().length != 27)
            player.sendMessage(Main.PREFIX + "Du darfst nur eine Kiste benutzen, keie Doppelte!");
        else if (!containsInventoryOneItem(chest.getInventory()))
            player.sendMessage(Main.PREFIX + "In der Kiste darf kein Item oder nur mehrere gleiche sein!");
        else {
            try {
                if (Integer.parseInt(event.getLine(1)) > 64) {
                    player.sendMessage(Main.PREFIX + "Du kannst nicht mehr als 64 Items auf einmal verkaufen!");
                    player.playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.25f);
                    return;
                }
                try {
                    Double.parseDouble(event.getLine(2));
                    /*if (isInventoryClear(chest.getInventory())) {
                        player.sendMessage(Main.PREFIX + "Als erstes muss mindestens ein Item dadrin sein! " +
                                "Nachher kannst du aber noch nachfüllen!");
                        player.playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.25f);
                        return;
                    }*/
                    if (OtherUtils.getKeysByValue(confirmationUserChests, chestBlock).size() == 0) {
                        confirmationUserSigns.put(player.getUniqueId(), sign.getBlock());
                        confirmationUserChests.put(player.getUniqueId(), chestBlock);
                        player.openInventory(getConfirmationGUI());
                        ItemStack firstItem = chest.getInventory().getContents()[0];
                        if (firstItem != null)
                            if (firstItem.hasItemMeta())
                                event.setLine(3, firstItem.getItemMeta().getDisplayName());
                            else
                                event.setLine(3, firstItem.getType().getKey().getKey().replaceAll("_",
                                        " "));
                        else
                            event.setLine(3, "Keine Items!");
                        event.setLine(0, player.getName());
                    } else {
                        player.sendMessage(Main.PREFIX + "Hier erstellt schon jemand einen ChestShop!");
                        player.playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.25f);
                    }
                } catch (NumberFormatException exception) {
                    player.sendMessage(Main.PREFIX + "In der dritten Zeile darf §lNUR§c der Preis des Items " +
                            "stehen, wenn du einen ChestShop erstellen möchtest! (Keine \"€\" Zeichen!)");
                    player.playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.25f);
                }
            } catch (NumberFormatException exception) {
                player.sendMessage(Main.PREFIX + "In der zweiten Zeile muss die Anzahl an Verkäufen pro Besuch " +
                        "stehen, wenn du einen ChestShop erstellen möchtest!");
                player.playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.25f);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getWhoClicked().getType().equals(EntityType.PLAYER)) return;
        if (!confirmationUserSigns.containsKey(event.getWhoClicked().getUniqueId())) return;
        if (!event.getView().getTitle().equals("§1§rChestShop erstellen?")) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() != null)
            if (event.getCurrentItem().getType().equals(Material.LIME_WOOL)) {
                if (Tag.WALL_SIGNS.isTagged(confirmationUserSigns.get(player.getUniqueId()).getType())) {
                    player.sendMessage(Main.PREFIX + "§aDu hast nun einen ChestShop erstellt!");
                    player.playSound(player.getEyeLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                            SoundCategory.AMBIENT, 0.5f, 1.5f);
                    SignShopManager.registerShop((Sign) confirmationUserSigns.get(player.getUniqueId()).getState());
                    confirmationUserSigns.remove(player.getUniqueId());
                    confirmationUserChests.remove(player.getUniqueId());
                    player.closeInventory();
                } else
                    player.sendMessage(Main.PREFIX + "Der ChestShop wurde nicht erstellt!");
            } else if (event.getCurrentItem().getType().equals(Material.RED_WOOL))
                player.closeInventory();
        event.setCancelled(true);
    }

    @EventHandler
    public void onShopUse(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if (!Tag.WALL_SIGNS.isTagged(event.getClickedBlock().getType())) return;
        if (!SignShopManager.registeredSigns.contains(event.getClickedBlock())) return;

        SignShopManager.useShop((Sign) event.getClickedBlock().getState(), event.getPlayer());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!(OtherUtils.getKeysByValue(confirmationUserSigns, event.getBlock()).size() == 0))
            if (!(OtherUtils.getKeysByValue(confirmationUserChests, event.getBlock()).size() == 0)) {
                player.sendMessage(Main.PREFIX + "Hier richtet gerade jemand einen ChestShop ein! Störe ihn" +
                        " dabei nicht!");
                event.setCancelled(true);
                return;
            }
        if (SignShopManager.registeredSigns.contains(event.getBlock()))
            SignShopManager.unregisterShop(event.getBlock());
        else if (SignShopManager.registeredChests.contains(event.getBlock()))
            SignShopManager.unregisterShop(SignShopManager.chestSignMap.get(event.getBlock()));
        else
            return;
        player.sendMessage(Main.PREFIX + "Der ChestShop wurde zerstört!");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!confirmationUserSigns.containsKey(event.getPlayer().getUniqueId())) return;
        if (!event.getView().getTitle().equals("§1§rChestShop erstellen?")) return;
        if (!event.getPlayer().getType().equals(EntityType.PLAYER)) return;
        Player player = (Player) event.getPlayer();
        player.sendMessage(Main.PREFIX + "Du hast keinen ChestShop erstellt!");
        player.playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.25f);
        confirmationUserSigns.remove(player.getUniqueId());
    }

    public static Inventory getConfirmationGUI() {
        Inventory inventory = Bukkit.createInventory(null, 9 * 3,
                "§1§rChestShop erstellen?");

        ItemStack ja = new ItemStack(Material.LIME_WOOL);
        ItemMeta jaMeta = ja.getItemMeta();
        jaMeta.setDisplayName("§aJa!");
        jaMeta.addItemFlags(ItemFlag.values());
        ja.setItemMeta(jaMeta);

        ItemStack nein = new ItemStack(Material.RED_WOOL);
        ItemMeta neinMeta = nein.getItemMeta();
        neinMeta.setDisplayName("§cNein!");
        neinMeta.addItemFlags(ItemFlag.values());
        nein.setItemMeta(neinMeta);

        inventory.setItem(11, ja);
        inventory.setItem(15, nein);

        return inventory;
    }

    public static Boolean isInventoryClear(Inventory inventory) {
        ItemSorter.sort(inventory);
        return inventory.getItem(0) == null;
    }

    public static Boolean containsInventoryOneItem(Inventory inventory) {
        if (isInventoryClear(inventory))
            return true;

        boolean oneItem = true;
        ItemStack oneItemStack = inventory.getContents()[0];
        for (ItemStack itemStack : inventory.getContents()) {
            if (oneItemStack != itemStack) {
                if (itemStack == null) break;
                if (oneItemStack.getType() != itemStack.getType()) {
                    if (oneItemStack.getItemMeta() != itemStack.getItemMeta()) {
                        oneItem = false;
                        break;
                    }
                }
            }
        }
        return oneItem;
    }
}