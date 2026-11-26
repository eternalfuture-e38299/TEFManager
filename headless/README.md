# tefm —— 无头 TEFManager

在 SSH / 服务器环境下管理 TEFKernel 与 TEF 附加包的命令行工具。
它不依赖图形界面，只使用 Python 3 标准库，**直接读写与 TEFManager 完全相同的磁盘格式**，
所以同一个工作目录可以随时交回 GUI 版继续管理（反之亦然）。

适用场景：

- 在 VPS / 独立服务器上部署 TEF 专用服（`TerrariaServer`）
- 通过 SSH 安装/启用/禁用 Mod，改 Mod 配置，重启服务
- 生成 systemd 单元，长期挂着跑
- 给 CI / 脚本提供可自动化的管理入口

---

## 快速开始

```bash
# 1. 放到 PATH 里（可选）
chmod +x tefm.py
sudo ln -s "$PWD/tefm.py" /usr/local/bin/tefm

# 2. 一条命令：在指定目录初始化工作路径，并选定内核 / TEFLoader / 打补丁
tefm init -w /srv/tefm --game /srv/terraria \
     --kernel ~/uploads/tefkernel-1.4.4.zip \
     --loader ~/uploads/tefloader.zip \
     --patch

# 3. 安装附加包（自动读 Manifest.json 判断类型并启用）
tefm -w /srv/tefm addon install ~/uploads/LuaLoader.zip ~/uploads/VeinMiner.zip

# 4. 检查 / 启动
tefm -w /srv/tefm doctor
tefm -w /srv/tefm run --server --world /srv/terraria/Worlds/MyWorld.wld
```

`-w / --workdir` 写在子命令前后都可以。

默认工作目录是 `~/.local/share/tefmanager`，可用 `-w` 或环境变量 `TEFM_WORKDIR` 指定：

```bash
tefm -w /srv/terfm status
export TEFM_WORKDIR=/srv/terfm
```

---

## 命令一览

| 命令 | 说明 |
| --- | --- |
| `tefm init --game DIR [--arch ARCH] [--kernel ZIP\|版本] [--loader ZIP\|标签] [--patch]` | 在 `-w` 指定目录初始化工作路径，可同时选定内核与 TEFLoader（也支持从缓存里按版本标签选） |
| `tefm status [--json]` | 内核版本、tefloader 状态、已安装/已启用的附加包 |
| `tefm doctor` | 逐项检查部署完整性（路径、可执行位、启用项对应的包文件） |
| `tefm kernel install ZIP\|LIB [--label L]` | 安装 `tefkernel.zip`，或直接安装裸内核库（`.so` / `.dll` / `.dylib`，构建产物即可）并加入版本缓存 |
| `tefm kernel list` / `use LABEL\|latest` | 列出 / 切换缓存中的内核版本 |
| `tefm kernel version` | 显示当前内核版本 |
| `tefm loader install ZIP [--label L]` | 保存 `tefloader.zip` 并加入缓存，同时解压运行时（Linux/macOS 用 `net472/`，Windows x86 用 `net452/`） |
| `tefm loader list` / `use LABEL\|latest` | 列出 / 切换缓存中的 tefloader |
| `tefm loader runtime` | 重新解压 tefloader 运行时（例如更新 zip 之后） |
| `tefm patch [--force]` | 复制原版启动器为 `tefloader.bin.<arch>`，加可执行位 |
| `tefm addon install ZIP... [--no-enable]` | 安装 ModLoader / Module / Plugin / Mod，默认自动启用 |
| `tefm addon list` | 同 `status` |
| `tefm addon enable/disable/remove ID` | 启用 / 禁用 / 删除附加包 |
| `tefm config list/get/set ID [KEY] [VALUE]` | 读写 `<private>/config.json` 的 `values` |
| `tefm run [--server] [--world W] [--dry-run] [-- 额外参数]` | 前台启动（`exec`，适合 systemd / SSH，信号直达游戏） |
| `tefm service [--output PATH] [--user] [--world W] [--extra ARGS]` | 生成 systemd 单元 |
| `tefm logs [-n N] [-f]` | 查看 / 跟踪内核日志 |

`--json` 可与任意命令一起使用（当前 `status`、`addon install`、`config get` 会输出 JSON）。

---

## 和 GUI 版共用的磁盘格式

