// Minimal HTTPS fetcher: node tools/fetch.js <url> [outFile]
const https = require("https");
const fs = require("fs");
const url = process.argv[2];
const out = process.argv[3];
if (!url) { console.error("usage: node fetch.js <url> [outFile]"); process.exit(2); }
function get(u, depth) {
  if (depth > 6) { console.error("too many redirects"); process.exit(1); }
  https.get(u, { timeout: 60000, headers: { "User-Agent": "dsh-fetch/1.0" } }, r => {
    if (r.statusCode >= 300 && r.statusCode < 400 && r.headers.location) {
      r.resume();
      const next = new URL(r.headers.location, u).toString();
      return get(next, depth + 1);
    }
    const chunks = [];
    r.on("data", c => chunks.push(c));
    r.on("end", () => {
      const buf = Buffer.concat(chunks);
      if (out) { fs.writeFileSync(out, buf); console.log("SAVED " + r.statusCode + " " + buf.length + " bytes -> " + out); }
      else { process.stdout.write(buf); }
      if (r.statusCode >= 400) process.exit(1);
    });
  }).on("error", e => { console.error("ERROR " + (e.code || e.message)); process.exit(1); })
    .on("timeout", function () { console.error("TIMEOUT"); this.destroy(); process.exit(1); });
}
get(url, 0);
