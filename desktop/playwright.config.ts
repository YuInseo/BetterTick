import { defineConfig } from "@playwright/test";
export default defineConfig({
 testDir:"./tests",testMatch:"**/*.spec.ts",retries:1,
 use:{baseURL:"http://127.0.0.1:1420",browserName:"chromium",headless:true,trace:"retain-on-failure",screenshot:"only-on-failure"},
 webServer:{command:"npm run dev -- --host 127.0.0.1",url:"http://127.0.0.1:1420",reuseExistingServer:!process.env.CI,timeout:120000}
});
