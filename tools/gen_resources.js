// Generates blockstate / model / loot table JSON.
//
// The Sieve Machine keeps Mekanism's Enrichment Chamber textures but gets its own geometry so the
// sieve screen is genuinely recessed a quarter block into the top.
//
// The factory tiers reuse Mekanism's Enriching Factory shell (which carries the per-tier LED).
// Mekanism splits that shell's top face across two elements with their own UV rectangles
// (`front_panel` samples texture rows 12..16 and `shell_01` samples rows 0..12 with a 180 degree
// rotation), which smears a plain square texture. So Mekanism's base model is read from the
// extracted Mekanism assets, its two top faces are re-pointed at the sieve grid with straight
// full-width UVs, and the result is written as our own model.
//
// Usage: node gen_resources.js <mod resources dir> <mekanism assets dir>
const fs = require("fs");
const path = require("path");

const RES = process.argv[2];
const MEK_ASSETS = process.argv[3];
if (!RES || !MEK_ASSETS) {
    console.error("usage: node gen_resources.js <mod resources dir> <mekanism assets dir>");
    process.exit(2);
}

const GRID = "mekexnihilo:block/sieve_grid_top";
const ENRICH = "mekanism:block/enrichment_chamber";
const FACTORY = "mekanism:block/factory/enriching";
const FACTORY_BASE_SRC = path.join(MEK_ASSETS, "models/block/factory/enriching/base.json");
const FACINGS = [["north", 0], ["south", 180], ["east", 90], ["west", -90]];
const TIERS = ["basic", "advanced", "elite", "ultimate"];

// The opening spans the middle 80% of the face (16 * 0.8 = 12.8), i.e. 1.6 units of rim all round.
const EDGE = 1.6;
// The screen floor sits a quarter block below the top face.
const FLOOR = 12;

function writeJson(relPath, value) {
    const file = path.join(RES, relPath);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(value, null, 2) + "\n");
    console.log("wrote " + relPath);
}

function blockstate(name) {
    const variants = {};
    for (const [facing, y] of FACINGS) {
        for (const active of [false, true]) {
            const model = `mekexnihilo:block/${name}${active ? "_active" : ""}`;
            variants[`facing=${facing},active=${active}`] = y === 0 ? { model } : { model, y };
        }
    }
    return { variants };
}

function lootTable(name) {
    return {
        type: "minecraft:block",
        pools: [{
            rolls: 1.0,
            bonus_rolls: 0.0,
            entries: [{ type: "minecraft:item", name: `mekexnihilo:${name}` }],
            conditions: [{ condition: "minecraft:survives_explosion" }],
        }],
    };
}

function emit(name, normalModel, activeModel) {
    writeJson(`assets/mekexnihilo/blockstates/${name}.json`, blockstate(name));
    writeJson(`assets/mekexnihilo/models/block/${name}.json`, normalModel);
    writeJson(`assets/mekexnihilo/models/block/${name}_active.json`, activeModel);
    writeJson(`assets/mekexnihilo/models/item/${name}.json`, { parent: `mekexnihilo:block/${name}` });
    writeJson(`data/mekexnihilo/loot_table/blocks/${name}.json`, lootTable(name));
}

