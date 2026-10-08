FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew && \
    ./gradlew :benchmark-s1:metrics-collector:bootJar --no-daemon


FROM eclipse-temurin:25-jre

WORKDIR /collector

COPY --from=build \
    /app/benchmark-s1/metrics-collector/build/libs/DaprMetricsCollector.jar \
    /collector/DaprMetricsCollector.jar

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow", "-jar", "/collector/DaprMetricsCollector.jar"]