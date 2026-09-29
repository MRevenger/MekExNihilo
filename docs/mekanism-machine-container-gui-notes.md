# Mekanism 10.7.19 (NeoForge 21.1.x / MC 1.21.1) — custom machine Container + Screen

Sources decompiled at `libs/mek-src/mekanism`; authoritative jar `libs/mekanism.jar`
(`META-INF/neoforge.mods.toml` → `version="10.7.19"`, `neoforge [21.1.194,)`, `minecraft [1.21.1]`).

---

## 1. Container base classes

### `mekanism/common/inventory/container/MekanismContainer.java`

```java
public abstract class MekanismContainer extends AbstractContainerMenu implements ISecurityContainer   // L65
    public static final int BASE_Y_OFFSET = 84;                                                       // L67
    public static final int TRANSPORTER_CONFIG_WINDOW = 0, SIDE_CONFIG_WINDOW = 1,
                            UPGRADE_WINDOW = 2, SKIN_SELECT_WINDOW = 3;                               // L68-71
    protected final Inventory inv;                                                                     // L73

    protected MekanismContainer(ContainerTypeRegistryObject<?> type, int id, Inventory inv)            // L94
        // -> super(type.get(), id); this.inv = inv;  (server: selectedWindows = new HashMap<>(1))

    @NotNull @Override protected Slot addSlot(@NotNull Slot slot)                                      // L113
    protected void addSlotsAndOpen()                                                                   // L136
    protected int  getInventoryYOffset()  { return BASE_Y_OFFSET; }                                    // L192  (=84)
    protected int  getInventoryXOffset()  { return 8; }                                                // L196
    protected void addInventorySlots(@NotNull Inventory inv)                                           // L200
    protected void addArmorSlots(@NotNull Inventory inv, int x, int y, int offhandOffset)              // L218
    protected HotBarSlot createHotBarSlot(@NotNull Inventory inv, int index, int x, int y)             // L229
    protected void addSlots()                                                                          // L233 (empty hook)
    @NotNull @Override public ItemStack quickMoveStack(@NotNull Player player, int slotID)             // L253 (implemented)
    public void track(ISyncableData data)                                                              // L462
    public void trackArray(int[]/long[]/boolean[]/...)                                                 // L475-523
    public void startTrackingServer(Object key, ISpecificContainerTracker tracker)                     // L142
    public List<ISyncableData> startTracking(Object key, ISpecificContainerTracker tracker)            // L149
    public void stopTracking(Object key)                                                               // L158
    @Override public void broadcastChanges()                                                           // L635
    public interface ISpecificContainerTracker { List<ISyncableData> getSpecificSyncableData(); }      // L683
```

`addSlotsAndOpen()` (L136-140) — **"must be called at end of extending classes constructors"**:

```java
protected void addSlotsAndOpen() {
    addSlots();
    addInventorySlots(inv);
    openInventory(inv);
}
```

**`addInventorySlots` semantics** (L200-216) — this is where the player inventory/hotbar come from.
It returns immediately if `this instanceof IEmptyContainer`, then:

```java
int yOffset = getInventoryYOffset();          // 84
int xOffset = getInventoryXOffset();          // 8
for (int slotY = 0; slotY < 3; slotY++)
  for (int slotX = 0; slotX < 9; slotX++)
    addSlot(new MainInventorySlot(inv, Inventory.getSelectionSize() + slotX + slotY * 9,   // indices 9..35
                                  xOffset + slotX * 18, yOffset + slotY * 18));
yOffset += 58;                                                                            // 142
for (int slotX = 0; slotX < Inventory.getSelectionSize(); slotX++)                        // hotbar 0..8
    addSlot(createHotBarSlot(inv, slotX, xOffset + slotX * 18, yOffset));
```

`addSlot` (L113-131) is the only supported way to register a slot: it also files the slot into
`inventoryContainerSlots / armorSlots / mainInventorySlots / hotBarSlots / offhandSlots` for the generic
`quickMoveStack`, and lets `IHasExtraData` slots add trackers via `hasExtraData.addTrackers(inv.player, this::track)`.

**Slot coordinates** are GUI-relative pixels, top-left of the 16×16 item area. Slot background (18×18) is drawn by
the GUI at `slot.x - 1, slot.y - 1`. Vanilla layout therefore: player main inv 8..160 x, y 84/102/120; hotbar y 142.

