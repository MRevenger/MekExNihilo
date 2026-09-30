// Draws the mod icon: "ExNihilo" with a superscript "Mek", in Mekanism's steel palette.
// Self-contained PNG writer so this needs no dependencies.
const fs = require('fs');
const zlib = require('zlib');

const SIZE = 256;

// 5x7 glyphs, only the letters the wordmark needs.
const FONT = {
  E: ['11111', '10000', '10000', '11110', '10000', '10000', '11111'],
  x: ['00000', '00000', '10001', '01010', '00100', '01010', '10001'],
  N: ['10001', '11001', '11001', '10101', '10011', '10011', '10001'],
  i: ['00100', '00000', '00100', '00100', '00100', '00100', '00100'],
  h: ['10000', '10000', '10110', '11001', '10001', '10001', '10001'],
  l: ['01100', '00100', '00100', '00100', '00100', '00100', '01110'],
  o: ['00000', '00000', '01110', '10001', '10001', '10001', '01110'],
  M: ['10001', '11011', '10101', '10101', '10001', '10001', '10001'],
  e: ['00000', '00000', '01110', '10001', '11111', '10000', '01110'],
  k: ['10000', '10010', '10100', '11000', '10100', '10010', '10001'],
};

const px = new Uint8Array(SIZE * SIZE * 4);
const put = (x, y, r, g, b, a = 255) => {
  if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) return;
  const o = (y * SIZE + x) * 4;
  const na = a / 255;
  px[o] = px[o] * (1 - na) + r * na;
  px[o + 1] = px[o + 1] * (1 - na) + g * na;
  px[o + 2] = px[o + 2] * (1 - na) + b * na;
  px[o + 3] = Math.min(255, px[o + 3] + a);
};
const rect = (x0, y0, w, h, r, g, b, a) => {
  for (let y = y0; y < y0 + h; y++) for (let x = x0; x < x0 + w; x++) put(x, y, r, g, b, a);
};

// Plate: dark steel, bevelled like a Mekanism machine face.
rect(0, 0, SIZE, SIZE, 24, 27, 31, 255);
for (let i = 0; i < SIZE; i++) {
  const t = i / (SIZE - 1);
  // Subtle top-left to bottom-right sheen.
  const v = Math.round(12 * (1 - t));
  rect(i, 0, 1, SIZE, 38 + v, 42 + v, 48 + v, 48);
}
rect(0, 0, SIZE, 4, 96, 104, 116, 255);
rect(0, 0, 4, SIZE, 88, 96, 108, 255);
rect(0, SIZE - 4, SIZE, 4, 16, 18, 21, 255);
rect(SIZE - 4, 0, 4, SIZE, 16, 18, 21, 255);
// Inner recess.
rect(12, 12, SIZE - 24, SIZE - 24, 30, 34, 39, 255);
rect(12, 12, SIZE - 24, 2, 20, 22, 26, 255);
rect(12, 12, 2, SIZE - 24, 20, 22, 26, 255);

// Wordmark.
const draw = (text, x0, y0, scale, [r, g, b]) => {
  let x = x0;
  for (const ch of text) {
    const glyph = FONT[ch];
    if (!glyph) { x += 4 * scale; continue; }
    for (let gy = 0; gy < 7; gy++) {
      for (let gx = 0; gx < 5; gx++) {
        if (glyph[gy][gx] === '1') rect(x + gx * scale, y0 + gy * scale, scale, scale, r, g, b, 255);
      }
    }
    x += 6 * scale;
  }
  return x;
};

// "ExNihilo" across the plate, "Mek" raised at its top right.
const SCALE = 5;
const wordWidth = 'ExNihilo'.length * 6 * SCALE - SCALE;
const startX = Math.round((SIZE - wordWidth) / 2);
const startY = 120;
draw('ExNihilo', startX + 2, startY + 2, SCALE, [10, 11, 13]);
draw('ExNihilo', startX, startY, SCALE, [214, 220, 228]);

const SUP = 2;
const supWidth = 'Mek'.length * 6 * SUP - SUP;
const supX = startX + wordWidth - supWidth;
const supY = startY - 7 * SUP - 14;
draw('Mek', supX + 1, supY + 1, SUP, [10, 11, 13]);
draw('Mek', supX, supY, SUP, [124, 176, 214]);

// Accent bar under the wordmark, Mekanism blue.
rect(startX, startY + 7 * SCALE + 18, wordWidth, 6, 124, 176, 214, 255);

// Faint sieve grid filling the space below, so the plate does not read as empty.
for (let gx = 0; gx < 9; gx++) {
  for (let gy = 0; gy < 2; gy++) {
    const x = startX + gx * 27;
    const y = startY + 7 * SCALE + 44 + gy * 27;
    rect(x, y, 22, 2, 58, 64, 74, 255);
    rect(x, y, 2, 22, 58, 64, 74, 255);
  }
}

// --- PNG encoding ---
const crcTable = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c;
  }
  return t;
})();
const crc32 = (buf) => {
  let c = -1;
  for (const b of buf) c = crcTable[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ -1) >>> 0;
};
const chunk = (type, data) => {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, crc]);
};
const ihdr = Buffer.alloc(13);
ihdr.writeUInt32BE(SIZE, 0);
ihdr.writeUInt32BE(SIZE, 4);
ihdr[8] = 8;   // bit depth
ihdr[9] = 6;   // RGBA
const raw = Buffer.alloc((SIZE * 4 + 1) * SIZE);
for (let y = 0; y < SIZE; y++) {
  raw[y * (SIZE * 4 + 1)] = 0;
  Buffer.from(px.buffer, y * SIZE * 4, SIZE * 4).copy(raw, y * (SIZE * 4 + 1) + 1);
}
const png = Buffer.concat([
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
  chunk('IHDR', ihdr),
  chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
  chunk('IEND', Buffer.alloc(0)),
]);

const out = process.argv[2];
fs.mkdirSync(require('path').dirname(out), { recursive: true });
fs.writeFileSync(out, png);
console.log(`${out} (${SIZE}x${SIZE}, ${png.length} bytes)`);
