### Deploy to HA local
**Deprecated: see [ha-deployment-guide.md](ha-deployment-guide.md) 4. Release & Deployment Workflow** 

```
# PowerShell
# Create the zip directly via git archive
git archive -o project.zip HEAD

# Extract it natively in PowerShell
Expand-Archive -Path project.zip -DestinationPath "\\homeassistant.local\addons\gamearena"

# Remove the temporary zip
Remove-Item project.zip
```

config.yaml
https://developers.home-assistant.io/docs/apps/configuration/

# Install

HA -> Settings -> Apps -> Install -> Local -> Game Arena

Settings -> Apps -> Game Arena -> Config:

Network
Configure the network ports that this app uses.
6433(GAME) -> 8080/tcp

Info -> Start

App is now available on homeassistant.local:6433 

Cloudflared -> Configuration -> Additional Hosts

gamearena.kotu.cz -> http://192.168.68.81:6433



Home Assistant App/Add-on Storage Rules (Supervisor 2026+ Specification)

1. STORAGE MAPPING (config.yaml)
   Never rely on the default '/data' directory for user-accessible or debuggable files. While persistent, the automatic '/data' volume is strictly isolated and hidden deep within the host system (/usr/share/hassio/addons/data/...), making it invisible to Samba, File Editor, or standard SSH.
   Instead, use the 'app_config':

   map:
    - type: app_config
      read_only: false

2. BEHAVIOR INSIDE DOCKER (Dockerfile / Application Code)
   When 'app_config' is defined, the Supervisor automatically binds this external host directory inside the running container EXACTLY to the '/config' path.
   Any other directory path created by your app (e.g., /app_config) will remain trapped inside the container's volatile/ephemeral storage layer.
   In your Dockerfile, explicitly point your application environment variables to '/config':

   ENV DATABASE_PATH=/config/gamearena.db

3. FILE LOCATION ON THE HOST (Accessing files via HA UI/Samba/SSH)
   Files written by your application to the container's inner '/config/' folder will instantly materialize on the host filesystem at this root path:
   /app_configs/local_gamearena/gamearena.db
   This folder is fully visible and accessible via Studio Code Server, File Editor, and Samba Share.

4. PROPAGATING STORAGE CHANGES (Deployment Gotcha)
   Home Assistant Supervisor completely ignores 'docker-compose.yml' files. If you change 'map' attributes or volume structures in 'config.yaml', the Supervisor will fail to update the active Docker mount points during a standard restart.
   To force execution of the new volume schema, you MUST:
   A) Trigger 'Reload' in the Add-on Store menu.
   B) Completely 'Uninstall' the add-on to wipe the old anonymous Docker volumes.
   C) Re-install and start the add-on fresh.