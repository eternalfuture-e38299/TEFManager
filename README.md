# 🎮 TEFManager
* [English](README-en.md)

TEFManager 是基于 **Kotlin Multiplatform** + **Compose Multiplatform** 开发的 TEFKernel 官方图形化管理工具。它为 Plugin、Module、ModLoader、Mod 四种组件类型提供了完整的可视化管理和包格式规范。


![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-purple.svg?logo=kotlin)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-blue.svg)
![AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-red.svg)
![Platforms](https://img.shields.io/badge/Platform-Android%20%7C%20Windows%20%7C%20Linux%20%7C%20macOS%20%7C%20iOS-green.svg)

## [![Telegram Channel](https://img.shields.io/badge/Official_Telegram_Channel-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/TEFModLoader)

## 🏛️ 社区准则
1. 文明用语，保持友善交流环境
2. 禁止讨论任何政治敏感话题（遵循中国和国际法律法规）
3. 不得传播暴力、极端或违法内容
4. 技术讨论应保持专业性和建设性

---

## 📖 概述

### 什么是 TEFManager？

TEFManager 是 TEFKernel 生态系统的**官方图形化管理前端**，它：

1. **实现了完整的包类型系统**：为 Plugin、Module、ModLoader、Mod 定义了标准化的包格式
2. **提供可视化界面**：跨平台 UI，支持 Android/Windows/Linux/macOS
3. **封装了 TEFKernel 目录结构**：自动管理 `plugin/`、`module/`、`modloader/`、`mods/` 目录
4. **支持依赖管理**：自动解析和安装内联依赖（inline dependencies）
5. **填补安卓生态空白**：内置语言包、材质包、字体包模块，让安卓玩家也能享受完整的模组体验
6. **双模式运行**：Android 支持 Root 模式和外部模式，适应不同用户需求

---

## 🌍 平台与语言支持

### 支持平台

| 平台             | 架构                        | 状态     | 说明                |
|:---------------|:--------------------------|:-------|:------------------|
| **🤖 Android** | ARM64, ARM32, x86, x86_64 | ✅ 完整支持 | 支持 Root 模式 + 外部模式 |
| **🪟 Windows** | x64                       | ✅ 完整支持 | 通过 JAR(Java21)    |
| **🐧 Linux**   | x64                       | ✅ 完整支持 | 通过 JAR(Java21)    |
| **🍎 macOS**   | x64                       | ✅ 完整支持 | 通过 JAR(Java21)    |

> **架构说明**：Android 平台支持四种主流架构（ARM64/ARM32/x86/x86_64），确保在各类 Android 设备上均可运行。

### 支持语言

| 语言          | 代码      | 状态     | 贡献者  |
|:------------|:--------|:-------|:-----|
| **简体中文**    | `zh-CN` | ✅ 完整支持 | 官方维护 |
| **English** | `en-US` | ✅ 完整支持 | 官方维护 |
| **Русский** | `ru-RU` | ✅ 完整支持 | 官方维护 |

---

## 🚀 桌面端快速启动指南

### ⚠️ 重要提示：JRE 环境配置

TEFManager 桌面版（Windows/Linux/macOS）以 JAR 文件形式分发，需要 **Java 21 或更高版本** 的运行环境。

由于一些不可抗力的技术原因，**部分发行版自带的 JRE 包可能存在兼容性问题**，导致 TEFManager 无法正常启动或运行异常。**强烈建议** 用户从微软官方 OpenJDK 发行版获取稳定可靠的 Java 运行时环境。

### 📥 推荐 JRE 安装方式

#### 方式一：Microsoft OpenJDK（官方推荐 ✅）

访问微软 OpenJDK 官方下载页面：

👉 **[https://learn.microsoft.com/java/openjdk/download](https://learn.microsoft.com/java/openjdk/download)**

**选择对应平台的安装包：**

| 平台      | 推荐版本    | 下载文件格式        |
|:--------|:--------|:--------------|
| Windows | Java 21 | `.msi` 安装包    |
| Linux   | Java 21 | `.tar.gz` 压缩包 |
| macOS   | Java 21 | `.tar.gz` 压缩包 |

> ✅ **为什么推荐 Microsoft OpenJDK？**
> - 微软官方维护，质量和稳定性有保障
> - 经过大量企业级应用验证
> - 与 TEFManager 兼容性最佳
> - 定期安全更新和漏洞修复

#### 方式二：其他 OpenJDK 发行版

如果无法访问微软源，也可使用以下替代方案：

| 发行版                 | 下载地址                             |
|:--------------------|:---------------------------------|
| **Adoptium**        | https://adoptium.net/            |
| **Amazon Corretto** | https://aws.amazon.com/corretto/ |
| **Azul Zulu**       | https://www.azul.com/downloads/  |

> ⚠️ **注意**：使用非微软 OpenJDK 发行版时，如遇启动问题，请优先尝试切换至 Microsoft OpenJDK。

---

### 🖥️ 从 JAR 启动 TEFManager

#### Windows 用户

**方法一：命令行启动（推荐）**

1. 打开命令提示符（CMD）或 PowerShell
2. 切换到 TEFManager JAR 文件所在目录
3. 执行启动命令：

```cmd
java -jar TEFManager.jar
```

**方法二：创建快捷方式**

1. 右键桌面 → 新建 → 快捷方式
2. 位置填写：
```
java -jar "C:\path\to\TEFManager.jar"
```
3. 将 `C:\path\to\` 替换为实际 JAR 文件路径

**方法三：双击运行（需正确关联 .jar 文件）**

> ⚠️ 如果双击无法打开，说明 `.jar` 文件未正确关联到 Java。请使用方法一或二。

---

#### Linux 用户

**终端启动：**

```bash
# 进入 JAR 文件所在目录
cd /path/to/TEFManager/

# 启动 TEFManager
java -jar TEFManager.jar
```

**创建桌面快捷方式（可选）：**

创建 `TEFManager.desktop` 文件：

```ini
[Desktop Entry]
Name=TEFManager
Comment=TEFKernel 图形化管理工具
Exec=java -jar /path/to/TEFManager.jar
Icon=/path/to/icon.png
Terminal=false
Type=Application
Categories=Game;
```

---

#### macOS 用户

**终端启动：**

```bash
# 进入 JAR 文件所在目录
cd /path/to/TEFManager/

# 启动 TEFManager
java -jar TEFManager.jar
```

**使用脚本启动（可选）：**

创建 `start.sh` 脚本：

```bash
#!/bin/bash
java -jar /path/to/TEFManager.jar
```

赋予执行权限：

```bash
chmod +x start.sh
```

双击 `start.sh` 或在终端中运行 `./start.sh` 启动。

---

### 🔧 常见启动问题排查

| 问题现象                           | 可能原因       | 解决方案                                      |
|:-------------------------------|:-----------|:------------------------------------------|
| `java: command not found`      | 未安装 Java   | 安装 Microsoft OpenJDK                      |
| `UnsupportedClassVersionError` | Java 版本过低  | 升级到 Java 21 或更高版本                         |
| 启动后闪退或界面异常                     | JRE 兼容性问题  | 切换至 Microsoft OpenJDK                     |
| 双击 .jar 无反应                    | 文件关联错误     | 使用命令行启动或重新关联 `.jar` 文件到 `javaw.exe`       |
| 内存不足错误                         | JVM 内存分配不足 | 增加内存：`java -Xmx2048m -jar TEFManager.jar` |

**验证 Java 安装：**

```bash
java -version
```

正确输出应包含类似 `openjdk version "21"` 或更高版本号。

---

### 📦 获取 TEFManager JAR 文件

| 来源                  | 说明               |
|:--------------------|:-----------------|
| **GitHub Releases** | 官方发布页面，推荐下载最新稳定版 |

---

## 包类型对比

| 类型            | 目录位置                    | 文件后缀      | 管理者                | 用途       |
|:--------------|:------------------------|:----------|:-------------------|:---------|
| **Plugin**    | `plugin/pkg/`           | `.tefpkg` | `PluginManager`    | 注册底层符号   |
| **Module**    | `module/pkg/`           | `.tefpkg` | `ModuleManager`    | 游戏功能扩展   |
| **ModLoader** | `modloader/pkg/`        | `.tefpkg` | `ModLoaderManager` | Mod 加载容器 |
| **Mod**       | `mods/{loader_id}/mod/` | `.tefpkg` | `ModManager`       | 最终用户模组   |

---

## 📦 包格式规范

### 通用包结构

所有 TEFManager 包都是一个 `.zip` 文件，包含以下标准文件：

```
package.zip
├── Manifest.json          # 包清单（必须）
├── Info.json              # 组件元信息（必须）
├── {file}                 # 主文件（动态库/包文件）
├── icon.png               # 图标文件（可选）
└── resources/             # 资源目录（可选）
└── ...
```

### Manifest.json 规范

`Manifest.json` 是包的**入口描述文件**，TEFManager 通过它识别包类型和安装方式。

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

**字段说明：**

| 字段             | 类型     | 必填        | 说明                                            |
|:---------------|:-------|:----------|:----------------------------------------------|
| `type`         | string | ✅         | 包类型：`plugin` / `module` / `modloader` / `mod` |
| `file`         | string | ✅         | 主文件路径（相对于 ZIP 根目录）                            |
| `resources`    | string | ❌         | 资源目录路径                                        |
| `iconFile`     | string | ❌         | 图标文件路径                                        |
| `parentLoader` | string | ⚠️ Mod 必填 | 所属 ModLoader 的 `pkgId`                        |
| `modloader`    | object | ❌         | 内联 ModLoader 依赖                               |
| `plugins`      | object | ❌         | 内联 Plugin 依赖                                  |

---

### Info.json 规范

`Info.json` 包含组件的元信息，用于在 TEFManager 中显示和版本管理。

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
    "brieflyDescribe": "简短描述",
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
    "brieflyDescribe": "TEFKernel 官方 ModLoader",
    "description": "基于 TEFKernel 的 ModLoader 实现",
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
    "brieflyDescribe": "简短描述",
    "description": "详细描述",
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

**Mod 特有字段：**

| 字段                  | 类型     | 说明                                          |
|:--------------------|:-------|:--------------------------------------------|
| `features`          | array  | Mod 特征标签列表                                  |
| `sizeCategory`      | string | 规模分类：`TINY`/`SMALL`/`MEDIUM`/`LARGE`/`HUGE` |
| `targetGameVersion` | string | 目标游戏版本                                      |
| `minGameVersion`    | string | 最低支持游戏版本                                    |
| `maxGameVersion`    | string | 最高支持游戏版本                                    |
| `conflicts`         | array  | 冲突的 Mod ID 列表                               |
| `stableVerified`    | bool   | 是否已验证稳定                                     |
| `experimental`      | bool   | 是否实验性版本                                     |
| `deprecated`        | bool   | 是否已弃用                                       |

**Mod Feature 枚举值：**

| 值                      | 说明            |
|:-----------------------|:--------------|
| `ASSISTANCE`           | 辅助型（小地图、坐标显示） |
| `QUALITY_OF_LIFE`      | 生活质量改善        |
| `UTILITY`              | 实用工具          |
| `ACCESSIBILITY`        | 辅助功能          |
| `VISUAL_ENHANCEMENT`   | 视觉增强          |
| `UI_IMPROVEMENT`       | UI 改进         |
| `NEW_CONTENT`          | 新增内容          |
| `CONTENT_EXPANSION`    | 内容扩展          |
| `OVERHAUL`             | 全面重制          |
| `MECHANICS_CHANGE`     | 机制修改          |
| `DIFFICULTY_MOD`       | 难度调整          |
| `PROGRESSION_SYSTEM`   | 进度系统          |
| `ADVENTURE`            | 冒险/探索         |
| `BUILDING`             | 建筑/建造         |
| `TECHNOLOGY`           | 科技/自动化        |
| `MAGIC`                | 魔法/神秘         |
| `FANTASY`              | 幻想/魔幻         |
| `SCI_FI`               | 科幻/未来         |
| `REALISM`              | 写实/真实         |
| `DECORATION`           | 装饰/美化         |
| `MULTIPLAYER_ENHANCED` | 多人游戏增强        |
| `SERVER_MANAGEMENT`    | 服务器管理         |
| `COMMUNICATION`        | 通信/聊天         |

---

### PlatformSupport 规范

`support` 字段用于声明组件的平台兼容性：

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

**架构支持：**

| 平台      | 架构字段           | 说明                    |
|:--------|:---------------|:----------------------|
| Android | `arm64`, `arm` | ARM64 / ARM32         |
| Windows | `x64`, `x86`   | 64位 / 32位             |
| Linux   | `x64`, `x86`   | 64位 / 32位             |
| macOS   | `arm64`, `x64` | Apple Silicon / Intel |
| iOS     | `arm64`        | 仅 ARM64               |

---

## 📁 目录结构

TEFManager 在 TEFKernel 工作目录基础上，为每种包类型创建了独立的目录：

```
工作目录/
├── plugin/                      # Plugin 目录
│   ├── enables.txt              # 启用的 Plugin 列表
│   ├── pkg/                     # Plugin 包文件
│   │   └── com.example.plugin.tefpkg
│   ├── icons/                   # Plugin 图标
│   │   └── com.example.plugin.icon
│   └── private/                 # Plugin 私有数据
│       └── com.example.plugin/
│
├── module/                      # Module 目录
│   ├── enables.txt              # 启用的 Module 列表
│   ├── pkg/                     # Module 包文件
│   │   └── com.example.module.tefpkg
│   ├── icons/                   # Module 图标
│   │   └── com.example.module.icon
│   └── private/                 # Module 私有数据
│       └── com.example.module/
│
├── modloader/                   # ModLoader 目录
│   ├── enables.txt              # 启用的 ModLoader 列表
│   ├── pkg/                     # ModLoader 包文件
│   │   └── eternal.future.kernelloader.tefpkg
│   ├── icons/                   # ModLoader 图标
│   │   └── eternal.future.kernelloader.icon
│   └── private/                 # ModLoader 私有数据
│       └── eternal.future.kernelloader/
│
└── mods/                        # Mod 目录
    └── eternal.future.kernelloader/   # 按 ModLoader 隔离
        ├── enables.txt          # 该 ModLoader 启用的 Mod 列表
        ├── db/                  # Mod 数据库
        ├── icons/               # Mod 图标
        │   └── com.example.mymod.icon
        ├── mod/                 # Mod 包文件
        │   └── com.example.mymod.tefpkg
        ├── private/             # Mod 私有数据
        │   └── com.example.mymod/
        └── resources/           # Mod 资源
            └── com.example.mymod/
```

---

## 🎨 内置扩展模块

TEFManager 内置了三个核心扩展模块，**填补了安卓端模组生态的空白**，让安卓玩家也能享受与桌面端同等的模组体验：

### 🗣️ 语言包模块 (LanguagePack-Extension)

为游戏提供多语言支持，允许玩家在不修改游戏本体的前提下切换界面语言。

- **支持格式**：`.lang` / `.json` / `.po`
- **功能**：游戏界面文本替换、字体适配、RTL 语言支持
- **适用平台**：全平台（Android / Windows / Linux / macOS）

> 📦 **项目地址**：[TEFManager-LanguagePack-Extension](https://github.com/eternalfuture-e38299/TEFManager-LanguagePack-Extension)

---

### 🎨 材质包模块 (TexturePack-Extension)

让安卓玩家也能像桌面端一样自由替换游戏纹理，享受高清材质和个性化视觉效果。

- **支持格式**：`pack.json` (Terraria 标准) / `Settings.json` (TLPro) / `pack_info.json` (TEFManager)
- **功能**：纹理替换、优先级排序、动态切换
- **适用平台**：Android

> 📦 **项目地址**：[TEFManager-TexturePack-Extension](https://github.com/eternalfuture-e38299/TEFManager-TexturePack-Extension)

---

### 🔤 字体包模块 (FontPack-Extension)

支持加载自定义字体，解决不同语言地区的字体显示问题，提升阅读体验。

- **支持格式**：`.zip`
- **功能**：字体替换
- **适用平台**：Android
- **⭐ 特别意义**：为中文、日文、韩文等非拉丁文字玩家提供更好的显示效果

> 📦 **项目地址**：[TEFManager-FontPack-Extension](https://github.com/eternalfuture-e38299/TEFManager-FontPack-Extension)

---

## 🤖 Android 双模式运行

TEFManager 在 Android 平台上支持两种运行模式，以适应不同用户的设备和权限情况：

### Root 模式

**原理**：通过 Xposed 框架（LSPosed 等）将 TEFKernel 注入到游戏进程。

**使用条件**：
- 设备已 Root
- 已安装 Xposed 框架（推荐 LSPosed）
- 在 Xposed 管理器中激活 TEFManager 模块，并将 Terraria 加入作用域
- 授予 TEFManager Root权限`

**优势**：
- 无需修改游戏 APK
- 支持游戏官方版本更新
- 与官方版本完美兼容

**配置建议**：如在 Xposed 框架中遇到闪退，可对 Terraria 启用「还原内联钩子」选项。

---

### 外部模式

**原理**：通过 TEFManager 自动修补游戏 APK，注入 TEFKernel 后重新打包安装。

**使用条件**：
- 无需 Root
- 需要游戏 APK 文件（未加密或可解包）

**优势**：
- 无需 Root 权限
- 适用范围更广
- 一键操作，简单易用

**注意事项**：
- 修补后安装的 APK 需要保留 TEFManager 后台运行
- 如遇到闪退，可尝试更换游戏安装包并重新修补安装

---

## 📦 打包指南

TEFManager 包的打包文档请参考TEFKenrel

---

## 🔗 相关链接

| 项目              | 链接                                                                                  | 说明       |
|:----------------|:------------------------------------------------------------------------------------|:---------|
| **TEFKernel**   | [GitHub](https://github.com/eternalfuture-e38299/tefkernel)                         | 跨平台运行时内核 |
| **TEFPkg-Tool** | [GitHub](https://github.com/eternalfuture-e38299/TEFPkg-Tool)                       | 包格式打包工具  |
| **材质包模块**       | [GitHub](https://github.com/eternalfuture-e38299/TEFManager-TexturePack-Extension)  | 材质包加载    |
| **语言包模块**       | [GitHub](https://github.com/eternalfuture-e38299/TEFManager-LanguagePack-Extension) | 跨平台语言包加载 |
| **字体包模块**       | [GitHub](https://github.com/eternalfuture-e38299/TEFManager-FontPack-Extension)     | 字体包加载    |

---

*TEFManager - 让 TEFKernel 生态触手可及！* 🚀🎮✨