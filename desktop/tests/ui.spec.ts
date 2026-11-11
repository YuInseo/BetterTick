import { test, expect } from "@playwright/test";
test("missing Firebase configuration shows actionable setup instructions",async({page})=>{
 await page.goto("/");
 await expect(page.getByRole("heading",{name:"BetterTick Desktop"})).toBeVisible();
 await expect(page.getByText("Firebase 설정이 필요합니다.",{exact:false})).toBeVisible();
});
