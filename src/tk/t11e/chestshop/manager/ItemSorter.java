package tk.t11e.chestshop.manager;
// Created by booky10 in EssentialsT (16:06 14.02.20)

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class ItemSorter {

    public static void sort(Inventory inventory) {
        int from = 0, size = inventory.getSize();
        if (inventory.getType() == InventoryType.PLAYER) {
            from = 9;
            size = 27;
        }

        ItemStack[] contents = inventory.getContents();
        ItemStack[] storage = new ItemStack[size];
        System.arraycopy(contents, from, storage, 0, size);

        Arrays.sort(storage, (item1, item2) -> {
            if (Objects.equals(item1, item2))
                return 0;

            if (isEmptyItem(item1))
                return 1;
            if (isEmptyItem(item2))
                return -1;

            int diff = item1.getType().compareTo(item2.getType());
            if (diff != 0)
                return diff;

            diff = Objects.requireNonNull(item1.getData()).getData() - Objects.requireNonNull(item2.getData()).getData();
            if (diff != 0)
                return diff;

            ItemMeta meta1 = item1.getItemMeta();
            ItemMeta meta2 = item2.getItemMeta();
            String name1 = meta1.hasDisplayName() ? meta1.getDisplayName() : "";
            String name2 = meta2.hasDisplayName() ? meta2.getDisplayName() : "";

            diff = name1.compareTo(name2);
            if (diff != 0)
                return diff;

            name1 = meta1.hasLocalizedName() ? meta1.getLocalizedName() : "";
            name2 = meta2.hasLocalizedName() ? meta2.getLocalizedName() : "";

            diff = name1.compareTo(name2);
            if (diff != 0)
                return diff;

            return item1.getDurability() - item2.getDurability();
        });

        Inventory vaporChest = Bukkit.createInventory(null, (size += 8) - (size % 9));
        for (ItemStack item : storage)
            if (isEmptyItem(item))
                break;
            else
                vaporChest.addItem(item);

        System.arraycopy(vaporChest.getContents(), 0, contents, from, size - 8);
        inventory.setContents(contents);
    }

    private static boolean isEmptyItem(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }
}