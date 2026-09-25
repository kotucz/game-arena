FROM eclipse-temurin:21-jre-alpine

WORKDIR /opt/gamearena

# Copy pre-compiled server distribution and WASM production build
# (paths relative to workspace root after running Gradle in GitHub Actions)
COPY server/build/install/server /opt/gamearena/server
COPY app/webApp/build/dist/wasmJs/productionExecutable /opt/gamearena/web

ENV GAMEARENA_CONFIG=/config/application.conf

EXPOSE 8080

# Grant execution permissions for the start script
RUN chmod +x /opt/gamearena/server/bin/server

ENTRYPOINT ["/opt/gamearena/server/bin/server"]