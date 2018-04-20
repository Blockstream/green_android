FROM debian:stretch@sha256:c908a4fcb2b2a1953bd40ebc12d9a4116868d72540efc27502ee6c2395b8a1e9

COPY stretch_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh
VOLUME /gb
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
ENV ANDROID_HOME=/opt
CMD cd /gb/app && ./prepare_fdroid.sh && cd /gb && ./gradlew --project-dir=bitcoinj/tools build && ./gradlew assembleProductionRelease assembleBtctestnetRelease assemblePublicregtestRelease
