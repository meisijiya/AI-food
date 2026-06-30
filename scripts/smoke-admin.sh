#!/bin/bash
# 管理后台冒烟测试
# 用法：BASE_URL=http://localhost:8081 ADMIN_USER=... ADMIN_PASS=... bash scripts/smoke-admin.sh

set -e
BASE_URL="${BASE_URL:-http://localhost:8081}"
ADMIN_USER="${ADMIN_USER:-smoke@aifood.local}"
ADMIN_PASS="${ADMIN_PASS:-testpass123}"

echo "=== 1. 健康检查 ==="
curl -sf $BASE_URL/admin/api/monitor/health > /dev/null || { echo "FAIL: 健康检查失败"; exit 1; }
echo "✅ 健康检查通过"

echo "=== 2. 登录 ==="
LOGIN_RESP=$(curl -sf -X POST $BASE_URL/admin/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}")
TOKEN=$(echo "$LOGIN_RESP" | /usr/bin/jq -r .data.token)
[ -z "$TOKEN" ] || [ "$TOKEN" = "null" ] && { echo "FAIL: 登录失败"; exit 1; }
echo "✅ 登录成功，token 长度: ${#TOKEN}"

AUTH="Authorization: Bearer $TOKEN"

echo "=== 3. /me ==="
curl -sf $BASE_URL/admin/api/auth/me -H "$AUTH" | /usr/bin/jq -r '.data | "\(.username) role=\(.role)"'

echo "=== 4. Dashboard summary ==="
curl -sf $BASE_URL/admin/api/dashboard/summary -H "$AUTH" | /usr/bin/jq '.data | {userCount, todayNew, systemHealth}'

echo "=== 5. 用户列表 ==="
curl -sf "$BASE_URL/admin/api/users?page=1&size=3" -H "$AUTH" | /usr/bin/jq '.data.total'

echo "=== 6. 对话列表 ==="
curl -sf "$BASE_URL/admin/api/conversations?page=1&size=3" -H "$AUTH" | /usr/bin/jq '.data.total'

echo "=== 7. Token 统计 ==="
curl -sf "$BASE_URL/admin/api/token-usage/stats?groupBy=day" -H "$AUTH" | /usr/bin/jq '.data | length'

echo "=== 8. 模型列表 ==="
curl -sf $BASE_URL/admin/api/models -H "$AUTH" | /usr/bin/jq '.data | length'

echo "=== 9. 审计日志 ==="
curl -sf "$BASE_URL/admin/api/audit-logs?page=1&size=3" -H "$AUTH" | /usr/bin/jq '.data.total'

echo ""
echo "🎉 全部冒烟测试通过！"