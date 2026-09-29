// Generates the single texture this addon draws itself: the white sieve grid on every machine's top.
//
// The top is a recessed screen. The Sieve Machine gets real geometry for that (see gen_resources.js:
// the floor sits a quarter block down); the factory tiers reuse Mekanism's factory shell, so for them
// the recess is implied by the darker frame around the edge.
//
// The weave itself covers the middle ~80% of the face, at a moderate density (4 x 4 cells).
//
// Usage: node gen_textures.js <output dir>
const zlib = require("zlib");
const fs = require("fs");
const path = require("path");

const OUT = process.argv[2];
if (!OUT) {
    console.error("usage: node gen_textures.js <output dir>");
    process.exit(2);
}

const CRC_TABLE = (() => {
    const table = new Int32Array(256);
    for (let n = 0; n < 256; n++) {
        let c = n;
        for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
        table[n] = c;
    }
    return table;
})();

function crc32(buf) {
    let c = 0xffffffff;
    for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
    return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length, 0);
    const typeBuf = Buffer.from(type, "ascii");
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
    return Buffer.concat([len, typeBuf, data, crc]);
}

function encodePng(width, height, pixels) {
    const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(width, 0);
    ihdr.writeUInt32BE(height, 4);
    ihdr[8] = 8;
    ihdr[9] = 6; // RGBA
    const raw = Buffer.alloc(height * (width * 4 + 1));
    for (let y = 0; y < height; y++) {
        const rowStart = y * (width * 4 + 1);
        raw[rowStart] = 0;
        pixels.copy(raw, rowStart + 1, y * width * 4, (y + 1) * width * 4);
    }
    return Buffer.concat([
        sig,
        chunk("IHDR", ihdr),
        chunk("IDAT", zlib.deflateSync(raw, { level: 9 })),
        chunk("IEND", Buffer.alloc(0)),
    ]);
}

const W = 16, H = 16;
const px = Buffer.alloc(W * H * 4);
const set = (x, y, c) => {
    if (x < 0 || y < 0 || x >= W || y >= H) return;
    const i = (y * W + x) * 4;
    px[i] = c[0]; px[i + 1] = c[1]; px[i + 2] = c[2]; px[i + 3] = 255;
};
const fill = (x0, y0, x1, y1, c) => {
    for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) set(x, y, c);
};

const FRAME = [15, 17, 19];
const FLOOR = [27, 29, 33];
const WEAVE = [236, 239, 243];

// Outer frame (sits under the machine's rim), then the recessed floor.
fill(0, 0, 15, 15, FRAME);
fill(1, 1, 14, 14, FLOOR);

// Moderate 4 x 4 weave covering the middle ~80% (pixels 2..14).
const STRANDS = [2, 5, 8, 11, 14];
for (const i of STRANDS) {
    for (let t = 2; t <= 14; t++) {
        set(i, t, WEAVE);
        set(t, i, WEAVE);
    }
}

fs.mkdirSync(OUT, { recursive: true });
fs.writeFileSync(path.join(OUT, "sieve_grid_top.png"), encodePng(W, H, px));
console.log("wrote sieve_grid_top.png (4x4 weave over the middle 80%, recessed frame)");
