FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew && \
    ./gradlew :benchmark-s1:smart-factory-service:shadowJar --no-daemon


FROM eclipse-temurin:25-jre

WORKDIR /service

COPY --from=build \
    /app/benchmark-s1/smart-factory-service/build/libs/backend.jar \
    /service/backend.jar

COPY benchmark-s1/smart-factory-service/models ./models
COPY benchmark-s1/smart-factory-service/imgs ./imgs

EXPOSE 6000

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow", "-jar", "/service/backend.jar"]