### `mekanism/common/inventory/container/tile/MekanismTileContainer.java`

```java
public class MekanismTileContainer<TILE extends TileEntityMekanism> extends MekanismContainer {       // L19
    @NotNull protected final TILE tile;                                                                // L24

    public MekanismTileContainer(ContainerTypeRegistryObject<?> type, int id, Inventory inv, @NotNull TILE tile) {  // L26
        super(type, id, inv);
        this.tile = tile;
        addContainerTrackers();
        addSlotsAndOpen();
    }

    protected void addContainerTrackers()   { tile.addContainerTrackers(this); }                       // L33-35
    public TILE getTileEntity()             { return tile; }                                           // L37
    @Override public boolean canPlayerAccess(@NotNull Player player)                                   // L41
        // IBlockSecurityUtils.INSTANCE.canAccess(player, level, tile.getBlockPos(), tile)
    @Override protected void openInventory(@NotNull Inventory inv)  { super...; tile.open(inv.player); }   // L51
    @Override protected void closeInventory(@NotNull Player player) { super...; tile.close(player); }      // L57
    @Override public boolean stillValid(@NotNull Player player)                                        // L63
        // tile.hasGui() && !tile.isRemoved() && WorldUtils.isBlockLoaded(tile.getLevel(), tile.getBlockPos())
    @Override protected void addSlots()                                                                // L69
    @Nullable public VirtualInventoryContainerSlot getUpgradeSlot()                                    // L93
    @Nullable public VirtualInventoryContainerSlot getUpgradeOutputSlot()                              // L98
}
```

`addSlots()` body (L69-90) — the automatic machine-slot layout:

```java
super.addSlots();
if (this instanceof IEmptyContainer) return;              // no inventory slots
if (tile.supportsUpgrades()) {                            // upgrade + upgrade-output virtual slots first
    addSlot(upgradeSlot = tile.getComponent().getUpgradeSlot().createContainerSlot());
    addSlot(upgradeOutputSlot = tile.getComponent().getUpgradeOutputSlot().createContainerSlot());
}
if (tile.hasInventory()) {
    List<IInventorySlot> inventorySlots = tile.getInventorySlots(null);   // TileEntityMekanism L1214
    for (IInventorySlot inventorySlot : inventorySlots) {
        Slot containerSlot = inventorySlot.createContainerSlot();         // IInventorySlot L182 (nullable)
        if (containerSlot != null) addSlot(containerSlot);
    }
}
```

**Abstract methods to implement: NONE.** Both `quickMoveStack` and `stillValid` are already implemented; a concrete
subclass only needs a constructor. Minimal real example (`FormulaicAssemblicatorContainer.java`, whole file):

```java
public class FormulaicAssemblicatorContainer extends MekanismTileContainer<TileEntityFormulaicAssemblicator> {  // L8
    public FormulaicAssemblicatorContainer(int id, Inventory inv, TileEntityFormulaicAssemblicator tile) {       // L10
        super(MekanismContainerTypes.FORMULAIC_ASSEMBLICATOR, id, inv, tile);
    }
    @Override protected int getInventoryYOffset() { return 148; }                                                // L15
}
```

Useful overridable hooks: `getInventoryXOffset()`, `getInventoryYOffset()`, `addSlots()`,
`addInventorySlots(Inventory)`, `addContainerTrackers()`, `createHotBarSlot(...)`, `addArmorSlots(...)`.
`DigitalMinerConfigContainer` shows suppressing the machine slots (`addSlots(){}` empty, L23-26).

---

## 2. Registration of container types (server side) — `ContainerTypeDeferredRegister`

`mekanism/common/registration/impl/ContainerTypeDeferredRegister.java`

