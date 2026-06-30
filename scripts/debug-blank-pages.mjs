import { chromium } from '/home/ubuntu/.local/node/lib/node_modules/playwright/index.mjs'

const BASE = 'http://119.29.52.111'

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()

  const consoleMsgs = []
  page.on('console', msg => consoleMsgs.push({ type: msg.type(), text: msg.text() }))
  page.on('pageerror', err => consoleMsgs.push({ type: 'pageerror', text: err.message }))

  // Login
  await page.goto(`${BASE}/admin/login`, { waitUntil: 'networkidle' })
  await page.fill('input[type="text"]', 'smoke@aifood.local')
  await page.fill('input[type="password"]', 'testpass123')
  await page.click('button:has-text("登录")')
  await page.waitForURL(/\/dashboard/, { timeout: 15000 })
  await page.waitForTimeout(2000)

  // Check user page
  console.log('=== /user ===')
  consoleMsgs.length = 0
  await page.goto(`${BASE}/admin/user`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(3000)

  // Check actual DOM
  const dom = await page.evaluate(() => ({
    mainHTML: document.querySelector('main')?.innerHTML?.length || 0,
    mainText: document.querySelector('main')?.textContent?.trim().slice(0, 200) || '',
    hasForm: !!document.querySelector('form'),
    hasTable: !!document.querySelector('table'),
    hasCards: document.querySelectorAll('.el-card').length,
    hasInputs: document.querySelectorAll('input').length,
    hasSelects: document.querySelectorAll('.el-select').length
  }))
  console.log('DOM:', JSON.stringify(dom, null, 2))

  // Check console
  console.log('Console msgs during /user:')
  consoleMsgs.forEach(m => console.log(` - [${m.type}] ${m.text.slice(0, 200)}`))

  // Try a working page for comparison
  console.log('\n=== /model (should work) ===')
  await page.goto(`${BASE}/admin/model`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(2000)
  const dom2 = await page.evaluate(() => ({
    mainHTML: document.querySelector('main')?.innerHTML?.length || 0,
    mainText: document.querySelector('main')?.textContent?.trim().slice(0, 200) || '',
    hasTable: !!document.querySelector('table')
  }))
  console.log('DOM /model:', JSON.stringify(dom2, null, 2))

  await browser.close()
}

main().catch(e => { console.error(e); process.exit(1) })