```
<workdir>/
  tefm.json                       本工具记录的游戏目录/架构/当前选用的内核与 tefloader
  packages/
    tefkernel/<版本>.zip          内核包缓存（可保留多个版本，随时 use 切换）
    tefloader/<标签>.zip          tefloader 包缓存
  tefkernel/
    info.json                     内核版本
    libtefkernel.<os>.<arch>.*    当前平台内核库
    tefloader.zip                 tefloader 运行时
  modloader/pkg/<pkgId>.tefpkg    ModLoader 包
  modloader/enables.txt           启用的 ID（一行一个）
  modloader/private/<pkgId>/...   该包的 Resources 内容
  modloader/icons/<pkgId>.icon
  module/...  plugin/...          同上
  mods/<modloader_id>/
    mod/<pkgId>                   Mod 清单（luamod.json / mymod.json 原样保存）
    private/<pkgId>/...           该 Mod 的 Resources 内容
    enables.txt
<game_dir>/
  Terraria.bin.<arch> / TerrariaServer.bin.<arch>   原版启动器
  tefloader.bin.<arch>                               补丁后的启动器
  tefloader.exe / .config / *.dll                    从 tefloader.zip 解压
```

说明：

- `db/` 是 GUI 版用于列表展示的加速缓存，运行时内核不读它，所以 `tefm` 不生成，也不影响使用；
  后来用 GUI 打开该工作目录时会自动重建。
- 附加包类型来自包内的 `Manifest.json`；`Mod` 类型还需要 `parentLoader` 指向已安装的 ModLoader。
- 支持包内 `modloader` / `plugins` 的 `inline` 依赖（会递归安装）。

---

## systemd 部署

```bash
# 先生成并检查脚本内容
tefm service --world /srv/terraria/Worlds/MyWorld.wld

# 写入系统级服务
sudo tefm service --output /etc/systemd/system/tef-server.service \
     --world /srv/terraria/Worlds/MyWorld.wld
sudo systemctl daemon-reload
sudo systemctl enable --now tef-server
journalctl -u tef-server -f

# 或者用户级（无 root）
tefm service --user --output ~/.config/systemd/user/tef-server.service
systemctl --user daemon-reload && systemctl --user enable --now tef-server
```

生成的 `ExecStart` 直接调用 `tefloader.bin.<arch>`，不经过 Python，
所以之后升级/移动本工具不影响服务运行。

> 首次开服如果需要交互式选择世界，先手动跑一次 `tefm run --server`；
> 之后在服务单元里用 `--world` 固定世界文件即可。

---

## 常见问题

**`tefm run` 报 “Program.SavePath is NULL ... update tefloader”**

服务端模式要求 tefloader 在初始化内核前预置 `Program.SavePath`（TEFKernel 1.4.4 起修复）。
用 `tefm loader install` 安装新的 `tefloader.zip` 即可；旧版会在启动时崩在
`Terraria.Main` 的类型初始化。

**官方 Linux 专用服目录没有 `Terraria.bin.*`**

有，名字是 `TerrariaServer.bin.x86_64`。`tefm patch` 会自动识别两种命名。

**改了 Mod 配置但游戏里没变化**

`tefm config set` 只改 `config.json`，大多数 Mod 在启动时读取，重启服务即可：

```bash
sudo systemctl restart tef-server
```

**我想直接装编译出来的内核 `.so`，没有 zip**

可以，直接传库文件即可（不需要 info.json）：

```bash
tefm -w /srv/tefm kernel install build/libtefkernel.linux.x86_64.so
# 默认标签是 local-x86_64；也可以：
tefm -w /srv/tefm kernel install build/libtefkernel.linux.x86_64.so --label 1.4.4
```

工具会按文件名里的平台/架构清理旧库；如果你装的是 x86 库但机器是 x86_64，
会打印警告（启动时会找不到匹配内核）。

**报 `PermissionError: /srv/tefm`**

`/srv` 一般属于 root。两种做法：

```bash
sudo tefm -w /srv/tefm init ...      # 系统级目录
# 或者用用户目录
tefm -w ~/tefm init ...
```

**tefloader 缓存标签是 `tefloader-<hash>`，看着不直观**

`tefloader.zip` 里没有版本元数据。安装时可以自己指定标签：

```bash
tefm -w /srv/tefm loader install tefloader-1.4.0.zip --label 1.4.0
tefm -w /srv/tefm loader use 1.4.0
```

**想换工作目录 / 多开几个服**

每个服使用独立的 `-w` 工作目录与独立的游戏目录副本即可：

```bash
tefm -w /srv/tefm-a init --game /srv/game-a
tefm -w /srv/tefm-b init --game /srv/game-b
```

**没有联网，怎么拿到 ZIP？**

`tefm` 不做下载，只做本地安装：在本机下载后用 `scp` 传到服务器，
再执行 `tefm addon install xxx.zip`。
