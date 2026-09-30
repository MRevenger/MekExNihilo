# MekExNihilo

通用机械 × 无中生有：天赐（Ex Deorum）的筛矿机

给 Minecraft 1.21.1 / NeoForge 21.1.249 用的附属模组。它把
[Ex Deorum（无中生有：天赐）](https://modrinth.com/mod/ex-deorum) 的筛网装进一台 Mekanism 机器，
用通用机械的能量自动筛矿。支持任意等级的 Ex Deorum 筛网和效率、时运附魔，数值都在配置文件里调。

机器贴图直接取自通用机械：筛矿机用富集仓，四个工厂等级用富集工厂。

---

## 1. 依赖

| 模组 | 版本 | 说明 |
|---|---|---|
| Minecraft | 1.21.1 | |
| NeoForge | 21.1.249+ | |
| Mekanism | **10.7.11+** | 必需，模组 ID `mekanism` |
| Ex Deorum | **3.3+** | 必需，模组 ID `exdeorum` |
| AllTheCompressed | 4.4.0 | **可选**，模组 ID `allthecompressed`；安装后自动启用压缩原料筛选 |

Ex Deorum 本身只依赖 NeoForge / Minecraft，**不需要额外的库模组**。

Ex Deorum 在版本之间改过配方查询 API，本模组不使用它的工具类，细节见第 6 节。

---


### 版本范围是怎么定出来的

`javap` 只比对成员名是不够的：Mekanism 在 10.7.x 期间改过**方法签名**
（例如 `TileEntityMekanism` 的方块参数从 `IBlockProvider` 变成了 `Holder<Block>`），
这种变化只有真正编译才会暴露。因此本模组的依赖下限是这样测出来的：

1. **编译扫描**（`tools/find_min_version.ps1`）：逐个版本切换依赖并执行 `compileJava`，
   取仍然能编译的最旧版本。
2. **API 比对**（`tools/check_versions.ps1`）：用 `javap` 确认依赖的每个类/成员在候选版本中都存在。
3. **实机验证**：在**最旧可用组合**上启动服务器，实际放置并驱动机器。

实测结果：

| 依赖 | 扫描范围 | 结果 |
|---|---|---|
| Ex Deorum | 3.3 ~ 3.12（全部 10 个 1.21.1 版本） | **3.3 起全部可用** |
| Mekanism | 10.7.0.55 ~ 10.7.19.85（21 个版本） | **10.7.11.76 起可用**；10.7.10.73 及更早编译失败 |

最旧可用组合（**Ex Deorum 3.3 + Mekanism 10.7.11.76**）的实机结果：

```
electric_sieve          hardness=1.5 requiresCorrectTool=false
basic_sieve_factory     hardness=1.5 requiresCorrectTool=false
advanced_sieve_factory  hardness=1.5 requiresCorrectTool=false
elite_sieve_factory     hardness=1.5 requiresCorrectTool=false
ultimate_sieve_factory  hardness=1.5 requiresCorrectTool=false
wrench tag: configurator matches=true tag size=1
tryWrench -> DISMANTLED removed=true pickedUpIntoInventory=1
sifting: tier=6 batch=64 consumed=353 outputs=177
```
无任何 `NoSuchMethodError` / `NoClassDefFoundError`。

构建时**针对最旧版本编译**（Ex Deorum 3.3 / Mekanism 10.7.11.76），
因此产物字节码只引用这些版本就已存在的 API。
## 2. 机器：筛矿机 (Sieve Machine)

- 注册名：`mekexnihilo:electric_sieve`
- 一台真正的通用机械机器：有能量、侧面配置、安全设置、红石控制、比较器、**升级插槽**（速度/能量等），
  并且会出现在通用机械自己的创造模式物品栏里。
- 外观使用通用机械的贴图（筛矿机=富集仓，工厂=富集工厂，自带等级 LED），
  运行时筛网会发出琥珀色光。

### 槽位

| 槽位 | 数量 | 说明 |
|---|---|---|
| 筛网槽 | 1 | 放入任意 Ex Deorum 筛网；只能放筛网，且只能放 1 个 |
| 输入槽 | **取决于机器等级**（可配置） | 筛矿机 1、基础 3、高级 4、精英 5、终极 8 |
| 输出槽 | **配置值 + 每级 4 个**（可配置） | 默认 12 / 16 / 20 / 24 / 28 |
| 能量槽 | 1 | 可放入能量立方 / 电池等物品为机器充能 |
| 升级槽 | 由通用机械提供 | 速度、能量等升级 |

**筛网槽是独立的「额外 (EXTRA)」槽位组，不属于输入组。**
在侧面配置界面中：输入槽显示为红色 (INPUT)、输出槽为蓝色 (OUTPUT)、
筛网槽为黄色 (EXTRA)、能量槽为绿色 (ENERGY)。
因此管道/漏斗**不会**把筛矿原料塞进筛网槽；只有把某一面显式设置为 EXTRA 时，
自动化才会去操作筛网槽（方便自动更换筛网）。

**界面会随机器增大**：输入槽排得更宽时窗口变宽；输出槽超过 5 行时网格从 4 列切换为 6 列，
窗口再相应增高。实测窗口尺寸：筛矿机 192×209，终极筛矿工厂 228×245。

界面右侧有一条**竖直能量条**（通用机械的标准样式），鼠标悬停可看到精确的能量数值；
左侧的能量槽仍可放入能量立方 / 电池等物品。

### 筛矿工厂与工厂安装器

对筛矿机使用**通用机械的工厂安装器**（基础安装器）即可把它转换为 **基础筛矿工厂**，
再用高级 / 精英 / 终极安装器逐级升级 —— 与通用机械自家机器的升级流程完全一致。

| 机器 | 输入槽 | 输出槽（默认） | 并行工序 | 下界合金筛网下单次处理量 |
|---|---|---|---|---|
| 筛矿机 | 1 | 12 | 1 | 64 |
| 基础筛矿工厂 | 3 | 16 | 3 | 192 |
| 高级筛矿工厂 | 4 | 20 | 5 | 320 |
| 精英筛矿工厂 | 5 | 24 | 7 | 448 |
| 终极筛矿工厂 | 8 | 28 | 9 | 576 |

- **外观**：直接复用通用机械的模型，**只把顶面换成白色筛网网格**：
  - 筛矿机 → `mekanism:block/enrichment_chamber`（富集仓）
  - 四个工厂 → `mekanism:block/factory/enriching/base` + 对应等级的 `front_led`
    （富集工厂，等级配色由通用机械自己的 LED 层提供：基础=绿、高级=红、精英=蓝、终极=紫）
  顶部筛网为 **4×4 中等密度白色网格，覆盖顶面中间约 80%**。
  **筛矿机的筛面是真正的几何凹陷**：顶面向内下沉 **1/4 格**（4/16 单位），
  四周留出机壳边框，所以能看出真实的凹槽而不是画上去的阴影。
  **四个工厂的顶面同样是真实几何凹陷**：生成资源时会读取通用机械的工厂基础模型，
  把顶盖（`shell_01`）压成凹槽底、前面板（`front_panel`）压到同一高度，
  再把 `core` / `shell_02` / `shell_03` 相应截短，最后补上一圈 y=12~16 的边框。
  这样凹槽底面正好在 **1/4 格**深处，与筛矿机一致；边框厚度取 2 单位是为了与
  通用机械自己的侧壳对齐（否则会露出一条侧壳顶面）。管线、端口与等级 LED 全部保留。

  各等级的配色来自通用机械自己的 LED 层：`factory/led` 贴图有 4 条横带
  （绿/橙红/蓝/紫），每个等级通过 `front_led/<tier>` 采样对应的一行，
  因此基础=绿、高级=红、精英=蓝、终极=紫与原版完全一致。
- 转换时**机器内的筛网、原料、能量、进度与侧面配置都会保留**；
  槽位按角色搬运（输入→输入、输出→输出），即使两侧槽位数不同也不会错位。
- 界面下方会额外显示「单次处理：N（并行 M）」。

> 技术说明：通用机械的安装器要求方块声明 `AttributeUpgradeable`，且方块实体返回非空的
> 升级数据，转换才会执行。筛矿机**刻意不带等级属性**（`AttributeTier`）——
> 基础安装器的 `fromTier` 为 `null`，只会匹配没有等级的方块，这一点与通用机械的富集仓一致。

### 合成配方

**筛矿机**（`mekexnihilo:electric_sieve`）—— 中间放**任意 Ex Deorum 筛子**：

```
铁锭     空      铁锭
红石   任意筛子   红石
铁锭     锇锭     铁锭
```

**四个筛矿工厂**沿用通用机械自家的工厂配方形状（`ACA / IPI / ACA`），
`P` 为上一级机器，因此整条产线可以像通用机械的工厂一样逐级合成：

| 工厂 | A（合金） | C（电路） | I（材料） | P（上一级） |
|---|---|---|---|---|
| 基础筛矿工厂 | `mekanism:alloys/basic` | `c:circuits/basic` | `c:ingots/iron` | 筛矿机 |
| 高级筛矿工厂 | `mekanism:alloys/infused` | `c:circuits/advanced` | `c:ingots/osmium` | 基础筛矿工厂 |
| 精英筛矿工厂 | `mekanism:alloys/reinforced` | `c:circuits/elite` | `c:ingots/gold` | 高级筛矿工厂 |
| 终极筛矿工厂 | `mekanism:alloys/atomic` | `c:circuits/ultimate` | `c:gems/diamond` | 精英筛矿工厂 |

> 「任意筛子」由物品标签 **`mekexnihilo:sieves`** 定义，当前列出 Ex Deorum 的全部 67 个
> `*_sieve` 物品（含压缩筛）。若只想让普通（非压缩）筛子可用，直接编辑
> `data/mekexnihilo/tags/item/sieves.json` 即可，无需改代码。
### 筛网等级

等级顺序直接取自 Ex Deorum 自己的筛网注册顺序
（与它的配方查看器 `meshOrder` 一致），共 6 级：

| 等级 | 筛网 | Item ID | 单次处理原料数 |
|---|---|---|---|
| 1 | 线 | `exdeorum:string_mesh` | 1 |
| 2 | 燧石 | `exdeorum:flint_mesh` | 4 |
| 3 | 铁 | `exdeorum:iron_mesh` | 8 |
| 4 | 金 | `exdeorum:golden_mesh` | 16 |
| 5 | 钻石 | `exdeorum:diamond_mesh` | 32 |
| 6 | 下界合金 | `exdeorum:netherite_mesh` | 64 |

筛网通过 Ex Deorum 的 `exdeorum:sieve_meshes` 标签识别。
数据包 / 其它附属新增的筛网也能放进去，等级按 1 处理。

### 产物

产物完全来自 **Ex Deorum 自己的筛矿配方**（`ERecipeTypes.SIEVE`），
所以和手动筛子、以及 Ex Deorum 自带的机械筛完全一致，
并且任何数据包 / KubeJS 改动过的配方都会自动生效。

Ex Deorum 把「掉落概率」编码在配方的 `result_amount`（一个数字提供器，通常是 0/1 概率）里，
本机器按同样方式对每个匹配配方取样，因此概率模型与 Ex Deorum 完全一致。

### 禁用某些输入原料

> **关于「Ex Deorum 的筛矿事件」**：Ex Deorum **没有**筛矿事件。
> 它的 `thedarkcolour.exdeorum.event` 包只有一个生命周期监听器 `EventHandler`，
> 筛矿逻辑本身不触发任何事件。它对 KubeJS 的支持是**配方层面**的
> （`exdeorum.removeDefaultSieveRecipes(...)`、`sieve_mesh` 配方过滤器、`exdeorum:sieve` 配方 schema）。
>
> 因此本模组提供了三种途径，按推荐顺序如下。

#### 1. 物品标签 `mekexnihilo:sieve_blacklist`（推荐，KubeJS 直接可用）

标签里的物品**既不会被机器筛取，也无法放入输入槽**。这是标准物品标签，
KubeJS 用 `ServerEvents.tags` 就能写，无需任何额外接口：

```js
// kubejs/server_scripts/sieve_filter.js
ServerEvents.tags('item', event => {
    event.add('mekexnihilo:sieve_blacklist', 'minecraft:gravel')
    event.add('mekexnihilo:sieve_blacklist', '#minecraft:sand')
})
```

纯数据包写法（`data/mekexnihilo/tags/item/sieve_blacklist.json`）：

```json
{ "replace": false, "values": ["minecraft:gravel", "#minecraft:sand"] }
```

#### 2. 事件 `SieveInputEvent`（条件化过滤）

本模组自己提供的事件，在 NeoForge 事件总线上触发，每次机器准备筛取一批原料时触发一次。
监听器可以从 `getInputs()` 中移除不需要处理的条目：

```java
// Java
NeoForge.EVENT_BUS.addListener(SieveInputEvent.class, event -> {
    // 例：只有下界合金筛网才允许筛沙子
    if (event.getMeshTier() < 6) {
        event.getInputs().removeIf(stack -> stack.is(Items.SAND));
    }
});
```

```js
// KubeJS（NativeEvents，具体写法随 KubeJS 版本略有差异）
NativeEvents.onEvent('com.mekexnihilo.api.SieveInputEvent', event => {
    event.inputs.removeIf(stack => stack.id === 'minecraft:sand')
})
```

被过滤掉的原料**不会占用单次处理量**，所以排在它后面的原料仍会正常加工 ——
不会有「一个被禁用的原料卡住整台机器」的情况。

#### 3. 直接增删 Ex Deorum 的筛矿配方

因为本机器读取的就是 Ex Deorum 的配方缓存，用 KubeJS 增删筛矿配方同样会立即生效：

```js
ServerEvents.recipes(event => {
    event.remove({ type: 'exdeorum:sieve', input: 'minecraft:gravel' })
})
```


### 附魔

把附魔打在**筛网**上（和 Ex Deorum 一样，筛网天然可附魔效率与时运）。

**效率 (Efficiency)**
- 每级减少加工时间 **5%**（`efficiencyReductionPerLevel`，可配置）。
- 减少量**最多 100%**：即使附魔等级远超原版上限，处理时间也不会低于 1 tick。
- 例：效率 V → 100 tick × (1 − 5×5%) = **75 tick**；效率 25 → **1 tick**。

**时运 (Fortune)**
- 每级提高 **20%** 产量（`fortuneBonusPerLevel`，可配置范围 **5%~50%**）。
- 实现方式是对 Ex Deorum 配方给出的产量做倍率，并用**概率取整**，
  因此长期平均产量正好是 `原始产量 × (1 + 时运等级 × 每级加成)`。

> 注：Ex Deorum 自己的机械筛用的是「效率每级 +17% 速度、时运每级 30% 概率追加一次」
> 的公式。本模组按需求方的规格实现，并且两个数值都可以在配置里改成与 Ex Deorum 一致。

---


### 挖掘与拆取

- **硬度**：所有机器都是**石头硬度（1.5）且不需要正确工具**，空手即可挖掉并掉落。
  通用机械自己的方块会强制 `requiresCorrectToolForDrops()`，本模组刻意绕开了这一点
  （改用接收完整 `Properties` 的构造器）。同时加入了 `minecraft:mineable/pickaxe` 标签，
  用镐挖会更快，但不用镐也能挖。
- **扳手拆取**：除通用机械自带的「配置器（扳手模式）」外，**任何属于通用扳手标签
  `c:tools/wrench` 的工具**都能潜行右键直接拆下机器并放进背包。
  通用机械原本只认自己的配置器或显式声明了扳手能力的物品，其它模组的扳手在机器上无效。

### 配置界面语言

配置界面完全使用 NeoForge 内置编辑器，翻译键格式为
`mekexnihilo.configuration.<分组>.<键>`（悬浮提示为再加 `.tooltip`）。
`zh_cn.json` 与 `en_us.json` 都已补全全部条目，因此在「模组列表 → MekExNihilo → Config」
里看到的是中文标签与说明，而不是原始键名。
### 压缩原料（AllTheCompressed）

安装 [AllTheCompressed](https://github.com/Pdiddy973/AllTheCompressed/releases) 后，本模组可以**对压缩方块进行筛选**，用其基础原料的配方产出并放大结果。

规则是**把基础筛矿事件重复执行 `round(底数 ^ 压缩层数)` 次**，而不是把单次产物乘倍。默认产物底数为 **9**，时间与能耗底数为 **1.5**：

| 输入 | 等同的普通原料 | 事件执行次数 | 平均产物 | 耗时 | 能耗 |
|---|---|---|---|---|---|
| `minecraft:sand` | 1 | 1 | 1 | 1 | 1 |
| `allthecompressed:sand_1x` | 9 | **9**（9¹） | 9 倍 | ×1.5 | ×1.5 |
| `allthecompressed:sand_2x` | 81 | **81**（9²） | 81 倍 | ×2.25 | ×2.25 |
| `allthecompressed:sand_3x` | 729 | **729**（9³） | 729 倍 | ×3.375 | ×3.375 |

> **安全上限**：重复次数按指数增长，若不限制，高层数配大底数会在**单个游戏刻内掷骰数亿次**并卡死服务器
> （9 重压缩配底数 9 是 9⁹ ≈ 3.87 亿次）。因此有 `maxCompressedRepeats`（默认 4096）截断，
> 实际只影响那些产物本来就装不下的层数。实测 `gravel_9x` 的理论倍率 3.87e8 被正确截断。
>
> **配置节按需出现**：AllTheCompressed **不是依赖**（连可选运行期依赖都不是），
> 物品仅靠注册名识别。未安装该模组时，`[compressed]` 整节不会生成、也不会显示。

> Ex Deorum 的筛矿配方大多把掉落写成一个概率
> （`BinomialDistributionGenerator`），一次判定只返回 0 或 1。
> 把这一次判定乘倍数，会把「25% 概率掉落」变成「必定掉若干个」；
> 把同一次判定独立重复多次，得到的则是二项分布：均值相同，但保留了原配方的概率特性。
>
> 实测（普通沙砾 vs `gravel_3x`，各 24 次单次筛选）：
> 普通结果为 `[3,2,2,4,3,1,...,0,4,2,1]`（5 种取值），
> 3 重压缩为 `[24,14,19,18,15,19,...,14,15,22]`（**10 种取值**，范围 14~25 而非 8 的倍数），
> 均值比 8.30 ≈ 2³。
| `allthecompressed:sand_3x` | 729 | **8**（2³） | 8 倍 | **8** | **8** |

产物、耗时、能耗各自有独立的底数配置（`compressedYieldBase` / `compressedTimeBase` /
`compressedEnergyBase`），可分别调整；总开关为 `enableCompressedSifting`，
**安装 AllTheCompressed 时默认开启**。

实现上**不依赖 AllTheCompressed**：它没有编译期依赖，也不调用它的任何 API。
物品通过注册名识别（`allthecompressed:<材料>_<层数>x`），基础原料再回查物品注册表
（先查 `minecraft:`，再跨命名空间搜索），因此该模组缺失或改版都不会导致崩溃。

> 黑名单标签 `mekexnihilo:sieve_blacklist` 对压缩原料同样生效：
> 把 `minecraft:sand` 加入黑名单，`allthecompressed:sand_1x` 及其更高层数也会一并被拒绝。

### 自动弹出输出

机器**每个游戏刻**尝试把输出送进相邻容器，每次移动**第一个非空输出槽的全部物品**。

Mekanism 自带的物品弹出在每次尝试后会等待 10 tick，快工厂根本喂不饱，因此物品改由本模组自行推送；
Mekanism 的弹出组件仍负责侧面配置、GUI 页签与存档。

为避免「机器早已被抽干却每刻都做无用尝试」，带有空闲节流：

- 每次**成功弹出重置计时器**
- 连续 **3 分钟**没能弹出任何东西 → 判定该机器未被使用，改为**每秒尝试一次**
- 配置项 `dynamicEject`（默认开）可关闭该节流，关闭后始终每刻尝试

实测：1 tick 内 `slot0 100→0`、相邻箱子 `0→100`，而第二个输出槽**未被触碰**；
下一 tick 才处理它。空闲 3640 tick 后 `throttled=true`，重新放入物品并成功弹出后 `throttled=false`。

### 已移除的配置项

`inputSlots` 与 `batchSizes` 不再可配置，改为固定的内置表 —— 它们同时决定 GUI 布局与菜单结构，
放开容易造成客户端/服务端不一致：

- 各等级输入槽：`1, 3, 4, 5, 8`（筛矿机 → 终极工厂）
- 各筛网等级单次处理量：`1, 4, 8, 16, 32, 64`

### 输出槽堆叠上限

压缩筛选的产量会成倍增长，原版 64 的堆叠远远放不下，因此**单个输出槽的容量默认提升到 8192**
（`outputSlotLimit`，可配置 64~8192）。

这能成立是因为 Mekanism 本身就支持超大堆叠：它的槽位通过
`SerializerHelper.saveOversized` 存档，绕开了原版 `ItemStack` 编解码器 1~99 的限制；
网络同步走 varint，同样没有 99 上限。实测 `stored=8192 / leftover=0 / reloaded=8192`，
存档往返完好。

> 顺带修掉了一个隐患：输出空间判定原本用 `Math.min(物品堆叠上限, 槽位上限)`，
> 而物品堆叠上限恒为 64，会让机器在槽位装到 64 个时就误判「放不下」而停机。
> 现在改用槽位自身的容量，实测单槽可累积到 832 并继续工作。

## 3. 配置文件

位置：`config/mekexnihilo-common.toml`（`COMMON` 类型）。

> 槽位数量必须客户端与服务端一致，所以这些配置放在 `COMMON` 里。
> 槽位数量在**机器建立时**读取：修改后新建或重新放置的机器会立刻使用新值，
> **已经放在世界里的机器保持原来的布局** —— 拆掉重放即可生效。
> 联机时两端配置需保持一致。

```toml
[machine]
    # 筛矿机的输出槽数量
    outputSlots = 12
    # 每提升一个工厂等级额外增加的输出槽
    outputSlotsPerTier = 4
    # 各机器等级的输入槽数量（筛矿机、基础、高级、精英、终极）
    inputSlots = [1, 3, 4, 5, 8]
    # 无效率附魔时一次加工需要的 tick 数
    baseTicks = 100
    # 每级效率减少的加工时间比例（0.05 = 5%）
    efficiencyReductionPerLevel = 0.05
    # 每级时运提高的产量比例（范围 0.05 ~ 0.50）
    fortuneBonusPerLevel = 0.2
    # 运行时每 tick 消耗的能量
    energyPerTick = 200
    # 内部能量缓存
    energyCapacity = 40000
    # 筛网是否消耗耐久（默认关闭，机器里的筛网永不损坏）
    damageMesh = false
    # Ex Deorum 中标记为 by_hand_only 的配方：
    # false = 机器跳过它们（与 Ex Deorum 自带的机械筛一致）
    # true  = 机器也处理它们
    siftByHandOnlyRecipes = false
    # true = 效率/时运受原版等级上限限制；false = 超过上限继续叠加
    respectEnchantmentLimits = false

[tiers]
    # 每个筛网等级单次可处理的原料数，默认 1,4,8,16,32,64
    batchSizes = [1, 4, 8, 16, 32, 64]
```

> **游戏内模组菜单查看配置**：本模组注册了 `IConfigScreenFactory` 扩展点，
> 因此在「模组列表」中选中 **MekExNihilo** 后会出现 **Config** 按钮，
> 点开就是 NeoForge 自带的配置编辑器，可以直接改并保存，无需手工编辑 toml。
> 该注册放在仅客户端的类里，专用服务器不会加载客户端的配置界面类。
> **关于「配置文件未生效」**：早期版本把槽位数缓存在 `static final` 字段里，
> 那会在类加载时把数值冻结，导致改配置看不到效果。现在改为
> **每次建立机器时从配置读取**，已用非默认配置实测验证
> （`inputSlots=[1,2,6,7,9]`、`outputSlots=9`、`outputSlotsPerTier=2`
> → 实测各机器为 1/9、2/11、6/13、7/15、9/17，完全一致）。

### 给筛网附魔（效率 / 时运）

**1.21 里附魔通过标签声明可用物品**：`效率` 只接受 `#minecraft:enchantable/mining`
（斧/镐/铲/锄/剪刀），`时运` 只接受 `#minecraft:enchantable/mining_loot`。
Ex Deorum 的筛网原本不在其中，所以**附魔台不会提供、铁砧也会拒绝附魔书** ——
筛网上根本没有附魔，机器自然也就没有加成。

本模组把全部 Ex Deorum 筛网加进了这两个标签（`replace: false`，与原本内容合并），
因此现在可以正常给筛网附上效率与时运。实测：

| 项目 | 结果 |
|---|---|
| 筛网可被效率附魔 | ✅ `canEnchant = true` |
| 筛网可被时运附魔 | ✅ `canEnchant = true` |
| 效率 V 加工时间 | 100 tick → **75 tick** |
| 时运 III 每个原料产量 | 1.83 → **3.88** |

> 若你的整合包用 KubeJS 或 `/give` 直接塞附魔，注意 1.21 的写法是
> `exdeorum:netherite_mesh[minecraft:enchantments={levels:{"minecraft:efficiency":5}}]`，
> 1.20.4 的 `{Enchantments:[{id:...,lvl:...}]}` 在 1.21 已经无效。
`respectEnchantmentLimits` 默认为 `false`：因为按「每级 -5%」计算，要达到 100% 减时
需要等级 20，而原版效率上限只有 V。设为 `true` 则会按原版上限截断。

---

## 4. 从源码构建

需要 **JDK 21**。

```powershell
# 仓库根目录
./build.ps1 build          # 等价于在 MekExNihilo/ 下执行 gradlew build
```

产物：`MekExNihilo/build/libs/mekexnihilo-1.0.4+mc1.21.1.jar`

开发环境运行：

```powershell
./build.ps1 runClient      # 启动客户端
./build.ps1 runServer      # 启动服务端
```

依赖 jar 放在仓库根的 `libs/`：

- `exdeorum-3.10.jar` —— 从 [Modrinth](https://modrinth.com/mod/ex-deorum) 下载
- `mekanism.jar` / `mekanism-api.jar` —— 由 `build.gradle` 从 ModMaven 自动解析（无需手动放置）

### 构建脚本说明

`build.ps1` 只是对 Gradle 的一层包装，用于处理本机环境的两个特殊情况：

1. 未设置 `JAVA_HOME` —— 脚本会指向本机 JDK 21；
2. Gradle 默认把原生库和缓存写到 `~/.gradle`，脚本改为使用仓库内的 `.gradle-home/`
   （通过 `GRADLE_USER_HOME` 与 `-Dorg.gradle.native.dir`）。

如果这两点在你的机器上不是问题，直接使用 `MekExNihilo/gradlew` 也可以。

---

## 5. 工程结构

```
MekExNihilo/
├── build.gradle                     ModDevGradle 构建脚本
├── settings.gradle                  仓库配置（含 ModMaven）
├── gradle.properties                版本与模组元数据
└── src/main/
    ├── java/com/mekexnihilo/
    │   ├── MekExNihilo.java                   主入口，注册配置与所有 DeferredRegister
    │   ├── MekExNihiloConfig.java             全部可配置项
    │   ├── MekExNihiloTags.java               本模组的物品标签（sieve_blacklist）
    │   ├── ExDeorumCompat.java              Ex Deorum 的筛网识别 / 等级 / 配方查询
    │   ├── api/SieveInputEvent.java         输入过滤事件（Ex Deorum 没有筛矿事件）
    │   ├── upgrade/SieveUpgradeData.java    工厂安装器转换时的数据搬运
    │   ├── SieveLayout.java                 槽位/界面几何，菜单与界面共用
    │   ├── tile/
    │   │   ├── TileEntitySieve.java         机器本体与加工逻辑
    │   │   └── TileEntitySieveFactory.java  工厂版本（并行加工）
    │   ├── inventory/container/SieveContainer.java   菜单
    │   ├── client/
    │   │   ├── MekExNihiloClient.java         界面注册
    │   │   └── gui/GuiSieve.java            筛矿机界面
    │   └── registry/                        方块、方块实体、菜单、创造栏、语言键
    └── resources/
        ├── META-INF/neoforge.mods.toml
        ├── pack.mcmeta
        ├── assets/mekexnihilo/                模型、方块状态、材质、语言文件
        └── data/mekexnihilo/
            ├── loot_table/                  掉落表
            └── tags/item/sieve_blacklist.json   输入黑名单标签（默认空）
```

设计要点：

- **所有与 Ex Deorum 的耦合都集中在 `ExDeorumCompat`**：筛网标签、等级顺序、配方查询。
  其余代码不直接依赖 Ex Deorum 的类。
- **槽位只定义一次**。`TileEntitySieve` 在 `getInitialInventory` 中按 `SieveLayout`
  给出的坐标创建槽位，`MekanismTileContainer` 会自动把它们变成菜单槽位，
  界面再用同一套坐标绘制进度条与文字，因此三者不可能错位。
- **加工逻辑不依赖通用机械的配方系统**。筛矿配方来自 Ex Deorum 的配方缓存而不是
  Mekanism 的 `RecipeType`，所以直接在 `onUpdateServer` 里处理，并复用 Mekanism 的
  能量容器、槽位与弹出（ejector）组件。
- **产物按「单个原料」结算**。高等级筛网一次处理 64 个原料时，产生的掉落物可能超过
  输出槽容量；逐个结算可以保证「放不下就停下等待」，既不会死锁也不会吞物品。

---

## 6. 开发笔记

几个踩过的坑，供改这个模组的人参考。

### Ex Deorum 的配方查询 API 改过签名

3.x 是静态的 `RecipeUtil.getSieveRecipes(mesh, stack)`，3.12 换成了
`RecipeUtil.getCaches(level).getSieveRecipes(...)`。调用任意一个，在另一个版本上都会
`NoSuchMethodError` 崩溃。本模组因此不碰它的工具类，改为从 `exdeorum:sieve` 配方类型
读 `RecipeManager` 并自行缓存。附带好处是读到的是 KubeJS / 数据包改造后的配方表。

### Mekanism 的弹出器最快每半秒一次

`TileComponentEjector` 每次尝试后把 `tickDelay` 设为 10 tick。自动化产线喂不饱这个速度，
所以物品改由机器自己每 tick 推送；弹出组件只保留侧面配置、GUI 页签和存档。

### 输出槽为什么能超过 64

NeoForge 的 `Item.ABSOLUTE_MAX_STACK_SIZE` 是 99，`ItemStack` 的编解码器把 count 限死在
1~99，直接用大数字会坏档。Mekanism 的槽位走 `SerializerHelper.saveOversized` 存档、
varint 同步，两条路径都没有这个上限，8192 才安全。

### 筛网原本不在附魔标签里

Ex Deorum 的筛网没有被加进 `minecraft:enchantable/mining` 与 `mining_loot`，
不补这两个标签，附在筛网上的效率与时运不会生效。

### 工厂顶面贴图要按 Mekanism 的 UV 拆法改

Mekanism 把工厂的顶面拆成 `front_panel`（uv `[0,12,16,16]`）和旋转 180° 的 `shell_01`，
直接改 uv 会让顶面纹理糊掉。重写 uv，让 v 线性映射到方块的 z 轴才对。

### 依赖版本范围是怎么定的

`10.7.10.73` 及更早的 Mekanism 编译不过（`Holder<Block>` 不能转 `IBlockProvider`、
`forSideWithConfig`、`TileEntityTypeRegistryObject<TileEntityMekanism>` 都对不上），
所以下限是 `10.7.11.76`。Ex Deorum 用 `javap` 逐个核对过 3.3 到 3.12 的 API 面，
全部可用，下限取 3.3。

---

## 7. 许可

MIT，见 [LICENSE](LICENSE)。Ex Deorum 与 Mekanism 分别遵循其各自的许可。
