# ============================================
# AI-food backend 多阶段 Dockerfile
# Stage 1: build  (Maven 编译)
# Stage 2: runtime (JRE 21 + 最小化镜像)
# ============================================

# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 先复制 pom.xml 利用 Docker 层缓存
COPY backend/pom.xml backend/pom.xml
RUN cd backend && mvn -B -ntp dependency:go-offline

# 复制源码并构建（跳过测试，CI 阶段跑测试）
COPY backend/src backend/src
RUN cd backend && mvn -B -ntp clean package -DskipTests

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 时区 + 中文 locale
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# 创建非 root 用户
RUN groupadd -r aifood && useradd -r -g aifood aifood \
    && mkdir -p /app/uploads /app/logs \
    && chown -R aifood:aifood /app

# 复制构建产物
COPY --from=build --chown=aifood:aifood /workspace/backend/target/*.jar /app/app.jar

USER aifood

EXPOSE 8080

# 健康检查（轻量，跳过 Spring Security 拦截的 /actuator/health）
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}"]
