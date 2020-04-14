package tk.t11e.chestshop.manager;
// Created by booky10 in ChestShop (21:48 06.04.20)

import com.sun.istack.internal.NotNull;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tk.t11e.api.util.UUIDFetcher;
import tk.t11e.chestshop.listener.SignListener;
import tk.t11e.chestshop.main.Main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SignShopManager {

    public static List<Block> registeredChests = new ArrayList<>();
    public static List<Block> registeredSigns = new ArrayList<>();
    public static HashMap<Block, Block> chestSignMap = new HashMap<>();
    public static HashMap<Block, Block> signChestMap = new HashMap<>();

    public static void registerShop(Sign sign) {
        if (!Tag.WALL_SIGNS.isTagged(sign.getType())) return;

        Block blockBehind = sign.getLocation()
                .add(((WallSign) sign.getBlockData()).getFacing().getOppositeFace().getDirection()).getBlock();
        if (!(blockBehind.getState() instanceof Chest)) return;

        registeredSigns.add(sign.getBlock());
        registeredChests.add(blockBehind);
        chestSignMap.put(blockBehind, sign.getBlock());
        signChestMap.put(sign.getBlock(), blockBehind);

        ((Chest) blockBehind.getState()).setCustomName("§1§rChestShop - by " + sign.getLine(0));
    }

    public static void unregisterShop(Block sign) {
        if (!registeredSigns.contains(sign)) return;
        if (!registeredChests.contains(signChestMap.get(sign))) return;
        if (!chestSignMap.containsValue(sign)) return;
        if (!signChestMap.containsKey(sign)) return;

        registeredSigns.remove(sign);
        registeredChests.remove(signChestMap.get(sign));
        chestSignMap.remove(signChestMap.get(sign));
        signChestMap.remove(sign);
    }

    public static void saveShops(FileConfiguration config) {
        config.set("registeredSigns", blocksToStrings(registeredSigns));
        config.set("registeredChests", blocksToStrings(registeredChests));

        config.set("chestSignMapKeys", blocksToStrings(chestSignMap.keySet()));
        config.set("chestSignMapValues", blocksToStrings(chestSignMap.values()));

        config.set("signChestMapKeys", blocksToStrings(signChestMap.keySet()));
        config.set("signChestMapValues", blocksToStrings(signChestMap.values()));
        /*

        TODO - saving - TESTING
        TODO - loading - TESTING
        TODO - using - TESTING
        TODO - clearing lists - TESTING

         */
    }

    public static void loadShops(FileConfiguration config) {
        registeredSigns.addAll(stringsToBlocks(config.getStringList("registeredSigns")));
        registeredChests.addAll(stringsToBlocks(config.getStringList("registeredChests")));

        List<Block> chestSignMapKeys = stringsToBlocks(config.getStringList("chestSignMapKeys"));
        List<Block> chestSignMapValues = stringsToBlocks(config.getStringList("chestSignMapValues"));
        chestSignMap.putAll(listsToMap(chestSignMapKeys, chestSignMapValues));

        List<Block> signChestMapKeys = stringsToBlocks(config.getStringList("signChestMapKeys"));
        List<Block> signChestMapValues = stringsToBlocks(config.getStringList("signChestMapValues"));
        signChestMap.putAll(listsToMap(signChestMapKeys, signChestMapValues));
    }

    public static void useShop(Sign shop, Player user) {
        if (!registeredSigns.contains(shop.getBlock())) return;

        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUIDFetcher.getUUID(shop.getLine(0)));
        int amount = Integer.parseInt(shop.getLine(1));
        double cost = Double.parseDouble(shop.getLine(2));
        String name = shop.getLine(3);
        Chest chest = (Chest) signChestMap.get(shop.getBlock()).getState();
        Inventory chestInventory = chest.getBlockInventory();
        if (!(shop.getLine(3).equals("Keine Items!") || shop.getLine(3).equals("Error!")))
            if (SignListener.containsInventoryOneItem(chestInventory))
                if (!SignListener.isInventoryClear(chestInventory))
                    if (chestInventory.getItem(0).getAmount() >= amount)
                        if (Main.economy.hasAccount(owner) && Main.economy.hasAccount(user))
                            if (Main.economy.has(user, cost))
                                if (user.getInventory().firstEmpty() != -1) {
                                    Main.economy.depositPlayer(owner, cost);
                                    Main.economy.withdrawPlayer(user, cost);

                                    ItemStack bought = chestInventory.getItem(0).clone();
                                    bought.setAmount(amount);
                                    chestInventory.getItem(0)
                                            .setAmount(chestInventory.getItem(0).getAmount() - amount);
                                    if (chestInventory.getItem(0) == null)
                                        shop.setLine(3, "Keine Items!");
                                    user.getInventory().addItem(bought);
                                    ItemSorter.sort(chestInventory);

                                    user.playSound(user.getEyeLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                                            SoundCategory.AMBIENT, 0.5f, 1.5f);
                                    user.sendMessage(Main.PREFIX + "§aDu hast \"" + name + "§a\" x" + amount +
                                            "§a für \"" + cost + "§a €\" gekauft");
                                } else
                                    user.sendMessage(Main.PREFIX + "Dein Inventar ist voll!");
                            else
                                user.sendMessage(Main.PREFIX + "Dazu hast du nicht genug Geld!");
                        else
                            user.sendMessage(Main.PREFIX + "Es ist ein Fehler passiert! Probiere es in 10 " +
                                    "Sekunden nochmal!");
                    else
                        user.sendMessage(Main.PREFIX + "Sag dem Besitzer, das der Shop nachgefüllt werden muss!");
                else
                    user.sendMessage(Main.PREFIX + "Sag dem Besitzer, das der Shop nachgefüllt werden muss!");
            else
                user.sendMessage(Main.PREFIX + "Sag dem Besitzer, das der Shop kaputt ist!");
        else
            user.sendMessage(Main.PREFIX + "Sag dem Besitzer, das der Shop kaputt ist!");
    }

    public static void cleanUpShops() {
        List<Block> registeredSigns = SignShopManager.registeredSigns;
        List<Block> registeredChests = SignShopManager.registeredChests;

        for (Block block : registeredSigns)
            if (!Tag.WALL_SIGNS.isTagged(block.getType()))
                unregisterShop(block);
        for (Block block : registeredChests)
            if (!(block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST))
                unregisterShop(chestSignMap.get(block));
            else {
                Bukkit.getScheduler().runTask(Main.main, () -> {
                    Chest chest = (Chest) block.getState();
                    Sign sign = (Sign) chestSignMap.get(chest.getBlock()).getState();

                    if (SignListener.isInventoryClear(chest.getInventory())) {
                        System.out.println(0);
                        sign.setLine(3, "Keine Items!");
                    } else if (!SignListener.containsInventoryOneItem(chest.getInventory())) {
                        System.out.println(1);
                        sign.setLine(3, "Error!");
                    } else {
                        System.out.println(2);
                        ItemStack firstItem = chest.getInventory().getItem(0);
                        if (firstItem.hasItemMeta())
                            sign.setLine(3, firstItem.getItemMeta().getDisplayName());
                        else
                            sign.setLine(3, firstItem.getType().getKey().getKey().replaceAll("_",
                                    " "));
                    }
                    sign.update();
                });
            }
    }

    private static List<String> blocksToStrings(Iterable<Block> blockList) {
        List<String> stringList = new ArrayList<>();
        for (Block block : blockList) {
            StringBuilder string = new StringBuilder();
            string.append(block.getWorld().getName()).append(":");
            string.append(block.getX()).append(":");
            string.append(block.getY()).append(":");
            string.append(block.getZ());
            stringList.add(string.toString());
        }
        return stringList;
    }

    private static List<Block> stringsToBlocks(Iterable<String> stringList) {
        List<Block> blockList = new ArrayList<>();
        for (String string : stringList) {
            World world = Bukkit.getWorld(string.split(":")[0]);
            if (world != null) {
                int x = Integer.parseInt(string.split(":")[1]);
                int y = Integer.parseInt(string.split(":")[2]);
                int z = Integer.parseInt(string.split(":")[3]);
                Block block = world.getBlockAt(x, y, z);
                blockList.add(block);
            }
        }
        return blockList;
    }

    @NotNull
    private static <T, E> HashMap<T, E> listsToMap(@NotNull List<T> keyList,
                                                   @NotNull List<E> valueList) {
        if (keyList.size() != valueList.size()) return null;
        HashMap<T, E> hashMap = new HashMap<>();
        for (int i = 0; i < keyList.size(); i++)
            hashMap.put(keyList.get(i), valueList.get(i));
        return hashMap;
    }
}