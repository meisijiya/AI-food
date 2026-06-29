#!/usr/bin/env python3
"""
WS 并发压测脚本（python asyncio + websockets）

测不同并发数下 WS 连接的：
- 总连接成功数
- 消息发送成功率
- 端到端延迟 P50 / P99
- 总错误数

输出 JSON 报告。

依赖：pip install websockets
"""

import asyncio
import json
import time
import sys
import statistics
from dataclasses import dataclass, asdict

try:
    import websockets
except ImportError:
    print("ERROR: pip install websockets", file=sys.stderr)
    sys.exit(1)


@dataclass
class TestResult:
    target_concurrent: int
    actual_connected: int
    messages_sent: int
    messages_failed: int
    duration_sec: float
    p50_latency_ms: float
    p99_latency_ms: float
    errors: list


async def test_concurrent(ws_url: str, token: str, concurrent: int,
                          messages_per_conn: int, conn_timeout: float = 5.0):
    """单次并发测试：建立 N 个连接，每连接发 M 条消息。"""
    latencies = []
    errors = []
    connected_count = 0
    messages_sent = 0
    messages_failed = 0

    async def one_connection(conn_id: int):
        nonlocal connected_count, messages_sent, messages_failed
        try:
            # M1 修复：token 走 Sec-WebSocket-Protocol 子协议
            async with websockets.connect(
                ws_url,
                subprotocols=[f"jwt.{token}"] if token else None,
                ping_interval=None,
                open_timeout=conn_timeout,
            ) as ws:
                connected_count += 1
                for msg_id in range(messages_per_conn):
                    t0 = time.perf_counter()
                    try:
                        await ws.send(json.dumps({
                            "type": "ping",
                            "from": "benchmark",
                            "conn": conn_id,
                            "msg": msg_id,
                            "ts": t0,
                        }))
                        # 等 echo（如果 server 回）— 5s timeout
                        try:
                            await asyncio.wait_for(ws.recv(), timeout=5.0)
                            t1 = time.perf_counter()
                            latencies.append((t1 - t0) * 1000)
                        except asyncio.TimeoutError:
                            # server 没回也算 send 成功（单向）
                            t1 = time.perf_counter()
                            latencies.append((t1 - t0) * 1000)
                        messages_sent += 1
                    except Exception as e:
                        messages_failed += 1
                        errors.append(f"send failed conn={conn_id} msg={msg_id}: {e}")
        except Exception as e:
            errors.append(f"connect failed conn={conn_id}: {type(e).__name__}: {e}")

    start = time.perf_counter()
    # 并发建立所有连接
    tasks = [asyncio.create_task(one_connection(i)) for i in range(concurrent)]
    await asyncio.gather(*tasks, return_exceptions=True)
    duration = time.perf_counter() - start

    # 计算延迟分位数
    p50 = statistics.median(latencies) if latencies else 0
    p99 = (sorted(latencies)[int(len(latencies) * 0.99)] if latencies else 0)

    return TestResult(
        target_concurrent=concurrent,
        actual_connected=connected_count,
        messages_sent=messages_sent,
        messages_failed=messages_failed,
        duration_sec=round(duration, 2),
        p50_latency_ms=round(p50, 2),
        p99_latency_ms=round(p99, 2),
        errors=errors[:5],  # 只记前 5 个错
    )


async def main():
    if len(sys.argv) < 3:
        print("Usage: ws-concurrent-test.py <ws_url> <token> [concurrent_list]")
        print("  ws_url: ws://host:port/ws/conversation/test")
        print("  token: JWT token (M1 子协议用)")
        print("  concurrent_list: 100,500,1000 (default)")
        sys.exit(1)

    ws_url = sys.argv[1]
    token = sys.argv[2] if len(sys.argv) < 4 or sys.argv[3] != "-" else None
    concurrent_list = [100, 500, 1000]
    if len(sys.argv) >= 5:
        concurrent_list = [int(x) for x in sys.argv[4].split(",")]

    messages_per_conn = 5
    results = []
    for n in concurrent_list:
        print(f"Testing {n} concurrent connections ({messages_per_conn} msgs each)...", file=sys.stderr)
        result = await test_concurrent(ws_url, token, n, messages_per_conn)
        results.append(asdict(result))
        print(f"  -> connected {result.actual_connected}/{n}, "
              f"sent {result.messages_sent}, failed {result.messages_failed}, "
              f"p99 {result.p99_latency_ms}ms", file=sys.stderr)

    # 输出 JSON
    report = {
        "ws_url": ws_url,
        "maven_token_used": bool(token),
        "messages_per_connection": messages_per_conn,
        "results": results,
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    asyncio.run(main())
