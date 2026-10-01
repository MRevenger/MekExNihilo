# MekExNihilo 功能规格说明

本文档描述 MekExNihilo 的完整行为，供其他实现者（人或 AI）作为对照基准。
目标：读完本文即可实现一个功能等价的模组，或据此检查另一个实现的差异。

- 当前版本：`1.0.7`
- 文档对应 jar：`mekexnihilo-1.0.7+mc1.21.1.jar`

---

## 1. 平台与身份

| 项 | 值 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.249+ |
| Java | 21 |
| modid | `mekexnihilo` |
| 显示名 | `MekExNihilo` |
| 资源/数据命名空间 | `mekexnihilo` |
| 配置文件 | `config/mekexnihilo-common.toml` |
| 配置类型 | `ModConfig.Type.COMMON`（槽位数必须客户端/服务端一致） |
| 许可 | MIT |

### 依赖

| 模组 | 版本 | 必需性 |
|---|---|---|
| Mekanism | `[10.7.11,)` | **必需** |
| Ex Deorum | `[3.3,)` | **必需** |
| AllTheCompressed | 4.4.0 | 可选 |

Mekanism 下限的证据：`10.7.10.73` 及更早编译失败（`Holder<Block>` 无法转 `IBlockProvider`、`forSideWithConfig`、`TileEntityTypeRegistryObject<TileEntityMekanism>` 签名不匹配）。
Ex Deorum 下限的证据：3.3 至 3.12 全部 10 个 1.21.1 版本的 API 面已用 `javap` 逐个核对，均可用。

**关键约束**：Addon 不引用 Ex Deorum 的 `RecipeUtil`。该类在版本间改过形状（3.x 是静态 `getSieveRecipes(mesh, stack)`，3.12 换成 `getCaches(level).getSieveRecipes(...)`），调用任一个都会在另一版本上抛 `NoSuchMethodError`。改为从 `ERecipeTypes.SIEVE` 读取原版 `RecipeManager` 并自行缓存。
副作用是能读到 KubeJS / 数据包改过的配方表，优于 Ex Deorum 自带缓存。

---

## 2. 注册内容

### 2.1 方块（5 个）

| 注册名 | 中文名 | 机器等级 | TileEntity |
|---|---|---|---|
| `mekexnihilo:electric_sieve` | 筛矿机 | 0 | `TileEntitySieve` |
| `mekexnihilo:basic_sieve_factory` | 基础筛矿工厂 | 1 | `TileEntitySieveFactory` |
| `mekexnihilo:advanced_sieve_factory` | 高级筛矿工厂 | 2 | 同上 |
| `mekexnihilo:elite_sieve_factory` | 精英筛矿工厂 | 3 | 同上 |
| `mekexnihilo:ultimate_sieve_factory` | 终极筛矿工厂 | 4 | 同上 |

- 硬度 `1.5`，抗爆 `6.0`
- **不设置** `requiresCorrectToolForDrops`（任何工具都能掉落）
- 加入 `minecraft:mineable/pickaxe` 标签
- 贴图直接复用 Mekanism：筛矿机用富集仓，四级工厂用富集工厂；仅把顶面换成白色网格（覆盖中间 80%，凹陷 1/4 格）

### 2.2 提供的标签

| 标签 | 用途 |
|---|---|
| `mekexnihilo:sieves` | 全部 67 个 Ex Deorum 筛子物品，用于合成配方 |
| `mekexnihilo:sieve_blacklist` | 默认**空**。列入的物品永不被筛、也无法放入输入槽 |
| `minecraft:enchantable/mining` | 追加 6 种 Ex Deorum 筛网（`replace: false`） |
| `minecraft:enchantable/mining_loot` | 同上 |

最后两个是**必须的**：Ex Deorum 的筛网不在原版附魔标签里，不补的话附在筛网上的效率与时运完全不生效。

### 2.3 公开 API

`com.mekexnihilo.api.SieveInputEvent`，在 `NeoForge.EVENT_BUS` 上、服务端触发，时机为一批物品即将被筛矿**之前**。

