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
import org.jetbrains.annotations.NotNull;

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

    // === ShiftAndF 功能整合 ===============================
    boolean shiftFEnabled = true;
    boolean hasPapi;
    List<String> shiftFCommands;
    // ====================================================

    @Override
    public void onEnable() {
        // === ShiftAndF 初始化 ===
        shiftFCommands = new LinkedList<>();
        initShiftFConfig();

        // ===== 原 HopeCraft 初始化 =====
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
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        // 初始化数据库
        initDatabase();
        say("插件HopeCraft已启用！");
    }

    @Override
    public void onDisable() {
        // === ShiftAndF 清理 ===
        shiftFCommands.clear();

        // === 原 HopeCraft 清理 ===
        // 保存所有在线玩家的统计数据
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerStats(player.getUniqueId());
        }
        
        // 安全关闭数据库
        try {
            if (connection != null && !connection.isClosed()) {
                // 执行一次检查点以确保所有数据都写入磁盘
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA wal_checkpoint(FULL)");
                }
                connection.close();
                getLogger().info("数据库连接已安全关闭");
            }
        } catch (SQLException e) {
            getLogger().severe("数据库关闭异常: " + e.getMessage());
            e.printStackTrace();
        }
        say("插件HopeCraft已禁用！");
    }

    // === ShiftAndF 配置处理 ===============================
    private void initShiftFConfig() {
        // 加载配置文件
        saveDefaultConfig();
        shiftFEnabled = getConfig().getBoolean("shiftF-enabled", true);

        // 加载命令列表
        loadCmd();

        // 检查PAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            hasPapi = true;
            getLogger().info("§9检测到PlaceholderAPI，启用占位符功能");
        } else {
            hasPapi = false;
            getLogger().info("§9未检测到PlaceholderAPI，禁用占位符功能");
        }
    }

    private void loadCmd() {
        List<String> tmpLst = getConfig().getStringList("shiftF-commands");
        for (String command : tmpLst) {
            if (command == null || command.length() < 2 ||
                    (command.charAt(0) != 'c' && command.charAt(0) != 'p') ||
                    command.charAt(1) != ':') {
                getLogger().warning("§e已忽略错误的shift+F命令格式：" + command);
            } else {
                shiftFCommands.add(command);
            }
        }
    }

    private void shiftFTrigger(Player player) {
        if (!shiftFEnabled || shiftFCommands.isEmpty()) return;
        if (!player.isSneaking()) return;

        for (String command : shiftFCommands) {
            String formattedCmd = formatCommand(command.substring(2), player);
            if (command.charAt(0) == 'c') {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
            } else {
                player.performCommand(formattedCmd);
            }
        }
    }

    private String formatCommand(String command, Player player) {
        if (command == null || command.isEmpty()) {
            return "";
        }

        // 检查 PlaceholderAPI 是否可用
        if (hasPapi && player != null) {
            try {
                // 使用反射调用 PlaceholderAPI 以避免编译依赖问题
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method setPlaceholders = papiClass.getMethod("setPlaceholders", Player.class, String.class);
                command = (String) setPlaceholders.invoke(null, player, command);
            } catch (Exception e) {
                getLogger().warning("PlaceholderAPI 处理失败: " + e.getMessage());
            }
        }
        return command;
    }
    // =====================================

    public void say(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }

    // ========== 数据库相关 ==========
    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(dataFolder, "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                // 启用外键约束（如果需要）
                stmt.execute("PRAGMA foreign_keys = ON");
                
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
            
            getLogger().info("数据库连接成功: " + dbFile.getAbsolutePath());
        } catch (Exception e) {
            getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                getLogger().warning("数据库连接已断开，正在重新连接...");
                initDatabase();
            }
            
            // 测试连接是否有效
            if (!connection.isValid(5)) {
                getLogger().warning("数据库连接无效，正在重新连接...");
                connection.close();
                initDatabase();
            }
        } catch (SQLException e) {
            getLogger().severe("数据库连接检查失败: " + e.getMessage());
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
                boolean canSign = !rs.next() || !today.equals(rs.getString("last_sign_date"));
                getLogger().info("玩家 " + uuid + " 签到检查结果: " + (canSign ? "可以签到" : "今日已签到"));
                return canSign;
            }
        } catch (SQLException e) {
            getLogger().severe("签到检查失败: " + e.getMessage());
            e.printStackTrace();
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
                int result = ps.executeUpdate();
                getLogger().info("玩家 " + player.getName() + " 签到完成，影响行数: " + result);
            }

            // 发放奖励
            giveSignReward(player);
        } catch (SQLException e) {
            getLogger().severe("签到处理失败: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "签到失败，请联系管理员");
            e.printStackTrace();
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
                    getLogger().info("成功加载玩家 " + uuid + " 的统计数据");
                } else {
                    getLogger().info("未找到玩家 " + uuid + " 的统计数据，使用默认值");
                    playerStats.put(uuid, new PlayerStats(0, 0, 0));
                }
            }
        } catch (SQLException e) {
            getLogger().severe("统计加载失败: " + e.getMessage());
            e.printStackTrace();
            // 即使加载失败，也放入默认值以避免空指针异常
            playerStats.put(uuid, new PlayerStats(0, 0, 0));
        }
    }

    private void savePlayerStats(UUID uuid) {
        PlayerStats stats = playerStats.get(uuid);
        if (stats == null) {
            getLogger().warning("尝试保存空统计数据: " + uuid);
            return;
        }

        try {
            ensureConnection();

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO player_stats VALUES (?, ?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, stats.kills);
                ps.setInt(3, stats.blocksBroken);
                ps.setInt(4, stats.deaths);
                int result = ps.executeUpdate();
                getLogger().info("成功保存玩家 " + uuid + " 的统计数据，影响行数: " + result);
            }
        } catch (SQLException e) {
            getLogger().severe("统计保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== 菜单系统 ==========
    private void openMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, "HopeCraft 主菜单");

        // === ShiftAndF 状态按钮 ===
        if(player.hasPermission("hopecraft.admin")) {
            ItemStack shiftFToggle = getItemStack();
            menu.setItem(22, shiftFToggle);  // 菜单中心附近
        }

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

    private @NotNull ItemStack getItemStack() {
        ItemStack shiftFToggle = new ItemStack(
                shiftFEnabled ? Material.LIME_DYE : Material.GRAY_DYE
        );
        ItemMeta shiftFMeta = shiftFToggle.getItemMeta();
        shiftFMeta.setDisplayName(ChatColor.YELLOW + "Shift+F快捷键");
        List<String> shiftFLore = new ArrayList<>();
        shiftFLore.add(ChatColor.GRAY + "状态: " + (shiftFEnabled ?
                ChatColor.GREEN + "已启用" : ChatColor.RED + "已禁用"));
        shiftFLore.add(ChatColor.GRAY + "可执行命令数量: " + ChatColor.GOLD + shiftFCommands.size());
        shiftFLore.add(ChatColor.DARK_GRAY + "点击切换状态");
        shiftFMeta.setLore(shiftFLore);
        shiftFToggle.setItemMeta(shiftFMeta);
        return shiftFToggle;
    }

    // ========== 命令处理 ==========
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String @NotNull [] args) {
        if (cmd.getName().equalsIgnoreCase("hopecraft")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
                return true;
            }

            // 处理子命令
            if (args.length > 0) {
                // === ShiftAndF 子命令 ===
                if (args[0].equalsIgnoreCase("shiftf")) {
                    if (!player.hasPermission("hopecraft.admin")) {
                        player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "用法: /hopecraft shiftf <on|off|reload>");
                        return true;
                    }

                    String subcmd = args[1].toLowerCase();
                    switch (subcmd) {
                        case "on":
                            shiftFEnabled = true;
                            player.sendMessage(ChatColor.GREEN + "Shift+F快捷键已启用");
                            return true;

                        case "off":
                            shiftFEnabled = false;
                            player.sendMessage(ChatColor.GREEN + "Shift+F快捷键已禁用");
                            return true;

                        case "reload":
                            reloadShiftFConfig();
                            player.sendMessage(ChatColor.GREEN + "Shift+F配置已重载");
                            return true;

                        default:
                            player.sendMessage(ChatColor.RED + "无效的命令: /hopecraft shiftf <on|off|reload>");
                            return true;
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "无效的命令: /hopecraft <shiftf>");
                }
                // 其他子命令...
            }

            // 默认打开菜单
            openMainMenu(player);
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("skull")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
                return true;
            }

            if (args.length == 0) {
                giveSkull(player, player.getName()); // 默认获取自己的头颅
            } else {
                giveSkull(player, args[0]); // 获取指定玩家的头颅
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("hopecraft")) {
            if (args.length == 1) {
                // 第一个参数补全
                completions.add("shiftf");
                // 可以添加其他主命令参数
            } else if (args.length == 2 && args[0].equalsIgnoreCase("shiftf")) {
                // /hopecraft shiftf <参数> 的补全
                completions.add("on");
                completions.add("off");
                completions.add("reload");
            }
        }
        
        // 过滤补全结果
        return filterCompletions(completions, args[args.length - 1]);
    }
    
    /**
     * 过滤补全建议，只返回匹配前缀的项
     * @param completions 所有补全建议
     * @param prefix 输入前缀
     * @return 匹配的补全建议
     */
    private List<String> filterCompletions(List<String> completions, String prefix) {
        if (prefix.isEmpty()) {
            return completions;
        }
        
        List<String> result = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(completion);
            }
        }
        return result;
    }

    // === ShiftAndF 重载方法 =====
    private void reloadShiftFConfig() {
        reloadConfig();
        shiftFCommands.clear();

        loadCmd();

        shiftFEnabled = getConfig().getBoolean("shiftF-enabled", true);
        getLogger().info("Shift+F配置已重载，加载命令数: " + shiftFCommands.size());
    }

    public void hasTrigger(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && shiftFEnabled && !shiftFCommands.isEmpty()) {
            shiftFTrigger(player);
            event.setCancelled(true);
        }
    }

    // ========== 内部类 ==========
        private record PlayerStats(int kills, int blocksBroken, int deaths) {

        public PlayerStats addKillStats() {
            return new PlayerStats(kills + 1, blocksBroken, deaths);
        }

        public PlayerStats addBlockBrokenStats() {
            return new PlayerStats(kills, blocksBroken + 1, deaths);
        }

        public PlayerStats addDeathStats() {
            return new PlayerStats(kills, blocksBroken, deaths + 1);
        }
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

                // === ShiftAndF 菜单按钮 ===
                case 22:
                    shiftFEnabled = !shiftFEnabled;
                    player.sendMessage(ChatColor.GREEN + "Shift+F快捷键: " +
                            (shiftFEnabled ? "已启用" : "已禁用"));
                    openMainMenu(player); // 刷新菜单显示新状态
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
                
                // 实时更新计分板
                if (killer.getScoreboard().getObjective("stats") != null) {
                    showPlayerStats(killer);
                }
            }
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            Player player = event.getPlayer();
            playerStats.compute(player.getUniqueId(), (uuid, stats) ->
                    stats == null ? new PlayerStats(0, 1, 0) : stats.addBlockBrokenStats());
            savePlayerStats(player.getUniqueId());
            
            // 实时更新计分板
            if (player.getScoreboard().getObjective("stats") != null) {
                showPlayerStats(player);
            }
        }

        @EventHandler
        public void onDeath(PlayerDeathEvent event) {
            Player player = event.getEntity();
            playerStats.compute(player.getUniqueId(), (uuid, stats) ->
                    stats == null ? new PlayerStats(0, 0, 1) : stats.addDeathStats());
            savePlayerStats(player.getUniqueId());
            
            // 实时更新计分板
            if (player.getScoreboard().getObjective("stats") != null) {
                showPlayerStats(player);
            }
        }
    }

    // ========== 快捷键监听器 ==========
    private class KeyListener implements Listener {
        @EventHandler(priority = EventPriority.HIGH)
        public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
            Player player = event.getPlayer();

            // 调试日志
            getLogger().info("检测到玩家交换手部物品: " + player.getName() +
                    ", 潜行状态: " + player.isSneaking());

            // 检查是否满足触发条件
            if (player.isSneaking() && shiftFEnabled && !shiftFCommands.isEmpty()) {
                getLogger().info("触发 Shift+F 快捷键: " + player.getName());

                // 执行命令
                shiftFTrigger(player);

                // 取消事件以防止物品交换
                event.setCancelled(true);
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

        obj.getScore(ChatColor.GREEN + "击杀数: " + ChatColor.WHITE).setScore(stats.kills());
        obj.getScore(ChatColor.BLUE + "挖掘数: " + ChatColor.WHITE).setScore(stats.blocksBroken());
        obj.getScore(ChatColor.RED + "死亡数: " + ChatColor.WHITE).setScore(stats.deaths());

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
    //debug


}
//神必彩蛋