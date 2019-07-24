FROM debian:buster@sha256:903779f30a7ee46937bfb21406f125d5fdace4178074e1cc71c49039ebf7f48f
COPY buster_deps.sh /deps.sh
RUN /deps.sh && rm /deps.sh
VOLUME /ga
ENV JAVA_HOME=/usr/lib/jvm/adoptopenjdk-8-hotspot-amd64
ENV ANDROID_HOME=/opt
CMD cd /ga/app && ./prepare_fdroid.sh && cd /ga && ./gradlew --project-dir=bitcoinj/tools build && ./gradlew assembleRelease
