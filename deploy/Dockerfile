FROM eclipse-temurin:21

RUN mkdir /opt/app

WORKDIR /opt/app

COPY ./*.jar /opt/app/app.jar
CMD ["java", "-jar", "app.jar"]