- **不可取消**。通过修改 `getInputs()`（可变 `List<ItemStack>`）剔除条目；剩余部分照常加工
- 访问器：`getSieve()`、`getMesh()`、`getMeshTier()`、`getInputs()`
- Java：`NeoForge.EVENT_BUS.addListener(SieveInputEvent.class, event -> ...)`
- KubeJS：`NativeEvents.onEvent('com.mekexnihilo.api.SieveInputEvent', event => ...)`

---

## 3. 槽位模型

每台机器有 4 类槽位：

| 类别 | 数量 | Mekanism `DataType` |
|---|---|---|
| 输入 | 由机器等级决定（见下表） | `INPUT` |
| 输出 | `outputSlots + 等级 × outputSlotsPerTier` | `OUTPUT` |
| 筛网 | 1 | **`EXTRA`**（不是 INPUT） |
| 能量 | 1 | `ENERGY` |

### 3.1 输入槽（固定表，不可配置）

| 机器等级 | 机器 | 输入槽 |
|---|---|---|
| 0 | 筛矿机 | 1 |
| 1 | 基础筛矿工厂 | 3 |
| 2 | 高级筛矿工厂 | 4 |
| 3 | 精英筛矿工厂 | 5 |
| 4 | 终极筛矿工厂 | 6 |

### 3.2 输出槽

```
输出槽数 = outputSlots + 机器等级 × outputSlotsPerTier
```

默认 `outputSlots=12`、`outputSlotsPerTier=4`，五级依次为 **12 / 16 / 20 / 24 / 28**。

### 3.3 输出槽堆叠上限

单个输出槽默认可放 **8192** 个物品（`outputSlotLimit`，范围 64~8192）。

实现要求：该槽位必须绕过原版堆叠上限。NeoForge 的 `Item.ABSOLUTE_MAX_STACK_SIZE` 是 99，
`ItemStack` 的编解码器把 count 限死在 1~99，直接塞大数字会**坏档**。
只有走 Mekanism 的 `SerializerHelper.saveOversized` 存档 + varint 网络同步才安全（这两条路径都没有 99 上限）。
槽位应实现为 `BasicInventorySlot` 的子类并设 `obeyStackLimit = false`。

**易错点**：输出空间判定不能用 `Math.min(物品堆叠上限, 槽位上限)`。物品堆叠上限恒为 64，
会让机器在槽位装到 64 个时就误判「放不下」而停机。必须使用槽位自身容量。

### 3.4 槽位数量何时生效

槽位数在**机器被建造或加载时**读取，不缓存进 `static` 字段（缓存会导致改配置无效）。
世界里已存在的机器保持建造时的布局；改配置后需拆掉重放。

---

## 4. GUI 规格

### 4.1 布局

```
┌─────────────────────────────────────────┐
│  ┌──┐   ┌──┬──┬──┬──┬──┬──┐            │
│  │筛│   │输│输│输│输│输│输│            │
│  │网│   └──┴──┴──┴──┴──┴──┘            │
│  └──┘   ▓▓▓▓▓ 进度条 ▓▓▓▓▓              │  █ ← 能量条
│  ┌──┐                                    │  █   （右侧竖直）
│  │能│   ┌──┬──┬──┬──┐                   │  █
│  │量│   │输│输│输│输│  输出网格          │  █
│  └──┘   ├──┼──┼──┼──┤                   │  █
│         │输│输│输│输│                   │  █
│         └──┴──┴──┴──┘                   │
├─────────────────────────────────────────┤
│         玩家背包（3×9）                  │
│         快捷栏（1×9）                    │
└─────────────────────────────────────────┘
```

要点：

- **左侧额外栏**：筛网槽、能量槽
- **顶部输入栏**：输入槽排成一行，固定
- **下部输出栏**：输出槽网格
- **右侧**：Mekanism 的竖直能量条
- **不绘制任何文字**

### 4.2 几何常量

| 常量 | 值 |
|---|---|
| 槽位尺寸 | 18 |
| 筛网槽 | (26, 17) |
| 能量槽 | (26, 43) |
| 输入起点 | (62, 17) |
| 进度条 | (62, 36) |
| 输出起点 | (62, 50) |
| 能量条 | y=16，高 52 |
| 最小宽度 | 176 |
| 右边距 | 16 |

### 4.3 自适应尺寸

