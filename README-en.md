# 🎮 TEFManager
* [中文](README.md)

TEFManager is the official graphical management tool for TEFKernel, built with **Kotlin Multiplatform** + **Compose Multiplatform**. It provides complete visual management and package format specifications for four component types: Plugin, Module, ModLoader, and Mod.


![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-purple.svg?logo=kotlin)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-blue.svg)
![AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-red.svg)
![Platforms](https://img.shields.io/badge/Platform-Android%20%7C%20Windows%20%7C%20Linux%20%7C%20macOS%20%7C%20iOS-green.svg)

## [![Telegram Channel](https://img.shields.io/badge/Official_Telegram_Channel-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/TEFModLoader)

## 🏛️ Community Guidelines
1. Use civilized language and maintain a friendly communication environment
2. Political sensitive topics are prohibited (comply with Chinese and international laws and regulations)
3. Do not spread violence, extremism, or illegal content
4. Technical discussions should remain professional and constructive

---

## 📖 Overview

### What is TEFManager?

TEFManager is the **official graphical management frontend** for the TEFKernel ecosystem. It:

1. **Implements a complete package type system**: Defines standardized package formats for Plugin, Module, ModLoader, and Mod
2. **Provides a visual interface**: Cross-platform UI supporting Android/Windows/Linux/macOS
3. **Encapsulates the TEFKernel directory structure**: Automatically manages the `plugin/`, `module/`, `modloader/`, and `mods/` directories
4. **Supports dependency management**: Automatically resolves and installs inline dependencies
5. **Fills the gap in the Android ecosystem**: Built-in language pack, texture pack, and font pack modules allow Android players to enjoy a complete modding experience
6. **Dual-mode operation**: Android supports Root mode and External mode to suit different user needs

---

## 🌍 Platform and Language Support

### Supported Platforms

| Platform       | Architecture              | Status         | Notes                              |
|:---------------|:--------------------------|:---------------|:-----------------------------------|
| **🤖 Android** | ARM64, ARM32, x86, x86_64 | ✅ Full support | Supports Root mode + External mode |
| **🪟 Windows** | x64                       | ✅ Full support | Via JAR (Java 21)                  |
| **🐧 Linux**   | x64                       | ✅ Full support | Via JAR (Java 21)                  |
| **🍎 macOS**   | x64                       | ✅ Full support | Via JAR (Java 21)                  |

> **Architecture note**: The Android platform supports four mainstream architectures (ARM64/ARM32/x86/x86_64), ensuring it can run on all kinds of Android devices.

### Supported Languages

| Language               | Code    | Status         | Contributor           |
|:-----------------------|:--------|:---------------|:----------------------|
| **Simplified Chinese** | `zh-CN` | ✅ Full support | Officially maintained |
| **English**            | `en-US` | ✅ Full support | Officially maintained |
| **Русский**            | `ru-RU` | ✅ Full support | Officially maintained |

---

## 🚀 Desktop Quick Start Guide

### ⚠️ Important: JRE Environment Configuration

The desktop version of TEFManager (Windows/Linux/macOS) is distributed as a JAR file and requires a **Java 21 or higher** runtime environment.

Due to certain unavoidable technical reasons, **the JRE packages bundled with some distributions may have compatibility issues**, causing TEFManager to fail to start or run abnormally. **It is strongly recommended** that users obtain a stable and reliable Java runtime from Microsoft's official OpenJDK distribution.

### 📥 Recommended JRE Installation Methods

#### Method 1: Microsoft OpenJDK (Officially Recommended ✅)

Visit the Microsoft OpenJDK official download page:

👉 **[https://learn.microsoft.com/java/openjdk/download](https://learn.microsoft.com/java/openjdk/download)**

**Select the installer for your platform:**

| Platform | Recommended Version | Download File Format |
|:---------|:--------------------|:---------------------|
| Windows  | Java 21             | `.msi` installer     |
| Linux    | Java 21             | `.tar.gz` archive    |
| macOS    | Java 21             | `.tar.gz` archive    |

> ✅ **Why recommend Microsoft OpenJDK?**
> - Officially maintained by Microsoft, with guaranteed quality and stability
> - Validated by a large number of enterprise-grade applications
> - Best compatibility with TEFManager
> - Regular security updates and vulnerability fixes

#### Method 2: Other OpenJDK Distributions

If you cannot access Microsoft's source, you can also use the following alternatives:

| Distribution        | Download URL                     |
|:--------------------|:---------------------------------|
| **Adoptium**        | https://adoptium.net/            |
| **Amazon Corretto** | https://aws.amazon.com/corretto/ |
| **Azul Zulu**       | https://www.azul.com/downloads/  |

> ⚠️ **Note**: When using a non-Microsoft OpenJDK distribution, if you encounter startup issues, please try switching to Microsoft OpenJDK first.

---

### 🖥️ Launching TEFManager from a JAR

#### Windows Users

**Method 1: Command-line launch (recommended)**

1. Open Command Prompt (CMD) or PowerShell
2. Change to the directory containing the TEFManager JAR file
3. Run the launch command:

```cmd
java -jar TEFManager.jar
```

**Method 2: Create a shortcut**

1. Right-click the desktop → New → Shortcut
2. In the location field, enter:
```
java -jar "C:\path\to\TEFManager.jar"
```
3. Replace `C:\path\to\` with the actual JAR file path

**Method 3: Double-click to run (requires correct .jar file association)**

> ⚠️ If double-clicking does not open it, the `.jar` file is not correctly associated with Java. Please use Method 1 or 2.

---

#### Linux Users

**Launch from terminal:**

```bash
# Change to the directory containing the JAR file
cd /path/to/TEFManager/

# Launch TEFManager
java -jar TEFManager.jar
```

**Create a desktop shortcut (optional):**

Create a `TEFManager.desktop` file:

```ini
[Desktop Entry]
Name=TEFManager
Comment=TEFKernel Graphical Management Tool
Exec=java -jar /path/to/TEFManager.jar
Icon=/path/to/icon.png
Terminal=false
Type=Application
Categories=Game;
```

---

#### macOS Users

**Launch from terminal:**

```bash
# Change to the directory containing the JAR file
cd /path/to/TEFManager/

# Launch TEFManager
java -jar TEFManager.jar
```

**Launch using a script (optional):**

Create a `start.sh` script:

```bash
#!/bin/bash
java -jar /path/to/TEFManager.jar
```

Grant execute permission:

```bash
chmod +x start.sh
```

Double-click `start.sh` or run `./start.sh` in the terminal to launch.

---

### 🔧 Common Startup Troubleshooting

| Symptom                           | Possible Cause          | Solution                                                               |
|:----------------------------------|:------------------------|:-----------------------------------------------------------------------|
| `java: command not found`         | Java not installed      | Install Microsoft OpenJDK                                              |
| `UnsupportedClassVersionError`    | Java version too low    | Upgrade to Java 21 or higher                                           |
| Crashes on startup or UI issues   | JRE compatibility issue | Switch to Microsoft OpenJDK                                            |
| Double-clicking .jar does nothing | File association error  | Launch from command line or re-associate `.jar` files with `javaw.exe` |
| Out of memory error               | Insufficient JVM memory | Increase memory: `java -Xmx2048m -jar TEFManager.jar`                  |

**Verify Java installation:**

```bash
java -version
```

The correct output should contain something like `openjdk version "21"` or a higher version number.

---

### 📦 Obtaining the TEFManager JAR File

| Source              | Description                                                                 |
|:--------------------|:----------------------------------------------------------------------------|
| **GitHub Releases** | Official release page; downloading the latest stable version is recommended |

---

## Package Type Comparison

| Type          | Directory Location      | File Extension | Manager            | Purpose                    |
|:--------------|:------------------------|:---------------|:-------------------|:---------------------------|
| **Plugin**    | `plugin/pkg/`           | `.tefpkg`      | `PluginManager`    | Register low-level symbols |
| **Module**    | `module/pkg/`           | `.tefpkg`      | `ModuleManager`    | Game feature extensions    |
| **ModLoader** | `modloader/pkg/`        | `.tefpkg`      | `ModLoaderManager` | Mod loading container      |
| **Mod**       | `mods/{loader_id}/mod/` | `.tefpkg`      | `ModManager`       | End-user mods              |

---

## 📦 Package Format Specification

### General Package Structure

All TEFManager packages are `.zip` files containing the following standard files:

```
package.zip
├── Manifest.json          # Package manifest (required)
├── Info.json              # Component metadata (required)
├── {file}                 # Main file (dynamic library / package file)
├── icon.png               # Icon file (optional)
└── resources/             # Resources directory (optional)
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
    "tefkernelMinVersion": "1.0.0",
    "parentLoader": "eternal.future.kernelloader",
    "parentLoaderMinVersion": "1.0.0",
    "updateInfoUrl": "https://example.com/mymod/update.json",
    "modRepositoryIndexUrl": "https://example.com/kernelloader/repository.json",
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

**Field descriptions:**

| Field                    | Type   | Applicable type    | Required | Description                                                                                      |
|--------------------------|--------|--------------------|----------|--------------------------------------------------------------------------------------------------|
| `type`                   | string | All                | ✅        | Package type: `plugin` / `module` / `modloader` / `mod`                                          |
| `file`                   | string | plugin / modloader | ✅        | Path to the main library or entry file                                                           |
| `resources`              | string | All                | ❌        | Resource directory prefix; extracted entirely to the target directory on installation            |
| `iconFile`               | string | All                | ❌        | Icon path, used for list display                                                                 |
| `tefkernelMinVersion`    | string | All                | ❌        | **Minimum TEFKernel version required by this package**; installation is refused if not satisfied |
| `parentLoader`           | string | mod                | ✅        | pkgId of the owning modloader                                                                    |
| `parentLoaderMinVersion` | string | mod                | ❌        | Minimum parentLoader version required by this mod                                                |
| `updateInfoUrl`          | string | mod / modloader    | ❌        | Update info JSON URL, used to check for new versions                                             |
| `modRepositoryIndexUrl`  | string | modloader          | ❌        | Mod repository index JSON URL, used to browse/install Mods                                       |
| `modloader`              | object | All                | ❌        | Inline modloader dependency                                                                      |
| `plugins`                | object | All                | ❌        | Inline plugin dependencies                                                                       |

> 💡 `updateInfoUrl` and `modRepositoryIndexUrl` are optional fields. If not declared, TEFManager will not perform update checks for that component, nor display the corresponding Mod repository.

---

### Info.json Specification

`Info.json` contains the component's metadata, used for display and version management in TEFManager.

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
    "brieflyDescribe": "Brief description",
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
    "brieflyDescribe": "TEFKernel official ModLoader",
    "description": "A ModLoader implementation based on TEFKernel",
    "version": "1.0.0",
    "versionCode": 1,
    "updateInfoUrl": "https://example.com/kernelloader/update.json",
    "modRepositoryIndexUrl": "https://example.com/kernelloader/repository.json",
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
    "updateInfoUrl": "https://example.com/mymod/update.json",
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

**Mod-specific fields:**

| Field                 | Type   | Description                                              |
|:----------------------|:-------|:---------------------------------------------------------|
| `features`            | array  | List of Mod feature tags                                 |
| `sizeCategory`        | string | Size category: `TINY`/`SMALL`/`MEDIUM`/`LARGE`/`HUGE`    |
| `targetGameVersion`   | string | Target game version                                      |
| `minGameVersion`      | string | Minimum supported game version                           |
| `maxGameVersion`      | string | Maximum supported game version                           |
| `conflicts`           | array  | List of conflicting Mod IDs                              |
| `stableVerified`      | bool   | Whether stability has been verified                      |
| `experimental`        | bool   | Whether it is an experimental version                    |
| `deprecated`          | bool   | Whether it is deprecated                                 |

**Mod Feature enum values:**

| Value                    | Description                              |
|:-------------------------|:-----------------------------------------|
| `ASSISTANCE`             | Assistive (minimap, coordinate display)  |
| `QUALITY_OF_LIFE`        | Quality of life improvements             |
| `UTILITY`                | Utilities                                |
| `ACCESSIBILITY`          | Accessibility                            |
| `VISUAL_ENHANCEMENT`     | Visual enhancement                       |
| `UI_IMPROVEMENT`         | UI improvements                          |
| `NEW_CONTENT`            | New content                              |
| `CONTENT_EXPANSION`      | Content expansion                        |
| `OVERHAUL`               | Complete overhaul                        |
| `MECHANICS_CHANGE`       | Mechanics changes                        |
| `DIFFICULTY_MOD`         | Difficulty adjustments                   |
| `PROGRESSION_SYSTEM`     | Progression system                       |
| `ADVENTURE`              | Adventure/exploration                    |
| `BUILDING`               | Building/construction                    |
| `TECHNOLOGY`             | Technology/automation                    |
| `MAGIC`                  | Magic/mysticism                          |
| `FANTASY`                | Fantasy                                  |
| `SCI_FI`                 | Sci-fi/futuristic                        |
| `REALISM`                | Realism                                  |
| `DECORATION`             | Decoration/beautification                |
| `MULTIPLAYER_ENHANCED`   | Multiplayer enhancements                 |
| `SERVER_MANAGEMENT`      | Server management                        |
| `COMMUNICATION`          | Communication/chat                       |

---

### PlatformSupport Specification

The `support` field is used to declare a component's platform compatibility:

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

**Architecture support:**

| Platform | Architecture Fields | Description              |
|:---------|:--------------------|:-------------------------|
| Android  | `arm64`, `arm`      | ARM64 / ARM32            |
| Windows  | `x64`, `x86`        | 64-bit / 32-bit          |
| Linux    | `x64`, `x86`        | 64-bit / 32-bit          |
| macOS    | `arm64`, `x64`      | Apple Silicon / Intel    |
| iOS      | `arm64`             | ARM64 only               |

---

### 📡 Update Functionality and Mod Repository

TEFManager supports checking for Mod and ModLoader updates from remote information sources, and supports browsing, searching, and installing Mods via a Mod repository index.

#### Update Info Fields

Two new optional fields have been added to `Info.json` to declare the update info source and Mod repository index:

| Field                   | Type   | Applicable type | Required | Description                                                             |
|:------------------------|:-------|:----------------|:---------|:------------------------------------------------------------------------|
| `updateInfoUrl`         | string | mod / modloader | ❌        | Update info JSON URL for this component, used to check for new versions |
| `modRepositoryIndexUrl` | string | modloader       | ❌        | Mod repository index JSON URL corresponding to this ModLoader           |

> 💡 These two fields can be declared in both `Manifest.json` and `Info.json`; TEFManager gives precedence to declarations in the component metadata.

#### AddonUpdateInfo Update Info Format

The JSON file pointed to by `updateInfoUrl` should follow the `AddonUpdateInfo` structure:

```json
{
    "version": "1.1.0",
    "versionCode": 2,
    "fileSize": "2.4 MB",
    "downloadUrl": "https://example.com/mymod/1.1.0.tefpkg",
    "updateLog": "Fixed several issues and added feature X"
}
```

**Field descriptions:**

| Field         | Type   | Required | Description                                                                                             |
|:--------------|:-------|:---------|:--------------------------------------------------------------------------------------------------------|
| `version`     | string | ✅        | Latest version number (semantic versioning)                                                             |
| `versionCode` | int    | ✅        | Latest version code, used to compare with the local `versionCode` to determine whether an update exists |
| `fileSize`    | string | ✅        | Human-readable description of the package size                                                          |
| `downloadUrl` | string | ✅        | Download URL of the new version package                                                                 |
| `updateLog`   | string | ✅        | Update log                                                                                              |

> TEFManager decides whether to prompt for an update by comparing the remote `versionCode` with the `versionCode` of the locally installed component.

#### ModRepositoryIndex Mod Repository Index Format

The JSON file pointed to by `modRepositoryIndexUrl` should follow the `ModRepositoryIndex` structure, used to list the Mods available in that ModLoader ecosystem:

```json
{
    "updatedAt": "2026-10-31T12:00:00Z",
    "loaderMinVersionCode": 1,
    "loaderMinVersion": "1.0.0",
    "mods": [
        {
            "pkgId": "com.example.mymod",
            "name": "My Mod",
            "author": "TEFKernel Team",
            "version": "1.1.0",
            "versionCode": 2,
            "brieflyDescribe": "Brief description",
            "features": ["NEW_CONTENT", "QUALITY_OF_LIFE"],
            "sizeCategory": "MEDIUM",
            "stableVerified": true,
            "experimental": false,
            "deprecated": false,
            "iconUrl": "https://example.com/mymod/icon.png",
            "downloadUrl": "https://example.com/mymod/1.1.0.tefpkg",
            "fileSize": "2.4 MB",
            "updateLog": "Fixed several issues and added feature X"
        }
    ]
}
```

**Top-level field descriptions:**

| Field                  | Type   | Required | Description                                          |
|:-----------------------|:-------|:---------|:-----------------------------------------------------|
| `updatedAt`            | string | ❌        | Index update time                                    |
| `loaderMinVersionCode` | int    | ✅        | Minimum ModLoader versionCode required by this index |
| `loaderMinVersion`     | string | ✅        | Minimum ModLoader version required by this index     |
| `mods`                 | array  | ✅        | List of Mod entries                                  |

**Mod entry (ModEntry) field descriptions:**

| Field             | Type   | Required | Description                                           |
|:------------------|:-------|:---------|:------------------------------------------------------|
| `pkgId`           | string | ✅        | Mod package ID                                        |
| `name`            | string | ✅        | Mod name                                              |
| `author`          | string | ✅        | Author                                                |
| `version`         | string | ✅        | Version number                                        |
| `versionCode`     | int    | ✅        | Version code                                          |
| `brieflyDescribe` | string | ❌        | Brief description                                     |
| `features`        | array  | ❌        | Feature tags (values same as `ModFeature` enum)       |
| `sizeCategory`    | string | ❌        | Size category (values same as `ModSizeCategory` enum) |
| `stableVerified`  | bool   | ❌        | Whether stability has been verified                   |
| `experimental`    | bool   | ❌        | Whether it is an experimental version                 |
| `deprecated`      | bool   | ❌        | Whether it is deprecated                              |
| `iconUrl`         | string | ❌        | Icon URL, may be empty                                |
| `downloadUrl`     | string | ✅        | Download URL                                          |
| `fileSize`        | string | ❌        | File size description                                 |
| `updateLog`       | string | ❌        | Update log                                            |

> `ModEntry` provides a `toAddonUpdateInfo()` method that can directly convert a repository entry into an `AddonUpdateInfo`, making it easy to reuse the unified update/install process.

#### Update and Repository Workflow

1. **Update check**: TEFManager reads the `updateInfoUrl` of installed Mods / ModLoaders, fetches the `AddonUpdateInfo`, compares the `versionCode`, and prompts the user to update if the remote is higher.
2. **Repository browsing**: TEFManager reads the `modRepositoryIndexUrl` of the current ModLoader, fetches the `ModRepositoryIndex`, and displays the list of installable Mods.
3. **Version compatibility check**: The `loaderMinVersionCode` / `loaderMinVersion` in the repository index are used to verify whether the current ModLoader meets the minimum version requirements of the index; if not, the user is prompted to update the ModLoader first.
4. **Install/Update**: After the user selects an entry, TEFManager downloads the `.tefpkg` package via the `downloadUrl` and completes the installation according to the package format specification.

> ⚠️ All remote JSON is an optional capability. Components without `updateInfoUrl` or `modRepositoryIndexUrl` configured will not participate in update checks or repository display, remaining consistent with the behavior of older versions.

#### Comparison of Package Types for Update and Repository Capabilities

| Type          | Directory Location      | File Extension | Manager            | Purpose                    | Update Check | Repository Index |
|:--------------|:------------------------|:---------------|:-------------------|:---------------------------|:-------------|:-----------------|
| **Plugin**    | `plugin/pkg/`           | `.tefpkg`      | `PluginManager`    | Register low-level symbols | ❌            | ❌                |
| **Module**    | `module/pkg/`           | `.tefpkg`      | `ModuleManager`    | Game feature extensions    | ❌            | ❌                |
| **ModLoader** | `modloader/pkg/`        | `.tefpkg`      | `ModLoaderManager` | Mod loading container      | ✅            | ✅                |
| **Mod**       | `mods/{loader_id}/mod/` | `.tefpkg`      | `ModManager`       | End-user mods              | ✅            | ❌                |


## 📁 Directory Structure

TEFManager creates an independent directory for each package type on top of the TEFKernel working directory:

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
        ├── enables.txt          # List of Mods enabled for this ModLoader
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

TEFManager includes three core extension modules that **fill the gap in the Android modding ecosystem**, allowing Android players to enjoy the same modding experience as on desktop:

### 🗣️ Language Pack Module (LanguagePack-Extension)

Provides multi-language support for the game, allowing players to switch the interface language without modifying the game itself.

- **Supported formats**: `.lang` / `.json` / `.po`
- **Features**: Game interface text replacement, font adaptation, RTL language support
- **Applicable platforms**: All platforms (Android / Windows / Linux / macOS)

> 📦 **Project URL**: [TEFManager-LanguagePack-Extension](https://github.com/eternalfuture-e38299/TEFManager-LanguagePack-Extension)

---

### 🎨 Texture Pack Module (TexturePack-Extension)

Allows Android players to freely replace game textures just like on desktop, enjoying HD textures and personalized visual effects.

- **Supported formats**: `pack.json` (Terraria standard) / `pack_info.json` (TEFManager)
- **Features**: Texture replacement, priority sorting, dynamic switching
- **Applicable platforms**: Android

> 📦 **Project URL**: [TEFManager-TexturePack-Extension](https://github.com/eternalfuture-e38299/TEFManager-TexturePack-Extension)

---

### 🔤 Font Pack Module (FontPack-Extension)

Supports loading custom fonts, solving font display issues in different language regions and improving the reading experience.

- **Supported formats**: `.zip`
- **Features**: Font replacement
- **Applicable platforms**: Android
- **⭐ Special significance**: Provides better display effects for players of non-Latin scripts such as Chinese, Japanese, and Korean

> 📦 **Project URL**: [TEFManager-FontPack-Extension](https://github.com/eternalfuture-e38299/TEFManager-FontPack-Extension)

---

### 🎨 Audio Pack Module (AudioPack-Extension)

Allows Android players to freely replace game audio just like on desktop.

- **Supported formats**: `pack.json` (Terraria standard) / `pack_info.json` (TEFManager)
- **Features**: Texture replacement, priority sorting, dynamic switching
- **Applicable platforms**: Android

> 📦 **Project URL**: [TEFManager-AudioPack-Extension](https://github.com/eternalfuture-e38299/TEFManager-AudioPack-Extension)

---

## 🤖 Android Dual-Mode Operation

TEFManager supports two operating modes on the Android platform to accommodate users' different devices and permission situations:

### Root Mode

**Principle**: Injects TEFKernel into the game process via the Xposed framework (LSPosed, etc.).

**Requirements**:
- The device is rooted
- The Xposed framework is installed (LSPosed recommended)
- Activate the TEFManager module in the Xposed manager and add Terraria to the scope
- Grant TEFManager Root permission

**Advantages**:
- No need to modify the game APK
- Supports official game version updates
- Perfectly compatible with official versions

**Configuration suggestion**: If you encounter crashes in the Xposed framework, you can enable the "Restore inline hooks" option for Terraria.

---

### External Mode

**Principle**: TEFManager automatically patches the game APK, injects TEFKernel, and repackages and installs it.

**Requirements**:
- No Root required
- Requires the game APK file (unencrypted or unpackable)

**Advantages**:
- No Root permission required
- Wider applicability
- One-click operation, simple and easy to use

**Notes**:
- The APK installed after patching requires TEFManager to remain running in the background
- If you encounter crashes, try replacing the game installation package and re-patching and installing

---

## ⚙️ Mod Settings API

Mods can declare settings in `Info.json`, and TEFManager will automatically generate the settings interface and save the user configuration as `config.json`.

### Info.json Example

```json
{
  "settings": [
    {
      "key": "feature_enabled",
      "title": "Enable Feature",
      "description": "Controls whether this feature is enabled",
      "type": "SWITCH",
      "defaultValue": true,
      "section": "General"
    },
    {
      "key": "output_multiplier",
      "title": "Output Multiplier",
      "description": "Adjusts the output quantity",
      "type": "INTEGER",
      "defaultValue": 2,
      "min": 1,
      "max": 10,
      "step": 1,
      "unit": "×",
      "section": "General"
    },
    {
      "key": "custom_message",
      "title": "Custom Text",
      "description": "For example, a custom prompt message",
      "type": "STRING",
      "defaultValue": "hello",
      "section": "Advanced"
    }
  ]
}
```

Supported setting types:

- `SWITCH`: Switch, corresponding to `true` or `false`
- `INTEGER`: Integer input, with configurable minimum value, maximum value, step, and unit
- `CHOICE`: Choose one from multiple options; an additional `options` array is required
- `STRING`: Single-line free text input; `defaultValue` is a string

Field descriptions:

- `section`: Optional, the group name of the setting item. Settings in the same `section` are grouped into the same card, ordered by their first appearance in `Info.json`; if left empty, no group title is displayed.

The configuration file is saved to the following location:

```text
<TEFManager data directory>/mods/<loaderId>/private/<pkgId>/config.json
```

After reading the configuration, the mod should perform its own type and value range validation. For detailed field definitions of setting items, refer to `ModItem.ModSetting`.

---

## 📦 Packaging Guide

For the packaging documentation of TEFManager packages, please refer to TEFKernel.

---

## 🔗 Related Links

| Project                  | Link                                                                                | Description                          |
|:-------------------------|:------------------------------------------------------------------------------------|:-------------------------------------|
| **TEFKernel**            | [GitHub](https://github.com/eternalfuture-e38299/tefkernel)                         | Cross-platform runtime kernel        |
| **TEFPkg-Tool**          | [GitHub](https://github.com/eternalfuture-e38299/TEFPkg-Tool)                       | Package format packaging tool        |
| **Texture Pack Module**  | [GitHub](https://github.com/eternalfuture-e38299/TEFManager-TexturePack-Extension)  | Texture pack loading                 |
| **Language Pack Module** | [GitHub](https://github.com/eternalfuture-e38299/TEFManager-LanguagePack-Extension) | Cross-platform language pack loading |
Here's the continuation (the final section of the README):

| **Font Pack Module** | [GitHub](https://github.com/eternalfuture-e38299/TEFManager-FontPack-Extension) | Font pack loading |
| **Audio Pack Module** | [GitHub](https://github.com/eternalfuture-e38299/TEFManager-AudioPack-Extension) | Audio pack loading |