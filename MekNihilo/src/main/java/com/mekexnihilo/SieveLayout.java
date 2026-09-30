package com.mekexnihilo;

/**
 * Slot geometry shared by the block entity, the menu and the screen.
 *
 * <p>Coordinates are GUI-relative pixels from the top-left of the machine area, so the menu's slots
 * and the screen's overlays cannot drift apart.
 *
 * <p>The window grows with the machine: the input row widens it, and once the output grid needs
 * more than five rows it switches from four to six columns and the height grows too.
 */
public final class SieveLayout {

    public static final int SLOT = 18;

    /** Slot that holds the Ex Deorum mesh. */
    public static final int MESH_X = 26;
    public static final int MESH_Y = 17;

    /** First input slot. Inputs are laid out in a single row. */
    public static final int INPUT_X = 62;
    public static final int INPUT_Y = 17;

    /** Progress bar drawn between the inputs and the outputs. */
    public static final int PROGRESS_X = 62;
    public static final int PROGRESS_Y = 36;

    /** Slot that accepts an energy cube / battery item. */
    public static final int ENERGY_X = 26;
    public static final int ENERGY_Y = 43;

    /** Top-left of the output grid. */
    public static final int OUTPUT_X = 62;
    public static final int OUTPUT_Y = 50;

    /** Output columns while the grid still fits in five rows, and beyond that. */
    public static final int OUTPUT_COLUMNS_NARROW = 4;
    public static final int OUTPUT_COLUMNS_WIDE = 6;

    /** At most this many output rows before the grid switches to the wider layout. */
    private static final int MAX_NARROW_ROWS = 5;

    public static final int MIN_WIDTH = 176;

    /**
     * Extra room along the right hand edge of the window for the vertical energy bar, so it sits
     * beside the slots instead of on top of them.
     */
    public static final int RIGHT_MARGIN = 16;

    /** Vertical energy bar, matching where Mekanism puts it on its own machines. */
    public static final int POWER_BAR_Y = 16;
    public static final int POWER_BAR_HEIGHT = 52;

    /** Vertical gap between the status band and the player inventory. */
    private static final int PLAYER_GAP = 8;

    /**
     * Height of the status band between the machine area and the player inventory. The status lines
     * are drawn here, across the full window width, so they can never end up underneath a slot.
     */
    public static final int INFO_HEIGHT = 4;

    /** First status line, and the spacing between them. */
    public static final int INFO_Y = 4;
    public static final int INFO_LINE = 11;

    /** Height taken by the three player inventory rows, the gap and the hotbar. */
    private static final int PLAYER_HEIGHT = 82;

    private SieveLayout() {}

    public static int inputSlotX(int index) {
        return INPUT_X + index * SLOT;
    }

    /**
     * How many columns the output grid uses. Wider machines switch to six columns so the window
     * does not get unreasonably tall.
     */
    public static int outputColumns(int outputSlots) {
        int rows = Math.max(1, (outputSlots + OUTPUT_COLUMNS_NARROW - 1) / OUTPUT_COLUMNS_NARROW);
        return rows <= MAX_NARROW_ROWS ? OUTPUT_COLUMNS_NARROW : OUTPUT_COLUMNS_WIDE;
    }

    public static int outputSlotX(int index, int outputSlots) {
        return OUTPUT_X + (index % outputColumns(outputSlots)) * SLOT;
    }

    public static int outputSlotY(int index, int outputSlots) {
        return OUTPUT_Y + (index / outputColumns(outputSlots)) * SLOT;
    }

    public static int outputRows(int outputSlots) {
        return Math.max(1, (outputSlots + outputColumns(outputSlots) - 1) / outputColumns(outputSlots));
    }

    /** Bottom edge of the machine area. */
    public static int machineBottom(int outputSlots) {
        int slotsBottom = INPUT_Y + SLOT;
        int outputsBottom = OUTPUT_Y + outputRows(outputSlots) * SLOT;
        int energyBottom = ENERGY_Y + SLOT;
        return Math.max(Math.max(slotsBottom, outputsBottom), energyBottom);
    }

    /** Y offset of the player's inventory inside the GUI, also used as the menu offset. */
    public static int playerInventoryY(int inputSlots, int outputSlots) {
        return machineBottom(outputSlots) + INFO_HEIGHT + PLAYER_GAP;
    }

    /** Absolute Y of the first status line inside the window. */
    public static int infoY(int outputSlots) {
        return machineBottom(outputSlots) + INFO_Y;
    }

    /** Window width: wide enough for the input row, the output grid and the energy bar. */
    public static int guiWidth(int inputSlots, int outputSlots) {
        int inputsRight = INPUT_X + inputSlots * SLOT;
        int outputsRight = OUTPUT_X + outputColumns(outputSlots) * SLOT;
        return Math.max(MIN_WIDTH, Math.max(inputsRight, outputsRight) + 6) + RIGHT_MARGIN;
    }

    public static int guiHeight(int inputSlots, int outputSlots) {
        return playerInventoryY(inputSlots, outputSlots) + PLAYER_HEIGHT;
    }

    /** X of the vertical energy bar: hard against the right hand edge of the window. */
    public static int powerBarX(int inputSlots, int outputSlots) {
        return guiWidth(inputSlots, outputSlots) - 12;
    }
}
