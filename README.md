# j2html Preview Plugin

An IntelliJ IDEA plugin for previewing j2html components with live rendering.

## Current Status: Phase 4 ✅

- ✅ Basic tool window with static HTML preview (Phase 1)
- ✅ Detect current Java file (Phase 2)
- ✅ Find j2html methods (Phase 3)
- ✅ **Execute and render (Phase 4)** ← NEW!
- ⏳ Preview providers (Phase 5)
- ⏳ Live updates (Phase 6)

## Phase 4 Features

### 🚀 Method Execution via Reflection
- **Execute Static Methods**: Runs static methods with zero parameters using Java reflection
- **Module ClassLoader**: Builds custom classloader with full module dependencies including j2html
- **Runtime Invocation**: Bridges PSI (static analysis) to runtime execution
- **HTML Rendering**: Automatically calls `.render()` on j2html objects to get HTML output

### 💎 Professional UI
- **Success Display**: Green banner with styled output container showing rendered HTML
- **Error Handling**: Clear, helpful error messages for all failure cases
- **Method Info**: Shows which method was executed above the rendered output

### 🛡️ Robust Error Handling
- Non-static methods → Clear error message
- Methods with parameters → "Parameter handling will be added in Phase 5"
- Uncompiled code → "Make sure the project is compiled"
- Null returns, exceptions, and other edge cases handled gracefully

### Phase 4a Scope (Currently Implemented)
- ✅ Static methods with zero parameters
- ❌ Non-static methods (Phase 5+)
- ❌ Methods with parameters (Phase 5)

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
