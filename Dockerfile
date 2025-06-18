# Java 21 JDK içeren hafif image
FROM eclipse-temurin:21-jdk-alpine

# Çalışma klasörü oluştur
WORKDIR /app

# Maven ile build edilmiş jar dosyasını image'e kopyala
COPY target/*.jar app.jar

# Uygulama hangi portu dinleyecek
EXPOSE 8099

# Spring Boot uygulamasını başlat
ENTRYPOINT ["java", "-jar", "app.jar"]