```java
public class ContainerTypeDeferredRegister extends MekanismDeferredRegister<MenuType<?>> {          // L28
    public ContainerTypeDeferredRegister(String modid) {                                             // L30
        super(Registries.MENU, modid, ContainerTypeRegistryObject::new);
    }

    // ---- use THIS one for a custom container class ----
    public <TILE extends TileEntityMekanism, CONTAINER extends MekanismTileContainer<TILE>>
    ContainerTypeRegistryObject<CONTAINER> register(String name, Class<TILE> tileClass,
                                                    IMekanismContainerFactory<TILE, CONTAINER> factory) {  // L65
        return registerMenu(name, () -> MekanismContainerType.tile(tileClass, factory));                    // L67
    }

    // generic MekanismTileContainer<TILE> with automatic slots
    public <TILE extends TileEntityMekanism> ContainerTypeRegistryObject<MekanismTileContainer<TILE>>
        register(String name, Class<TILE> tileClass)                                                    // L42
        // factory = (id, inv, data) -> new MekanismTileContainer<>(registryObject, id, inv, data)

    public <TILE extends TileEntityMekanism> ContainerTypeRegistryObject<EmptyTileContainer<TILE>>
        registerEmpty(String name, Class<TILE> tileClass)                                               // L53

    public <CONTAINER extends AbstractContainerMenu> ContainerTypeRegistryObject<CONTAINER>
        register(String name, MenuSupplier<CONTAINER> factory)                                          // L92
        // -> new MenuType<>(factory, FeatureFlags.VANILLA_SET)

    public <CONTAINER extends AbstractContainerMenu> ContainerTypeRegistryObject<CONTAINER>
        register(String name, IContainerFactory<CONTAINER> factory)                                     // L96
    public <CONTAINER extends AbstractContainerMenu> ContainerTypeRegistryObject<CONTAINER>
        registerMenu(String name, Supplier<MenuType<CONTAINER>> supplier)                               // L104

    public <TILE extends TileEntityMekanism> ContainerBuilder<TILE> custom(String name, Class<TILE> tc) // L112
    //  ContainerBuilder: offset(int,int) L128, armorSideBar() L134, armorSideBar(x,y) L138,
    //                    armorSideBar(x,y,offhandOffset) L142, build() L149
}
```

Functional interface (`MekanismContainerType.java` L109-113):

```java
@FunctionalInterface
public interface IMekanismContainerFactory<T, CONTAINER extends AbstractContainerMenu> {
    CONTAINER create(int id, Inventory inv, T data);      // T == your tile class
}
```

Import path: `mekanism.common.inventory.container.type.MekanismContainerType.IMekanismContainerFactory`.

`MekanismContainerType` / `BaseMekanismContainerType` (`.../container/type/`):

```java
public class MekanismContainerType<T, CONTAINER extends AbstractContainerMenu>
      extends BaseMekanismContainerType<T, CONTAINER, IMekanismContainerFactory<T, CONTAINER>> {     // L23
    public static <TILE extends TileEntityMekanism, CONTAINER extends AbstractContainerMenu>
    MekanismContainerType<TILE, CONTAINER> tile(Class<TILE> type, IMekanismContainerFactory<TILE, CONTAINER> ctor) { // L25
        return new MekanismContainerType<>(type, ctor, (id, inv, buf) -> ctor.create(id, inv, getTileFromBuf(buf, type)));
    }
    @Nullable public CONTAINER create(int id, Inventory inv, Object data)                             // L57  (server)
    @Nullable public MenuConstructor create(Object data)                                              // L65
    @NotNull private static <TILE extends BlockEntity> TILE getTileFromBuf(FriendlyByteBuf buf, Class<TILE> type) // L74
        // buf.readBlockPos(); WorldUtils.getTileEntity(type, Minecraft.getInstance().level, pos)
}
public abstract class BaseMekanismContainerType<T, CONTAINER extends AbstractContainerMenu, FACTORY>
      extends MenuType<CONTAINER> {                                                                   // L8
    protected BaseMekanismContainerType(Class<T> type, FACTORY mekanismConstructor, IContainerFactory<CONTAINER> constructor) {
        super(constructor, FeatureFlags.VANILLA_SET);                                                 // L14
    }
}
```

**Network contract:** the client-side menu factory reads a `BlockPos` first from the open-menu buffer, then looks the
tile up in the *client* level. The open path is `TileEntityMekanism.openGui` (`TileEntityMekanism.java` L588-614):

```java
player.openMenu(Attribute.getOrThrow(getBlockHolder(), AttributeGui.class).getProvider(this, true), buffer -> {
    buffer.writeBlockPos(worldPosition);
    encodeExtraContainerData(buffer);      // L616, override to append extra data
});
```

