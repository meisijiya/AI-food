// Records.vue 虚拟滚动 1000+ 行压力测试 (r3 POC 验证)
// 目标: 验证 @tanstack/vue-virtual 在 1000+ 数据下
//   1. DOM 节点数 < 50 (virtualized, 不随数据量增长)
//   2. 滚动到中部 + 底部不卡
//   3. 删除单条不破 virtualizer
//   4. 排序切换不破
//
// mock 策略: page.route 拦截 /api/auth/login + /api/record/list + /api/record/delete
//   - 不依赖后端 + 不依赖真实账号 + 测试稳定可重复
//
// ── 测试发现 (r3 POC bug) ─────────────────────────────────────────
// 该测试同时充当 r3 POC 的真实压力验证。当前实现(r3 POC)在 1000+ 数据下
// 不会真的虚拟化 DOM。原因: .records-container 用的是 `min-height: 100vh`
// 而非 `height: 100vh`,没有 max-height 约束。容器随内容增长到 130,000+px,
// 真正的滚动元素变成 body,而 useVirtualizer() 绑的是 .records-container
// (整高 130,000px) — virtualizer 看不到 viewport,所以全部 1000 项都渲了。
//
// 修复: 把 .records-container 的 min-height 改成 height: 100vh(或加
// max-height: calc(100vh - <nav>px)),virtualizer 立刻正常 — sanity 步骤
// 已证明(强制设 max-height 后 DOM = 11)。

import { chromium } from '/home/ubuntu/.local/node/lib/node_modules/playwright/index.mjs'
import fs from 'node:fs'
import path from 'node:path'

const BASE = process.env.BASE_URL || 'http://127.0.0.1:4173'
const OUT = '/home/ubuntu/.local/share/opencode/worktree/ce001bf198281167fbee74d415b3a556510875ba/silent-sailor/frontend/e2e/screenshots'
fs.mkdirSync(OUT, { recursive: true })

const TOTAL_ROWS = parseInt(process.env.TOTAL_ROWS || '1000', 10)

// ── 1. 1000+ 行 fixture ─────────────────────────────────────────────
function makeRecords(total) {
  return Array.from({ length: total }, (_, i) => ({
    sessionId: `sess-${String(i).padStart(4, '0')}`,
    foodName: `Food ${i} — 测试菜名 ${i}`,
    reason: `这是 food ${i} 的推荐理由,故意写长一点触发多行,验证 clamp + measureElement 实测高度`,
    createdAt: new Date(Date.now() - i * 3600_000).toISOString(),
    status: 'completed',
    mode: 'inertia'
  }))
}

let RECORDS = makeRecords(TOTAL_ROWS) // mock 中的可变状态 — delete 后会减少

// ── 2. mock handlers ────────────────────────────────────────────────
async function mockLogin(route) {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 200, message: 'ok',
      data: {
        token: `mock-token-${Date.now()}`,
        userId: 1,
        username: 'smoke',
        nickname: 'Smoke',
        email: 'smoke@aifood.local',
        avatar: null
      }
    })
  })
}

// mockList: 一次性返全部 1000 条(模拟 size=10000 的 backend),
// 后续 page>=1 返 [] 让前端 finished=true。这样测的是"1000 行已加载,
// virtualizer 该只渲 viewport 附近"的场景,而不是测分页。
async function mockList(route) {
  const url = new URL(route.request().url())
  const page = parseInt(url.searchParams.get('page') || '0', 10)
  const list = page === 0 ? RECORDS : []
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 200, message: 'ok',
      data: { list, total: RECORDS.length }
    })
  })
}

async function mockDelete(route) {
  const url = route.request().url()
  const m = url.match(/\/record\/delete\/([^/?]+)/)
  if (m) {
    const sid = decodeURIComponent(m[1])
    const idx = RECORDS.findIndex(r => r.sessionId === sid)
    if (idx >= 0) RECORDS.splice(idx, 1)
  }
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 200, message: 'ok', data: null })
  })
}

// catch-all: 任何未被上面精确匹配的 /api/* 都返回 200 + 空数据
// 目的: 避免 vite preview 返回 401/403/404 → 触发 api 拦截器的 logout 链
async function mockCatchAll(route) {
  const url = route.request().url()
  let data = null
  if (url.includes('/auth/logout')) data = null
  else if (url.includes('/record/pending')) data = { hasPending: false }
  else if (url.includes('/user/info')) data = { userId: 1, username: 'smoke', nickname: 'Smoke', email: 'smoke@aifood.local', avatar: null }
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 200, message: 'ok', data })
  })
}

