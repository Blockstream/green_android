FROM debian:stretch@sha256:f1f61086ea01a72b30c7287adee8c929e569853de03b7c462a8ac75e0d0224c4

COPY stretch_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh
VOLUME /gb
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
ENV ANDROID_HOME=/opt
CMD cd /gb/app && ./prepare_fdroid.sh && cd /gb && ./gradlew --project-dir=bitcoinj/tools build && ./gradlew assembleProductionRelease assembleBtctestnetRelease assemblePublicregtestRelease
