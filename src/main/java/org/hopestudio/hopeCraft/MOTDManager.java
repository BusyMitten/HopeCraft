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
        
        // 加载双层MOTD配置
        line1 = motdConfig.getString("motd.line1", "§6欢迎来到HopeCraft服务器");
        line2 = motdConfig.getString("motd.line2", "§e插件升级到1.6.8 | 每日签到福利拿到手软！");
        
        // 兼容旧版配置
        if (motdConfig.contains("motd") && !motdConfig.contains("motd.line1")) {
            currentMotd = motdConfig.getString("motd");
            // 尝试分割旧版MOTD为两行
            if (currentMotd != null && currentMotd.contains("|")) {
                String[] parts = currentMotd.split("\\|", 2);
                line1 = parts[0].trim();
                line2 = parts.length > 1 ? parts[1].trim() : "";
            } else {
                line1 = currentMotd;
                line2 = "";
            }
            // 保存为新格式
            setMotd(line1, line2);
        }
        
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
        // 组合双层MOTD
        currentMotd = line1 + "\n" + line2;
        Bukkit.getServer().setMotd(currentMotd);
        plugin.getLogger().info("MOTD已更新: ");
        plugin.getLogger().info("第一行: " + line1.replace("§", "&"));
        plugin.getLogger().info("第二行: " + line2.replace("§", "&"));
    }

    private void saveConfig() {
        try {
            motdConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存MOTD配置失败: " + e.getMessage());
        }
    }
    private String line1; // 第一行MOTD
    private String line2; // 第二行MOTD



    public void setMotd(String newLine1, String newLine2) {
        line1 = newLine1;
        line2 = newLine2;
        motdConfig.set("motd.line1", line1.replace("§", "&"));
        motdConfig.set("motd.line2", line2.replace("§", "&"));
        saveConfig();
        applyMotd();

        // 长度校验（单行限制+总长度限制）
        if (newLine1.length() > 60) {
            plugin.getLogger().warning("MOTD第一行超过60字符限制，显示可能不全");
        }
        if (newLine2.length() > 60) {
            plugin.getLogger().warning("MOTD第二行超过60字符限制，显示可能不全");
        }
        if ((newLine1 + newLine2).length() > 256) {
            plugin.getLogger().warning("MOTD总长度超过256字符限制");
        }
    }



    // 新增获取单行方法
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }

}
