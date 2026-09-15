const fs = require('fs');
const path = require('path');

// Root of repository and frontend
const frontendDir = path.resolve(__dirname, '..');
const repoDir = path.resolve(frontendDir, '..');

const targetVersionInput = process.argv[2];

const files = {
  packageJson: path.join(frontendDir, 'package.json'),
  tauriConf: path.join(frontendDir, 'src-tauri', 'tauri.conf.json'),
  cargoToml: path.join(frontendDir, 'src-tauri', 'Cargo.toml'),
  cargoLock: path.join(frontendDir, 'src-tauri', 'Cargo.lock'),
  platformJs: path.join(frontendDir, 'src', 'utils', 'platform.js'),
  downloadsSection: path.join(frontendDir, 'src', 'components', 'DownloadsSection.jsx'),
  generateNsis: path.join(frontendDir, 'scripts', 'generate-nsis-assets.cjs'),
  readme: path.join(repoDir, 'README.md'),
  desktopReleaseYml: path.join(repoDir, '.github', 'workflows', 'desktop-release.yml'),
};

function readCurrentVersions() {
  console.log('--- Current Version Audit ---');
  if (fs.existsSync(files.packageJson)) {
    const pkg = JSON.parse(fs.readFileSync(files.packageJson, 'utf8'));
    console.log(`package.json:                     ${pkg.version}`);
  }
  if (fs.existsSync(files.tauriConf)) {
    const tauri = JSON.parse(fs.readFileSync(files.tauriConf, 'utf8'));
    console.log(`tauri.conf.json:                  ${tauri.version}`);
  }
  if (fs.existsSync(files.cargoToml)) {
    const content = fs.readFileSync(files.cargoToml, 'utf8');
    const match = content.match(/\[package\][\s\S]*?version\s*=\s*"([^"]+)"/);
    console.log(`Cargo.toml:                       ${match ? match[1] : 'not found'}`);
  }
  if (fs.existsSync(files.platformJs)) {
    const content = fs.readFileSync(files.platformJs, 'utf8');
    const match = content.match(/APP_VERSION\s*=\s*['"]([^'"]+)['"]/);
    console.log(`platform.js:                      ${match ? match[1] : 'not found'}`);
  }
  if (fs.existsSync(files.downloadsSection)) {
    const content = fs.readFileSync(files.downloadsSection, 'utf8');
    const verMatch = content.match(/RELEASE_VERSION\s*=\s*['"]([^'"]+)['"]/);
    const tagMatch = content.match(/RELEASE_TAG\s*=\s*['"]([^'"]+)['"]/);
    console.log(`DownloadsSection.jsx:             ${verMatch ? `v${verMatch[1]}` : (tagMatch ? tagMatch[1] : 'not found')}`);
  }
  if (fs.existsSync(files.readme)) {
    const content = fs.readFileSync(files.readme, 'utf8');
    const match = content.match(/### Official Download Releases \((v[^)]+)\)/);
    console.log(`README.md:                        ${match ? match[1] : 'not found'}`);
  }
  if (fs.existsSync(files.desktopReleaseYml)) {
    const content = fs.readFileSync(files.desktopReleaseYml, 'utf8');
    const match = content.match(/default:\s*['"](v[^'"]+)['"]/);
    console.log(`desktop-release.yml:              ${match ? match[1] : 'not found'}`);
  }
  console.log('-----------------------------');
}

if (!targetVersionInput) {
  readCurrentVersions();
  console.log('\nUsage: node scripts/bump-version.cjs <new-version>');
  console.log('Example: node scripts/bump-version.cjs 1.0.1');
  process.exit(0);
}

// Clean and validate target version
const cleanVersion = targetVersionInput.trim().replace(/^v/i, '');
if (!/^\d+\.\d+\.\d+(-[a-zA-Z0-9.]+)?$/.test(cleanVersion)) {
  console.error(`Invalid semver version: "${targetVersionInput}". Expected format: X.Y.Z (e.g. 1.0.1)`);
  process.exit(1);
}

const tag = `v${cleanVersion}`;
console.log(`Updating all repository components to version ${cleanVersion} (${tag})...\n`);

// 1. package.json
if (fs.existsSync(files.packageJson)) {
  const content = fs.readFileSync(files.packageJson, 'utf8');
  const updated = content.replace(/"version":\s*"[^"]+"/, `"version": "${cleanVersion}"`);
  fs.writeFileSync(files.packageJson, updated, 'utf8');
  console.log(`[OK] Updated package.json -> ${cleanVersion}`);
}