- **宽度**：`max(最小宽度, 输入行宽, 输出网格宽) + 右边距`
- **输出网格列数**：默认 4 列；当所需行数超过 5 行时切换为 **6 列**（否则窗口会过高）
- **高度**：随输出行数增长
- 菜单与屏幕必须共用同一套几何计算，避免槽位与覆盖层错位

> 窗口尺寸未在默认配置下重新实测。早期一次非默认配置（`inputSlots=[1,2,6,7,9]`、`outputSlots=9`）
> 测得的尺寸已不适用于当前配置，此处不再列出具体数字。

---

## 5. 加工算法

### 5.1 单次操作流程

1. **空闲检查**：筛网槽为空 → 停机；无输入 → 停机；输出无空位 → 停机；能量不足 → 停机
2. **收集批次**：从所有可用输入槽收集单件物品，上限为 `单次处理量 × 并行数`
3. **触发 `SieveInputEvent`**，再按上限裁剪批次
4. **快照时运等级**（防止加工中途换筛网改变结果）
5. **推进进度**：每 tick `operatingTicks++`，直到 `>= ticksRequired`
6. **逐个物品结算**：对批次中每个物品调用 `rollDrops`；**该物品的产物必须能全部装下才消耗它**
7. 回到步骤 1

**第 6 步的顺序很重要**：强筛网一次可能掉落超过输出容量。若要求整批一次性装下，机器会永久卡死。
逐个结算 + 装不下就停，保证已产出的部分不丢失。

### 5.2 掉落计算

- 配方来源：`ERecipeTypes.SIEVE` 类型，遍历 `RecipeManager`
- 筛选条件：配方匹配当前筛网 + 输入物品
- `byHandOnly` 的配方**默认跳过**（与 Ex Deorum 自带机械筛一致），由 `siftByHandOnlyRecipes` 开关
- 掉落数量：从配方的 `result_amount` number provider 采样。
  Ex Deorum 多用 `BinomialDistributionGenerator`（按概率返回 0 或 1）
- **时运**：作为乘数作用在采样结果上，采用**概率取整**，使长期平均恰好为 `base × multiplier`
  - `multiplier = 1 + 时运等级 × fortuneBonusPerLevel`
  - 概率取整：`whole = floor(exact)`；若 `random() < exact - whole` 则 `whole++`

### 5.3 加工时间

```
ticksRequired = baseTicks
              × (1 - min(1, 效率等级 × efficiencyReductionPerLevel))   ← 下限 1 tick
              × MekanismUtils.getTicks(...)                             ← 速度升级
              × 压缩时间倍率                                             ← 仅压缩原料
```

- `baseTicks` 默认 100
- `efficiencyReductionPerLevel` 默认 0.05（每级 −5%）
- 减免总上限 100%，最终不低于 1 tick
- 速度升级必须通过 `MekanismUtils.getTicks` 应用，且在**升级变化时立即重算**
  （`recalculateUpgrades`）。不重算的话新速度要到下一次操作才生效，表现为「升级没作用」
- 实测：8 个速度升级使 `ticksRequired` 从 100 变为 10

### 5.4 附魔等级读取

- 效率、时运从筛网槽的物品读取
- 附魔在 1.21 是注册表驱动的，需通过 `level.registryAccess()` 解析 holder，没有静态字段可用
- `respectEnchantmentLimits`（默认 `false`）：为 `false` 时超过原版上限继续叠加。
  这是默认值，因为要达到 100% 减时需要 20 级效率，而原版上限是 V

---

## 6. 筛网等级与吞吐

按 Ex Deorum 自身的排序，等级 1~6：

| 等级 | 筛网 | 单次处理量 |
|---|---|---|
| 1 | 线（string） | 1 |
| 2 | 燧石（flint） | 2 |
| 3 | 铁（iron） | 4 |
| 4 | 金（golden） | 16 |
| 5 | 钻石（diamond） | 32 |
| 6 | 下界合金（netherite） | 64 |

**该表固定不可配置**，注意第三到第四级之间从 4 直接跳到 16。

### 工厂并行

工厂等级 1~4 的并行工序数为 **3 / 5 / 7 / 9**（取自 Mekanism 的 `FactoryTier`）。

```
有效单次处理量 = 单次处理量 × 并行数
```

