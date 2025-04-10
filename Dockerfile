FROM eclipse-temurin:21-jre-ubi9-minimal
WORKDIR /home/container

LABEL org.opencontainers.image.source="https://github.com/casterlabs/dit"

# code
COPY ./bot/target/dit.jar /home/container

# entrypoint
CMD [ "java", "-XX:+CrashOnOutOfMemoryError", "-jar", "dit.jar" ]
