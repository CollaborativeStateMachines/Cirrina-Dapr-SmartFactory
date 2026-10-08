FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew && \
    ./gradlew smartFactory:bootJar --no-daemon


FROM eclipse-temurin:25-jre

WORKDIR /smartfactory

COPY --from=build \
    /app/smartFactory/build/libs/SmartFactoryApp.jar \
    /smartfactory/SmartFactoryApp.jar

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow", "-jar", "/smartfactory/SmartFactoryApp.jar"]