import assert from "node:assert/strict";
import { readFileSync, existsSync } from "node:fs";
import { test } from "node:test";
const read = (p) => readFileSync(new URL(p, import.meta.url), "utf8");
test("desktop configuration has valid Tauri bundle targets",()=>{
 const config=JSON.parse(read("../src-tauri/tauri.conf.json"));
 assert.equal(config.identifier,"com.bettertick.desktop");
 assert.equal(config.build.frontendDist,"../dist");
 assert.deepEqual(config.bundle.targets,["nsis","msi"]);
});
test("Firebase credentials are not embedded in source",()=>{
 const source=read("../src/App.tsx");
 assert.match(source,/VITE_FIREBASE_API_KEY/);
 assert.doesNotMatch(source,/AIza[0-9A-Za-z_-]{25,}/);
});
test("critical desktop screens and Firestore synchronization are wired",()=>{
 const app=read("../src/App.tsx");
 for(const screen of ["today","upcoming","calendar","board","focus","habits","matrix","tags"])assert.ok(app.includes(screen),"missing screen "+screen);
 assert.match(app,/onSnapshot/);
 assert.match(app,/setDoc/);
});
test("Tauri Rust application and icon source exist",()=>{
 for(const path of ["../src-tauri/src/main.rs","../src-tauri/src/lib.rs","../src-tauri/Cargo.toml","../icon.svg"])assert.ok(existsSync(new URL(path,import.meta.url)),path);
});
