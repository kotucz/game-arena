FROM eclipse-temurin:21-jdk AS build

WORKDIR /src
COPY . .

# Kotlin/Wasm's downloaded Node.js binary requires libatomic on Linux.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libatomic1 \
    && rm -rf /var/lib/apt/lists/*

# Make the Gradle wrapper executable and convert CRLF line endings from Windows checkouts.
RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew

RUN ./gradlew --no-daemon :app:webApp:wasmJsBrowserDistribution :server:installDist

FROM eclipse-temurin:21-jre

WORKDIR /opt/gamearena
COPY --from=build /src/server/build/install/server /opt/gamearena/server
COPY --from=build /src/app/webApp/build/dist/wasmJs/productionExecutable /opt/gamearena/web

ENV WEB_ROOT=/opt/gamearena/web
ENV PORT=8080
ENV DATABASE_PATH=/data/gamearena.db
VOLUME ["/data"]
EXPOSE 8080

ENTRYPOINT ["/opt/gamearena/server/bin/server"]
