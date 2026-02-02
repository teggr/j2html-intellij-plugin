# j2html Preview Plugin

An IntelliJ IDEA plugin for previewing j2html components with live rendering.

## Current Status: Phase 3
- ✅ Basic tool window with static HTML preview (Phase 1)
- ✅ Detect current Java file (Phase 2)
- ✅ Find j2html methods (Phase 3)
- ⏳ Execute and render (Phase 4)
- ⏳ Preview providers (Phase 5)
- ⏳ Live updates (Phase 6)

## Phase 3 Features
- **PSI-Based Method Detection**: Uses IntelliJ's Program Structure Interface to analyze Java code
- **Method Discovery**: Automatically finds methods that return j2html types (ContainerTag, DomContent, Tag, etc.)
- **Method Selector Dropdown**: Interactive dropdown showing all discovered j2html methods with their signatures
- **Method Signature Display**: Shows method name, parameters with types, and return type
- **Context-Aware Messages**: Different HTML previews for different scenarios (methods found, no methods, non-Java files, etc.)
- **File Detection**: Automatically detects the currently open file in the editor
- **Live Updates**: Preview updates automatically when you switch between files
- **Event-Driven Architecture**: Uses IntelliJ's MessageBus system to listen for file changes

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
