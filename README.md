# j2html Preview Plugin

An IntelliJ IDEA plugin for previewing j2html components with live rendering.

## Current Status: Phase 1
- ✅ Basic tool window with static HTML preview
- ⏳ Detect current Java file (Phase 2)
- ⏳ Find j2html methods (Phase 3)
- ⏳ Execute and render (Phase 4)
- ⏳ Preview providers (Phase 5)
- ⏳ Live updates (Phase 6)

## Development

### Prerequisites
- JDK 17 or later
- IntelliJ IDEA (for development)

### Running the Plugin
```bash
./gradlew runIde
```

This will launch a new IntelliJ instance with the plugin installed.

### Building

```bash
./gradlew buildPlugin
```

The distributable plugin will be in `build/distributions/`.

## About j2html

[j2html](https://j2html.com/) is a Java library for generating HTML in a typesafe way.
