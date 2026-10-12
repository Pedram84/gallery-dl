# gallery-dl for Android

A native Android application embedding and executing the complete, official **gallery-dl (v1.32.16)** image and manga downloading engine.

## Overview

This project integrates the upstream [gallery-dl](https://github.com/mikf/gallery-dl) repository into a modern Android application built with Jetpack Compose, Material 3, and an embedded Chaquopy Python 3.11 runtime.

The native Python engine executes directly on Android, preserving all 500+ site extractors, URL processing logic, options parser, and authentication handlers.

## Architecture

1. **Embedded Python Runtime (Chaquopy 17.0.0)**:
   - Hosts CPython 3.11 targeting `arm64-v8a` and `x86_64` ABIs.
   - Bundles required networking dependencies (`requests>=2.11.0`, `urllib3`, `certifi`, `idna`, `charset-normalizer`).
   - Executes the original `gallery_dl.main()` entry point with structured argument passing.
   - Real-time stdout and stderr stream redirection to Android UI.

2. **Kotlin & Jetpack Compose UI**:
   - **Downloader**: Input target URLs, toggle standard flags (`-g`, `-j`, `--write-metadata`, `/O`, `-v`), add custom CLI flags, and monitor live streaming console output.
   - **Downloads**: Review past and ongoing download jobs with real-time status badges, exit codes, file counts, logs, and rerun capabilities.
   - **Sites & Extractors**: Searchable catalog of 500+ supported extractors directly queried from the engine's extractor registry, with example URLs.
   - **Config & Cookies**: In-app JSON editor for `config.json` with syntax validation and templates (Default, Manga/Chapters, Proxy/Anonymity), plus Netscape `cookies.txt` importer.
   - **Interactive Terminal**: Direct CLI execution shell for running commands like `gallery-dl --version`, `gallery-dl --help`, `gallery-dl --list-modules`, and custom flags.

3. **Android Platform Integration**:
   - Edge-to-edge support with safe drawing insets.
   - Share Intent integration: share any URL from mobile browsers (Chrome, Firefox, etc.) directly into gallery-dl.
   - Foreground Service support (`DownloadService`) for uninterrupted downloads.
   - Custom adaptive launcher icons across all screen densities.

## Upstream Source Code Integrity

All 606 original Python source files in `gallery_dl/` are preserved 100% unmodified and verified via SHA-256 checksums against the official `v1.32.16` release.
