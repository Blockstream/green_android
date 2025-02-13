FROM debian:bullseye@sha256:e8c184b56a94db0947a9d51ec68f42ef5584442f20547fa3bd8cbd00203b2e7a

# Deps
COPY bullseye_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh

# Volume
VOLUME /ga

# Env
ENV JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64
ENV ANDROID_HOME=/opt

# Cache Gradle
COPY gradle /gradle
COPY gradlew /gradlew
RUN ./gradlew --version && rm -rf /gradle*

# Entrypoint
COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
