FROM ubuntu:26.04@sha256:d66b366dac61b575bee2aef07e9c26e8070096fbe84a3d4d3eb0da069b5d76db

# Deps
COPY ubuntu_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh

# Volume
VOLUME /ga

# Env
ENV JAVA_HOME=/usr/lib/jvm/jbr-25
# apksigner runs the java on PATH
ENV PATH=/usr/lib/jvm/jbr-25/bin:${PATH}
ENV ANDROID_HOME=/opt

# Builds use the image's JDK: a missing one fails the build instead of being downloaded through foojay
RUN mkdir -p /root/.gradle && echo "org.gradle.java.installations.auto-download=false" >> /root/.gradle/gradle.properties

# Cache Gradle
COPY gradle /gradle
COPY gradlew /gradlew
RUN ./gradlew --version && rm -rf /gradle*

# Entrypoint
COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
