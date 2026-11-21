# Mod 自定义界面（稳定 ABI）

Mod 可以在自己的目录里附带一个页面 JAR，TEFManager 加载后按节点树渲染成原生界面。

## 目录约定

```
<mod 数据>/private/<pkgId>/page/
├── android.jar
└── desktop.jar
```

## 稳定 ABI（不会随 Compose 版本变化）

Mod 只依赖 `modpage-api.jar`（compileOnly），**不依赖 Compose**：

```kotlin
class Page : ModPage {
    override fun title(): String = "My Page"

    override fun render(api: ModPageApi): PageNode = PageNode(
        type = "column",
        children = listOf(
            PageNode("text", mapOf("value" to "Hello ${api.privatePath}")),
            PageNode("button", mapOf("id" to "click", "label" to "点我")),
        ),
    )

    override fun onEvent(id: String, value: String?, api: ModPageApi) {
        if (id == "click") api.setState("count", "1")
    }
}
```

- `ModPageApi` 提供 `privatePath`、`dataPath`、`state(key)`、`setState(key,value)`、`openUrl`
- `PageNode` 类型：`column / row / text / button / checkbox / slider / input / separator / spacer / progress / collapsing / image`
- 状态保存在 `<private>/page-state.json`
- 类名由 `Info.json` 的 `pageClass` 指定，默认 `default.PageClass`

宿主使用自身的 Compose 版本渲染节点树，Mod 不需要重新编译。
