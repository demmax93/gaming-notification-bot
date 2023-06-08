FROM openjdk:17-alpine

RUN mkdir /app
WORKDIR /app

COPY cache/maven/build/libs/tmp-1.0.0.jar /app/app.jar

ENTRYPOINT [ "java", "-jar", "/app/app.jar" ]