`AttributeGui` (`common/block/attribute/AttributeGui.java` L11-24) wraps
`Supplier<ContainerTypeRegistryObject<? extends MekanismContainer>> containerRegistrar`; block types get it through
`BlockTypeTile.BlockTileBuilder.withGui(Supplier<ContainerTypeRegistryObject<? extends MekanismContainer>>)` /
`withGui(..., ILangEntry customName)` (L49-55). `TileEntityMekanism` L348: `hasGui = Attribute.has(block, AttributeGui.class)`,
and `hasGui()` L423-425 is what `stillValid` and `openGui` check — **without `withGui(...)` the GUI never opens**.

`ContainerTypeRegistryObject.getProvider(...)` (`ContainerTypeRegistryObject.java` L44-53) resolves the
`MenuConstructor` and wraps it in `ContainerProvider` (`ContainerProvider.shouldTriggerClientSideContainerClosingOnOpen()`
→ `resetMousePosition`).

Registrar registration to the mod bus (as the addon must do):
`MekanismContainerTypes.java` L92 `new ContainerTypeDeferredRegister(Mekanism.MODID)`;
`Mekanism.java` L238 `MekanismContainerTypes.CONTAINER_TYPES.register(modEventBus);`
`MekanismGenerators.java` L75 `GeneratorsContainerTypes.CONTAINER_TYPES.register(modEventBus);`

`MekanismContainerTypes` registration examples (L112, L124, L127, L132, L134):

```java
public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityEnergizedSmelter>>
    ENERGIZED_SMELTER = CONTAINER_TYPES.register(MekanismBlocks.ENERGIZED_SMELTER, TileEntityEnergizedSmelter.class);      // L124
public static final ContainerTypeRegistryObject<FormulaicAssemblicatorContainer>
    FORMULAIC_ASSEMBLICATOR = CONTAINER_TYPES.register(MekanismBlocks.FORMULAIC_ASSEMBLICATOR,
        TileEntityFormulaicAssemblicator.class, FormulaicAssemblicatorContainer::new);                                      // L127
public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityOredictionificator>>
    OREDICTIONIFICATOR = CONTAINER_TYPES.custom(MekanismBlocks.OREDICTIONIFICATOR, TileEntityOredictionificator.class)
        .offset(30, 64).build();                                                                                            // L132
```

`custom(...).build()` (L149-172) creates an anonymous `MekanismTileContainer` overriding
`getInventoryXOffset()/getInventoryYOffset()` (`super + offsetX/Y`) and, when `armorSideBar(...)` was used,
`addInventorySlots` to append armour + offhand after the normal player inventory.

`MekanismGenerators` (`generators/common/registries/GeneratorsContainerTypes.java`) is the definitive in-repo example of a
**separate modid** consuming this API (L25-42).

---

## 3. GUI / Screen

Hierarchy (all in `mekanism/client/gui`):

```
AbstractContainerScreen<CONTAINER>
  └─ VirtualSlotContainerScreen<CONTAINER>        (VirtualSlotContainerScreen.java L21)
       └─ GuiMekanism<CONTAINER>                  (GuiMekanism.java L51, implements IGuiWrapper)
            └─ GuiMekanismTile<TILE, CONTAINER>   (GuiMekanismTile.java L15)
                 └─ GuiConfigurableTile<TILE, CONTAINER>  (GuiConfigurableTile.java L11)
                      └─ GuiElectricMachine<...>          (machine/GuiElectricMachine.java L18)
```

Constructors (all `protected` at the base level, so a subclass in any package can call `super`):

```java
protected GuiMekanism(CONTAINER container, Inventory inv, Component title)                       // GuiMekanism L69
protected GuiMekanismTile(CONTAINER container, Inventory inv, Component title) {                 // GuiMekanismTile L24
    super(container, inv, title); tile = container.getTileEntity();
}
protected GuiConfigurableTile(CONTAINER container, Inventory inv, Component title)               // GuiConfigurableTile L17
public GuiElectricMachine(CONTAINER container, Inventory inv, Component title) {                 // GuiElectricMachine L20
    super(container, inv, title);
    dynamicSlots = true;
}
```

