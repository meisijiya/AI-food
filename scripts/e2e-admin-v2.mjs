import { chromium } from '/home/ubuntu/.local/node/lib/node_modules/playwright/index.mjs'
import fs from 'fs'
import path from 'path'

const BASE = 'http://119.29.52.111'
const OUT = '/home/ubuntu/.local/share/opencode/worktree/ce001bf198281167fbee74d415b3a556510875ba/silent-sailor/docs/screenshots/admin-test-2026-06-30-v2'

const pages = [
  { sidebar: 'Dashboard', name: 'dashboard' },
  { sidebar: '用户管理', name: 'user-management' },
  { sidebar: 'AI 对话', name: 'conversation' },
  { sidebar: 'Token 用量', name: 'token-usage' },
  { sidebar: '模型管理', name: 'model' },
  { sidebar: '推荐记录', name: 'recommendation' },
  { sidebar: '系统监控', name: 'monitor' },
  { sidebar: '操作日志', name: 'audit-log' }
]

const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } })
const page = await ctx.newPage()

const allErrors = []
page.on('console', m => { if (m.type() === 'error') allErrors.push({ url: page.url(), text: m.text() }) })
page.on('pageerror', e => allErrors.push({ url: page.url(), message: e.message }))

await page.goto(`${BASE}/admin/login`, { waitUntil: 'networkidle' })
await page.screenshot({ path: path.join(OUT, '01-login.png'), fullPage: true })

await page.fill('input[type=text]', 'smoke@aifood.local')
await page.fill('input[type=password]', 'testpass123')
await page.click('button:has-text("登录")')
await page.waitForURL(/dashboard/, { timeout: 15000 })
await page.waitForTimeout(2000)
await page.screenshot({ path: path.join(OUT, '02-dashboard.png'), fullPage: true })

for (const p of pages) {
  console.log(`→ click ${p.sidebar}`)
  // 用 sidebar 菜单 click 触发 SPA 路由(不会完整 reload)
  await page.click(`text="${p.sidebar}"`)
  await page.waitForTimeout(2500)
  const stat = await page.evaluate(() => ({
    url: location.href,
    bodyLen: document.body.textContent?.length,
    hasTable: !!document.querySelector('table'),
    hasForm: !!document.querySelector('form'),
    inputs: document.querySelectorAll('input').length,
    selects: document.querySelectorAll('.el-select').length
  }))
  await page.screenshot({ path: path.join(OUT, `03-${p.name}.png`), fullPage: true })
  console.log(`  ${JSON.stringify(stat)}`)
}

fs.writeFileSync(path.join(OUT, 'report.json'), JSON.stringify({ errors: allErrors }, null, 2))
console.log('\nTotal errors:', allErrors.length)
if (allErrors.length) allErrors.slice(0, 10).forEach(e => console.log(' -', e.url, e.text || e.message))

await browser.close()
