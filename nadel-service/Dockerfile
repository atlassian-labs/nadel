FROM openjdk:9-jre-slim
RUN mkdir /nadel-service
WORKDIR /nadel-service
RUN touch nadel-dsl.txt
ADD build/libs/nadel-service-0.0.1-SNAPSHOT.jar /nadel-service/nadel-service.jar
EXPOSE 8080
CMD java -jar nadel-service.jar