**Recommended for an addon machine:** extend `GuiMekanismTile<MyTile, MyContainer>` (or `GuiConfigurableTile` if the tile
implements `ISideConfiguration`), with ctor `public GuiMyMachine(MyContainer container, Inventory inv, Component title)`,
and set `dynamicSlots = true` so the container's slots are auto-drawn.

Element API:

```java
protected void addGuiElements()                                              // GuiMekanism L133
    // { if (dynamicSlots) addSlots(); }   -- override, call super.addGuiElements() first
protected <T extends GuiElement> T addRenderableWidget(T element)            // GuiMekanism L156 (returns the element)
protected void drawForegroundText(GuiGraphics, int mouseX, int mouseY)       // GuiMekanism L449
protected void renderTitleText(GuiGraphics)                                  // L180
protected void renderInventoryText(GuiGraphics)                              // L196
protected ResourceLocation getButtonLocation(String name)                    // L214 -> gui/button/<name>.png
```

Element constructors (all take GUI-relative x/y):

| Element | Signature | Source |
|---|---|---|
| `GuiUpArrow` | `GuiUpArrow(IGuiWrapper gui, int x, int y)` (8×10) | GuiUpArrow.java L12 |
| `GuiVerticalPowerBar` | `GuiVerticalPowerBar(IGuiWrapper gui, IEnergyContainer container, int x, int y)` / `(..., int desiredHeight)`; also `IBarInfoHandler` overloads | GuiVerticalPowerBar.java L22/L26/L40/L44 (tex `gui/bar/vertical_power.png`, 4×52, default height 52) |
| `GuiEnergyTab` | `GuiEnergyTab(IGuiWrapper gui, IInfoHandler handler)` / `(IGuiWrapper, MachineEnergyContainer<?>, LongSupplier lastEnergyUsed)` / `(IGuiWrapper, MachineEnergyContainer<?>, BooleanSupplier isActive)` | GuiEnergyTab.java L39/L44/L50 (placed at `-26,137`, 26×26) |
| `GuiProgress` | `GuiProgress(IProgressInfoHandler handler, ProgressType type, IGuiWrapper gui, int x, int y)` and `IBooleanProgressInfoHandler` overload | GuiProgress.java L35/L39 |
| `GuiSlot` | `GuiSlot(SlotType type, IGuiWrapper gui, int x, int y)` | GuiSlot.java L66 |

Chainable on `GuiProgress`: `.warning(WarningType, BooleanSupplier)` L51, `.colored(ColorDetails)` L45,
`.recipeViewerCategory(tile)` / `.recipeViewerCategories(...)` L104, `.recipeViewerCrafting()`.
`ProgressType` (ProgressType.java L8-19): `BAR(25,9)`, `LARGE_RIGHT(48,8)`, `LARGE_LEFT(48,8)`, `TALL_RIGHT(20,15)`,
`RIGHT(32,8)`, `SMALL_RIGHT(28,8)`, `SMALL_LEFT(28,8)`, `BI(16,6)`, `FLAME(13,13)`, `INSTALLING(10,14)`,
`UNINSTALLING(12,12)`, `DOWN(8,20)`. `SlotType` (SlotType.java L9-20): `NORMAL(18,18)`, `DIGITAL`, `POWER`, `EXTRA`,
`INPUT`, `OUTPUT`, `OUTPUT_WIDE(42,26)`, `OUTPUT_LARGE(36,54)`, `ORE`, `INNER_HOLDER_SLOT`.

Canonical `addGuiElements` body (`GuiElectricMachine` L26-34):

```java
super.addGuiElements();
addRenderableWidget(new GuiUpArrow(this, 68, 38));
addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 164, 16))
      .warning(WarningType.NOT_ENOUGH_ENERGY, tile.getWarningCheck(RecipeError.NOT_ENOUGH_ENERGY));
addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getActive));
addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.BAR, this, 86, 38).recipeViewerCategory(tile))
      .warning(WarningType.INPUT_DOESNT_PRODUCE_OUTPUT, tile.getWarningCheck(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT));
```

### Background texture and dimensions