// ── 3. 启动 browser + 安装路由 ──────────────────────────────────────
const browser = await chromium.launch({ headless: true })
// 移动端 viewport — Records.vue 的设计目标(手机端)
const ctx = await browser.newContext({
  viewport: { width: 390, height: 844 },
  deviceScaleFactor: 2
})
const page = await ctx.newPage()

const consoleErrors = []
const pageErrors = []
const apiCalls = []
page.on('console', m => {
  const t = m.text()
  if (m.type() === 'error' &&
      !t.includes('Failed to load resource') &&  // favicon 等静态资源
      !t.includes('favicon') &&
      !t.includes('manifest.json')) {
    consoleErrors.push(t)
  }
})
page.on('pageerror', e => pageErrors.push(e.message))
page.on('request', req => {
  if (req.url().includes('/api/')) apiCalls.push(`${req.method()} ${req.url()}`)
})

// 路由必须在 goto 之前注册
// 用单一 dispatcher — playwright 后注册的 glob 会覆盖先注册的(更宽泛的
// catch-all 会抢走 login 这种精确匹配),所以统一在一个 handler 里按 URL 分发
await page.route('**/api/**', async (route) => {
  const url = route.request().url()
  if (url.includes('/api/auth/login')) return mockLogin(route)
  if (/\/api\/record\/list(\?|$)/.test(url)) return mockList(route)
  if (url.includes('/api/record/delete/')) return mockDelete(route)
  return mockCatchAll(route)
})

// ── 4. login → records ──────────────────────────────────────────────
const tLoginStart = Date.now()
await page.goto(`${BASE}/login`)
await page.fill('input[type=email]', 'smoke@aifood.local')
await page.fill('input[type=password]', 'testpass123')
// 提交按钮是 .cta-button (type=submit),不要被 tab 按钮 "登录" 误命中
await page.click('button.cta-button[type="submit"]')
try {
  await page.waitForURL((url) => !String(url).endsWith('/login'), { timeout: 15000 })
} catch (e) {
  console.error('✗ login redirect timeout — current url:', page.url())
  console.error('  api calls so far:', apiCalls)
  console.error('  console errors:', consoleErrors)
  await page.screenshot({ path: path.join(OUT, 'debug-login.png'), fullPage: false })
  throw e
}
const loginMs = Date.now() - tLoginStart
console.log(`✓ login (mocked): ${loginMs}ms, url=${page.url()}`)

// 跳到 records — 必须走 SPA,不能 page.goto(否则 token 在内存里被重置 —
// 安全修复 M2 后 token 仅在 Pinia state,刷新即失)
const tFetchStart = Date.now()
await page.click('a.nav-item[href="/records"]').catch(async () => {
  await page.click('a[href="/records"]').catch(() => {})
})
await page.waitForURL(/\/records$/, { timeout: 10000 }).catch(() => {
  console.error('✗ nav to /records failed, current url:', page.url())
})
try {
  await page.waitForSelector('.record-item-wrapper', { timeout: 15000 })
} catch (e) {
  console.error('✗ no record cards rendered')
  console.error('  current url:', page.url())
  console.error('  api calls:', apiCalls)
  console.error('  page text:', await page.evaluate(() => document.body.innerText.slice(0, 500)))
  throw e
}
// 等首屏稳定(records.length === TOTAL_ROWS) — 用 fetchRecords 完成的副作用
// Records.vue 在 records > 0 + finished 时渲染 .no-more
await page.waitForFunction(
  (n) => {
    const wrappers = document.querySelectorAll('.record-item-wrapper[data-index]')
    return wrappers.length >= 1 || !!document.querySelector('.no-more')
  },
  { timeout: 10000 }
).catch(() => {})
await page.waitForTimeout(800)
const fetchMs = Date.now() - tFetchStart
console.log(`✓ first page fetch+render: ${fetchMs}ms`)

// ── 5. 初始 DOM count ───────────────────────────────────────────────
async function countCards() {
  return page.locator('.record-item-wrapper[data-index]').count()
}

async function containerStats() {
  return page.evaluate(() => {
    const c = document.querySelector('.records-container')
    const l = document.querySelector('.record-list')
    const w = document.querySelectorAll('.record-item-wrapper[data-index]')
    const indices = Array.from(w).map(x => parseInt(x.dataset.index, 10)).sort((a, b) => a - b)
    return {
      containerSh: c?.scrollHeight, containerCh: c?.clientHeight,
      containerOh: c?.offsetHeight, containerH: c?.getBoundingClientRect().height,
      listSh: l?.scrollHeight, listCh: l?.clientHeight,
      wrapperCount: w.length,
      firstIndex: indices[0], lastIndex: indices[indices.length - 1],
      bodySh: document.body.scrollHeight,
      bodyCh: document.body.clientHeight,
      windowInnerHeight: window.innerHeight
    }
  })
}

