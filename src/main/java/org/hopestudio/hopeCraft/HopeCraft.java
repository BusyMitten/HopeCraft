package org.hopestudio.hopeCraft;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.security.Key;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class HopeCraft extends JavaPlugin {

    private Connection connection;
    // 添加统计信息存储Map
    private Map<UUID, PlayerStats> playerStats = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        say("插件HopeCraft已启用！");
        Objects.requireNonNull(getCommand("hopecraft")).setExecutor(this);
        Objects.requireNonNull(getCommand("hopecraft")).setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(), this);
        // 注册新的监听器用于统计
        Bukkit.getPluginManager().registerEvents(new StatsListener(), this);
        Bukkit.getPluginManager().registerEvents(new KeyPressListener(), this);
        // 注册新的监听器用于统计
        Bukkit.getPluginManager().registerEvents(new StatsListener(), this);
        // 注册按键监听器
        Bukkit.getPluginManager().registerEvents(new KeyPressListener(), this);
        // 初始化数据库
        initDatabase();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        say("插件HopeCraft已禁用！");
        
        // 关闭数据库连接
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void say(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }

    // 初始化数据库
    private void initDatabase() {
        try {
            // 确保数据文件夹存在
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            
            // 使用SQLite数据库
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder().getAbsolutePath() + "/sign_in.db");
            
            // 创建签到表
            String createTableSQL = "CREATE TABLE IF NOT EXISTS sign_ins (" +
                    "player_uuid TEXT PRIMARY KEY, " +
                    "last_sign_in_date TEXT NOT NULL)";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
            }
            
            // 创建玩家统计表
            String createStatsTableSQL = "CREATE TABLE IF NOT EXISTS player_stats (" +
                    "player_uuid TEXT PRIMARY KEY, " +
                    "kills INTEGER DEFAULT 0, " +
                    "blocks_broken INTEGER DEFAULT 0, " +
                    "deaths INTEGER DEFAULT 0)";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createStatsTableSQL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 确保数据库连接有效
    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initDatabase();
        }
        
        // 如果仍然无法建立连接，则抛出异常
        if (connection == null || connection.isClosed()) {
            throw new SQLException("无法建立数据库连接");
        }
    }

    // 检查玩家是否今日已签到
    private boolean hasSignedInToday(UUID playerUUID) {
        try {
            ensureConnection();
            String selectSQL = "SELECT last_sign_in_date FROM sign_ins WHERE player_uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
                pstmt.setString(1, playerUUID.toString());
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    String lastSignInDate = rs.getString("last_sign_in_date");
                    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                    return today.equals(lastSignInDate);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 执行签到
    private void signIn(UUID playerUUID) {
        try {
            ensureConnection();
            String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            
            // 检查玩家是否已存在签到记录
            String selectSQL = "SELECT player_uuid FROM sign_ins WHERE player_uuid = ?";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSQL)) {
                selectStmt.setString(1, playerUUID.toString());
                ResultSet rs = selectStmt.executeQuery();
                
                if (rs.next()) {
                    // 更新签到日期
                    String updateSQL = "UPDATE sign_ins SET last_sign_in_date = ? WHERE player_uuid = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                        updateStmt.setString(1, today);
                        updateStmt.setString(2, playerUUID.toString());
                        updateStmt.executeUpdate();
                    }
                } else {
                    // 插入新记录
                    String insertSQL = "INSERT INTO sign_ins (player_uuid, last_sign_in_date) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                        insertStmt.setString(1, playerUUID.toString());
                        insertStmt.setString(2, today);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 加载玩家统计数据
    private void loadPlayerStats(UUID playerUUID) {
        try {
            ensureConnection();
            String selectSQL = "SELECT kills, blocks_broken, deaths FROM player_stats WHERE player_uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
                pstmt.setString(1, playerUUID.toString());
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    PlayerStats stats = new PlayerStats(
                        rs.getInt("kills"),
                        rs.getInt("blocks_broken"),
                        rs.getInt("deaths")
                    );
                    playerStats.put(playerUUID, stats);
                } else {
                    // 如果没有记录，创建默认记录
                    PlayerStats stats = new PlayerStats(0, 0, 0);
                    playerStats.put(playerUUID, stats);
                    
                    String insertSQL = "INSERT OR IGNORE INTO player_stats (player_uuid, kills, blocks_broken, deaths) VALUES (?, 0, 0, 0)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                        insertStmt.setString(1, playerUUID.toString());
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 保存玩家统计数据
    private void savePlayerStats(UUID playerUUID) {
        try {
            ensureConnection();
            PlayerStats stats = playerStats.get(playerUUID);
            if (stats != null) {
                String updateSQL = "UPDATE player_stats SET kills = ?, blocks_broken = ?, deaths = ? WHERE player_uuid = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
                    pstmt.setInt(1, stats.getKills());
                    pstmt.setInt(2, stats.getBlocksBroken());
                    pstmt.setInt(3, stats.getDeaths());
                    pstmt.setString(4, playerUUID.toString());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 增加击杀数
    public void addKill(UUID playerUUID) {
        PlayerStats stats = playerStats.get(playerUUID);
        if (stats != null) {
            stats.addKill();
            savePlayerStats(playerUUID);
        }
    }

    // 增加挖掘方块数
    public void addBlockBroken(UUID playerUUID) {
        PlayerStats stats = playerStats.get(playerUUID);
        if (stats != null) {
            stats.addBlockBroken();
            savePlayerStats(playerUUID);
        }
    }

    // 增加死亡数
    public void addDeath(UUID playerUUID) {
        PlayerStats stats = playerStats.get(playerUUID);
        if (stats != null) {
            stats.addDeath();
            savePlayerStats(playerUUID);
        }
    }

    // 获取玩家统计数据
    public PlayerStats getPlayerStats(UUID playerUUID) {
        return playerStats.get(playerUUID);
    }

    // 显示玩家信息计分板
    private void showPlayerStats(Player player) {
        // 加载玩家统计数据
        loadPlayerStats(player.getUniqueId());
        
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("stats", "dummy", "§e§l"+player.getName()+"§e§l的信息");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // 获取玩家统计数据
        PlayerStats stats = getPlayerStats(player.getUniqueId());
        int kills = stats != null ? stats.getKills() : 0;
        int blocksBroken = stats != null ? stats.getBlocksBroken() : 0;
        int deaths = stats != null ? stats.getDeaths() : 0;

        // 设置统计信息
        objective.getScore("§b§l击杀生物数: §f").setScore(kills);
        objective.getScore("§6§l挖掘方块数: §f").setScore(blocksBroken);
        objective.getScore("§c§l死亡数: §f").setScore(deaths);

        player.setScoreboard(scoreboard);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("hopecraft")) {
            // 判断是否是玩家
            if(!(sender instanceof Player p)) {
                sender.sendMessage("§c只用玩家才能执行此命令！");
                return false;
            }
            // 打开容器
            Inventory inv = Bukkit.createInventory(null, 54, "HopeCraft面板");
            p.openInventory(inv);
            // 传送到出生点
            ItemStack is = new ItemStack(Material.COMPASS);
            ItemMeta im = is.getItemMeta();
            im.setDisplayName("§a点我传送到主城");
            im.setLore(List.of("传送"));
            is.setItemMeta(im);
            inv.addItem(is);
            inv.setItem(0, is);
            // 签到
            is = new ItemStack(Material.GOLD_BLOCK);
            im = is.getItemMeta();
            im.setDisplayName("§e签到");
            im.setLore(List.of("点我签到"));
            is.setItemMeta(im);
            inv.addItem(is);
            inv.setItem(1, is);
            // 计分板
            is = new ItemStack(Material.CHERRY_HANGING_SIGN);
            im = is.getItemMeta();
            im.setDisplayName("§a我的信息");
            im.setLore(List.of("点我查看我的信息"));
            is.setItemMeta(im);
            inv.addItem(is);
            inv.setItem(2, is);
            // 烟花
            is = new ItemStack(Material.FIREWORK_ROCKET);
            im = is.getItemMeta();
            im.setDisplayName("§a点我发射烟花");
            im.setLore(List.of("emm……别炸到人了"));
            is.setItemMeta(im);
            inv.addItem(is);
            inv.setItem(3, is);
            return true;
        }
        return true;
    }

    // 玩家统计数据类
    public static class PlayerStats {
        private int kills;
        private int blocksBroken;
        private int deaths;

        public PlayerStats(int kills, int blocksBroken, int deaths) {
            this.kills = kills;
            this.blocksBroken = blocksBroken;
            this.deaths = deaths;
        }

        public int getKills() {
            return kills;
        }

        public int getBlocksBroken() {
            return blocksBroken;
        }

        public int getDeaths() {
            return deaths;
        }

        public void addKill() {
            kills++;
        }

        public void addBlockBroken() {
            blocksBroken++;
        }

        public void addDeath() {
            deaths++;
        }
    }

    public static class InventoryClickListener implements Listener {
        @EventHandler
        public void onClick(InventoryClickEvent e) {
            // 修改: if (e.getInventory().getTitle().equals("HopeCraft面板") && e.getCurrentItem() != null) {
            if (e.getView().getTitle().equals("HopeCraft面板") && e.getCurrentItem() != null) {
                if(e.getRawSlot()==0) {
                    Player p = (Player) e.getWhoClicked();
                    p.teleport(Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation());
                    p.closeInventory();
                    e.setCancelled(true);
                    p.sendMessage("§a已传送到主城！");
                }
                else if(e.getRawSlot()==1) {
                    Player p = (Player) e.getWhoClicked();
                    // 实现签到逻辑
                    HopeCraft plugin = (HopeCraft) Bukkit.getPluginManager().getPlugin("HopeCraft");
                    if (plugin != null) {
                        UUID playerUUID = p.getUniqueId();

                        // 检查是否今日已签到
                        if (plugin.hasSignedInToday(playerUUID)) {
                            p.sendMessage("§c你今天已经签到过了！");
                        } else {
                            // 执行签到
                            plugin.signIn(playerUUID);

                            // 给予奖励（例如5个钻石）
                            Inventory inv = p.getInventory();
                            Random r = new Random();
                            int n1 = r.nextInt(6);
                            int n2 = r.nextInt(6);
                            int n3 = r.nextInt(6);
                            int n4 = r.nextInt(4);
                            int n5 = r.nextInt(2);
                            inv.addItem(new ItemStack(Material.EMERALD, n1));
                            inv.addItem(new ItemStack(Material.IRON_INGOT, n2));
                            inv.addItem(new ItemStack(Material.GOLD_INGOT, n3));
                            inv.addItem(new ItemStack(Material.DIAMOND, n4));
                            inv.addItem(new ItemStack(Material.NETHERITE_INGOT, n5));

                            String s = String.format("§a签到成功！获得%d个绿宝石，%d个铁锭，%d个金锭，%d个钻石，%d个下界合金锭奖励！",
                                    n1,n2,n3,n4,n5);

                            p.sendMessage(s);
                        }
                    }
                    p.closeInventory();
                    e.setCancelled(true);
                }
                else if(e.getRawSlot()==2) {
                    Player p = (Player) e.getWhoClicked();
                    HopeCraft plugin = (HopeCraft) Bukkit.getPluginManager().getPlugin("HopeCraft");
                    if (plugin != null) {
                        plugin.showPlayerStats(p);
                        p.sendMessage("§a已显示你的信息！");
                    }
                    p.closeInventory();
                    e.setCancelled(true);
                }
                else if(e.getRawSlot()==3) {
                    Player p = (Player) e.getWhoClicked();
                    PluginManager pm = Bukkit.getPluginManager();
                    Plugin plugin = pm.getPlugin("fireworks");
                    if(plugin!=null) Bukkit.getServer().dispatchCommand(p, "fireworks");
                    else p.sendMessage("§c请联系服务器管理员安装fireworks插件！");
                    p.closeInventory();
                    e.setCancelled(true);
                }
            }
        }
    }

    // 新增统计监听器
    public class StatsListener implements Listener {
        @EventHandler
        public void onEntityKill(EntityDeathEvent e) {
            if (e.getEntity().getKiller() != null) {
                Player player = e.getEntity().getKiller();
                HopeCraft plugin = (HopeCraft) Bukkit.getPluginManager().getPlugin("HopeCraft");
                if (plugin != null) {
                    plugin.addKill(player.getUniqueId());
                    // 实时更新计分板
                    if (player.getScoreboard().getObjective("stats") != null) {
                        plugin.showPlayerStats(player);
                    }
                }
            }
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent e) {
            Player player = e.getPlayer();
            HopeCraft plugin = (HopeCraft) Bukkit.getPluginManager().getPlugin("HopeCraft");
            if (plugin != null) {
                plugin.addBlockBroken(player.getUniqueId());
                // 实时更新计分板
                if (player.getScoreboard().getObjective("stats") != null) {
                    plugin.showPlayerStats(player);
                }
            }
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent e) {
            Player player = e.getEntity();
            HopeCraft plugin = (HopeCraft) Bukkit.getPluginManager().getPlugin("HopeCraft");
            if (plugin != null) {
                plugin.addDeath(player.getUniqueId());
                // 实时更新计分板
                if (player.getScoreboard().getObjective("stats") != null) {
                    plugin.showPlayerStats(player);
                }
            }
        }
    }

    public static class KeyPressListener implements Listener {
        private final Set<UUID> pressingShift = new HashSet<>();

        @EventHandler
        public void onShiftPress(PlayerToggleSneakEvent event) {
            if(event.isSneaking()) {
                pressingShift.add(event.getPlayer().getUniqueId());
            } else {
                pressingShift.remove(event.getPlayer().getUniqueId());
            }
        }

        @EventHandler
        public void onKeyPress(PlayerItemHeldEvent event) {
            Player player = event.getPlayer();
            // 检查玩家是否按住了Shift键并且切换到了第5个物品槽位（索引为4，F键）把这里写好就行了，我妈妈让我走了
            if (pressingShift.contains(player.getUniqueId()) && event.getNewSlot() == 4) {
                // 延迟执行命令以避免与副手交互冲突
                Bukkit.getScheduler().runTaskLater(
                        Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("HopeCraft")),
                        () -> {
                            // 再次检查玩家是否仍然按住Shift键，以确保这不是副手操作
                            if (pressingShift.contains(player.getUniqueId())) {
                                simulateCommand(player);
                            }
                        },
                        1L // 延迟1tick执行
                );
            }
        }

        private void simulateCommand(Player player) {
            player.performCommand("hopecraft");
            pressingShift.remove(player.getUniqueId());
        }
    }
}
//喵喵喵