* `GuiMekanism.BASE_BACKGROUND = MekanismUtils.getResource(ResourceType.GUI, "base.png")` (L53)
  → `Mekanism.rl("gui/base.png")` (MekanismUtils L417-419, `Mekanism.rl` L277-279 = namespace only)
  → **ResourceLocation `mekanism:gui/base.png`**, real file **`assets/mekanism/gui/base.png`**.
  Verified against the jar: 260 PNGs under `assets/mekanism/gui/`, **zero** under `assets/mekanism/textures/gui/`.
  `ResourceType` enum (MekanismUtils L864-883): `GUI("gui")`, `GUI_BUTTON("gui/button")`, `GUI_BAR("gui/bar")`,
  `GUI_GAUGE("gui/gauge")`, `GUI_HUD("gui/hud")`, `GUI_ICONS("gui/icons")`, `GUI_MODE("gui/mode")`,
  `GUI_PROGRESS("gui/progress")`, `GUI_RADIAL("gui/radial")`, `GUI_SLOT("gui/slot")`, `GUI_TAB("gui/tabs")`;
  `getPrefix()` returns `prefix + "/"` (L891-893).
* `renderBg` (`GuiMekanism` L712-721) 9-slices it:
  `GuiUtils.renderBackgroundTexture(guiGraphics, BASE_BACKGROUND, 4, 4, leftPos, topPos, imageWidth, imageHeight, 256, 256);`
  (`GuiUtils.renderBackgroundTexture` L49-52 → `blitNineSlicedSized`). Anything smaller than 8×8 in either dimension is skipped.
* `imageWidth`/`imageHeight` come from `AbstractContainerScreen`: defaults `176` / `166`
  (verified in the NeoForge-patched MC sources: `protected int imageWidth = 176; protected int imageHeight = 166;`).
  `getXSize()` / `getYSize()` are added by the NeoForge patch: `public int getXSize() { return imageWidth; }`,
  `public int getYSize() { return imageHeight; }` (they satisfy `IGuiWrapper` L32/34).
* Label positions (same file): `titleLabelY = 6`, `inventoryLabelX = 8`, `inventoryLabelY = imageHeight - 94`.
  Because these are assigned in the super constructor, a subclass that changes `imageHeight` must redo the label:
  `GuiFormulaicAssemblicator` L43-48 does `imageHeight += 64; inventoryLabelY = imageHeight - 94; dynamicSlots = true;`.
  Other variants: `GuiPortableTeleporter` `imageHeight = 172`; `GuiSeismicReader` `150×182`; `GuiModuleTweaker` `imageWidth = 266`.
