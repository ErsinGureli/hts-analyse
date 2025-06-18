########################
# 1. Aşama – Build
########################
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Bağımlılık önbelleği için önce pom.xml kopyala
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Kaynak kodu ekle ve derle
COPY src ./src
RUN mvn -B package -DskipTests

########################
# 2. Aşama – Run
########################
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Derlenmiş jar’ı build aşamasından kopyala
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8099

ENTRYPOINT ["java","-jar","app.jar"]
