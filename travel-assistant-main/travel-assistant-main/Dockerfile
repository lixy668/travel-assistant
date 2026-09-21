# ================= 构建阶段 =================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先拷 pom 拉依赖，利用 Docker 层缓存，改代码时不用重新下依赖
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# ================= 运行阶段 =================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ENV TZ=Asia/Shanghai

COPY --from=build /build/target/*.jar app.jar

EXPOSE 3200
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
