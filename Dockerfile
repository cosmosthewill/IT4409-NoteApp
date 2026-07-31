FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S noteapp && adduser -S noteapp -G noteapp
COPY --from=builder /workspace/target/NoteApp-*.jar /app/app.jar

USER noteapp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
