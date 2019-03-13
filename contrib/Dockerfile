FROM debian:stretch@sha256:72e996751fe42b2a0c1e6355730dc2751ccda50564fec929f76804a6365ef5ef

COPY stretch_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh
VOLUME /gb
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
ENV ANDROID_HOME=/opt
CMD cd /gb/app && ./prepare_fdroid.sh && cd /gb && ./gradlew --project-dir=bitcoinj/tools build && ./gradlew assembleRelease
