FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew && \
    ./gradlew :benchmark-s1:event-publisher:shadowJar --no-daemon


FROM eclipse-temurin:25-jre

WORKDIR /publisher

COPY --from=build \
    /app/benchmark-s1/event-publisher/build/libs/DaprSmartFactoryEventPublisher.jar \
    /publisher/DaprSmartFactoryEventPublisher.jar

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow", "-jar", "/publisher/DaprSmartFactoryEventPublisher.jar"]