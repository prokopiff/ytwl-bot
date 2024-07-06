ARG ARCH=
FROM ${ARCH}/eclipse-temurin:22

WORKDIR /
ARG JAR_FILE=target/ytbot.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dlogging.file.name=/logs/bot.log", "-jar", "/app.jar"]
