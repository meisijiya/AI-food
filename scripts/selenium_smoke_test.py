#!/usr/bin/env python3
"""
AI-food Selenium 集成测试

目标：
- 启动浏览器（Chrome）
- 访问 http://localhost:3000
- 验证 5 件事：页面加载、登录入口、注册入口、API 健康、API 文档入口

运行环境要求：
- Docker Compose 已启动（前端 3000 + 后端 8080）
- Python 3.8+ 环境
- 已安装 selenium + webdriver-manager：
    pip install selenium webdriver-manager

运行：
    python scripts/selenium_smoke_test.py
    # 或者
    python scripts/selenium_smoke_test.py --headless  # 无头模式
    python scripts/selenium_smoke_test.py --base-url http://localhost:3000  # 自定义地址
"""

import sys
import time
import argparse
from pathlib import Path
from datetime import datetime

try:
    from selenium import webdriver
    from selenium.webdriver.common.by import By
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC
    from selenium.common.exceptions import TimeoutException, WebDriverException
except ImportError:
    print("ERROR: 缺少依赖，请先运行：")
    print("  pip install selenium webdriver-manager")
    sys.exit(1)

try:
    from webdriver_manager.chrome import ChromeDriverManager
    from selenium.webdriver.chrome.service import Service
    USE_WDM = True
except ImportError:
    USE_WDM = False
    print("[warn] webdriver-manager 未安装，将使用系统已安装的 ChromeDriver")


# ============================================
# 测试用例
# ============================================