例：终极工厂 + 下界合金筛网 = `64 × 9 = 576`。

---

## 7. 能量

- `energyPerTick` 默认 **200** FE/t
- `energyCapacity` 默认 **40000** FE
- 速度升级会提高每 tick 能耗（由 Mekanism 的 `MachineEnergyContainer` 处理）
- 压缩原料按倍率放大每 tick 能耗（见第 9 节）

---

## 8. 自动弹出输出

**每个游戏刻**尝试把第一个非空输出槽的**全部**物品推入相邻容器。

- 只处理侧面配置里设为 `DataType.OUTPUT` 的面
- 未配置输出面 → 不弹出
- 一次只处理**一个**输出槽（第一个非空的）；下一 tick 再处理下一个
- 若目标容器只接受一部分，剩余留在槽内

### 8.1 为什么不用 Mekanism 自带的弹出器

`TileComponentEjector` 每次尝试后把 `tickDelay` 设为 **10 tick**（`TICKS_PER_HALF_SECOND`），
即最快每半秒一次。快工厂喂不饱这个速度。

做法：保留 `TileComponentEjector` 负责侧面配置、GUI 页签与存档，但用
`setCanEject(type -> type != TransmissionType.ITEM)` 关闭它的物品弹出，物品由 TileEntity 自行推送。

### 8.2 空闲节流

- 每次**成功弹出重置计时器**
- 连续 **3 分钟**（3600 tick）未能弹出任何东西 → 判定该机器未被使用，改为**每秒尝试一次**
- `dynamicEject`（默认 `true`）可关闭节流，关闭后始终每刻尝试

---

## 9. 压缩原料（AllTheCompressed，可选）

**仅在该模组加载时生效**；未加载时 `[compressed]` 配置节**不生成也不显示**。

### 9.1 识别方式

- **无编译期依赖**，仅靠注册名识别：`allthecompressed:<材料>_<层数>x`
- 基础原料回查物品注册表：先试 `minecraft:<材料>`，再跨命名空间搜索
- 识别结果按物品缓存；模组缺失/改版不会崩溃

### 9.2 加工规则

压缩物品用其**基础原料的筛选配方**加工：

| 维度 | 规则 | 默认底数 |
|---|---|---|
| 产量 | 重复执行筛矿事件 `round(底数^层数)` 次 | `compressedYieldBase = 9.0` |
| 加工时间 | `底数^层数` 倍 | `compressedTimeBase = 1.5` |
| 每 tick 能耗 | `底数^层数` 倍 | `compressedEnergyBase = 1.5` |

**产量必须是「重复事件」而不是「把单次产物乘倍」**。Ex Deorum 的掉落是概率（`BinomialDistributionGenerator` 返回 0/1），
把一次判定乘 8 会把「25% 概率掉落」变成「必定掉 8 个」；重复 8 次独立判定则保留原分布，均值相同。

实测（沙砾，各 24 次单次筛选）：
- 普通结果 5 种取值
- `gravel_3x` 结果 **10 种取值**，范围 14~25（而不是 8 的倍数），均值比 8.30 ≈ 2³

### 9.3 安全上限

`maxCompressedRepeats` 默认 **4096**，范围 1~65536。

重复次数随层数**指数增长**：9 重压缩配底数 9 是 9⁹ ≈ 3.87 亿次，会在单个游戏刻内掷骰数亿次并卡死服务器。
实测 `gravel_9x` 的理论倍率 3.87e8 被正确截断到 4096。

### 9.4 与黑名单的关系

压缩物品**继承其基础原料的黑名单条目**：把 `minecraft:sand` 列入黑名单，`allthecompressed:sand_1x` 及更高层数也一并被拒。

---

## 10. 输入过滤

三条并行的过滤途径：

1. **物品标签 `mekexnihilo:sieve_blacklist`**（推荐）
   - 物品既**无法放入**输入槽，也**不会被筛**
   - 是普通物品标签，数据包或 KubeJS 均可写入
2. **`SieveInputEvent`**（条件化）
   - 时机为批次收集之后、消耗之前
   - 通过修改 `getInputs()` 剔除
3. **直接增删 Ex Deorum 的筛矿配方**

