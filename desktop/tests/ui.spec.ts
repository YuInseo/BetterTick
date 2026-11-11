import { test, expect } from "@playwright/test";
test.beforeEach(async({page})=>{await page.goto("/");await expect(page.getByText("BetterTick",{exact:true}).first()).toBeVisible()});
test("add, complete, uncomplete and delete a task",async({page})=>{
 await page.locator("#quick-add").fill("QA sample task");
 await page.getByRole("button",{name:"추가",exact:true}).click();
 await expect(page.getByText("QA sample task").first()).toBeVisible();
 await page.getByTitle("완료 전환").click();
 await page.getByRole("button",{name:"완료",exact:true}).click();
 await expect(page.getByText("QA sample task").first()).toBeVisible();
 await page.getByTitle("완료 전환").click();
 await page.getByRole("button",{name:"전체",exact:true}).click();
 await page.getByText("QA sample task").first().click();
 await page.getByRole("button",{name:"삭제",exact:true}).click();
 await expect(page.getByText("QA sample task")).toHaveCount(0);
});
test("calendar next and previous buttons navigate correctly",async({page})=>{
 await page.getByRole("button",{name:"캘린더",exact:true}).click();
 const now=new Date();const next=new Date(now.getFullYear(),now.getMonth()+1,1);
 await page.locator(".heading .actions button").nth(1).click();
 await expect(page.getByText(next.getFullYear()+"년 "+(next.getMonth()+1)+"월")).toBeVisible();
 await page.locator(".heading .actions button").first().click();
 await expect(page.getByText(now.getFullYear()+"년 "+(now.getMonth()+1)+"월")).toBeVisible();
});
test("focus timer starts, pauses and resets",async({page})=>{
 await page.getByRole("button",{name:"집중",exact:true}).click();
 await expect(page.locator(".timer")).toHaveText("25:00");
 await page.getByRole("button",{name:"시작",exact:true}).click();
 await expect(page.getByRole("button",{name:"일시정지"})).toBeVisible();
 await page.getByRole("button",{name:"초기화"}).click();
 await expect(page.locator(".timer")).toHaveText("25:00");
});
test("sidebar can close and reopen",async({page})=>{
 await page.getByTitle("사이드바 닫기").click();
 await expect(page.locator(".sidebar")).toHaveCount(0);
 await page.locator(".toolbar .icon-button").first().click();
 await expect(page.locator(".sidebar")).toBeVisible();
});
test("new list creation and navigation",async({page})=>{
 page.on("dialog",dialog=>dialog.accept("QA List"));
 await page.getByTitle("목록 추가").click();
 await expect(page.getByRole("button",{name:"QA List"})).toBeVisible();
});
