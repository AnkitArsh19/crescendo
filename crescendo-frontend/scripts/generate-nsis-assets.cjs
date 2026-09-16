const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

function createBmp24(width, height, rgbBuffer) {
  const rowStride = (width * 3 + 3) & ~3;
  const padding = rowStride - width * 3;
  const imageSize = rowStride * height;
  const fileSize = 54 + imageSize;
  const buf = Buffer.alloc(fileSize);
  
  // BMP Header
  buf.write('BM', 0);
  buf.writeUInt32LE(fileSize, 2);
  buf.writeUInt32LE(54, 10); // offset
  
  // DIB Header (BITMAPINFOHEADER)
  buf.writeUInt32LE(40, 14);
  buf.writeInt32LE(width, 18);
  buf.writeInt32LE(height, 22);
  buf.writeUInt16LE(1, 26);
  buf.writeUInt16LE(24, 28);
  buf.writeUInt32LE(0, 30);
  buf.writeUInt32LE(imageSize, 34);
  
  let offset = 54;
  for (let y = height - 1; y >= 0; y--) {
    for (let x = 0; x < width; x++) {
      const srcIdx = (y * width + x) * 3;
      buf[offset++] = rgbBuffer[srcIdx + 2]; // B
      buf[offset++] = rgbBuffer[srcIdx + 1]; // G
      buf[offset++] = rgbBuffer[srcIdx];     // R
    }
    for (let p = 0; p < padding; p++) buf[offset++] = 0;
  }
  return buf;
}

async function generate() {
  let appVersion = 'v1.0.3';
  try {
    const tauriConfPath = path.resolve(__dirname, '../src-tauri/tauri.conf.json');
    if (fs.existsSync(tauriConfPath)) {
      const conf = JSON.parse(fs.readFileSync(tauriConfPath, 'utf8'));
      if (conf.version) {
        appVersion = `v${conf.version}`;
      }
    }
  } catch {
    // fallback to v1.0.0
  }

  const sidebarSvg = `
    <svg width="164" height="314" viewBox="0 0 164 314" xmlns="http://www.w3.org/2000/svg">
      <rect width="164" height="314" fill="#000000"/>
      <circle cx="82" cy="92" r="54" stroke="rgba(255,255,255,0.08)" stroke-width="1" fill="none"/>
      <circle cx="82" cy="92" r="76" stroke="rgba(255,255,255,0.04)" stroke-width="0.5" fill="none"/>
      
      <!-- Logo icon -->
      <g transform="translate(54, 47) scale(0.28)">
        <path d="M60.5 98.1878L134.5 132.688V93.6878L60.5 59.1878V98.1878Z" fill="#FFFFFF"/>
        <path d="M124 178.188L53 225.688V257.688L124 211.688V178.188Z" fill="#FFFFFF"/>
        <path d="M119.5 25.1878L191.5 53.1879L182.5 0.687897L119.5 25.1878Z" fill="#FFFFFF"/>
        <path d="M86.5 193.688L51.5 217.188L4.5 187.188V143.188L86.5 193.688Z" fill="#FFFFFF"/>
        <path d="M122 170.188L94.5 187.188L31.5 149.188L58 134.188L122 170.188Z" fill="#FFFFFF"/>
        <path d="M24 144.688L7 134.688L56 104.688L75.5 114.188L24 144.688Z" fill="#FFFFFF"/>
        <path d="M140 86.1879L63 51.6878L114 30.6878L187.5 59.1878L140 86.1879Z" fill="#FFFFFF"/>
        <path d="M56 267.688L157 285.188L128 221.188L92 244.438L56 267.688Z" fill="#FFFFFF"/>
      </g>

      <text x="82" y="165" font-family="'Segoe UI', -apple-system, Arial, sans-serif" font-size="14.5" font-weight="700" fill="#FFFFFF" text-anchor="middle" letter-spacing="2.5">CRESCENDO</text>
      <text x="82" y="184" font-family="'Segoe UI', -apple-system, Arial, sans-serif" font-size="8" font-weight="600" fill="#A1A1AA" text-anchor="middle" letter-spacing="1.5">WORKFLOW ENGINE</text>

      <line x1="28" y1="206" x2="136" y2="206" stroke="#27272A" stroke-width="1"/>
      <text x="82" y="230" font-family="'Segoe UI', -apple-system, Arial, sans-serif" font-size="8.5" font-weight="500" fill="#E4E4E7" text-anchor="middle" letter-spacing="0.5">Native Desktop Client</text>
    </svg>
  `;

  const headerSvg = `
    <svg width="150" height="57" viewBox="0 0 150 57" xmlns="http://www.w3.org/2000/svg">
      <rect width="150" height="57" fill="#FFFFFF"/>
      <!-- NSIS standard places icon on right side of header -->
      <g transform="translate(98, 7) scale(0.14)">
        <path d="M60.5 98.1878L134.5 132.688V93.6878L60.5 59.1878V98.1878Z" fill="#000000"/>
        <path d="M124 178.188L53 225.688V257.688L124 211.688V178.188Z" fill="#000000"/>
        <path d="M119.5 25.1878L191.5 53.1879L182.5 0.687897L119.5 25.1878Z" fill="#000000"/>
        <path d="M86.5 193.688L51.5 217.188L4.5 187.188V143.188L86.5 193.688Z" fill="#000000"/>
        <path d="M122 170.188L94.5 187.188L31.5 149.188L58 134.188L122 170.188Z" fill="#000000"/>
        <path d="M24 144.688L7 134.688L56 104.688L75.5 114.188L24 144.688Z" fill="#000000"/>
        <path d="M140 86.1879L63 51.6878L114 30.6878L187.5 59.1878L140 86.1879Z" fill="#000000"/>
        <path d="M56 267.688L157 285.188L128 221.188L92 244.438L56 267.688Z" fill="#000000"/>
      </g>
    </svg>
  `;

  const outDir = path.resolve(__dirname, '../src-tauri/icons');
  if (!fs.existsSync(outDir)) {
    fs.mkdirSync(outDir, { recursive: true });
  }

  const sidebarRaw = await sharp(Buffer.from(sidebarSvg)).removeAlpha().raw().toBuffer();
  const sidebarBmp = createBmp24(164, 314, sidebarRaw);
  fs.writeFileSync(path.join(outDir, 'sidebar.bmp'), sidebarBmp);

  const headerRaw = await sharp(Buffer.from(headerSvg)).removeAlpha().raw().toBuffer();
  const headerBmp = createBmp24(150, 57, headerRaw);
  fs.writeFileSync(path.join(outDir, 'header.bmp'), headerBmp);

  console.log('NSIS sidebar.bmp generated:', sidebarBmp.length, 'bytes');
  console.log('NSIS header.bmp generated:', headerBmp.length, 'bytes');
}

generate().catch(err => {
  console.error('Error generating NSIS assets:', err);
  process.exit(1);
});
