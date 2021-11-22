FROM debian:bullseye@sha256:e8c184b56a94db0947a9d51ec68f42ef5584442f20547fa3bd8cbd00203b2e7a
COPY bullseye_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh
VOLUME /ga
ENV JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64
ENV ANDROID_HOME=/opt
CMD cd /ga && ./prepare_fdroid.sh && ./gradlew assembleRelease
