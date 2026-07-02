/**
 * P3 e2e — admin-web 本地全流程验证
 * 目标: login + 9 个 admin pages 渲染 + 数据联通验证
 * BASE: http://127.0.0.1:5174 (vite dev with proxy)
 */
import { chromium } from '/home/ubuntu/.local/node/lib/node_modules/playwright/index.mjs'
import fs from 'fs'
import path from 'path'

const BASE = 'http://127.0.0.1:5174'
const OUT = '/home/ubuntu/.local/share/opencode/worktree/ce001bf198281167fbee74d415b3a556510875ba/silent-sailor/docs/screenshots/p3-2026-07-03'

const pages = [
  { sidebar: 'Dashboard', name: 'dashboard' },
  { sidebar: '用户管理', name: 'user' },
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
const allNetwork = []
page.on('console', m => {
  if (m.type() === 'error') allErrors.push({ url: page.url(), text: m.text() })
})
page.on('pageerror', e => allErrors.push({ url: page.url(), message: e.message }))
page.on('response', r => {
  // 抓 admin-api 调用
  if (r.url().includes('/admin/api/') && r.status() >= 400) {
    allNetwork.push({ url: r.url(), status: r.status() })
  }
})

// ===== 1. Login flow =====
console.log('→ /admin/login')
await page.goto(`${BASE}/admin/login`, { waitUntil: 'networkidle', timeout: 15000 })
await page.screenshot({ path: path.join(OUT, '01-login.png'), fullPage: true })

const loginVisible = await page.isVisible('input[placeholder*="用户"]')
console.log('  login form visible:', loginVisible)

await page.fill('input[placeholder*="用户"]', 'smokeuser')
await page.fill('input[type=password]', 'testpass123')
await page.click('button:has-text("登录")')
await page.waitForURL(/dashboard/, { timeout: 15000 })
await page.waitForTimeout(2500)
await page.screenshot({ path: path.join(OUT, '02-dashboard.png'), fullPage: true })
const dashStats = await page.evaluate(() => ({
  bodyLen: document.body.textContent?.length,
  metricCards: document.querySelectorAll('.metric-card').length,
  echartCanvases: document.querySelectorAll('canvas').length
}))
console.log('  dashboard stats:', JSON.stringify(dashStats))

// ===== 2. 8 sub pages =====
for (const p of pages) {
  console.log(`→ click ${p.sidebar}`)
  await page.click(`text="${p.sidebar}"`)
  await page.waitForTimeout(2500)
  const stat = await page.evaluate(() => ({
    url: location.href,
    bodyLen: document.body.textContent?.length,
    hasTable: !!document.querySelector('table'),
    hasForm: !!document.querySelector('form'),
    tableRows: document.querySelectorAll('table tbody tr').length,
    inputs: document.querySelectorAll('input').length,
    selects: document.querySelectorAll('.el-select').length,
    cards: document.querySelectorAll('.el-card').length
  }))
  await page.screenshot({ path: path.join(OUT, `03-${p.name}.png`), fullPage: true })
  console.log(`  ${JSON.stringify(stat)}`)
}

// ===== 3. Summary =====
const summary = {
  errors: allErrors,
  networkErrors: allNetwork,
  errorsTotal: allErrors.length,
  networkErrorsTotal: allNetwork.length,
  loginReached: dashStats.metricCards >= 4,
  pagesWithTable: 0
}
fs.writeFileSync(path.join(OUT, 'report.json'), JSON.stringify(summary, null, 2))

console.log('\n=== Summary ===')
console.log('console errors:', allErrors.length)
console.log('admin-api HTTP>=400:', allNetwork.length)
if (allErrors.length) allErrors.slice(0, 10).forEach(e => console.log('  err:', e.url, e.text || e.message))
if (allNetwork.length) allNetwork.slice(0, 10).forEach(e => console.log('  net:', e.status, e.url))

await browser.close()