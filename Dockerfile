# Сборка приложения
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Сначала копируем только pom.xml — зависимости скачаются один раз и закешируются слоем.
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Теперь исходники и финальная сборка.
COPY src ./src
RUN mvn -q clean package -DskipTests

# Запуск приложения
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
