// Generates the crafting recipes, the sieve ingredient tag, and the enchantment tags that make
// Ex Deorum meshes valid targets for Efficiency and Fortune.
//
// The Sieve Machine recipe is the one specified by the pack author. The four factory tiers follow
// Mekanism's own factory pattern (ACA / IPI / ACA with tier alloys and circuits), with the previous
// machine in the centre, so the whole line can be crafted up tier by tier exactly like Mekanism's
// factories.
//
// Usage: node gen_recipes.js <exdeorum jar> <mod resources dir>
const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");

const JAR = process.argv[2];
const RES = process.argv[3];
if (!JAR || !RES) {
    console.error("usage: node gen_recipes.js <exdeorum jar> <mod resources dir>");
    process.exit(2);
}

function writeJson(relPath, value) {
    const file = path.join(RES, relPath);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(value, null, 2) + "\n");
    console.log("wrote " + relPath);
}

/** Lists Ex Deorum item ids whose model name matches the given suffix. */
function itemsEndingWith(suffix) {
    const listing = execFileSync("powershell", [
        "-NoProfile", "-Command",
        `Add-Type -AssemblyName System.IO.Compression.FileSystem;` +
        `$z=[System.IO.Compression.ZipFile]::OpenRead('${JAR}');` +
        `$z.Entries | Where-Object { $_.FullName -match '^assets/exdeorum/models/item/(.+)' + '${suffix}' + '\\.json$' } |` +
        ` ForEach-Object { ($_.FullName -split '/')[-1] -replace '\\.json$','' };` +
        `$z.Dispose()`,
    ], { encoding: "utf8" });
    const items = listing.split(/\r?\n/).map(s => s.trim()).filter(Boolean).sort();
    if (items.length === 0) {
        console.error(`no items ending in "${suffix}" found in ` + JAR);
        process.exit(1);
    }
    console.log(`found ${items.length} items ending in "${suffix}"`);
    return items;
}

const sieves = itemsEndingWith("_sieve");
const meshes = itemsEndingWith("_mesh");

// --- "any sieve" ingredient ---------------------------------------------------------------------
writeJson("data/mekexnihilo/tags/item/sieves.json", {
    replace: false,
    values: sieves.map(s => `exdeorum:${s}`),
});

// --- let Efficiency and Fortune be applied to meshes --------------------------------------------
// In 1.21 an enchantment declares the items it may be applied to through a tag. Without these the
// enchanting table never offers Efficiency/Fortune on a mesh and an anvil refuses the books, so the
// mesh in the machine would always have no enchantment at all. `replace: false` merges with the
// vanilla contents instead of overwriting them.
const ENCHANTABLE_TAGS = {
    // Efficiency (and the other mining enchantments).
    "enchantable/mining": meshes,
    // Fortune.
    "enchantable/mining_loot": meshes,
};
for (const [tagPath, items] of Object.entries(ENCHANTABLE_TAGS)) {
    writeJson(`data/minecraft/tags/item/${tagPath}.json`, {
        replace: false,
        values: items.map(s => `exdeorum:${s}`),
    });
}

// --- Sieve Machine ------------------------------------------------------------------------------
//   iron   -      iron
//   redstone  sieve  redstone
//   iron   osmium  iron
writeJson("data/mekexnihilo/recipe/electric_sieve.json", {
    type: "minecraft:crafting_shaped",
    category: "misc",
    key: {
        I: { tag: "c:ingots/iron" },
        R: { tag: "c:dusts/redstone" },
        S: { tag: "mekexnihilo:sieves" },
        O: { tag: "c:ingots/osmium" },
    },
    pattern: ["I I", "RSR", "IOI"],
    result: { count: 1, id: "mekexnihilo:electric_sieve" },
});

// --- Factory tiers, using Mekanism's factory pattern and tier materials -------------------------
const FACTORIES = [
    { tier: "basic", alloy: "mekanism:alloys/basic", circuit: "c:circuits/basic", ingot: "c:ingots/iron", from: "mekexnihilo:electric_sieve" },
    { tier: "advanced", alloy: "mekanism:alloys/infused", circuit: "c:circuits/advanced", ingot: "c:ingots/osmium", from: "mekexnihilo:basic_sieve_factory" },
    { tier: "elite", alloy: "mekanism:alloys/reinforced", circuit: "c:circuits/elite", ingot: "c:ingots/gold", from: "mekexnihilo:advanced_sieve_factory" },
    { tier: "ultimate", alloy: "mekanism:alloys/atomic", circuit: "c:circuits/ultimate", ingot: "c:gems/diamond", from: "mekexnihilo:elite_sieve_factory" },
];

for (const f of FACTORIES) {
    writeJson(`data/mekexnihilo/recipe/${f.tier}_sieve_factory.json`, {
        type: "minecraft:crafting_shaped",
        category: "misc",
        key: {
            A: { tag: f.alloy },
            C: { tag: f.circuit },
            I: { tag: f.ingot },
            P: { item: f.from },
        },
        pattern: ["ACA", "IPI", "ACA"],
        result: { count: 1, id: `mekexnihilo:${f.tier}_sieve_factory` },
    });
}
