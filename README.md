# j2html Preview Plugin

An IntelliJ IDEA plugin for previewing j2html components with live rendering.

## Current Status: Phase 4 (Compile-on-Demand)
- ✅ Basic tool window with static HTML preview (Phase 1)
- ✅ Detect current Java file (Phase 2)
- ✅ Find j2html methods (Phase 3)
- ✅ Execute and render with automatic compilation (Phase 4)
- ⏳ Preview providers (Phase 5)
- ⏳ Live updates (Phase 6)

## Features

### Phase 4: Compile-on-Demand Execution
- Automatically compiles the module before executing methods
- Asynchronous compilation with progress indication
- Thread-safe UI updates using SwingUtilities.invokeLater()
- Graceful error handling for compilation failures
- Works seamlessly with code changes without manual builds

### How It Works
1. Open a Java file with j2html methods
2. Select a method from the dropdown in the j2html Preview tool window
3. Plugin automatically compiles the module
4. Executes the method and displays rendered HTML
5. Compilation errors are shown with detailed error counts

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
