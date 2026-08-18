# 🎮 TEFManager

TEFManager is an official graphical management tool for TEFKernel, developed based on **Kotlin Multiplatform** + **Compose Multiplatform**. It provides complete visual management and package format specifications for four component types: Plugin, Module, ModLoader, and Mod.

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-purple.svg?logo=kotlin)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-blue.svg)
![AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-red.svg)
![Platforms](https://img.shields.io/badge/Platform-Android%20%7C%20Windows%20%7C%20Linux%20%7C%20macOS%20%7C%20iOS-green.svg)

## [![Telegram Channel](https://img.shields.io/badge/Official_Telegram_Channel-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/TEFModLoader)

## 🏛️ Community Guidelines
1. Use civilized language and maintain a friendly communication environment
2. Do not discuss any politically sensitive topics (comply with Chinese and international laws and regulations)
3. Do not disseminate violent, extremist, or illegal content
4. Technical discussions should remain professional and constructive

---

## 📖 Overview

### What is TEFManager?

TEFManager is the **official graphical management frontend** for the TEFKernel ecosystem. It:

1. **Implements a complete package type system**: Defines standardized package formats for Plugin, Module, ModLoader, and Mod
2. **Provides a visual interface**: Cross-platform UI supporting Android/Windows/Linux/macOS
3. **Encapsulates TEFKernel directory structure**: Automatically manages `plugin/`, `module/`, `modloader/`, and `mods/` directories
4. **Supports dependency management**: Automatically resolves and installs inline dependencies
5. **Fills the Android ecosystem gap**: Built-in language pack, texture pack, and font pack modules, allowing Android players to enjoy a complete modding experience
6. **Dual-mode operation**: Android supports Root mode and External mode to accommodate different user needs

---

## 🌍 Platform and Language Support

### Supported Platforms

| Platform       | Architectures             | Status | Description                        |
|:---------------|:--------------------------|:-------|:-----------------------------------|
| **🤖 Android** | ARM64, ARM32, x86, x86_64 | ✅ Full | Supports Root mode + External mode |
| **🪟 Windows** | x64                       | ✅ Full | Via JAR(Java21)                    |
| **🐧 Linux**   | x64                       | ✅ Full | Via JAR(Java21)                    |
| **🍎 macOS**   | x64                       | ✅ Full | Via JAR(Java21)                    |

> **Architecture Note**: The Android platform supports four mainstream architectures (ARM64/ARM32/x86/x86_64), ensuring compatibility across various Android devices.

### Supported Languages

| Language               | Code    | Status | Maintainer |
|:-----------------------|:--------|:-------|:-----------|
| **Simplified Chinese** | `zh-CN` | ✅ Full | Official   |
| **English**            | `en-US` | ✅ Full | Official   |
| **Русский**            | `ru-RU` | ✅ Full | Official   |

---

## Package Type Comparison

| Type          | Directory Location      | File Extension | Manager            | Purpose                    |
|:--------------|:------------------------|:---------------|:-------------------|:---------------------------|
| **Plugin**    | `plugin/pkg/`           | `.tefpkg`      | `PluginManager`    | Register low-level symbols |
| **Module**    | `module/pkg/`           | `.tefpkg`      | `ModuleManager`    | Game feature extension     |
| **ModLoader** | `modloader/pkg/`        | `.tefpkg`      | `ModLoaderManager` | Mod loading container      |
| **Mod**       | `mods/{loader_id}/mod/` | `.tefpkg`      | `ModManager`       | End-user mod               |

---

## 📦 Package Format Specification

### Common Package Structure

All TEFManager packages are `.zip` files containing the following standard files:

```
package.zip
├── Manifest.json          # Package manifest (required)
├── Info.json              # Component metadata (required)
├── {file}                 # Main file (dynamic library/package file)
├── icon.png               # Icon file (optional)
└── resources/             # Resource directory (optional)
└── ...
```

### Manifest.json Specification

`Manifest.json` is the **entry description file** of the package. TEFManager uses it to identify the package type and installation method.

```json
{
    "type": "plugin | module | modloader | mod",
    "file": "libplugin.android.arm64.so",
    "resources": "resources/",
    "iconFile": "icon.png",
    "parentLoader": "eternal.future.kernelloader",
    "modloader": {
        "type": "inline",
        "file": "modloader_inline.zip"
    },
    "plugins": {
        "type": "inline",
        "files": [
            "plugin1_inline.zip",
            "plugin2_inline.zip"
        ]
    }
}
```

**Field Descriptions:**

| Field          | Type   | Required    | Description                                             |
|:---------------|:-------|:------------|:--------------------------------------------------------|
| `type`         | string | ✅           | Package type: `plugin` / `module` / `modloader` / `mod` |
| `file`         | string | ✅           | Main file path (relative to ZIP root)                   |
| `resources`    | string | ❌           | Resource directory path                                 |
| `iconFile`     | string | ❌           | Icon file path                                          |
| `parentLoader` | string | ⚠️ Mod only | The `pkgId` of the ModLoader this Mod belongs to        |
| `modloader`    | object | ❌           | Inline ModLoader dependency                             |
| `plugins`      | object | ❌           | Inline Plugin dependencies                              |

---

### Info.json Specification

`Info.json` contains component metadata, used for display and version management in TEFManager.

#### Plugin Info.json

```json
{
    "pkgId": "com.example.myplugin",
    "name": "My Plugin",
    "author": "TEFKernel Team",
    "description": "A sample plugin",
    "version": "1.0.0",
    "versionCode": 1
}
```

#### Module Info.json

```json
{
    "pkgId": "com.example.mymodule",
    "name": "My Module",
    "author": "TEFKernel Team",
    "description": "A sample module",
    "brieflyDescribe": "Short description",
    "version": "1.0.0",
    "versionCode": 1,
    "detailsURL": "https://example.com",
    "dependence": [
        {
            "pkgId": "com.example.dependency",
            "minVersionCode": 1,
            "maxVersionCode": 10
        }
    ],
    "support": {
        "android": { "arm64": true, "arm": true },
        "windows": { "x64": true, "x86": true },
        "linux": { "x64": true },
        "mac": { "arm64": true, "x64": true },
        "ios": { "arm64": true }
    }
}
```

#### ModLoader Info.json

```json
{
    "pkgId": "eternal.future.kernelloader",
    "name": "KernelLoader",
    "author": "TEFKernel Team",
    "brieflyDescribe": "TEFKernel Official ModLoader",
    "description": "A TEFKernel-based ModLoader implementation",
    "version": "1.0.0",
    "versionCode": 1,
    "dependence": [
        {
            "pkgId": "com.example.plugin",
            "minVersionCode": 1
        }
    ],
    "support": {
        "android": { "arm64": true, "arm": true },
        "windows": { "x64": true, "x86": true },
        "linux": { "x64": true, "x86": true },
        "mac": { "arm64": true, "x64": true }
    }
}
```

#### Mod Info.json

```json
{
    "pkgId": "com.example.mymod",
    "name": "My Mod",
    "author": "TEFKernel Team",
    "brieflyDescribe": "Brief description",
    "description": "Detailed description",
    "version": "1.0.0",
    "versionCode": 1,
    "targetGameVersion": "1.4.4",
    "minGameVersion": "1.4.0",
    "maxGameVersion": "1.4.9",
    "features": [
        "NEW_CONTENT",
        "QUALITY_OF_LIFE"
    ],
    "sizeCategory": "MEDIUM",
    "stableVerified": true,
    "experimental": false,
    "deprecated": false,
    "hasExtendedContent": false,
    "detailsURL": "https://example.com",
    "dependence": [
        {
            "pkgId": "com.example.dependency",
            "minVersionCode": 1
        }
    ],
    "conflicts": ["com.example.conflicting_mod"],
    "support": {
        "android": { "arm64": true, "arm": true },
        "windows": { "x64": true }
    }
}
```

**Mod-Specific Fields:**

| Field               | Type   | Description                                           |
|:--------------------|:-------|:------------------------------------------------------|
| `features`          | array  | List of Mod feature tags                              |
| `sizeCategory`      | string | Size category: `TINY`/`SMALL`/`MEDIUM`/`LARGE`/`HUGE` |
| `targetGameVersion` | string | Target game version                                   |
| `minGameVersion`    | string | Minimum supported game version                        |
| `maxGameVersion`    | string | Maximum supported game version                        |
| `conflicts`         | array  | List of conflicting Mod IDs                           |
| `stableVerified`    | bool   | Whether stability has been verified                   |
| `experimental`      | bool   | Whether this is an experimental version               |
| `deprecated`        | bool   | Whether this is deprecated                            |

**Mod Feature Enum Values:**

| Value                  | Description                             |
|:-----------------------|:----------------------------------------|
| `ASSISTANCE`           | Assistance (minimap, coordinates, etc.) |
| `QUALITY_OF_LIFE`      | Quality of life improvements            |
| `UTILITY`              | Utilities                               |
| `ACCESSIBILITY`        | Accessibility features                  |
| `VISUAL_ENHANCEMENT`   | Visual enhancements                     |
| `UI_IMPROVEMENT`       | UI improvements                         |
| `NEW_CONTENT`          | New content additions                   |
| `CONTENT_EXPANSION`    | Content expansions                      |
| `OVERHAUL`             | Complete overhaul                       |
| `MECHANICS_CHANGE`     | Mechanics changes                       |
| `DIFFICULTY_MOD`       | Difficulty adjustments                  |
| `PROGRESSION_SYSTEM`   | Progression system changes              |
| `ADVENTURE`            | Adventure/exploration                   |
| `BUILDING`             | Building/construction                   |
| `TECHNOLOGY`           | Technology/automation                   |
| `MAGIC`                | Magic/mystical                          |
| `FANTASY`              | Fantasy/magical                         |
| `SCI_FI`               | Sci-fi/futuristic                       |
| `REALISM`              | Realism/realistic                       |
| `DECORATION`           | Decoration/aesthetics                   |
| `MULTIPLAYER_ENHANCED` | Multiplayer enhancements                |
| `SERVER_MANAGEMENT`    | Server management                       |
| `COMMUNICATION`        | Communication/chat                      |

---

### PlatformSupport Specification

The `support` field declares component platform compatibility:

```json
{
    "support": {
        "android": { "arm64": true, "arm": true },
        "windows": { "x64": true, "x86": true },
        "linux": { "x64": true, "x86": true },
        "mac": { "arm64": true, "x64": true },
        "ios": { "arm64": true }
    }
}
```

**Architecture Support:**

| Platform | Architecture Fields | Description           |
|:---------|:--------------------|:----------------------|
| Android  | `arm64`, `arm`      | ARM64 / ARM32         |
| Windows  | `x64`, `x86`        | 64-bit / 32-bit       |
| Linux    | `x64`, `x86`        | 64-bit / 32-bit       |
| macOS    | `arm64`, `x64`      | Apple Silicon / Intel |
| iOS      | `arm64`             | ARM64 only            |

---

## 📁 Directory Structure

TEFManager creates independent directories for each package type on top of the TEFKernel working directory:

```
Working Directory/
├── plugin/                      # Plugin directory
│   ├── enables.txt              # List of enabled Plugins
│   ├── pkg/                     # Plugin package files
│   │   └── com.example.plugin.tefpkg
│   ├── icons/                   # Plugin icons
│   │   └── com.example.plugin.icon
│   └── private/                 # Plugin private data
│       └── com.example.plugin/
│
├── module/                      # Module directory
│   ├── enables.txt              # List of enabled Modules
│   ├── pkg/                     # Module package files
│   │   └── com.example.module.tefpkg
│   ├── icons/                   # Module icons
│   │   └── com.example.module.icon
│   └── private/                 # Module private data
│       └── com.example.module/
│
├── modloader/                   # ModLoader directory
│   ├── enables.txt              # List of enabled ModLoaders
│   ├── pkg/                     # ModLoader package files
│   │   └── eternal.future.kernelloader.tefpkg
│   ├── icons/                   # ModLoader icons
│   │   └── eternal.future.kernelloader.icon
│   └── private/                 # ModLoader private data
│       └── eternal.future.kernelloader/
│
└── mods/                        # Mod directory
    └── eternal.future.kernelloader/   # Isolated by ModLoader
        ├── enables.txt          # List of Mods enabled by this ModLoader
        ├── db/                  # Mod database
        ├── icons/               # Mod icons
        │   └── com.example.mymod.icon
        ├── mod/                 # Mod package files
        │   └── com.example.mymod.tefpkg
        ├── private/             # Mod private data
        │   └── com.example.mymod/
        └── resources/           # Mod resources
            └── com.example.mymod/
```

---

## 🎨 Built-in Extension Modules

TEFManager includes three core extension modules that **fill the gap in the Android modding ecosystem**, allowing Android players to enjoy the same modding experience as desktop users:

### 🗣️ Language Pack Module (LanguagePack-Extension)

Provides multi-language support for games, allowing players to switch interface languages without modifying the game itself.

- **Supported Formats**: `.lang` / `.json` / `.po`
- **Features**: Game interface text replacement, font adaptation, RTL language support
- **Supported Platforms**: All platforms (Android / Windows / Linux / macOS)

> 📦 **Repository**: [TEFManager-LanguagePack-Extension](https://github.com/eternalfuture-e38299/TEFManager-LanguagePack-Extension)

---

### 🎨 Texture Pack Module (TexturePack-Extension)

Allows Android players to freely replace game textures just like on desktop, enjoying HD textures and personalized visuals.

- **Supported Formats**: `pack.json` (Terraria standard) / `Settings.json` (TLPro) / `pack_info.json` (TEFManager)
- **Features**: Texture replacement, priority ordering, dynamic switching
- **Supported Platforms**: Android

> 📦 **Repository**: [TEFManager-TexturePack-Extension](https://github.com/eternalfuture-e38299/TEFManager-TexturePack-Extension)

---

### 🔤 Font Pack Module (FontPack-Extension)

Supports loading custom fonts to solve font display issues across different language regions and improve reading experience.

- **Supported Formats**: `.ttf` / `.otf` / `.woff2`
- **Features**: Font replacement, font size adjustment, multi-font mixing
- **Supported Platforms**: Android
- **⭐ Special Significance**: Provides better display for non-Latin script users (Chinese, Japanese, Korean, etc.)

> 📦 **Repository**: [TEFManager-FontPack-Extension](https://github.com/eternalfuture-e38299/TEFManager-FontPack-Extension)

---

## 🤖 Android Dual-Mode Operation

TEFManager supports two operation modes on the Android platform to accommodate different devices and user permission scenarios:

### Root Mode

**Principle**: Injects TEFKernel into the game process via the Xposed framework (LSPosed, etc.).

**Requirements**:
- Device is rooted
- Xposed framework installed (LSPosed recommended)
- Activate the TEFManager module in the Xposed manager and add Terraria to the scope
- Grant Root permissions to TEFManager

**Advantages**:
- No need to modify the game APK
- Supports official game version updates
- Fully compatible with official versions

**Configuration Suggestion**: If you encounter crashes in the Xposed framework, enable the "Restore Inline Hooks" option for Terraria.

---

### External Mode

**Principle**: Automatically patches the game APK through TEFManager, injects TEFKernel, and repackages it for installation.

**Requirements**:
- No Root required
- Game APK file (unencrypted or extractable)

**Advantages**:
- No Root permission required
- Broader applicability
- One-click operation, simple and easy to use

**Important Notes**:
- The patched APK requires TEFManager to remain running in the background
- If crashes occur, try using a different game installation package and repatch

---

## 📦 Packaging Guide

For TEFManager package packaging documentation, please refer to TEFKernel.

---

## 🔗 Related Links

| Project                    | Link                                                                                           | Description                    |
|:---------------------------|:-----------------------------------------------------------------------------------------------|:-------------------------------|
| **TEFKernel**              | [GitHub](https://github.com/eternalfuture-e38299/tefkernel)                                    | Cross-platform runtime kernel  |
| **TEFPkg-Tool**            | [GitHub](https://github.com/eternalfuture-e38299/TEFPkg-Tool)                                  | Package format packaging tool  |
| **Texture Pack Module**    | [GitHub](https://github.com/eternalfuture-e38299/TEFManager-TexturePack-Extension)             | Texture pack loading           |
| **Language Pack Module**   | [GitHub](https://github.com/eternalfuture-e38299/TEFManager-LanguagePack-Extension)            | Cross-platform language pack   |
| **Font Pack Module**       | [GitHub](https://github.com/eternalfuture-e38299/TEFManager-FontPack-Extension)                | Font pack loading              |

---

*TEFManager - Making the TEFKernel ecosystem accessible to all!* 🚀🎮✨