class SmokeTest:
    """AI-food 冒烟测试 — 验证 5 个关键页面/接口。"""

    def __init__(self, base_url: str = "http://localhost:3000", backend_url: str = "http://localhost:8080", headless: bool = True):
        self.base_url = base_url
        self.backend_url = backend_url
        self.headless = headless
        self.driver = None
        self.results = []
        self.screenshot_dir = Path("docs/screenshots")
        self.screenshot_dir.mkdir(parents=True, exist_ok=True)

    def setup(self):
        """启动 Chrome 浏览器。"""
        print(f"\n==> 启动 Chrome (headless={self.headless})")
        options = webdriver.ChromeOptions()
        if self.headless:
            options.add_argument("--headless=new")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--window-size=1920,1080")
        # 真实 UA，避免被反爬
        options.add_argument(
            "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
        # 关闭自动化标记
        options.add_argument("--disable-blink-features=AutomationControlled")

        try:
            if USE_WDM:
                service = Service(ChromeDriverManager().install())
                self.driver = webdriver.Chrome(service=service, options=options)
            else:
                self.driver = webdriver.Chrome(options=options)
            self.driver.set_page_load_timeout(30)
            print("    Chrome 启动成功")
        except WebDriverException as e:
            print(f"    ERROR: Chrome 启动失败：{e}")
            print("    请确认：")
            print("    1. Chrome 已安装")
            print("    2. ChromeDriver 与 Chrome 版本匹配")
            print("    3. pip install selenium webdriver-manager")
            sys.exit(1)

    def teardown(self):
        """关闭浏览器。"""
        if self.driver:
            self.driver.quit()
            print("    Chrome 关闭")

    def run(self):
        """运行所有测试用例。"""
        self.setup()
        try:
            # Test 1: 前端首页加载
            self.test_01_frontend_homepage()
            # Test 2: 后端健康检查
            self.test_02_backend_health()
            # Test 3: Knife4j API 文档
            self.test_03_knife4j_docs()
            # Test 4: 注册页面入口
            self.test_04_register_entry()
            # Test 5: 登录页面入口
            self.test_05_login_entry()
        finally:
            self.teardown()
            self.print_summary()

    def assert_true(self, name: str, condition: bool, detail: str = ""):
        """记录测试结果。"""
        status = "✅ PASS" if condition else "❌ FAIL"
        self.results.append((name, condition, detail))
        print(f"    {status}  {name}{(' — ' + detail) if detail else ''}")

    def screenshot(self, name: str):
        """保存截图。"""
        if not self.driver:
            return
        ts = datetime.now().strftime("%Y%m%d_%H%M%S")
        path = self.screenshot_dir / f"{ts}_{name}.png"
        try:
            self.driver.save_screenshot(str(path))
            print(f"    📸 截图：{path}")
        except Exception as e:
            print(f"    [warn] 截图失败：{e}")

    # ---------------- 测试用例 ----------------

    def test_01_frontend_homepage(self):
        """测试 1: 前端首页加载。"""
        print("\n==> Test 1: 前端首页加载")
        try:
            self.driver.get(self.base_url)
            WebDriverWait(self.driver, 10).until(
                lambda d: d.title is not None
            )
            title = self.driver.title
            self.assert_true(
                "首页 title 不为空",
                bool(title and title.strip()),
                f"title='{title}'"
            )
            # 检查页面有 Vue 挂载点
            try:
                WebDriverWait(self.driver, 5).until(
                    EC.presence_of_element_located((By.CSS_SELECTOR, "#app"))
                )
                self.assert_true("Vue 挂载点 #app 存在", True)
            except TimeoutException:
                self.assert_true("Vue 挂载点 #app 存在", False, "找不到 #app")
            self.screenshot("01_homepage")
        except (TimeoutException, WebDriverException) as e:
            self.assert_true("首页加载", False, str(e))

    def test_02_backend_health(self):
        """测试 2: 后端健康检查。"""
        print("\n==> Test 2: 后端健康检查")
        try:
            self.driver.get(f"{self.backend_url}/actuator/health")
            time.sleep(1)
            body = self.driver.find_element(By.TAG_NAME, "body").text
            self.assert_true(
                "健康检查返回 UP",
                "UP" in body,
                f"body={body[:100]}"
            )
            self.screenshot("02_health")
        except (TimeoutException, WebDriverException) as e:
            self.assert_true("健康检查", False, str(e))

    def test_03_knife4j_docs(self):
        """测试 3: Knife4j API 文档。"""
        print("\n==> Test 3: Knife4j API 文档")
        try:
            self.driver.get(f"{self.backend_url}/doc.html")
            WebDriverWait(self.driver, 10).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, ".knife4j-swagger-container, .swagger-ui"))
            )
            self.assert_true("Knife4j 文档加载", True, f"title={self.driver.title}")
            self.screenshot("03_knife4j")
        except (TimeoutException, WebDriverException) as e:
            self.assert_true("Knife4j 文档加载", False, str(e))

    def test_04_register_entry(self):
        """测试 4: 注册页面入口。"""
        print("\n==> Test 4: 注册页面入口")
        try:
            self.driver.get(f"{self.base_url}/#/register")
            time.sleep(2)
            url = self.driver.current_url
            self.assert_true(
                "注册路由跳转",
                "register" in url or "/register" in url,
                f"url={url}"
            )
            # 检查页面有表单
            forms = self.driver.find_elements(By.TAG_NAME, "input")
            self.assert_true(
                "注册页有 input 元素",
                len(forms) >= 1,
                f"找到 {len(forms)} 个 input"
            )
            self.screenshot("04_register")
        except (TimeoutException, WebDriverException) as e:
            self.assert_true("注册页面入口", False, str(e))

    def test_05_login_entry(self):
        """测试 5: 登录页面入口。"""
        print("\n==> Test 5: 登录页面入口")
        try:
            self.driver.get(f"{self.base_url}/#/login")
            time.sleep(2)
            url = self.driver.current_url
            self.assert_true(
                "登录路由跳转",
                "login" in url or "/login" in url,
                f"url={url}"
            )
            forms = self.driver.find_elements(By.TAG_NAME, "input")
            self.assert_true(
                "登录页有 input 元素",
                len(forms) >= 1,
                f"找到 {len(forms)} 个 input"
            )
            self.screenshot("05_login")
        except (TimeoutException, WebDriverException) as e:
            self.assert_true("登录页面入口", False, str(e))

    # ---------------- 总结 ----------------

    def print_summary(self):
        """打印测试结果总结。"""
        total = len(self.results)
        passed = sum(1 for _, ok, _ in self.results if ok)
        failed = total - passed

        print("\n" + "=" * 60)
        print(f"测试总结：{passed}/{total} 通过，{failed} 失败")
        print("=" * 60)
        for name, ok, detail in self.results:
            status = "✅" if ok else "❌"
            print(f"  {status}  {name}")
        print("=" * 60)

        if failed > 0:
            print(f"\n⚠️  有 {failed} 个测试失败，请检查：")
            print("    1. Docker Compose 是否启动：docker compose ps")
            print("    2. 前端 3000 端口：curl http://localhost:3000")
            print("    3. 后端 8080 端口：curl http://localhost:8080/actuator/health")
            print("    4. 浏览器 Console 错误：按 F12 查看")
            sys.exit(1)
        else:
            print("\n🎉 全部通过！AI-food A2 基础包装完成")
            sys.exit(0)


# ============================================
# 入口
# ============================================

def main():
    parser = argparse.ArgumentParser(description="AI-food Selenium 冒烟测试")
    parser.add_argument("--base-url", default="http://localhost:3000", help="前端地址")
    parser.add_argument("--backend-url", default="http://localhost:8080", help="后端地址")
    parser.add_argument("--headless", action="store_true", default=True, help="无头模式（默认）")
    parser.add_argument("--no-headless", dest="headless", action="store_false", help="有界面模式（看得到浏览器）")
    args = parser.parse_args()

    test = SmokeTest(
        base_url=args.base_url,
        backend_url=args.backend_url,
        headless=args.headless,
    )
    test.run()


if __name__ == "__main__":
    main()
