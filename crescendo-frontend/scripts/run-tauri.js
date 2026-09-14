import { spawn } from 'child_process';
import path from 'path';
import os from 'os';
import http from 'http';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');

// Helper to check if dev server is already listening
function isServerRunning(port) {
  return new Promise((resolve) => {
    const req = http.get(`http://127.0.0.1:${port}`, (res) => {
      resolve(true);
      res.resume();
    });
    req.on('error', () => resolve(false));
    req.setTimeout(500, () => {
      req.destroy();
      resolve(false);
    });
  });
}

async function main() {
  const cargoBin = path.join(os.homedir(), '.cargo', 'bin');
  const env = { ...process.env };

  // Ensure .cargo/bin is present in PATH for Tauri / Cargo commands
  const pathKey = Object.keys(env).find(k => k.toLowerCase() === 'path') || 'PATH';
  const currentPath = env[pathKey] || '';
  if (!currentPath.split(path.delimiter).some(p => p.toLowerCase() === cargoBin.toLowerCase())) {
    env[pathKey] = `${cargoBin}${path.delimiter}${currentPath}`;
  }

  let args = process.argv.slice(2);
  const tauriBin = path.join(rootDir, 'node_modules', '@tauri-apps', 'cli', 'tauri.js');

  // If running "tauri dev" and port 5173 is already up (e.g. from dev.ps1),
  // skip starting a redundant second Vite instance
  if (args.includes('dev')) {
    const is5173Up = await isServerRunning(5173);
    if (is5173Up) {
      console.log('[crescendo-desktop] Frontend dev server already active on port 5173. Connecting directly...');
      args.push('-c', JSON.stringify({ build: { beforeDevCommand: '' } }));
    }
  }

  const child = spawn(process.execPath, [tauriBin, ...args], {
    cwd: rootDir,
    stdio: 'inherit',
    env
  });

  child.on('exit', (code) => {
    process.exit(code ?? 0);
  });
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