// 2. tauri.conf.json
if (fs.existsSync(files.tauriConf)) {
  const content = fs.readFileSync(files.tauriConf, 'utf8');
  const updated = content.replace(/"version":\s*"[^"]+"/, `"version": "${cleanVersion}"`);
  fs.writeFileSync(files.tauriConf, updated, 'utf8');
  console.log(`[OK] Updated tauri.conf.json -> ${cleanVersion}`);
}

// 3. Cargo.toml
if (fs.existsSync(files.cargoToml)) {
  const content = fs.readFileSync(files.cargoToml, 'utf8');
  const updated = content.replace(
    /(\[package\][\s\S]*?version\s*=\s*)"[^"]+"/,
    `$1"${cleanVersion}"`
  );
  fs.writeFileSync(files.cargoToml, updated, 'utf8');
  console.log(`[OK] Updated Cargo.toml -> ${cleanVersion}`);
}

// 4. Cargo.lock
if (fs.existsSync(files.cargoLock)) {
  const content = fs.readFileSync(files.cargoLock, 'utf8');
  const updated = content.replace(
    /(\[\[package\]\]\s*\r?\nname\s*=\s*"crescendo-desktop"\s*\r?\nversion\s*=\s*)"[^"]+"/,
    `$1"${cleanVersion}"`
  );
  fs.writeFileSync(files.cargoLock, updated, 'utf8');
  console.log(`[OK] Updated Cargo.lock (crescendo-desktop) -> ${cleanVersion}`);
}

// 5. DownloadsSection.jsx
if (fs.existsSync(files.downloadsSection)) {
  let content = fs.readFileSync(files.downloadsSection, 'utf8');
  
  // Normalize constants to use RELEASE_VERSION
  if (!content.includes('const RELEASE_VERSION =')) {
    content = content.replace(
      /const RELEASE_TAG\s*=\s*'[^']+';/,
      `const RELEASE_VERSION = '${cleanVersion}';\nconst RELEASE_TAG = \`v\${RELEASE_VERSION}\`;`
    );
  } else {
    content = content.replace(
      /const RELEASE_VERSION\s*=\s*'[^']+';/,
      `const RELEASE_VERSION = '${cleanVersion}';`
    );
  }
  
  // Make sure RELEASE_BASE and RELEASE_PAGE use template literals or updated strings
  content = content.replace(
    /const RELEASE_BASE\s*=\s*[^;]+;/,
    `const RELEASE_BASE = \`https://github.com/AnkitArsh19/crescendo/releases/download/\${RELEASE_TAG}\`;`
  );
  content = content.replace(
    /const RELEASE_PAGE\s*=\s*[^;]+;/,
    `const RELEASE_PAGE = \`https://github.com/AnkitArsh19/crescendo/releases/tag/\${RELEASE_TAG}\`;`
  );
  
  // Update asset URLs to use ${RELEASE_VERSION}
  content = content.replace(
    /Crescendo_[0-9.]+_x64-setup\.exe/,
    `Crescendo_\${RELEASE_VERSION}_x64-setup.exe`
  );
  content = content.replace(
    /Crescendo_[0-9.]+_x64_en-US\.msi/,
    `Crescendo_\${RELEASE_VERSION}_x64_en-US.msi`
  );
  content = content.replace(
    /Crescendo_[0-9.]+_universal\.dmg/,
    `Crescendo_\${RELEASE_VERSION}_universal.dmg`
  );
  content = content.replace(
    /Crescendo_[0-9.]+_amd64\.AppImage/,
    `Crescendo_\${RELEASE_VERSION}_amd64.AppImage`
  );
  content = content.replace(
    /Crescendo_[0-9.]+_amd64\.deb/,
    `Crescendo_\${RELEASE_VERSION}_amd64.deb`
  );
  content = content.replace(
    /Crescendo-[0-9.]+-1\.x86_64\.rpm/,
    `Crescendo-\${RELEASE_VERSION}-1.x86_64.rpm`
  );
  
  fs.writeFileSync(files.downloadsSection, content, 'utf8');
  console.log(`[OK] Updated DownloadsSection.jsx -> ${cleanVersion} (${tag})`);
}

