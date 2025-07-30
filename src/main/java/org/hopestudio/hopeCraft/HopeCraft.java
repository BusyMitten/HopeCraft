package org.hopestudio.hopeCraft;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HopeCraft extends JavaPlugin {

    private Connection connection;
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private File dataFolder;

    @Override
    public void onEnable() {
        // 初始化数据目录
        dataFolder = getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        // 安全注册命令
        PluginCommand cmd = getCommand("hopecraft");
        if (cmd == null) {
            getLogger().severe("命令注册失败！请检查plugin.yml配置");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);

        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);
        Bukkit.getPluginManager().registerEvents(new StatsListener(), this);
        Bukkit.getPluginManager().registerEvents(new KeyListener(), this);

        // 初始化数据库
        initDatabase();
        say("插件HopeCraft已启用！");
    }

    @Override
    public void onDisable() {
        // 安全关闭数据库
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            getLogger().warning("数据库关闭异常: " + e.getMessage());
        }
        say("插件HopeCraft已禁用！");
    }

    public void say(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }

    // ========== 数据库相关 ==========
    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/data.db");

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS sign_ins (" +
                        "player_uuid TEXT PRIMARY KEY, " +
                        "last_sign_date TEXT, " +
                        "streak_days INTEGER DEFAULT 0)");

                stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                        "player_uuid TEXT PRIMARY KEY, " +
                        "kills INTEGER DEFAULT 0, " +
                        "blocks_broken INTEGER DEFAULT 0, " +
                        "deaths INTEGER DEFAULT 0)");
            }
        } catch (Exception e) {
            getLogger().severe("数据库初始化失败: " + e.getMessage());
        }
    }

    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initDatabase();
        }
    }

    // ========== 签到系统 ==========
    private boolean canSignToday(UUID uuid) {
        try {
            ensureConnection();
            String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT last_sign_date FROM sign_ins WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return !rs.next() || !today.equals(rs.getString("last_sign_date"));
            }
        } catch (SQLException e) {
            getLogger().warning("签到检查失败: " + e.getMessage());
            return false;
        }
    }

    private void processSignIn(Player player) {
        UUID uuid = player.getUniqueId();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        try {
            ensureConnection();

            // 更新签到数据
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO sign_ins (player_uuid, last_sign_date, streak_days) " +
                            "VALUES (?, ?, COALESCE((SELECT streak_days FROM sign_ins WHERE player_uuid = ?), 0) + 1)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, today);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            }

            // 发放奖励
            giveSignReward(player);
        } catch (SQLException e) {
            getLogger().warning("签到处理失败: " + e.getMessage());
        }
    }

    private void giveSignReward(Player player) {
        Random random = new Random();
        Map<Material, Integer> rewards = Map.of(
                Material.DIAMOND, random.nextInt(5) + 1,
                Material.EMERALD, random.nextInt(3) + 1,
                Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, random.nextInt(7) + 1
        );

        // 尝试添加到背包
        for (Map.Entry<Material, Integer> entry : rewards.entrySet()) {
            ItemStack item = new ItemStack(entry.getKey(), entry.getValue());
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(item);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        player.sendMessage(ChatColor.GREEN + "签到成功！奖励已发放");
    }

    // ========== 统计系统 ==========
    private void loadPlayerStats(UUID uuid) {
        try {
            ensureConnection();

            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT kills, blocks_broken, deaths FROM player_stats WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    playerStats.put(uuid, new PlayerStats(
                            rs.getInt("kills"),
                            rs.getInt("blocks_broken"),
                            rs.getInt("deaths")
                    ));
                }
            }
        } catch (SQLException e) {
            getLogger().warning("统计加载失败: " + e.getMessage());
        }
    }

    private void savePlayerStats(UUID uuid) {
        PlayerStats stats = playerStats.get(uuid);
        if (stats == null) return;

        try {
            ensureConnection();

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO player_stats VALUES (?, ?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, stats.kills);
                ps.setInt(3, stats.blocksBroken);
                ps.setInt(4, stats.deaths);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            getLogger().warning("统计保存失败: " + e.getMessage());
        }
    }

    // ========== 菜单系统 ==========
    private void openMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, "HopeCraft 主菜单");

        // 传送按钮
        ItemStack teleportItem = new ItemStack(Material.COMPASS);
        ItemMeta teleportMeta = teleportItem.getItemMeta();
        teleportMeta.setDisplayName(ChatColor.GREEN + "传送到主城");
        teleportMeta.setLore(List.of(ChatColor.GRAY + "点击传送到服务器主城"));
        teleportItem.setItemMeta(teleportMeta);
        menu.setItem(10, teleportItem);

        // 签到按钮
        ItemStack signItem = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta signMeta = signItem.getItemMeta();
        signMeta.setDisplayName(ChatColor.GOLD + "每日签到");
        signMeta.setLore(List.of(
                ChatColor.GRAY + "点击领取每日奖励",
                canSignToday(player.getUniqueId()) ?
                        ChatColor.GREEN + "可签到" :
                        ChatColor.RED + "今日已签到"
        ));
        signItem.setItemMeta(signMeta);
        menu.setItem(13, signItem);

        // 统计按钮
        ItemStack statsItem = new ItemStack(Material.CHERRY_HANGING_SIGN);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(ChatColor.BLUE + "我的统计");
        statsMeta.setLore(List.of(ChatColor.GRAY + "查看你的游戏统计"));
        statsItem.setItemMeta(statsMeta);
        menu.setItem(16, statsItem);

        // 烟花按钮
        ItemStack fireworkItem = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta fireworkMeta = fireworkItem.getItemMeta();
        fireworkMeta.setDisplayName(ChatColor.RED + "发射烟花");
        fireworkMeta.setLore(List.of(ChatColor.GRAY + "点击发射烟花"));
        fireworkItem.setItemMeta(fireworkMeta);
        menu.setItem(3, fireworkItem);

        // 获取头颅按钮
        ItemStack skullItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta skullMeta = skullItem.getItemMeta();
        skullMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "获取头颅");
        skullMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "点击获取玩家头颅",
                ChatColor.YELLOW + "输入 /skull <玩家名> 获取"
        ));
        skullItem.setItemMeta(skullMeta);
        menu.setItem(1, skullItem); // 放在第1格

        player.openInventory(menu);
    }

    // ========== 命令处理 ==========
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("hopecraft")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
                return true;
            }
            Player player = (Player) sender;
            openMainMenu(player);
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("skull")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
                return true;
            }
            Player player = (Player) sender;

            if (args.length == 0) {
                giveSkull(player, player.getName()); // 默认获取自己的头颅
            } else {
                giveSkull(player, args[0]); // 获取指定玩家的头颅
            }
            return true;
        }
        return false;
    }

    // ========== 内部类 ==========
    private static class PlayerStats {
        private int kills;
        private int blocksBroken;
        private int deaths;

        public PlayerStats(int kills, int blocksBroken, int deaths) {
            this.kills = kills;
            this.blocksBroken = blocksBroken;
            this.deaths = deaths;
        }

        public int getKills() { return kills; }
        public int getBlocksBroken() { return blocksBroken; }
        public int getDeaths() { return deaths; }

        public PlayerStats addKillStats() { return new PlayerStats(kills + 1, blocksBroken, deaths); }
        public PlayerStats addBlockBrokenStats() { return new PlayerStats(kills, blocksBroken + 1, deaths); }
        public PlayerStats addDeathStats() { return new PlayerStats(kills, blocksBroken, deaths + 1); }
    }

    private class InventoryListener implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (!event.getView().getTitle().equals("HopeCraft 主菜单")) return;

            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            switch (event.getRawSlot()) {
                case 10: // 传送
                    player.teleport(Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation());
                    player.sendMessage(ChatColor.GREEN + "已传送到主城！");
                    player.closeInventory();
                    break;

                case 13: // 签到
                    if (canSignToday(player.getUniqueId())) {
                        processSignIn(player);
                    } else {
                        player.sendMessage(ChatColor.RED + "你今天已经签到过了！");
                    }
                    player.closeInventory();
                    break;

                case 16: // 统计
                    showPlayerStats(player);
                    player.closeInventory();
                    break;

                case 3: // 烟花
                    player.performCommand("fireworks");
                    player.sendMessage(ChatColor.RED + "烟花已发射！");
                    player.closeInventory();
                    break;

                case 1: // 获取头颅
                    giveSkull(player, player.getName());
                    player.sendMessage(ChatColor.GREEN + "已获取你的头颅！输入 /skull <玩家名> 可获取其他玩家头颅");
                    player.closeInventory();
                    break;
            }
        }
    }

    private class StatsListener implements Listener {
        @EventHandler
        public void onKill(EntityDeathEvent event) {
            Player killer = event.getEntity().getKiller();
            if (killer != null) {
                playerStats.compute(killer.getUniqueId(), (uuid, stats) ->
                        stats == null ? new PlayerStats(1, 0, 0) : stats.addKillStats());
                savePlayerStats(killer.getUniqueId());
            }
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            Player player = event.getPlayer();
            playerStats.compute(player.getUniqueId(), (uuid, stats) ->
                    stats == null ? new PlayerStats(0, 1, 0) : stats.addBlockBrokenStats());
            savePlayerStats(player.getUniqueId());
        }

        @EventHandler
        public void onDeath(PlayerDeathEvent event) {
            Player player = event.getEntity();
            playerStats.compute(player.getUniqueId(), (uuid, stats) ->
                    stats == null ? new PlayerStats(0, 0, 1) : stats.addDeathStats());
            savePlayerStats(player.getUniqueId());
        }
    }

    // ========== 修复的快捷键监听器 ==========
    private class KeyListener implements Listener {
        private final Set<UUID> shiftPressed = new HashSet<>();
        private final Map<UUID, Long> lastPressTime = new ConcurrentHashMap<>();

        @EventHandler
        public void onShiftPress(PlayerToggleSneakEvent event) {
            UUID uuid = event.getPlayer().getUniqueId();
            if (event.isSneaking()) {
                shiftPressed.add(uuid);
            } else {
                shiftPressed.remove(uuid);
            }
        }

        @EventHandler
        public void onSlotChange(PlayerItemHeldEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            // 检查Shift+F组合键
            if (shiftPressed.contains(uuid) && event.getNewSlot() == 4) {
                long now = System.currentTimeMillis();
                if (now - lastPressTime.getOrDefault(uuid, 0L) < 500) return;

                lastPressTime.put(uuid, now);

                // 延迟执行命令以避免与副手交互冲突
                Bukkit.getScheduler().runTaskLater(HopeCraft.this, () -> {
                    if (shiftPressed.contains(uuid)) {
                        openMainMenu(player);
                        shiftPressed.remove(uuid);
                    }
                }, 1L);
            }
        }
    }

    // ========== 辅助方法 ==========
    private void showPlayerStats(Player player) {
        UUID uuid = player.getUniqueId();
        loadPlayerStats(uuid);
        PlayerStats stats = playerStats.getOrDefault(uuid, new PlayerStats(0, 0, 0));

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("stats", "dummy",
                ChatColor.YELLOW + "" + ChatColor.BOLD + player.getName() + " 的统计");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore(ChatColor.GREEN + "击杀数: " + ChatColor.WHITE).setScore(stats.getKills());
        obj.getScore(ChatColor.BLUE + "挖掘数: " + ChatColor.WHITE).setScore(stats.getBlocksBroken());
        obj.getScore(ChatColor.RED + "死亡数: " + ChatColor.WHITE).setScore(stats.getDeaths());

        player.setScoreboard(board);
    }

    // ========== 获取头颅功能 ==========
    private void giveSkull(Player player, String targetName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) {
            player.sendMessage(ChatColor.RED + "获取头颅失败！无法创建头颅元数据");
            return;
        }

        try {
            // 获取离线玩家
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);

            // 检查玩家是否存在
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                player.sendMessage(ChatColor.RED + "玩家 " + targetName + " 不存在或从未登录过服务器");
                return;
            }

            // 设置头颅所有者
            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + offlinePlayer.getName() + "的头颅");
            skull.setItemMeta(meta);

            // 添加到玩家背包
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(skull);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), skull);
            }
            player.sendMessage(ChatColor.GREEN + "成功获取 " + offlinePlayer.getName() + " 的头颅！");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "获取头颅失败！错误: " + e.getMessage());
            getLogger().warning("获取头颅失败: " + e.getMessage());
        }
    }
}