// --- Sieve Machine: Enrichment Chamber textures, custom geometry with a recessed screen ---------
// Modelled as a body plus a four-piece ring, which is the only way to get a real hole in the top
// face. Outer faces keep the correct band of Mekanism's textures via explicit UVs.
function machineModel(front) {
    const E = EDGE, F = FLOOR;
    const outer = (texture, cullface) => ({ uv: [0, 0, 16, 4], texture, ...(cullface ? { cullface } : {}) });
    return {
        parent: "block/block",
        textures: {
            particle: front,
            front,
            sides: `${ENRICH}/right`,
            west: `${ENRICH}/right`,
            east: `${ENRICH}/left`,
            south: `${ENRICH}/back`,
            down: `${ENRICH}/bottom`,
            top: `${ENRICH}/top`,
            grid: GRID,
        },
        elements: [
            {
                name: "body",
                from: [0, 0, 0],
                to: [16, F, 16],
                faces: {
                    down: { uv: [0, 0, 16, 16], texture: "#down", cullface: "down" },
                    // The recess floor, seen through the opening. The texture's weave covers the
                    // middle 80%, which is exactly what the opening exposes.
                    up: { uv: [0, 0, 16, 16], texture: "#grid" },
                    north: { uv: [0, 4, 16, 16], texture: "#front" },
                    south: { uv: [0, 4, 16, 16], texture: "#south", cullface: "south" },
                    west: { uv: [0, 4, 16, 16], texture: "#west", cullface: "west" },
                    east: { uv: [0, 4, 16, 16], texture: "#east", cullface: "east" },
                },
            },
            // Ring pieces. Their downward faces are omitted: the body's top face already backs them.
            {
                name: "rim_north",
                from: [0, F, 0],
                to: [16, 16, E],
                faces: {
                    up: { uv: [0, 0, 16, E], texture: "#top", cullface: "up" },
                    north: outer("#front"),
                    south: outer("#top"),
                },
            },
            {
                name: "rim_south",
                from: [0, F, 16 - E],
                to: [16, 16, 16],
                faces: {
                    up: { uv: [0, 16 - E, 16, 16], texture: "#top", cullface: "up" },
                    south: outer("#south", "south"),
                    north: outer("#top"),
                },
            },
            {
                name: "rim_west",
                from: [0, F, E],
                to: [E, 16, 16 - E],
                faces: {
                    up: { uv: [0, E, E, 16 - E], texture: "#top", cullface: "up" },
                    west: { uv: [E, 0, 16 - E, 4], texture: "#west", cullface: "west" },
                    east: outer("#top"),
                },
            },
            {
                name: "rim_east",
                from: [16 - E, F, E],
                to: [16, 16, 16 - E],
                faces: {
                    up: { uv: [16 - E, E, 16, 16 - E], texture: "#top", cullface: "up" },
                    east: { uv: [E, 0, 16 - E, 4], texture: "#east", cullface: "east" },
                    west: outer("#top"),
                },
            },
        ],
    };
}

emit("electric_sieve",
        machineModel(`${ENRICH}/front`),
        machineModel(`${ENRICH}/front_active`));

// --- Factory tiers: Mekanism's Enriching Factory shell, sieve grid mapped onto the whole top -----
// Reads Mekanism's own shell so the port/led/bevel details stay in sync with whatever Mekanism
// version is installed, and only rewrites the two top faces.
function factoryBaseModel() {
    const base = JSON.parse(fs.readFileSync(FACTORY_BASE_SRC, "utf8"));
    // The top of the block is the front lip (z 0..4) plus the main shell lid (z 4..16).
    const TOPS = {
        front_panel: [0, 0, 16, 4],
        shell_01: [0, 4, 16, 16],
    };
    let patched = 0;
    for (const element of base.elements) {
        const uv = TOPS[element.name];
        if (!uv || !element.faces.up) {
            continue;
        }
        // Straight, full-width UVs: texture v now maps linearly onto block z, so the weave drawn
        // across the middle 80% of the texture lands on the middle 80% of the top face.
        element.faces.up = { uv, texture: "#grid", cullface: "up" };
        patched++;
    }
    if (patched !== Object.keys(TOPS).length) {
        throw new Error(`expected ${Object.keys(TOPS).length} top faces, patched ${patched}`);
    }
    base.textures.grid = GRID;
    return base;
}

writeJson("assets/mekexnihilo/models/block/sieve_factory_base.json", factoryBaseModel());

function factoryModel(tier, active) {
    const baseTextures = {};
    if (active) {
        baseTextures.front = `${FACTORY}/enriching_factory_front_active`;
    }
    return {
        loader: "neoforge:composite",
        parent: "block/block",
        textures: { particle: `${FACTORY}/enriching_factory_front` },
        children: {
            base: { parent: "mekexnihilo:block/sieve_factory_base", textures: baseTextures },
            front_led: { parent: `mekanism:block/factory/front_led/${active ? "active/" : ""}${tier}` },
        },
    };
}

for (const tier of TIERS) {
    emit(`${tier}_sieve_factory`, factoryModel(tier, false), factoryModel(tier, true));
}
