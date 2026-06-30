import { chromium } from '/home/ubuntu/.local/node/lib/node_modules/playwright/index.mjs'

const BASE = 'http://119.29.52.111'

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()

  const consoleMsgs = []
  page.on('console', msg => consoleMsgs.push({ type: msg.type(), text: msg.text() }))
  page.on('pageerror', err => consoleMsgs.push({ type: 'pageerror', text: err.message, stack: err.stack }))
  page.on('requestfailed', req => consoleMsgs.push({ type: 'requestfail', url: req.url(), err: req.failure()?.errorText }))

  // Login
  await page.goto(`${BASE}/admin/login`, { waitUntil: 'networkidle' })
  await page.fill('input[type="text"]', 'smoke@aifood.local')
  await page.fill('input[type="password"]', 'testpass123')
  await page.click('button:has-text("登录")')
  await page.waitForURL(/\/dashboard/, { timeout: 15000 })
  await page.waitForTimeout(2000)

  console.log('=== /user deep debug ===')
  consoleMsgs.length = 0
  await page.goto(`${BASE}/admin/user`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(3000)

  // Get all elements at top of body
  const debug = await page.evaluate(() => {
    return {
      url: location.href,
      bodyText: document.body.textContent?.trim().slice(0, 500) || '',
      mainExists: !!document.querySelector('main'),
      mainChildrenCount: document.querySelector('main')?.children.length || 0,
      asideExists: !!document.querySelector('aside'),
      headerExists: !!document.querySelector('header'),
      appHtml: document.getElementById('app')?.innerHTML?.length || 0,
      appChildren: Array.from(document.getElementById('app')?.children || []).map(c => c.tagName)
    }
  })
  console.log('Debug:', JSON.stringify(debug, null, 2))

  console.log('\nConsole messages:')
  consoleMsgs.forEach(m => console.log(` [${m.type}] ${m.text?.slice(0, 200) || m.url}`))

  await browser.close()
}

main().catch(e => { console.error(e); process.exit(1) })
