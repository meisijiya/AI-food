#!/usr/bin/env bash
# ============================================
# check-secrets.sh — pre-commit 敏感信息扫描
# ============================================
# 用法：
#   scripts/check-secrets.sh           # 扫暂存区 + 工作区
#   scripts/check-secrets.sh --staged  # 只扫 git staged 文件
#   scripts/check-secrets.sh --all     # 扫整个 git 历史
# 退出码：0 = OK；1 = 发现疑似敏感信息
#
# 配套：
#   .pre-commit-config.yaml（pre-commit 框架）
#   AGENTS.md §"🔐 敏感信息处理" 全局规则
# ============================================

set -euo pipefail

# ---------- 配置 ----------
# 常见敏感 pattern（兜底用；gitleaks 规则更全）
# 通用赋值 pattern 要求：
#   - 紧跟引号包裹：catch `apiKey = "..."` 这类代码硬编码
#   - 或裸 = 后 ≥24 字节（避免误报 Java for 循环的 `String token :`）
PATTERNS=(
  # API key 前缀（无需引号也能识别）
  'sk-[A-Za-z0-9]{20,}'                # OpenAI / DeepSeek
  'sk-ant-[A-Za-z0-9-]{20,}'           # Anthropic
  'AKIA[0-9A-Z]{16}'                   # AWS access key
  'AIza[0-9A-Za-z_-]{35}'              # GCP API key
  'ghp_[A-Za-z0-9]{30,}'               # GitHub PAT
  'xox[baprs]-[A-Za-z0-9-]{10,}'       # Slack
  # 通用赋值：要求引号包裹（强信号）
  '(api[_-]?key|apikey|secret|password|passwd|access[_-]?key)\s*[=:]\s*["\x27][A-Za-z0-9+/=_-]{16,}["\x27]'
  # 通用赋值：裸 = + 长度 ≥24（Properties 风格 / 等号紧贴）
  '(api[_-]?key|apikey|secret|password|passwd|access[_-]?key)=[A-Za-z0-9+/=_-]{24,}'
  # 私钥
  '-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----'
  # JWT
  'eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}'
)

# 必须被 .gitignore 覆盖的文件
MUST_BE_IGNORED=(
  ".env"
  ".env.local"
  ".env.*.local"
)

# ---------- 颜色 ----------
RED=$'\033[0;31m'
GREEN=$'\033[0;32m'
YELLOW=$'\033[1;33m'
CYAN=$'\033[0;36m'
NC=$'\033[0m'

info()  { printf "${CYAN}[INFO]${NC} %s\n" "$*"; }
ok()    { printf "${GREEN}[ OK ]${NC} %s\n" "$*"; }
warn()  { printf "${YELLOW}[WARN]${NC} %s\n" "$*"; }
fail()  { printf "${RED}[FAIL]${NC} %s\n" "$*" >&2; }

# ---------- 工具检测 ----------
HAS_GITLEAKS=false
HAS_TRUFFLEHOG=false
command -v gitleaks >/dev/null 2>&1 && HAS_GITLEAKS=true
command -v trufflehog >/dev/null 2>&1 && HAS_TRUFFLEHOG=true

# ---------- 步骤 1：检查 .env 是否被 git 忽略 ----------
info "步骤 1/3：检查 .env 是否被 .gitignore 覆盖"
for f in "${MUST_BE_IGNORED[@]}"; do
  if [ -f "$f" ]; then
    # 文件存在才检查
    if git check-ignore -q "$f" 2>/dev/null; then
      ok "$f → 已被 .gitignore 覆盖"
    else
      fail "$f 存在但未被 .gitignore 覆盖！请在 .gitignore 加一行：$f"
      IGNORE_FAIL=1
    fi
  fi
done
# 显式检查常见的真实文件 + 权限
for f in .env .env.local; do
  if [ -f "$f" ]; then
    if git check-ignore -q "$f" 2>/dev/null; then
      ok "$f → 已被 .gitignore 覆盖"
    else
      fail "$f 存在但未被 .gitignore 覆盖！请在 .gitignore 加一行：$f"
      IGNORE_FAIL=1
    fi
    # 权限检查
    perms=$(stat -c '%a' "$f" 2>/dev/null || stat -f '%Lp' "$f" 2>/dev/null || echo "")
    if [ -n "$perms" ] && [ "$perms" != "600" ] && [ "$perms" != "400" ]; then
      warn "$f 权限是 $perms，建议 chmod 600"
    else
      ok "$f 权限 = ${perms:-?} ✓"
    fi
  fi
done

# ---------- 步骤 2：扫工作区 / staged 文件 ----------
SCAN_MODE="staged"
[ "${1:-}" = "--all" ] && SCAN_MODE="all"
[ "${1:-}" = "--staged" ] && SCAN_MODE="staged"
[ "${1:-}" = "--working" ] && SCAN_MODE="working"