const initialCount = await countCards()
const initialStats = await containerStats()
console.log(`  initial DOM cards: ${initialCount}`)
console.log(`  container stats: sh=${initialStats.containerSh} ch=${initialStats.containerCh} listSh=${initialStats.listSh} bodySh=${initialStats.bodySh} winH=${initialStats.windowInnerHeight}`)
console.log(`  visible indices: first=${initialStats.firstIndex} last=${initialStats.lastIndex}`)
await page.screenshot({ path: path.join(OUT, 'records-1000-01-initial.png'), fullPage: false })

// ── 6. 滚到中部 ─────────────────────────────────────────────────────
const midTop = Math.floor(initialStats.containerSh / 2)
const tMidStart = Date.now()
await page.evaluate((top) => {
  const el = document.querySelector('.records-container')
  if (el) {
    el.scrollTop = top
    el.dispatchEvent(new Event('scroll'))
  }
}, midTop)
await page.waitForTimeout(1500)
const midCount = await countCards()
const midStats = await containerStats()
const midMs = Date.now() - tMidStart
console.log(`✓ mid scroll (top=${midTop}px): DOM=${midCount}, ${midMs}ms`)
console.log(`  visible indices: first=${midStats.firstIndex} last=${midStats.lastIndex}`)
await page.screenshot({ path: path.join(OUT, 'records-1000-02-mid.png'), fullPage: false })

// ── 7. 滚到底部 ─────────────────────────────────────────────────────
const tBottomStart = Date.now()
await page.evaluate(() => {
  const el = document.querySelector('.records-container')
  if (el) {
    el.scrollTop = el.scrollHeight
    el.dispatchEvent(new Event('scroll'))
  }
})
// .no-more 出现 = finished=true(后续 page mock 返回 [])
await page.waitForFunction(() => !!document.querySelector('.no-more'), { timeout: 10000 }).catch(() => {})
await page.waitForTimeout(800)
const bottomCount = await countCards()
const bottomStats = await containerStats()
const bottomMs = Date.now() - tBottomStart
console.log(`✓ bottom scroll: DOM=${bottomCount}, ${bottomMs}ms`)
console.log(`  visible indices: first=${bottomStats.firstIndex} last=${bottomStats.lastIndex} (last=${TOTAL_ROWS - 1}=Food ${TOTAL_ROWS - 1}?)`)
await page.screenshot({ path: path.join(OUT, 'records-1000-03-bottom.png'), fullPage: false })

// ── 8. 排序切换 — 应重置 records → fetchRecords(true) ─────────────
const sortBtnText0 = (await page.locator('.sort-btn').textContent()).trim()
await page.click('.sort-btn')
// 排序后等待 records 重新渲染 + 稳定
await page.waitForTimeout(1500)
const sortBtnText1 = (await page.locator('.sort-btn').textContent()).trim()
const afterSortCount = await countCards()
console.log(`✓ sort toggle: "${sortBtnText0}" → "${sortBtnText1}", DOM=${afterSortCount}`)
await page.screenshot({ path: path.join(OUT, 'records-1000-04-sorted.png'), fullPage: false })

// ── 9. 删除单条 ─────────────────────────────────────────────────────
// 滚回顶部让 delete 按钮可见
await page.evaluate(() => {
  const el = document.querySelector('.records-container')
  if (el) el.scrollTop = 0
})
await page.waitForTimeout(500)
const beforeDeleteTotal = RECORDS.length
const firstDeleteBtn = page.locator('.record-card .card-delete').first()
// force: true 绕过 SVG 拦截(actionability check)
await firstDeleteBtn.click({ force: true })
await page.waitForTimeout(1200) // 等 showSuccess + records filter
const afterDeleteTotal = RECORDS.length
const afterDeleteCount = await countCards()
console.log(`✓ delete one: mock total ${beforeDeleteTotal} → ${afterDeleteTotal}, DOM=${afterDeleteCount}`)
await page.screenshot({ path: path.join(OUT, 'records-1000-05-deleted.png'), fullPage: false })

// ── 10. (诊断) sanity: 强制约束 scrollContainer 高度 → virtualizer 才生效 ──
// 这步证明: r3 POC 的 virtualizer 逻辑本身没问题,问题在 .records-container
// 用了 min-height: 100vh 而非 height: 100vh(或 max-height),导致容器随内容
// 增长到 130,000+px,body 才是真正滚动元素,virtualizer 看不到 viewport。
const tSanityStart = Date.now()
await page.evaluate(() => {
  const c = document.querySelector('.records-container')
  if (c) {
    c.style.maxHeight = `${window.innerHeight - 60}px`  // 留 60px 给 nav-bar
    c.style.overflow = 'auto'
  }
  window.dispatchEvent(new Event('resize'))
})
await page.waitForTimeout(1500)
const sanityCount = await countCards()
const sanityStats = await containerStats()
console.log(`\n[sanity] 强制 max-height=${await page.evaluate(() => window.innerHeight - 60)}px 后:`)
console.log(`  container stats: sh=${sanityStats.containerSh} ch=${sanityStats.containerCh}`)
console.log(`  DOM cards: ${sanityCount} (期望 < 50,证明 virtualizer 本身工作)`)
await page.screenshot({ path: path.join(OUT, 'records-1000-06-constrained-sanity.png'), fullPage: false })
const sanityMs = Date.now() - tSanityStart

