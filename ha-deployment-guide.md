# Game Arena: Home Assistant Add-on Deployment Guide

This guide details the complete deployment workflow for running **Game Arena** (Kotlin Multiplatform + Ktor + Compose WASM) as a Home Assistant add-on.

The deployment model leverages **GitHub Actions** for heavy container compilation and multi-arch image assembly, publishing directly to **GitHub Container Registry (GHCR)**. Home Assistant runs as a lightweight client pulling strictly pre-built images.

---

## 1. Project Directory & Add-on Structure

### Home Assistant SMB / Add-on Directory
To keep the local Home Assistant storage minimal, only the `config.yaml` file needs to reside on the HA host inside `/addons/gamearena/`:

```
/addons/gamearena/
└── config.yaml
```

### `config.yaml` Configuration

```yaml
name: "Game Arena"
slug: "gamearena"
version: "v1.1.1"
description: "Game Arena - Kotlin KMP backend + Compose Wasm frontend"
arch:
  - amd64
  - aarch64
image: "ghcr.io/kotucz/game-arena"
ports:
  8080/tcp: 6433
map:
  - app_config:rw
init: false
```

> **Important:** The `image:` property points directly to your GHCR repository path. Home Assistant automatically appends `:{version}` (e.g., `:v1.1.1`) to locate the exact image tag.

---

## 2. GitHub Actions CI/CD Workflow

Create or update `.github/workflows/deploy.yml` in your repository. This workflow triggers on Git tags starting with `v*` or manually via `workflow_dispatch`.

```yaml
name: Build and Publish Docker Image

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:
    inputs:
      tag_name:
        description: 'Version tag to publish (e.g. v1.1.1)'
        required: false
        default: 'latest'

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Set up QEMU for multi-arch builds
        uses: docker/setup-qemu-action@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Determine Version Tag
        id: vars
        run: |
          if [ "${{ github.event_name }}" = "workflow_dispatch" ]; then
            echo "TAG=${{ github.event.inputs.tag_name }}" >> $GITHUB_OUTPUT
          else
            echo "TAG=${{ github.ref_name }}" >> $GITHUB_OUTPUT
          fi

      - name: Build and Push Container Image
        uses: docker/build-push-action@v5
        with:
          context: .
          platforms: linux/amd64,linux/arm64
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ steps.vars.outputs.TAG }}
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
```

---

## 3. Pre-Deployment Setup Requirements

### 1. `.dockerignore` Verification
Ensure `.dockerignore` in your project root excludes developer environments without blocking CI Docker context resolution:

```
# Git & IDE Caches
.git
.gitignore
.idea/
*.iml
.gradle/

# Allow CI build context output while ignoring local workspace clutter
**/build/
!**/build/distributions/
```

### 2. GHCR Package Visibility
1. Navigate to your GitHub Profile $\rightarrow$ **Packages** $\rightarrow$ **`game-arena`**.
2. Click **Package Settings** in the right-hand panel.
3. Set visibility to **Public** so Home Assistant Supervisor can pull images without requiring private credentials.

---

## 4. Release & Deployment Workflow

Whenever you are ready to publish a new version (e.g., `v1.1.1`):

### Step 1: Create and Push Git Tag
Tag the release in your Git repository and push it to GitHub:

```bash
git tag v1.1.1
git push origin v1.1.1
```

*(Alternatively, run the workflow manually via GitHub Web UI under **Actions** $\rightarrow$ **Build and Publish** $\rightarrow$ **Run workflow** specifying `v1.1.1`).*

### Step 2: Verify GHCR Build
Monitor the run in GitHub Actions. Once complete, confirm that `ghcr.io/kotucz/game-arena:v1.1.1` is listed under your repository packages.

### Step 3: Update `config.yaml` in Home Assistant
Update the `version:` field in `/addons/gamearena/config.yaml` on your Home Assistant SMB share:

```yaml
version: "v1.1.1"
```

### Step 4: Refresh Home Assistant Supervisor
1. Open Home Assistant UI $\rightarrow$ **Settings** $\rightarrow$ **Add-ons** $\rightarrow$ **Add-on Store**.
2. Click the **three dots ⋮** in the top-right corner $\rightarrow$ click **Check for updates** / **Reload**.
3. Open the **Game Arena** add-on page and click **Update** / **Rebuild**.

---

## 5. Running Locally via Docker Compose

To run the exact pre-built container locally for testing:

```yaml
# docker-compose.yml
services:
  gamearena:
    image: ghcr.io/kotucz/game-arena:v1.1.1
    ports:
      - "8080:8080"
    volumes:
      - gamearena-data:/data
    restart: unless-stopped

volumes:
  gamearena-data:
```

Execution command:
```bash
docker compose pull && docker compose up -d
```