# 物品名称表（Material 参考）

签到奖励的 `material` 字段使用 Bukkit/ Paper 的 `Material` 枚举英文名（全大写、下划线分隔）。

本表为**常用可发放物品**精选（当前 Paper API 共 2154 个枚举值，无法全部列出）。名称均来自真实枚举，可直接用于 `config.yml` 的 `sign-rewards`。

> 避免使用 `LEGACY_` 前缀的名称（旧版兼容别名）以及 `SPAWN_EGG`（刷怪蛋）等特殊物品。

---

## 一、宝石 / 矿物 / 金属（材料）

| 物品名 | 说明 |
| --- | --- |
| `DIAMOND` | 钻石 |
| `EMERALD` | 绿宝石 |
| `GOLD_INGOT` | 金锭 |
| `GOLD_NUGGET` | 金粒 |
| `IRON_INGOT` | 铁锭 |
| `IRON_NUGGET` | 铁粒 |
| `NETHERITE_INGOT` | 下界合金锭 |
| `NETHERITE_SCRAP` | 下界残骸 |
| `LAPIS_LAZULI` | 青金石 |
| `REDSTONE` | 红石粉 |
| `COAL` | 煤炭 |
| `QUARTZ` | 石英 |
| `AMETHYST_SHARD` | 紫水晶碎片 |
| `COPPER_INGOT` | 铜锭 |
| `COPPER_NUGGET` | 铜粒 |
| `RAW_IRON` | 粗铁 |
| `RAW_GOLD` | 粗金 |
| `RAW_COPPER` | 粗铜 |

### 对应方块（大份奖励用）

`DIAMOND_BLOCK` · `EMERALD_BLOCK` · `GOLD_BLOCK` · `IRON_BLOCK` · `NETHERITE_BLOCK` · `LAPIS_BLOCK` · `REDSTONE_BLOCK` · `COAL_BLOCK` · `QUARTZ_BLOCK` · `AMETHYST_BLOCK` · `COPPER_BLOCK` · `RAW_IRON_BLOCK` · `RAW_GOLD_BLOCK` · `RAW_COPPER_BLOCK`

---

## 二、稀有 / 特殊物品

| 物品名 | 说明 |
| --- | --- |
| `NETHERITE_UPGRADE_SMITHING_TEMPLATE` | 下界合金升级模板（原版默认奖励） |
| `TOTEM_OF_UNDYING` | 不死图腾 |
| `ELYTRA` | 鞘翅 |
| `ENDER_PEARL` | 末影珍珠 |
| `ENDER_EYE` | 末影之眼 |
| `ENDER_CHEST` | 末影箱 |
| `SHULKER_BOX` | 潜影盒（另有 16 种颜色变体，如 `RED_SHULKER_BOX`、`BLUE_SHULKER_BOX` 等） |
| `EXPERIENCE_BOTTLE` | 经验瓶 |

### 盔甲纹样模板（Armor Trim，举例）

`DUNE_ARMOR_TRIM_SMITHING_TEMPLATE` · `COAST_ARMOR_TRIM_SMITHING_TEMPLATE` · `WILD_ARMOR_TRIM_SMITHING_TEMPLATE` · `SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE` · `SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE` · `TIDE_ARMOR_TRIM_SMITHING_TEMPLATE` · `WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE` 等（共十余种）

---

## 三、食物 / 回复类

| 物品名 | 说明 |
| --- | --- |
| `APPLE` | 苹果 |
| `GOLDEN_APPLE` | 金苹果 |
| `ENCHANTED_GOLDEN_APPLE` | 附魔金苹果（极稀有，谨慎发放） |
| `GOLDEN_CARROT` | 金胡萝卜 |
| `BREAD` | 面包 |
| `BAKED_POTATO` | 烤马铃薯 |
| `CARROT` | 胡萝卜 |
| `POTATO` | 马铃薯 |
| `BEETROOT` | 甜菜根 |
| `SWEET_BERRIES` | 甜浆果 |
| `MELON_SLICE` | 西瓜片 |
| `COOKED_BEEF` | 熟牛排 |
| `COOKED_CHICKEN` | 熟鸡肉 |
| `COOKED_PORKCHOP` | 熟猪排 |
| `COOKED_MUTTON` | 熟羊肉 |
| `COOKED_RABBIT` | 熟兔肉 |
| `COOKED_COD` | 熟鳕鱼 |
| `COOKED_SALMON` | 熟鲑鱼 |
| `RABBIT_STEW` | 兔肉煲 |
| `BEETROOT_SOUP` | 甜菜汤 |
| `HONEY_BOTTLE` | 蜂蜜瓶 |

---

## 四、工具 / 装备

### 钻石系列

`DIAMOND_SWORD` · `DIAMOND_PICKAXE` · `DIAMOND_AXE` · `DIAMOND_SHOVEL` · `DIAMOND_HOE` · `DIAMOND_HELMET` · `DIAMOND_CHESTPLATE` · `DIAMOND_LEGGINGS` · `DIAMOND_BOOTS`

### 下界合金系列

`NETHERITE_SWORD` · `NETHERITE_PICKAXE` · `NETHERITE_AXE` · `NETHERITE_SHOVEL` · `NETHERITE_HOE` · `NETHERITE_HELMET` · `NETHERITE_CHESTPLATE` · `NETHERITE_LEGGINGS` · `NETHERITE_BOOTS`

### 其他金属系列（命名规律相同）

金：`GOLDEN_` 前缀（如 `GOLDEN_SWORD`、`GOLDEN_PICKAXE` …）
铁：`IRON_` 前缀（如 `IRON_SWORD`、`IRON_HELMET` …）
铜：`COPPER_` 前缀（如 `COPPER_SWORD`、`COPPER_AXE` …）

---

## 五、如何获取完整物品列表

如需完整 2154 个 `Material` 名称（例如查找某个具体方块），可从本地 Paper API jar 提取：

```bash
# 需将路径替换为实际的 paper-api jar 路径
SPIGOT=~/.m2/repository/io/papermc/paper/paper-api/<版本>/paper-api-<版本>.jar
javap -classpath "$SPIGOT" org.bukkit.Material \
  | grep -E "public static final org.bukkit.Material" \
  | sed -E 's/.*Material ([A-Z_]+);/\1/' | sort
```

更权威的在线参考（按所用服务端版本核对）：
<https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html>

---

## 使用注意

- 名称必须**完全匹配**枚举（大小写敏感）。拼写错误会被插件跳过并输出控制台警告。
- 必须是**物品**（可被玩家拿在手上/放进背包），建筑方块、液体、空气等不可用。
- 不同 MC 版本间物品名可能变动，请以实际运行版本为准。
