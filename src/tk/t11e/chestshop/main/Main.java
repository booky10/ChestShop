package tk.t11e.chestshop.main;
// Created by booky10 in ChestShop (21:21 06.04.20)

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import tk.t11e.chestshop.listener.SignListener;
import tk.t11e.chestshop.manager.SignShopManager;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Main extends JavaPlugin {

    public static final String PREFIX = "§7[§bCraftTMB§7]§c ";
    private final File shopSavesFile = new File(getDataFolder(), "chestSaves.yml");
    private FileConfiguration shopSaves;
    public static Economy economy;
    public static Main main;

    @Override
    public void onEnable() {
        main = this;
        setupEconomy();

        getDataFolder().mkdirs();
        try {
            if (!shopSavesFile.exists())
                shopSavesFile.createNewFile();
        } catch (IOException exception) {
            getLogger().severe(exception.toString());
        }
        shopSaves = new YamlConfiguration();

        Bukkit.getScheduler().runTaskLaterAsynchronously(this,
                () -> SignShopManager.loadShops(YamlConfiguration.loadConfiguration(shopSavesFile)), 20);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, SignShopManager::cleanUpShops,
                20 * 5/*seconds*/, 20 * 10/*seconds*/);

        Bukkit.getPluginManager().registerEvents(new SignListener(), this);
    }

    @Override
    public void onDisable() {
        SignShopManager.saveShops(getShopSaves());
        saveShopSaves();
    }

    public File getShopSavesFile() {
        return shopSavesFile;
    }

    public FileConfiguration getShopSaves() {
        return shopSaves;
    }

    public void saveShopSaves() {
        try {
            getShopSaves().save(getShopSavesFile());
        } catch (IOException exception) {
            getLogger().severe(exception.toString());
        }
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)
            economy = economyProvider.getProvider();
    }
}