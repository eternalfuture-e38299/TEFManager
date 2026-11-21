# Mod 自定义界面（原生页面）

Mod 可以在自己的数据目录里附带一个页面 JAR，TEFManager 会把它加载成界面并显示。

## 目录约定

```
<mod 数据目录>/
└── private/
    └── <pkgId>/
        └── page/
            ├── android.jar    # Android
            └── desktop.jar    # JVM / 桌面
```

页面存在时，Mod 卡片会出现“界面”按钮。

## 页面类约定

页面 JAR 里的类需要满足（与旧 TEFModLoader 页面兼容）：

```kotlin
package my.mod.page

class Page {
    var params: Map<String, *> = emptyMap()

    val Content: @Composable () -> Unit = {
        Text("private-path: ${params["private-path"]}")
        Text("data-path: ${params["data-path"]}")
    }
}
```

- 类名写在 `Info.json` 的 `pageClass` 字段；缺省为 `default.PageClass`
- 可选：提供 `fun initialize(api: Map<String, *>)`，加载时会调用
- 宿主注入参数：
  - `private-path` = `<mod 数据目录>/private/<pkgId>`
  - `data-path` = `<mod 数据目录>`

## 实现说明

- Android 用 `DexClassLoader`，ClassLoader 以 App 的 ClassLoader 为父级，保证页面能看到 Compose
- JVM 用 `URLClassLoader`
- ClassLoader 会按页面 JAR 路径缓存，页面关闭时释放
- 加载失败显示错误提示，不再导致宿主崩溃