// ── 11. 总结 + 通过判定 ────────────────────────────────────────────
const DOM_LIMIT = 50  // virtualizer + overscan 5*2 + viewport ≈ 17, 留 50 富余
const assertions = [
  { name: 'initial DOM < 50', pass: initialCount < DOM_LIMIT, actual: initialCount },
  { name: 'mid DOM < 50',    pass: midCount    < DOM_LIMIT, actual: midCount },
  { name: 'bottom DOM < 50', pass: bottomCount < DOM_LIMIT, actual: bottomCount },
  { name: 'after sort DOM < 50', pass: afterSortCount < DOM_LIMIT, actual: afterSortCount },
  { name: 'after delete DOM < 50', pass: afterDeleteCount < DOM_LIMIT, actual: afterDeleteCount },
  { name: 'no JS errors',    pass: pageErrors.length === 0, actual: pageErrors.length },
  { name: '[sanity] virtualizer works with max-height', pass: sanityCount < DOM_LIMIT, actual: sanityCount }
]

console.log('\n=== Assertions ===')
let allPass = true
for (const a of assertions) {
  const mark = a.pass ? '✓' : '✗'
  console.log(`  ${mark} ${a.name} (actual=${a.actual})`)
  if (!a.pass) allPass = false
}

console.log('\n=== Console errors (filtered) ===')
console.log(`  count: ${consoleErrors.length}`)
consoleErrors.slice(0, 5).forEach(e => console.log(`    - ${e}`))

console.log('\n=== Perf ===')
console.log(`  fetch first page:  ${fetchMs}ms`)
console.log(`  scroll to mid:     ${midMs}ms`)
console.log(`  scroll to bottom:  ${bottomMs}ms`)
console.log(`  sanity check:      ${sanityMs}ms`)

// 诊断信息: 帮后续 debug 看出问题
const diagnosis = {
  containerMinHeight: await page.evaluate(() => getComputedStyle(document.querySelector('.records-container')).minHeight),
  containerMaxHeight: await page.evaluate(() => getComputedStyle(document.querySelector('.records-container')).maxHeight),
  containerOverflow: await page.evaluate(() => getComputedStyle(document.querySelector('.records-container')).overflowY),
  // 当初状态:
  initialContainerH: initialStats.containerH,
  initialBodyH: initialStats.bodySh,
  initialWindowH: initialStats.windowInnerHeight
}

console.log('\n=== Diagnosis (r3 POC issue) ===')
console.log(`  .records-container min-height: ${diagnosis.containerMinHeight}`)
console.log(`  .records-container max-height: ${diagnosis.containerMaxHeight}  ← 缺这个!`)
console.log(`  .records-container overflow-y: ${diagnosis.containerOverflow}`)
console.log(`  initial containerH: ${diagnosis.initialContainerH}px vs windowH: ${diagnosis.initialWindowH}px`)
console.log(`  → 容器无 max-height 约束,内容增长到 ${diagnosis.initialContainerH}px,body 才是滚动元素`)
console.log(`  → virtualizer.getScrollElement() 拿到的是 records-container(整高),所以渲染全部 1000 项`)

const report = {
  totalRows: TOTAL_ROWS,
  domCounts: { initial: initialCount, mid: midCount, bottom: bottomCount, afterSort: afterSortCount, afterDelete: afterDeleteCount, sanity: sanityCount },
  perf: { fetchMs, midMs, bottomMs, sanityMs, loginMs },
  consoleErrors: consoleErrors.length,
  pageErrors: pageErrors.length,
  pageErrorsList: pageErrors.slice(0, 10),
  assertions: assertions.map(a => ({ name: a.name, pass: a.pass, actual: a.actual })),
  diagnosis,
  pass: allPass && consoleErrors.length === 0
}
fs.writeFileSync(path.join(OUT, 'records-1000-report.json'), JSON.stringify(report, null, 2))

console.log(`\n${allPass && consoleErrors.length === 0 ? '✅ PASS' : '❌ FAIL'} — report: ${OUT}/records-1000-report.json`)

await browser.close()
process.exit(allPass && consoleErrors.length === 0 ? 0 : 1)
