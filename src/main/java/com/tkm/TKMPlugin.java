package com.tkm;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import com.tkm.command.TKMCommand;
import com.tkm.listener.ChatListener;
import com.tkm.listener.GUIListener;
import com.tkm.listener.QuitListener;
import com.tkm.manager.GameManager;

public class TKMPlugin extends JavaPlugin {

    private static TKMPlugin instance;
    private Economy economy;
    private GameManager gameManager;
    private ChatListener chatListener;

    public static TKMPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault veya ekonomi eklentisi bulunamadı! Plugin kapanıyor.");
            getLogger().severe("Lütfen Vault + Essentials gibi bir ekonomi eklentisi kurun.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        gameManager = new GameManager(this);

        TKMCommand cmd = new TKMCommand(this);
        getCommand("tkm").setExecutor(cmd);
        getCommand("tkm").setTabCompleter(cmd);

        chatListener = new ChatListener(this);
        Bukkit.getPluginManager().registerEvents(chatListener, this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(this), this);

        getLogger().info("TKM aktif! /tkm ile oynanabilir.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.refundAll("Sunucu kapanıyor");
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public void reload() {
        reloadConfig();
    }

    public Economy getEconomy() { return economy; }
    public GameManager getGameManager() { return gameManager; }
    public ChatListener getChatListener() { return chatListener; }
}
