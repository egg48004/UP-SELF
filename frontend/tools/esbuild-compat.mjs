import { registerHooks, createRequire } from "node:module";
import { pathToFileURL } from "node:url";
import { dirname, delimiter } from "node:path";
const require = createRequire(import.meta.url);
const native = require("esbuild/package.json");
const wasm = require("esbuild-wasm/package.json");
if (native.version !== wasm.version) throw new Error("esbuild and esbuild-wasm versions must match.");
process.env.PATH = dirname(process.execPath) + delimiter + (process.env.PATH || "");
const entry = pathToFileURL(require.resolve("esbuild-wasm")).href;
registerHooks({ resolve(specifier, context, nextResolve) {
  return nextResolve(specifier === "esbuild" ? entry : specifier, context);
}});
