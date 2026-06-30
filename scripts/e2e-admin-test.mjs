#!/usr/bin/env node
// Admin 后台无头浏览器端到端测试
// 用 Playwright 跑一遍所有主要页面,捕获 console error / 渲染失败 / 交互问题
import { chromium } from '/home/ubuntu/.local/node/lib/node_modules/playwright/index.mjs'
import fs from 'fs'
import path from 'path'

const BASE = process.env.BASE_URL || 'http://119.29.52.111'
const ADMIN_USER = 'smoke@aifood.local'
const ADMIN_PASS = 'testpass123'
const OUT_DIR = '/home/ubuntu/.local/share/opencode/worktree/ce001bf198281167fbee74d415b3a556510875ba/silent-sailor/docs/screenshots/admin-test-2026-06-30'

const findings = []
const pages = [
  { path: '/login', name: 'login', auth: false },
  { path: '/dashboard', name: 'dashboard', auth: true },
  { path: '/user', name: 'user-management', auth: true },
  { path: '/conversation', name: 'conversation', auth: true },
  { path: '/token-usage', name: 'token-usage', auth: true },
  { path: '/model', name: 'model', auth: true },
  { path: '/recommendation', name: 'recommendation', auth: true },
  { path: '/monitor', name: 'monitor', auth: true },
  { path: '/audit-log', name: 'audit-log', auth: true }
]

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()

  const consoleErrors = []
  const networkFails = []
  const pageErrors = []

  page.on('console', msg => {
    if (msg.type() === 'error') consoleErrors.push({ url: page.url(), text: msg.text() })
  })
  page.on('pageerror', err => {
    pageErrors.push({ url: page.url(), message: err.message })
  })
  page.on('requestfailed', req => {
    networkFails.push({ url: req.url(), failure: req.failure()?.errorText })
  })
  page.on('response', resp => {
    if (resp.status() >= 400) {
      networkFails.push({ url: resp.url(), status: resp.status() })
    }
  })

  // Step 1: 访问登录页
  console.log('1. /login')
  await page.goto(`${BASE}/admin/login`, { waitUntil: 'networkidle' })
  await page.screenshot({ path: path.join(OUT_DIR, '01-login.png'), fullPage: true })

  // Step 2: 登录
  console.log('2. login submit')
  await page.fill('input[placeholder*="用户名"], input[type="text"]', ADMIN_USER)
  await page.fill('input[type="password"]', ADMIN_PASS)
  await page.click('button:has-text("登录")')
  await page.waitForURL(/\/dashboard/, { timeout: 15000 })
  await page.screenshot({ path: path.join(OUT_DIR, '02-dashboard.png'), fullPage: true })

  // Step 3: 遍历所有页面
  for (const p of pages.slice(1)) { // skip login
    console.log(`3.${p.name} → /admin${p.path}`)
    const before = consoleErrors.length
    await page.goto(`${BASE}/admin${p.path}`, { waitUntil: 'networkidle', timeout: 15000 })
    await page.waitForTimeout(800)  // 等 ECharts/数据加载
    await page.screenshot({ path: path.join(OUT_DIR, `03-${p.name}.png`), fullPage: true })
    const after = consoleErrors.length
    findings.push({
      page: p.name,
      consoleErrorsAdded: after - before
    })
  }

  // Step 4: 写报告
  const report = {
    base: BASE,
    timestamp: new Date().toISOString(),
    consoleErrors,
    pageErrors,
    networkFails,
    perPageFindings: findings
  }
  fs.writeFileSync(path.join(OUT_DIR, 'report.json'), JSON.stringify(report, null, 2))
  console.log('\n=== REPORT ===')
  console.log('console errors:', consoleErrors.length)
  console.log('page errors:', pageErrors.length)
  console.log('network fails:', networkFails.length)
  console.log('per-page new errors:', findings)
  if (consoleErrors.length) {
    console.log('\nFirst 5 console errors:')
    consoleErrors.slice(0, 5).forEach(e => console.log(' -', e.url, e.text.slice(0, 200)))
  }
  if (pageErrors.length) {
    console.log('\nFirst 5 page errors:')
    pageErrors.slice(0, 5).forEach(e => console.log(' -', e.url, e.message.slice(0, 200)))
  }
  if (networkFails.length) {
    console.log('\nFirst 5 network fails:')
    networkFails.slice(0, 5).forEach(e => console.log(' -', e.url, JSON.stringify(e.status || e.failure)))
  }

  await browser.close()
}

main().catch(e => { console.error(e); process.exit(1) })