// 6. platform.js
if (fs.existsSync(files.platformJs)) {
  const content = fs.readFileSync(files.platformJs, 'utf8');
  const updated = content.replace(
    /APP_VERSION\s*=\s*['"][^'"]+['']/,
    `APP_VERSION = '${cleanVersion}'`
  );
  fs.writeFileSync(files.platformJs, updated, 'utf8');
  console.log(`[OK] Updated platform.js -> ${cleanVersion}`);
}

// 7. generate-nsis-assets.cjs fallback
if (fs.existsSync(files.generateNsis)) {
  const content = fs.readFileSync(files.generateNsis, 'utf8');
  const updated = content.replace(
    /let appVersion = 'v[0-9.]+';/,
    `let appVersion = '${tag}';`
  );
  fs.writeFileSync(files.generateNsis, updated, 'utf8');
  console.log(`[OK] Updated generate-nsis-assets.cjs fallback -> ${tag}`);
}

// 7. README.md
if (fs.existsSync(files.readme)) {
  let content = fs.readFileSync(files.readme, 'utf8');
  
  // Section header: ### Official Download Releases (v...)
  content = content.replace(
    /### Official Download Releases \(v[^)]+\)/,
    `### Official Download Releases (${tag})`
  );
  
  // Download URLs in table
  content = content.replace(
    /https:\/\/github\.com\/AnkitArsh19\/crescendo\/releases\/download\/v[0-9.]+\/Crescendo_[0-9.]+_x64-setup\.exe/g,
    `https://github.com/AnkitArsh19/crescendo/releases/download/${tag}/Crescendo_${cleanVersion}_x64-setup.exe`
  );
  content = content.replace(
    /https:\/\/github\.com\/AnkitArsh19\/crescendo\/releases\/download\/v[0-9.]+\/Crescendo_[0-9.]+_x64_en-US\.msi/g,
    `https://github.com/AnkitArsh19/crescendo/releases/download/${tag}/Crescendo_${cleanVersion}_x64_en-US.msi`
  );
  content = content.replace(
    /https:\/\/github\.com\/AnkitArsh19\/crescendo\/releases\/download\/v[0-9.]+\/Crescendo_[0-9.]+_universal\.dmg/g,
    `https://github.com/AnkitArsh19/crescendo/releases/download/${tag}/Crescendo_${cleanVersion}_universal.dmg`
  );
  content = content.replace(
    /https:\/\/github\.com\/AnkitArsh19\/crescendo\/releases\/download\/v[0-9.]+\/Crescendo_universal\.app\.tar\.gz/g,
    `https://github.com/AnkitArsh19/crescendo/releases/download/${tag}/Crescendo_universal.app.tar.gz`
  );
  content = content.replace(
    /https:\/\/github\.com\/AnkitArsh19\/crescendo\/releases\/download\/v[0-9.]+\/Crescendo_[0-9.]+_amd64\.AppImage/g,
    `https://github.com/AnkitArsh19/crescendo/releases/download/${tag}/Crescendo_${cleanVersion}_amd64.AppImage`
  );
  content = content.replace(
    /https:\/\/github\.com\/AnkitArsh19\/crescendo\/releases\/download\/v[0-9.]+\/Crescendo_[0-9.]+_amd64\.deb/g,
    `https://github.com/AnkitArsh19/crescendo/releases/download/${tag}/Crescendo_${cleanVersion}_amd64.deb`
  );
  content = content.replace(
    /https:\/\/github\.com\/AnkitArsh19\/crescendo\/releases\/download\/v[0-9.]+\/Crescendo-[0-9.]+-1\.x86_64\.rpm/g,
    `https://github.com/AnkitArsh19/crescendo/releases/download/${tag}/Crescendo-${cleanVersion}-1.x86_64.rpm`
  );
  
  // Releases tag link
  content = content.replace(
    /https:\/\/github\.com\/AnkitArsh19\/crescendo\/releases\/tag\/v[0-9.]+/g,
    `https://github.com/AnkitArsh19/crescendo/releases/tag/${tag}`
  );
  
  fs.writeFileSync(files.readme, content, 'utf8');
  console.log(`[OK] Updated README.md -> ${cleanVersion} (${tag})`);
}

// 8. desktop-release.yml workflow
if (fs.existsSync(files.desktopReleaseYml)) {
  let content = fs.readFileSync(files.desktopReleaseYml, 'utf8');
  content = content.replace(
    /default:\s*'v[0-9.]+'/,
    `default: '${tag}'`
  );
  content = content.replace(
    /echo "tag=\${{ inputs\.tag_name \|\| 'v[0-9.]+' }}"/,
    `echo "tag=\${{ inputs.tag_name || '${tag}' }}"`
  );
  fs.writeFileSync(files.desktopReleaseYml, content, 'utf8');
  console.log(`[OK] Updated .github/workflows/desktop-release.yml -> ${tag}`);
}

console.log(`\nSuccessfully bumped all files to version ${cleanVersion} (${tag}).`);