**批次收集顺序约束**：候选物品**先全部**交给事件，**再**按上限裁剪。
若顺序反过来，被否决的物品会占掉批次名额，排在其后的可用原料会被永久饿死。

**槽位校验约束**：输入槽的 `isItemValid` 在**客户端也会被调用**（GUI 拖拽时判断能否放入），
因此它**绝不能访问配方**（配方在服务端的 `RecipeManager` 里）。
某物品能否被当前筛网加工，只在实际运行时于批次收集阶段判定；不能加工的物品就留在槽里等待。

---

## 11. 工厂安装器与升级

- 筛矿机声明 `AttributeUpgradeable` 指向基础工厂
- 筛矿机**刻意不声明** `AttributeTier`：基础安装器的 "from" 为 `null`，只匹配没有 tier 的方块
  （与 Mekanism 自己的富集仓行为一致）
- 升级链：`electric_sieve → basic → advanced → elite → ultimate →（顶级不再升级）`

**内容保留**：转换时按槽位**角色**保存内容，不是按原始索引。
因为布局随等级变化（筛矿机 1 输入 / 12 输出，基础工厂 3 输入 / 16 输出），按索引复制会把输出物品塞进输入槽。
保存的是**分离的副本**，不是活动槽位的引用，因此旧方块实体被替换后数据仍有效。

---

## 12. 完整配置参考

### `[machine]`

| 键 | 默认 | 范围 | 说明 |
|---|---|---|---|
| `outputSlots` | 12 | 5~60 | 筛矿机的输出槽数 |
| `outputSlotsPerTier` | 4 | 0~20 | 每级工厂额外增加的输出槽 |
| `outputSlotLimit` | 8192 | 64~8192 | 单槽堆叠上限 |
| `baseTicks` | 100 | 1~72000 | 无效率附魔时一次加工的 tick 数 |
| `efficiencyReductionPerLevel` | 0.05 | 0.0~1.0 | 每级效率减少的时间比例 |
| `fortuneBonusPerLevel` | 0.20 | 0.05~0.50 | 每级时运提高的产量比例 |
| `energyPerTick` | 200 | 1~`Long.MAX` | 运行时每 tick 能耗 |
| `energyCapacity` | 40000 | 1~`Long.MAX` | 内部能量缓存 |
| `damageMesh` | `false` | — | 筛网是否消耗耐久 |
| `siftByHandOnlyRecipes` | `false` | — | 是否处理 `by_hand_only` 配方 |
| `respectEnchantmentLimits` | `false` | — | 是否遵守原版附魔等级上限 |
| `dynamicEject` | `true` | — | 是否启用空闲弹出节流 |

### `[compressed]`（仅 AllTheCompressed 加载时存在）

| 键 | 默认 | 范围 | 说明 |
|---|---|---|---|
| `enableCompressedSifting` | `true` | — | 总开关 |
| `compressedYieldBase` | 9.0 | 1.0~64.0 | 产量底数（事件重复次数 = `round(底数^层数)`） |
| `compressedTimeBase` | 1.5 | 1.0~64.0 | 时间倍率底数 |
| `compressedEnergyBase` | 1.5 | 1.0~64.0 | 能耗倍率底数 |
| `maxCompressedRepeats` | 4096 | 1~65536 | 单次操作事件数上限 |

### 翻译键格式（NeoForge 陷阱）

NeoForge 只对**分组**使用分组名拼键；对**配置项**回退到 `<modid>.configuration.<叶子名>`（**不含分组**）。

因此 `mekexnihilo.configuration.machine.outputSlots` 这种写法**永远不会被查询**，界面会显示原始键名。
必须用 `.translation("...")` **显式**声明每个条目与分组的键。

tooltip 键是翻译键 + `.tooltip`；分组按钮是翻译键 + `.button`；界面标题是 `<modid>.configuration.title`。

### 已移除的配置项

`inputSlots` 与 `batchSizes` 曾可配置，现已改为固定表（见 3.1 与第 6 节）。
原因：它们同时决定 GUI 布局与菜单结构，放开容易造成客户端/服务端不一致。

---

## 13. 合成配方

### 筛矿机

```
I I
RSR
IOI
```
`I` = `#c:ingots/iron`，`R` = `#c:dusts/redstone`，`S` = `#mekexnihilo:sieves`，`O` = `#c:ingots/osmium`

