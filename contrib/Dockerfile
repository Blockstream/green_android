FROM debian:stretch

COPY stretch_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh
VOLUME /gb
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
ENV ANDROID_NDK=/opt/android-ndk-r14b
ENV ANDROID_HOME=/opt
CMD cd /gb/app && ./prepare_fdroid.sh && cd /gb && ./gradlew --project-dir=bitcoinj/tools build && ./gradlew assembleProductionRelease assembleBtctestnetRelease assemblePublicregtestRelease
