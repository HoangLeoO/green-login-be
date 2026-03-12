# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
# Sửa lỗi line ending (CRLF -> LF) của Windows và cấp quyền thực thi
RUN apk add --no-cache dos2unix && \
    dos2unix gradlew && \
    chmod +x gradlew
RUN ./gradlew build -x test

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