* For an addon texture, ship it at `assets/<modid>/gui/<name>.png` and use
  `ResourceLocation.fromNamespaceAndPath(modid, "gui/<name>.png")`; override
  `protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY)` and either call
  `super.renderBg(...)` (keeps Mekanism's base.png) or call `GuiUtils.renderBackgroundTexture(...)` yourself.

### Slots on screen

`GuiMekanism.addSlots()` (L662-697) — invoked from `addGuiElements()` when `dynamicSlots == true`:

```java
for (Slot slot : menu.slots) {
    if (slot instanceof InventoryContainerSlot containerSlot) {
        ContainerSlotType slotType = containerSlot.getSlotType();
        DataType dataType = findDataType(containerSlot);           // side-config aware
        SlotType type = ...;                                       // NORMAL / POWER / mapped from DataType
        GuiSlot guiSlot = new GuiSlot(type, this, slot.x - 1, slot.y - 1);
        containerSlot.addWarnings(guiSlot);
        SlotOverlay overlay = containerSlot.getSlotOverlay();
        if (overlay != null) guiSlot.with(overlay);
        if (slotType == ContainerSlotType.VALIDITY) guiSlot.validity(() -> checkValidity(index));
        addRenderableWidget(guiSlot);
    } else {
        addRenderableWidget(new GuiSlot(SlotType.NORMAL, this, slot.x - 1, slot.y - 1));
    }
}
```

So **every** slot in `menu.slots` (machine slots *and* the player inventory/hotbar) is displayed, at
`(slot.x - 1, slot.y - 1)`, i.e. container coordinates map 1:1 to GUI pixels. There is no hard slot-count limit; the
limit is the 18×18 slot grid inside `imageWidth × imageHeight`.

Element coordinates are GUI-relative: `GuiElement` L84-89 (`super(gui.getGuiLeft() + x, gui.getGuiTop() + y, ...)`,
`relativeX = x; relativeY = y`), and `leftPos = (width - imageWidth)/2`, `topPos = (height - imageHeight)/2`.

---

## 4. Client registration (screens) — addon side

`mekanism/client/ClientRegistrationUtil.java`:

```java
public static <C extends AbstractContainerMenu, U extends Screen & MenuAccess<C>> void registerScreen(
      RegisterMenuScreensEvent event, ContainerTypeRegistryObject<C> type, ScreenConstructor<C, U> factory) {   // L138
    event.register(type.get(), factory);
}

// generics workaround only, NOT needed by an addon:
public static <TILE extends TileEntityElectricMachine, C extends MekanismTileContainer<TILE>> void
      registerElectricScreen(RegisterMenuScreensEvent event, ContainerTypeRegistryObject<C> type)                // L145
```

Event class (verified in `neoforge-21.1.215-sources.jar`, `net/neoforged/neoforge/client/event/RegisterMenuScreensEvent.java`):

```java
public class RegisterMenuScreensEvent extends Event implements IModBusEvent {
    public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(
            MenuType<? extends M> menuType, MenuScreens.ScreenConstructor<M, U> screenConstructor);
    // throws IllegalStateException on duplicate registration
}
```

`MenuScreens.ScreenConstructor` (vanilla `net/minecraft/client/gui/screens/MenuScreens.java`):

```java
interface ScreenConstructor<T extends AbstractContainerMenu, U extends Screen & MenuAccess<T>> {
    U create(T menu, Inventory inventory, Component title);
}
```

Because it is an `IModBusEvent`, listen on the **mod event bus**. Mekanism does it with the annotation
(`ClientRegistration.java` L226 `@EventBusSubscriber(modid = Mekanism.MODID, value = Dist.CLIENT)` and L390-391
`@SubscribeEvent public static void registerScreens(RegisterMenuScreensEvent event)`), and MekanismGenerators does
exactly the same from its own modid (`GeneratorsClientRegistration.java` L61 + L113-132):

```java
@EventBusSubscriber(modid = "mymod", value = Dist.CLIENT)
public class MyClientRegistration {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MyContainerTypes.MY_MACHINE.get(), GuiMyMachine::new);
        // or: ClientRegistrationUtil.registerScreen(event, MyContainerTypes.MY_MACHINE, GuiMyMachine::new);
    }
}
```

Alternative: `modEventBus.addListener(RegisterMenuScreensEvent.class, MyClientRegistration::registerScreens);` from a
`Dist.CLIENT`-guarded place. Keep screen classes in a client-only class so they are never loaded on a dedicated server.
The lambda/ctor-ref form is needed when generics cannot be inferred, e.g.
`(MekanismTileContainer<TileEntitySolarGenerator> c, Inventory i, Component t) -> new GuiSolarGenerator<>(c, i, t)`
(GeneratorsClientRegistration L129).

---

## 5. How a custom container gets the tile's inventory (`IContentsListener`)

* `mekanism/api/IContentsListener.java` L6-13:
  `@FunctionalInterface public interface IContentsListener { void onContentsChanged(); }`
* `IInventorySlot extends INBTSerializable<CompoundTag>, IContentsListener` (`api/inventory/IInventorySlot.java` L20) and has
  `@Nullable default Slot createContainerSlot()` L182 (default `null`) — this is what `MekanismTileContainer.addSlots()` uses.
* `BasicInventorySlot implements IInventorySlot` (`common/inventory/slot/BasicInventorySlot.java` L28); static factories
  `at(@Nullable IContentsListener listener, int x, int y)` L30, `at(Predicate<ItemStack> validator, listener, x, y)` L34,
  `at(validator, listener, x, y, limit)` L38, `at(canExtract, canInsert, listener, x, y)` L46, plus `BiPredicate<ItemStack, AutomationType>` variants L52/L57.
  Each slot stores its own GUI x/y (`getGuiX()` L107) which `createContainerSlot()` turns into `Slot` coordinates.
* Specialised slots live in `common/inventory/slot/` (`InputInventorySlot`, `OutputInventorySlot`, `EnergyInventorySlot`, ...).
* The tile owns the slots: override
  `protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener)`
  (`TileEntityRecipeMachine.java` L186; 1-arg variant `TileEntityMekanism.java` L1208) and use
  `InventorySlotHelper.forSideWithConfig(this)` + `builder.addSlot(...)` + `builder.build()` — see
  `TileEntityElectricMachine.java` L84-92.
* The container reads them back with `tile.getInventorySlots(null)` → `List<IInventorySlot>`
  (`TileEntityMekanism.java` L1212-1216, `final`), gated by `hasInventory()` L443-445
  (`itemHandlerManager != null && itemHandlerManager.canHandle()`).
* Container data sync: `MekanismTileContainer` ctor calls `addContainerTrackers()` (L29) → `tile.addContainerTrackers(this)`
  (`TileEntityMekanism.java` L921). Example `TileEntityProgressMachine.java` L90-94:

```java
@Override public void addContainerTrackers(MekanismContainer container) {
    super.addContainerTrackers(container);
    container.track(SyncableInt.create(this::getOperatingTicks, this::setOperatingTicks));
    container.track(SyncableInt.create(this::getTicksRequired, value -> ticksRequired = value));
}
```

  which is why `tile::getScaledProgress` (`TileEntityProgressMachine` L35,
  `getOperatingTicks() / (double) ticksRequired`) is usable client-side in `new GuiProgress(...)`.
  Available helpers: `SyncableInt.create(IntSupplier getter, IntConsumer setter)` (SyncableInt.java L46), `trackArray(...)`,
  `startTracking/stopTracking`, plus `MekanismContainer.ISpecificContainerTracker` for grouped trackers.
  A container may also override `addContainerTrackers()` itself (see `DigitalMinerConfigContainer` L28-31).

---

## 6. Copy-paste checklist for the addon

1. **Tile** `class TileEntityMyMachine extends TileEntityElectricMachine` (or `TileEntityMekanism` /
   `TileEntityProgressMachine`), ctor `(Holder<Block> blockProvider, BlockPos pos, BlockState state)` →
   `super(blockProvider, pos, state)` (`TileEntityMekanism.java` L278). Override `getInitialInventory(...)`.
2. **Container** `class MyMachineContainer extends MekanismTileContainer<TileEntityMyMachine>` with
   `public MyMachineContainer(int id, Inventory inv, TileEntityMyMachine tile) { super(MyContainerTypes.MY_MACHINE, id, inv, tile); }`
   plus optional `getInventoryXOffset()/getInventoryYOffset()/addSlots()/addContainerTrackers()` overrides.
3. **Container type** `public static final ContainerTypeDeferredRegister CONTAINER_TYPES = new ContainerTypeDeferredRegister(MODID);`
   and `public static final ContainerTypeRegistryObject<MyMachineContainer> MY_MACHINE =
   CONTAINER_TYPES.register("my_machine", TileEntityMyMachine.class, MyMachineContainer::new);`
   then `CONTAINER_TYPES.register(modEventBus);` in the `@Mod` constructor.
4. **Block/BlockType** built with `.withGui(() -> MyContainerTypes.MY_MACHINE)` so `AttributeGui` (and therefore
   `hasGui()` / `openGui`) works. Without it, right-clicking will not open anything
   (`BlockTile.useWithoutItem` L70-78 → `tile.openGui(player)`; `TileEntityMekanism.openGui` L588-614).
5. **Screen** `class GuiMyMachine extends GuiMekanismTile<TileEntityMyMachine, MyMachineContainer>` (or
   `GuiConfigurableTile`): ctor `super(container, inv, title); dynamicSlots = true;`, override `addGuiElements()` →
   `super.addGuiElements()` then `addRenderableWidget(...)` for arrows/power bar/progress/energy tab.
6. **Client registration** in a `Dist.CLIENT` `@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)` class:
   `@SubscribeEvent public static void registerScreens(RegisterMenuScreensEvent event) { event.register(MyContainerTypes.MY_MACHINE.get(), GuiMyMachine::new); }`
7. **Texture** (if custom): `assets/<modid>/gui/my_machine.png`; `ResourceLocation.fromNamespaceAndPath(modid, "gui/my_machine.png")`;
   override `renderBg` and keep `imageWidth = 176`, choose `imageHeight` (multiple of whatever your art needs), then set
   `inventoryLabelY = imageHeight - 94`.
