# 每日签到奖励配置说明

签到奖励已从硬编码改为**配置文件驱动**，无需重新打包插件，修改 `config.yml` 并重启服务器即可生效。

## 配置文件位置

```
plugins/HopeCraft/config.yml
```

签到奖励位于文件中的 `sign-rewards` 段：

```yaml
# 每日签到奖励配置
# 每一项代表一份奖励，字段说明：
#   material: 物品英文名（参考下方链接）
#   min:      发放数量下限（含）
#   max:      发放数量上限（含）
# 数量会在 min~max 之间随机。修改后重启服务器或重载配置即可生效。
sign-rewards:
  - material: DIAMOND
    min: 1
    max: 5
  - material: EMERALD
    min: 1
    max: 3
  - material: NETHERITE_UPGRADE_SMITHING_TEMPLATE
    min: 1
    max: 7
```

## 字段说明

| 字段       | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| `material` | 物品英文名（Bukkit `Material` 枚举），区分大小写、用下划线分隔单词 |
| `min`      | 发放数量的**下限**（含），必须为非负整数                     |
| `max`      | 发放数量的**上限**（含），必须为非负整数                     |

实际发放数量 = `min` 到 `max` 之间的随机整数（含两端）。

## 修改示例

把奖励改成「固定 3 个苹果 + 随机 1~10 个金锭」：

```yaml
sign-rewards:
  - material: APPLE
    min: 3
    max: 3
  - material: GOLD_INGOT
    min: 1
    max: 10
```

新增第三种奖励（如绿宝石块）：

```yaml
sign-rewards:
  - material: DIAMOND
    min: 1
    max: 5
  - material: EMERALD
    min: 1
    max: 3
  - material: NETHERITE_UPGRADE_SMITHING_TEMPLATE
    min: 1
    max: 7
  - material: EMERALD_BLOCK
    min: 1
    max: 2
```

> 注意：`sign-rewards` 是**列表**，可自由增删奖励项；不存在数量上限（旧代码的 `Map.of` 上限为 10 项，现已无此限制）。

## 如何生效

1. 修改服务器 `plugins/HopeCraft/config.yml` 中的 `sign-rewards`。
2. **重启服务器**（或重启插件）。

首次运行时，如果配置中不存在 `sign-rewards` 段，插件会自动写入默认奖励配置到 `config.yml`，方便直接编辑。

## 容错与校验

- **物品名无效**（拼写错误、非物品类型）：该项被跳过，并在控制台输出警告，不影响其他奖励。
- **整个 `sign-rewards` 缺失或为空**：自动写入默认奖励配置。
- **`min > max`**：自动校正为 `max = min`（即固定数量发放）。

## `material` 名称参考

`material` 使用 Bukkit 的 `Material` 枚举英文名（全大写、下划线分隔），例如：

- `DIAMOND`、`EMERALD`、`GOLD_INGOT`、`IRON_INGOT`
- `NETHERITE_INGOT`、`NETHERITE_UPGRADE_SMITHING_TEMPLATE`
- `DIAMOND_BLOCK`、`EMERALD_BLOCK`、`GOLDEN_APPLE`
- `EXPERIENCE_BOTTLE`（经验瓶）

常用可发放物品的完整分类表见 **[materials.md](materials.md)**（含宝石、稀有物、食物、工具装备及完整列表提取方法）。

官方在线参考（按所用服务端版本核对）：
<https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html>

## 相关代码位置

- 配置加载：`HopeCraft.java` → `loadSignRewards()`
- 奖励发放：`HopeCraft.java` → `giveSignReward(Player)`
- 默认配置写入：`HopeCraft.java` → `createRewardMap(...)` / `getInt(...)`

## 注意事项

- 物品英文名必须是**当前服务端版本支持的 `Material`**，跨版本可能改名（如旧版 `GOLDEN_APPLE` 等请对照所用 Paper/Spigot 版本）。
- 配置文件使用 YAML 格式，请保持正确的缩进（每项用两个空格缩进 `min`/`max`）。
