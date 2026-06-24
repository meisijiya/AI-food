# Selenium 集成测试使用指南

> **A2 基础包装目标**：用 Selenium 验证 5 个关键页面/接口可访问，作为"作品能跑"的硬证据。

---

## 一、脚本能做啥

`scripts/selenium_smoke_test.py` 自动启动 Chrome 浏览器，验证：

| # | 测试 | 验证内容 |
|---|------|---------|
| 1 | 前端首页加载 | 页面有 title + Vue `#app` 挂载点 |
| 2 | 后端健康检查 | `/actuator/health` 返回 `UP` |
| 3 | Knife4j 文档 | `/doc.html` Swagger UI 加载 |
| 4 | 注册入口 | `/#/register` 路由 + 表单存在 |
| 5 | 登录入口 | `/#/login` 路由 + 表单存在 |

每个测试都会**截图**到 `docs/screenshots/` 目录——可直接放简历 / GitHub README。

---

## 二、环境要求

```bash
# 1. Python 3.8+
python --version

# 2. Docker Compose 已启动（前端 + 后端）
docker compose up -d
# 等待 30-60 秒让 MySQL/Redis 初始化完

# 3. 安装 Python 依赖
pip install -r scripts/requirements.txt
```

---

## 三、运行

```bash
# 默认：headless 模式（无浏览器窗口，适合 CI）
python scripts/selenium_smoke_test.py

# 有界面模式（看得到浏览器跑）
python scripts/selenium_smoke_test.py --no-headless

# 自定义地址（比如远程服务器）
python scripts/selenium_smoke_test.py --base-url http://your-server:3000 --backend-url http://your-server:8080
```

---

## 四、输出示例

```
==> 启动 Chrome (headless=True)
    Chrome 启动成功

==> Test 1: 前端首页加载
    ✅ PASS  首页 title 不为空 — title='AI美食推荐'
    ✅ PASS  Vue 挂载点 #app 存在
    📸 截图：docs/screenshots/20260624_091500_01_homepage.png

==> Test 2: 后端健康检查
    ✅ PASS  健康检查返回 UP — body={"status":"UP",...}

==> Test 3: Knife4j API 文档
    ✅ PASS  Knife4j 文档加载

==> Test 4: 注册页面入口
    ✅ PASS  注册路由跳转
    ✅ PASS  注册页有 input 元素 — 找到 3 个 input

==> Test 5: 登录页面入口
    ✅ PASS  登录路由跳转
    ✅ PASS  登录页有 input 元素 — 找到 2 个 input

============================================================
测试总结：9/9 通过，0 失败
============================================================

🎉 全部通过！AI-food A2 基础包装完成
```

---

## 五、截图清单

运行后 `docs/screenshots/` 会有 5 张 PNG：

```
docs/screenshots/
├── 20260624_091500_01_homepage.png       # 前端首页
├── 20260624_091500_02_health.png         # 健康检查
├── 20260624_091500_03_knife4j.png        # API 文档
├── 20260624_091500_04_register.png       # 注册页
└── 20260624_091500_05_login.png          # 登录页
```

**简历 / GitHub README 直接引用**：
- 主页截图证明「能跑」
- Knife4j 截图证明「接口完整」
- 注册 / 登录截图证明「业务流程可走通」

---

## 六、常见问题

### 1. `Chrome 启动失败`

```
WebDriverException: 'chromedriver' executable needs to be in PATH
```

**解决**：
```bash
pip install webdriver-manager  # 自动管理 chromedriver
```

### 2. `Connection refused`

```
http://localhost:3000 拒绝连接
```

**解决**：
```bash
docker compose ps           # 确认 4 个服务都 UP
docker compose logs frontend  # 看前端日志
```

### 3. `健康检查返回 DOWN`

```
{"status":"DOWN"}
```

**解决**：
```bash
docker compose logs mysql     # MySQL 没起来？
docker compose logs redis     # Redis 没起来？
docker compose restart backend # 重启 backend
```

### 4. 想跑在 CI（GitHub Actions）

```yaml
- name: Selenium 冒烟测试
  run: |
    docker compose up -d
    sleep 60  # 等初始化
    pip install -r scripts/requirements.txt
    python scripts/selenium_smoke_test.py
```

---

## 七、扩展

`SmokeTest` 类可继续加测试用例：

```python
# 测 AI 推荐流程
def test_06_ai_recommend(self):
    self.driver.get(f"{self.base_url}/#/match")
    time.sleep(2)
    # 模拟 7 个参数
    # ...
```

加完后调用 `test.run()` 即可。

---

## 八、相关文件

```
AI-food/
├── scripts/
│   ├── selenium_smoke_test.py    # 本测试
│   ├── requirements.txt          # Python 依赖
│   └── README.md                 # 本文档
└── docs/
    └── screenshots/              # 截图（git 忽略）
```
