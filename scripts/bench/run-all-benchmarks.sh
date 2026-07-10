#!/bin/bash
# scripts/bench/run-all-benchmarks.sh
# 一键跑完所有量化数据 — 需 backend image 已 build + master 已 commit 09684ce
# 输出 JSON 报告到 /tmp/aifood-bench-result.json

set -e

# 配置
BACKEND_IMAGE="ai-food-backend"
WS_URL="ws://localhost:8080/ws/conversation/test"
BENCH_USER="bench_$(date +%s)"
BENCH_PASS="BenchPass123!"
BENCH_EMAIL="${BENCH_USER}@bench.com"

# 初始化结果 JSON
RESULT_FILE="/tmp/aifood-bench-result.json"
echo '{"timestamp": "'$(date -Iseconds)'", "results": [' > $RESULT_FILE
FIRST=1

add_result() {
    local name="$1"
    local data="$2"
    if [ $FIRST -eq 0 ]; then echo "," >> $RESULT_FILE; fi
    FIRST=0
    echo -n "  {\"name\": \"$name\", \"data\": $data}" >> $RESULT_FILE
}

# === 1. 镜像大小 ===
echo "===[1/5] 镜像大小==="
IMAGE_SIZE=$(sudo -E docker images $BACKEND_IMAGE --format "{{.Size}}" | head -1)
echo "Backend image size: $IMAGE_SIZE"
add_result "image_size" "{\"size\": \"$IMAGE_SIZE\"}"

# === 2. 冷启动时间 ===
echo "===[2/5] 冷启动时间==="
# 先停 backend
sudo -E docker compose stop backend >/dev/null 2>&1 || true
sleep 2

START=$(date +%s%N)
sudo -E docker compose up -d backend >/dev/null 2>&1
while true; do
    STATUS=$(sudo -E docker inspect --format='{{.State.Health.Status}}' aifood-backend 2>/dev/null || echo "starting")
    if [ "$STATUS" = "healthy" ]; then
        END=$(date +%s%N)
        COLD_START_MS=$(( (END - START) / 1000000 ))
        echo "Cold start: ${COLD_START_MS}ms"
        add_result "cold_start" "{\"duration_ms\": $COLD_START_MS}"
        break
    fi
    if [ "$STATUS" = "unhealthy" ]; then
        echo "Backend unhealthy, see logs:"
        sudo -E docker compose logs --tail=20 backend
        exit 1
    fi
    sleep 1
done

# === 3. Bloom 误判率 ===
echo "===[3/5] Bloom 误判率==="
# 在 host 端用 maven 跑 benchmark（连 aifood-redis）
BLOOM_RESULT=$(cd /home/ubuntu/projects/AI-food && sudo -E docker run --rm \
    -m 1g --memory-swap 1g \
    -v /home/ubuntu/projects/AI-food:/workspace \
    -w /workspace/backend \
    -e MAVEN_OPTS="-Xmx256m" \
    --network aifood_aifood-net \
    maven:3.9-eclipse-temurin-21 \
    mvn -B -ntp test-compile -q 2>&1 | tail -5)
echo "Bloom test-compile: $BLOOM_RESULT" | head -3

# 跑 benchmark（用 exec:java 目标，绕开 Spring Boot PropertiesLauncher）
BLOOM_OUTPUT=$(cd /home/ubuntu/projects/AI-food && sudo -E docker run --rm \
    -m 1g --memory-swap 1g \
    -v /home/ubuntu/projects/AI-food:/workspace \
    -w /workspace/backend \
    -e MAVEN_OPTS="-Xmx256m" \
    --network aifood_aifood-net \
    maven:3.9-eclipse-temurin-21 \
    mvn -B -ntp exec:java -Dexec.mainClass=com.ai.food.benchmark.BloomFprBenchmark -Dexec.classpathScope=test -q 2>&1 | tail -30)
echo "Bloom benchmark output:"
echo "$BLOOM_OUTPUT"

# 提取 JSON（最后一行）
BLOOM_JSON=$(echo "$BLOOM_OUTPUT" | grep -E "^\{$" -A 1000 | python3 -c "
import sys, json
text = sys.stdin.read()
# 找第一个 { 开始的 JSON
start = text.find('{\n  \"config\"')
if start == -1:
    print('{}')
else:
    end = text.find('}\n}', start) + 3
    print(text[start:end] if end > 0 else '{}')
" 2>/dev/null || echo "{}")

if [ -z "$BLOOM_JSON" ] || [ "$BLOOM_JSON" = "{}" ]; then
    BLOOM_JSON='{"error": "Bloom benchmark failed", "raw": "'$(echo "$BLOOM_OUTPUT" | tr '\n' ' ' | head -c 500)'"}'
fi
add_result "bloom_fpr" "$BLOOM_JSON"

# === 4. AI 限流测试 ===
echo "===[4/5] AI 接口限流（M6）==="
REGISTER_RESP=$(curl -sS -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$BENCH_USER\",\"password\":\"$BENCH_PASS\",\"email\":\"$BENCH_EMAIL\"}")
echo "register: $REGISTER_RESP"

LOGIN_RESP=$(curl -sS -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$BENCH_USER\",\"password\":\"$BENCH_PASS\"}")
TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null || echo "")
echo "login token: ${TOKEN:0:20}..."

if [ -z "$TOKEN" ]; then
    echo "Login failed, skip rate limit test"
    add_result "ai_rate_limit" '{"error": "login failed"}'
else
    # 11 次 /api/ai/chat 看 status (AiController.java 已删，该 endpoint 不存在，暂时注释)
    # RATE_RESULTS=""
    # for i in {1..11}; do
    #     HTTP=$(curl -sS -o /dev/null -w "%{http_code}" \
    #         -X POST "http://localhost:8080/api/ai/chat?systemPrompt=test&message=test" \
    #         -H "Authorization: Bearer $TOKEN")
    #     echo "  Request $i: HTTP $HTTP"
    #     RATE_RESULTS="$RATE_RESULTS $HTTP"
    # done
    # # 解析：前 10 次 200/401，第 11 次 429
    # RATE_JSON=$(python3 -c "
# statuses = '$RATE_RESULTS'.strip().split()
# limit_triggered = '429' in statuses
# trigger_at = statuses.index('429') + 1 if limit_triggered else -1
# print(json.dumps({'requests': statuses, 'limit_triggered': limit_triggered, 'trigger_at_request': trigger_at}))
# ")
    # add_result "ai_rate_limit" "$RATE_JSON"
    add_result "ai_rate_limit" '{"skipped": "AiController removed, /api/ai/chat not available"}'
fi

# === 5. WS 并发压测 ===
echo "===[5/5] WS 并发压测==="
if [ -n "$TOKEN" ]; then
    WS_RESULT=$(python3 scripts/bench/ws-concurrent-test.py "$WS_URL" "$TOKEN" 100,500,1000 2>/dev/null || echo '{"error": "WS benchmark failed"}')
    add_result "ws_concurrent" "$WS_RESULT"
else
    add_result "ws_concurrent" '{"error": "no token for WS test"}'
fi

# 结束
echo "" >> $RESULT_FILE
echo "]}" >> $RESULT_FILE

echo ""
echo "===Benchmark 完成==="
echo "结果：$RESULT_FILE"
cat $RESULT_FILE
