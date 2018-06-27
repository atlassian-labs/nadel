#FROM openjdk:9-jdk-slim AS builder
#COPY build.gradle build.gradle
#COPY gradlew gradlew
#COPY gradle gradle
#COPY settings.gradle settings.gradle
#COPY gradle.properties gradle.properties
#COPY nadel-dsl nadel-dsl
#COPY nadel-service nadel-service
#
#RUN /gradlew build
#
#FROM openjdk:9-jre-slim
#COPY --from=builder /nadel-service/build/libs/nadel-service-0.0.1-SNAPSHOT.jar nadel-service.jar
#EXPOSE 8080
#CMD java -jar nadel-service.jar
