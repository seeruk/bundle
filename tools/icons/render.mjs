import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { Resvg } from "@resvg/resvg-js";

const scriptDir = dirname(new URL(import.meta.url).pathname);
const symbolDir = dirname(createRequire(import.meta.url).resolve("pixelarticons/svg/clock.svg"));

const config = JSON.parse(readFileSync(resolve(scriptDir, "icons.json"), "utf8"));

// Returns a filesystem path for the given symbol name, if one can be found.
function symbolPath(symbol) {
    const local = resolve(scriptDir, "symbols", `${symbol}.svg`);
    return existsSync(local) ? local : resolve(symbolDir, `${symbol}.svg`);
}

// The symbols from pixelarticons, and also our custom symbol(s) sit on a 24x24 grid. Here, each
// unit is snapped to a pixel so that everything remains sharp when displayed full-size.
function iconSvg({ size, radius, symbolScale, symbolColour, colour, symbol }) {
    const inner = readFileSync(symbolPath(symbol), "utf8")
        .replace(/^[\s\S]*?<svg[^>]*>|<\/svg>\s*$/g, "")
        .replace(/\s(fill|stroke)="[^"]*"/g, "");
    const unit = Math.max(1, Math.round((size * symbolScale) / 24));
    const offset = (size - unit * 24) / 2;

    return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}">
    <rect width="${size}" height="${size}" rx="${radius}" fill="${colour}"/>
    <g transform="translate(${offset} ${offset}) scale(${unit})" fill="${symbolColour}" shape-rendering="crispEdges">${inner}</g>
  </svg>`;
}

// Render the given SVG as a PNG.
function png(svg, width) {
    return new Resvg(svg, { fitTo: width ? { mode: "width", value: width } : { mode: "original" } })
        .render()
        .asPng();
}

// Render the preview card.
function previewSvg(icons) {
    const row = 160;
    const cells = icons.map(({ mod, svg }, i) => {
        const y = i * row + 16;
        const href = (width) => `data:image/png;base64,${png(svg, width).toString("base64")}`;
        return `
            <text x="16" y="${y + 72}" font-family="sans-serif" font-size="18" fill="#111">${mod}</text>
            <image x="240" y="${y}" width="128" height="128" href="${href(128)}"/>
            <image x="400" y="${y + 48}" width="32" height="32" href="${href(32)}"/>
            <rect x="464" y="${y}" width="200" height="128" fill="#1e1e22"/>
            <image x="480" y="${y}" width="128" height="128" href="${href(128)}"/>
            <image x="620" y="${y + 48}" width="32" height="32" href="${href(32)}"/>
        `;
    });

    return `<svg xmlns="http://www.w3.org/2000/svg" width="680" height="${icons.length * row + 16}">
    <rect width="100%" height="100%" fill="#f4f4f5"/>${cells.join("")}
  </svg>`;
}

// Write and return all of the icons.
const icons = Object.entries(config.mods)
    .map(([mod, overrides]) => {
        const svg = iconSvg({ ...config.defaults, ...overrides });
        const out = resolve(scriptDir, config.output.replaceAll("{mod}", mod));
        mkdirSync(dirname(out), { recursive: true });
        writeFileSync(out, png(svg));
        console.log(`wrote ${out}`);
        return { mod, svg };
    });

writeFileSync(resolve(scriptDir, "preview.png"), png(previewSvg(icons)));
console.log(`wrote ${resolve(scriptDir, "preview.png")}`);
