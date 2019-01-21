FROM debian:stretch@sha256:58a80e0b6aa4d960ee2a5452b0230c406c47ed30a66555ba753c8e1710a434f5

COPY stretch_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh
VOLUME /gb
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
ENV ANDROID_HOME=/opt
CMD cd /gb/app && ./prepare_fdroid.sh && cd /gb && ./gradlew --project-dir=bitcoinj/tools build && ./gradlew assembleRelease
