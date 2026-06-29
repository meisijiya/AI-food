package com.ai.food.benchmark;

import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * Bloom filter 误判率 (False Positive Rate) 实测 benchmark。
 *
 * <p>简历亮点数据：在 3 个规模 (1K / 10K / 100K) 下，向同一个
 * expectedInsertions=100_000、fpp=0.01 配置的 RBloomFilter 插入
 * N 个 unique ID，再查询 100K 个明确不在集合中的 ID，统计误判率。
 *
 * <p>运行前置：本地 Redis (127.0.0.1:6379) 处于可用状态。
 * 运行方式：IDE 直接跑 main，或 {@code mvn -pl backend
 * test-compile exec:java -Dexec.mainClass=com.ai.food.benchmark.BloomFprBenchmark}。
 */
public class BloomFprBenchmark {

    /** 三个插入规模，覆盖 3 个数量级，便于观察 FPR 随负载因子上移的趋势。 */
    private static final int[] SIZES = {1_000, 10_000, 100_000};

    /** 每次规模下查询的"肯定不在集合中"的 ID 数量，越大越能逼近真实 FPR。 */
    private static final long QUERIES_PER_SIZE = 100_000L;

    /** Bloom 容量按 100K 配置，fpp=1%，与项目 BloomFilterService 配置对齐。 */
    private static final long EXPECTED_INSERTIONS = 100_000L;

    private static final double FALSE_PROBABILITY = 0.01;

    public static void main(String[] args) {
        // 1. 直连本地 Redis，绕开 Spring 容器，保持 main method 可独立运行
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonClient redisson = Redisson.create(config);

        try {
            StringBuilder result = new StringBuilder();
            result.append("{\n");
            result.append("  \"config\": {\"expectedInsertions\": ")
                  .append(EXPECTED_INSERTIONS)
                  .append(", \"fpp\": ")
                  .append(FALSE_PROBABILITY)
                  .append("},\n");
            result.append("  \"results\": [\n");

            for (int i = 0; i < SIZES.length; i++) {
                int size = SIZES[i];

                // 每次按 size 区分 key，避免上一次残留元素污染 FPR 测量
                RBloomFilter<String> filter =
                        redisson.getBloomFilter("benchmark-bloom-" + size);
                filter.delete();                          // 清空旧 bit array
                filter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);

                // 2. 插入 N 个 unique ID（user-0 .. user-(N-1)）
                for (int j = 0; j < size; j++) {
                    filter.add("user-" + j);
                }

                // 3. 查询 N 个明确不在集合中的 ID，统计误判
                long fpCount = 0L;
                for (long j = 0; j < QUERIES_PER_SIZE; j++) {
                    if (filter.contains("never-inserted-" + j)) {
                        fpCount++;
                    }
                }

                // 4. FPR = 误判命中数 / 总负样本查询数
                double fpr = (double) fpCount / QUERIES_PER_SIZE;

                result.append(String.format(
                        "    {\"size\": %d, \"queries\": %d, \"fp_count\": %d, \"fpr\": %.6f}",
                        size, QUERIES_PER_SIZE, fpCount, fpr));
                if (i < SIZES.length - 1) {
                    result.append(",");
                }
                result.append("\n");
            }

            result.append("  ]\n");
            result.append("}\n");

            System.out.println(result);
        } finally {
            // 5. 关闭 client 释放 Netty 连接 / EventLoop 线程
            redisson.shutdown();
        }
    }
}