### 四级工厂

统一形状：

```
ACA
IPI
ACA
```

| 产物 | A（合金） | C（电路） | I（锭） | P（前一等级） |
|---|---|---|---|---|
| 基础 | `#mekanism:alloys/basic` | `#c:circuits/basic` | `#c:ingots/iron` | 筛矿机 |
| 高级 | `#mekanism:alloys/infused` | `#c:circuits/advanced` | `#c:ingots/osmium` | 基础工厂 |
| 精英 | `#mekanism:alloys/reinforced` | `#c:circuits/elite` | `#c:ingots/gold` | 高级工厂 |
| 终极 | `#mekanism:alloys/atomic` | `#c:circuits/ultimate` | `#c:gems/diamond` | 精英工厂 |

注：`mekanism:alloys/basic` 在通用机械里就是 `minecraft:redstone`，因此基础工厂的配方与 Mekanism 自家基础灌注工厂一致。

每个方块另有一个 loot table。

---

## 14. 资源要求

| 资源 | 说明 |
|---|---|
| 方块模型 | 机身 + 顶面网格，工厂用 Mekanism 富集工厂的复合模型 |
| 顶面贴图 | `sieve_grid_top.png`，4×4 编织网格覆盖中间 80%，深色边框 |
| 图标 | `icon.png`（256×256），`logoFile="icon.png"` |
| 语言文件 | `zh_cn.json` + `en_us.json` |

**模型陷阱**：Mekanism 把工厂顶面拆成 `front_panel`（uv `[0,12,16,16]`）和旋转 180° 的 `shell_01`。
直接改 uv 会让顶面纹理糊掉。必须重写 uv，让 `v` 线性映射到方块的 z 轴。
顶面需凹陷 1/4 格：`front_panel` 到 y=12，`shell_01` 作为 y 11~12 的地板，四周补 y 12~16 的边框。

---

## 15. 实现时必须匹配的行为清单

供对照检查。第 1、2 条（两张固定表）已在 1.0.7 的实际服务端启动日志中确认；
第 6~12、15~16 条在服务端实测确认过；
第 3~5、13~14 条来自早期实机验证。

**未验证的部分**：GUI 的可视外观（无文字、左侧额外栏、自适应尺寸）只做过编译与几何计算，
没有在实机中打开确认。窗口尺寸见 4.3 的说明。

1. 输入槽数为 `1/3/4/5/6`，输出槽数为 `12/16/20/24/28`
2. 单次处理量为 `1/2/4/16/32/64`（注意 4→16 的跳跃）
3. 筛网槽是 **EXTRA** 数据类，不是 INPUT
4. 单个输出槽可累积到 8192，跨存档往返完好
5. 输出空间判定使用槽位容量，而非 `min(物品上限, 槽位上限)`
6. 每 tick 弹出首个非空输出槽的全部物品，下一 tick 才处理下一个
7. 空闲 3 分钟后弹出降为每秒一次；成功弹出即重置
8. 速度升级立即生效（8 个升级：100 → 10 tick）
9. 效率 V → 75 tick；效率 25（超上限）→ 1 tick（100% 上限生效）
10. 筛网必须能被附魔（补 `enchantable/mining` 与 `mining_loot` 标签），否则效率/时运无效
11. 被过滤的原料不占用批次名额，不影响其后的可用原料
12. 某原料当前筛网无法加工时留在槽内，不阻塞其他原料
13. 机器可用任意模组的扳手（`c:tools/wrench`）拆取，无需 Mekanism 配置器
14. 工厂安装器转换后内容按角色保留（筛网仍在筛网槽、输入仍在输入槽、输出仍在输出槽）
15. GUI 不绘制任何文字，尺寸随槽位数自适应
16. 客户端资源加载无 `mekexnihilo` 缺失材质/模型警告

---

## 16. 明确不在范围内

- 不添加任何新的筛网或原料（全部来自 Ex Deorum）
- 不添加新的筛矿配方（读取 Ex Deorum 的）
- 不替换 Ex Deorum 的配方查询工具类
- 不要求 AllTheCompressed 存在
- 除筛矿机与四级工厂外不添加其他机器