info "步骤 2/3：扫描敏感信息（模式：$SCAN_MODE）"

# 初始化数组（set -u 兼容：空数组必须先声明）
FILES=()
SECRET_FOUND=0

# 决定扫哪些文件
if git rev-parse --git-dir >/dev/null 2>&1; then
  case "$SCAN_MODE" in
    staged)
      while IFS= read -r line; do
        FILES+=("$line")
      done < <(git diff --cached --name-only --diff-filter=ACM 2>/dev/null | grep -vE '\.(png|jpg|jpeg|gif|pdf|zip|tar|gz|jar|war|class|min\.js|min\.css)$' || true)
      ;;
    working)
      while IFS= read -r line; do
        FILES+=("$line")
      done < <(git ls-files --others --exclude-standard 2>/dev/null | head -200 || true)
      while IFS= read -r line; do
        FILES+=("$line")
      done < <(git diff --cached --name-only 2>/dev/null || true)
      ;;
    all)
      while IFS= read -r line; do
        FILES+=("$line")
      done < <(git ls-files 2>/dev/null | head -1000 || true)
      ;;
  esac
else
  warn "非 git 仓库，跳过文件扫描（仅做 .env 权限 + gitignore 检查）"
fi

# 优先用 gitleaks
if [ "$HAS_GITLEAKS" = "true" ]; then
  info "使用 gitleaks（高精度）..."
  if [ "$SCAN_MODE" = "all" ]; then
    if gitleaks detect --source . --no-banner -v >/tmp/gitleaks.log 2>&1; then
      :
    else
      tail -20 /tmp/gitleaks.log
      SECRET_FOUND=1
    fi
  else
    if [ ${#FILES[@]} -gt 0 ] || [ "$SCAN_MODE" = "staged" ]; then
      if gitleaks protect --staged --no-banner >/tmp/gitleaks.log 2>&1; then
        :
      else
        tail -20 /tmp/gitleaks.log
        SECRET_FOUND=1
      fi
    fi
  fi
elif [ "$HAS_TRUFFLEHOG" = "true" ]; then
  info "使用 trufflehog（高精度）..."
  if trufflehog git file://. --only-verified >/tmp/trufflehog.log 2>&1; then
    :
  else
    tail -20 /tmp/trufflehog.log
    SECRET_FOUND=1
  fi
else
  # 兜底：grep 扫
  warn "gitleaks/trufflehog 未安装，使用 regex 兜底（精度低，建议装 gitleaks）"
  if [ ${#FILES[@]} -gt 0 ]; then
    # 排除示例/测试文件
    FILTERED_FILES=()
    for f in "${FILES[@]}"; do
      case "$f" in
        *.example|*.sample|tests/*|*test*.go|*_test.py|*_test.java|*.md|README*) continue ;;
      esac
      FILTERED_FILES+=("$f")
    done

    if [ ${#FILTERED_FILES[@]} -gt 0 ]; then
      COMBINED_PATTERN=$(printf '%s|' "${PATTERNS[@]}")
      COMBINED_PATTERN="${COMBINED_PATTERN%|}"
      HITS=$(grep -nE "$COMBINED_PATTERN" "${FILTERED_FILES[@]}" 2>/dev/null | head -20 || true)
      if [ -n "$HITS" ]; then
        fail "发现疑似敏感信息（regex 兜底匹配）："
        echo "$HITS" >&2
        SECRET_FOUND=1
      fi
    fi
  fi
fi

# ---------- 步骤 3：扫 git 历史 ----------
if [ "$SCAN_MODE" = "all" ] && git rev-parse --git-dir >/dev/null 2>&1; then
  info "步骤 3/3：扫 git 历史..."
  if [ "$HAS_GITLEAKS" = "true" ]; then
    if gitleaks detect --source . --no-banner --log-opts="--all" 2>&1 | tail -10; then
      :
    else
      SECRET_FOUND=1
    fi
  else
    warn "跳过（需要 gitleaks 才能扫 git 历史）"
  fi
fi

# ---------- 总结 ----------
echo
if [ "${IGNORE_FAIL:-0}" = "1" ] || [ "$SECRET_FOUND" = "1" ]; then
  fail "检查未通过：发现敏感信息风险"
  echo
  echo "处理建议："
  echo "  1. 把真实值移到 .env（确保 .gitignore 覆盖 + chmod 600）"
  echo "  2. 用占位符（your-key-here / <YOUR_KEY>）替换代码/doc 里的真实值"
  echo "  3. 如已泄漏：去控制台吊销 + 重新生成 key（不要只删文件）"
  exit 1
else
  ok "检查通过：未发现敏感信息泄漏"
  exit 0
fi
