FROM debian:buster@sha256:2f04d3d33b6027bb74ecc81397abe780649ec89f1a2af18d7022737d0482cefe
COPY buster_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh
VOLUME /ga
ENV JAVA_HOME=/usr/lib/jvm/adoptopenjdk-8-hotspot-amd64
ENV ANDROID_HOME=/opt
CMD cd /ga/app && ./prepare_fdroid.sh && cd /ga && ./gradlew assembleRelease
