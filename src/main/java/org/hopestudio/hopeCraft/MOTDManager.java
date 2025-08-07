package org.hopestudio.hopeCraft;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class MOTDManager {
    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration motdConfig;
    private String currentMotd;

    public MOTDManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupConfig();
    }

    private void setupConfig() {
        configFile = new File(plugin.getDataFolder(), "motd.yml");
        if (!configFile.exists()) {
            plugin.saveResource("motd.yml", false);
            plugin.getLogger().info("已创建默认MOTD配置文件");
        }
        reloadConfig();
    }

    public void reloadConfig() {
        motdConfig = YamlConfiguration.loadConfiguration(configFile);
        currentMotd = motdConfig.getString("motd", "§6欢迎来到HopeCraft服务器§e | 版本1.21+");
        applyMotd();
    }

    public void setMotd(String newMotd) {
        currentMotd = newMotd;
        motdConfig.set("motd", newMotd);
        saveConfig();
        applyMotd();
        if (newMotd.length() > 256) {
            plugin.getLogger().warning("MOTD超过256字符限制，将被截断");
            newMotd = newMotd.substring(0, 256);
        }
    }

    public String getMotd() {
        return currentMotd;
    }

    private void applyMotd() {
        Bukkit.getServer().setMotd(currentMotd);
        plugin.getLogger().info("MOTD已更新: " + currentMotd.replace("§", "&"));
    }

    private void saveConfig() {
        try {
            motdConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存MOTD配置失败: " + e.getMessage());
        }
    }
}