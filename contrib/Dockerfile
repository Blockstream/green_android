FROM debian:buster@sha256:125ab9ab9718f4dba6c3342407bb1923afce4f6b2a12b3a502d818274db9faf9
COPY buster_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh
VOLUME /ga
ENV JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64
ENV ANDROID_HOME=/opt
CMD cd /ga && ./prepare_fdroid.sh && ./gradlew assembleRelease
