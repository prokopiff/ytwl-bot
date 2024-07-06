# docker buildx build . -t vprokopiv/ytbot --build-arg ARCH=arm32v7 --platform linux/arm/v7
# docker push vprokopiv/ytbot:latest

ARG ARCH=
FROM ${ARCH}/amazoncorretto:22

WORKDIR /
ARG JAR_FILE=target/ytbot.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dlogging.file.name=/logs/bot.log", "-jar", "/app.jar"]
