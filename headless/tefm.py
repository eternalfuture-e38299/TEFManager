#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
tefm —— 无头 TEFManager（命令行版）

面向 SSH / 服务器部署：不需要图形界面，按 TEFManager 在磁盘上的同一套格式
安装内核、tefloader、ModLoader / Module / Plugin / Mod，并启动专用服。

目录格式（与 GUI 版一致）：

    <workdir>/
      tefkernel/
        info.json                     内核版本
        libtefkernel.<os>.<arch>.*    内核动态库
        tefloader.zip                 tefloader 运行时压缩包
      modloader/pkg/<pkgId>.tefpkg
      modloader/enables.txt
      modloader/private/<pkgId>/...
      modloader/icons/<pkgId>.icon
      module/...  plugin/...          同上结构
      mods/<modloader_id>/mod/<pkgId>             Mod 清单（luamod.json / mymod.json）
      mods/<modloader_id>/private/<pkgId>/...     资源
      mods/<modloader_id>/enables.txt

    <game_dir>/
      Terraria.bin.<arch>             原版启动器（补丁源）
      tefloader.bin.<arch>            由上面复制而来，实际被启动的二进制
      tefloader.exe / tefloader.exe.config / *.dll   从 tefloader.zip 解压

同一个 workdir 可以同时被 GUI 版使用。
"""

import argparse
import fnmatch
import json
import os
import platform as _platform
import re
import shlex
import shutil
import stat
import subprocess
import sys
import tempfile
import time
import zipfile
from pathlib import Path

APP = "tefm"
DEFAULT_WORKDIR = Path.home() / ".local" / "share" / "tefmanager"

TYPE_ROOTS = {
    "modloader": "modloader",
    "module": "module",
    "plugin": "plugin",
    "mod": None,  # mods/<parentLoader>
}

ADDON_TYPE_NAMES = {
    "modloader": "ModLoader",
    "module": "Module",
    "plugin": "Plugin",
    "mod": "Mod",
}


# ---------------------------------------------------------------------------
# 基础工具
# ---------------------------------------------------------------------------

class TeFmError(Exception):
    pass


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


def info(msg):
    print(msg)


def warn(msg):
    eprint(f"warning: {msg}")


def die(msg, code=1):
    eprint(f"error: {msg}")
    sys.exit(code)


def norm_arch(machine=None):
    """把 platform.machine() 规范化成 TEFManager 用的 x86_64 / x86 / arm64 / arm。"""
    m = (machine or _platform.machine() or "").lower()
    if m in ("x86_64", "amd64", "x64"):
        return "x86_64"
    if m in ("i386", "i486", "i586", "i686", "x86"):
        return "x86"
    if m in ("aarch64", "arm64"):
        return "arm64"
    if m.startswith("arm"):
        return "arm"
    return m or "unknown"


def short_arch(arch):
    """KernelLoader 用的架构名：x64 / x86 / arm64 / arm。"""
    return {"x86_64": "x64"}.get(arch, arch)


def os_name():
    p = sys.platform
    if p.startswith("win"):
        return "windows"
    if p == "darwin":
        return "macos"
    if p.startswith("linux"):
        return "linux"
    return p


def lib_suffix():
    return {"windows": ".dll", "macos": ".dylib"}.get(os_name(), ".so")


def is_windows():
    return os_name() == "windows"


def _is_complete_config(path):
    if not Path(path).is_file():
        return False
    try:
        j = json.loads(Path(path).read_text(encoding="utf-8-sig"))
    except Exception:
        return False
    return bool(isinstance(j, dict) and j.get("game_dir"))


def read_json(path, default=None):
    try:
        with open(path, "r", encoding="utf-8-sig") as f:
            return json.load(f)
    except FileNotFoundError:
        return default
    except json.JSONDecodeError as e:
        raise TeFmError(f"JSON 解析失败 {path}: {e}")


def write_json(path, data, indent=2):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=indent)
        f.write("\n")
    os.replace(tmp, path)


def version_key(v):
    """把版本号拆成可比较的 key（支持 1.4.6 / 3.2.1.0 / 2.6.5-light）。"""
    parts = re.split(r"[._+\-]", str(v or ""))
    key = []
    for p in parts:
        if p.isdigit():
            key.append((1, int(p)))
        elif p:
            key.append((0, p))
    return key


def version_lt(a, b):
    return version_key(a) < version_key(b)


def safe_member(name):
    """拒绝 zip 里的绝对路径 / .. 路径。"""
    name = name.replace("\\", "/")
    parts = [p for p in name.split("/") if p not in ("", ".")]
    if name.startswith("/") or ".." in parts:
        return None
    return "/".join(parts)


def zip_extract_prefix(zf, prefix, dest, strip_prefix=True):
    """把 zip 中以 prefix 开头的文件解压到 dest（可剥掉 prefix）。"""
    prefix = (prefix or "").strip("/")
    pre = prefix + "/" if prefix else ""
    dest = Path(dest)
    count = 0
    for zi in zf.infolist():
        if zi.is_dir():
            continue
        m = safe_member(zi.filename)
        if m is None:
            warn(f"跳过可疑路径: {zi.filename}")
            continue
        if pre and not m.startswith(pre):
            continue
        rel = m[len(pre):] if (pre and strip_prefix) else m
        if not rel:
            continue
        out = dest / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        with zf.open(zi) as src, open(out, "wb") as dst:
            shutil.copyfileobj(src, dst)
        count += 1
    return count


# ---------------------------------------------------------------------------
# 上下文
# ---------------------------------------------------------------------------

class Ctx:
    def __init__(self, workdir, as_json=False):
        if workdir:
            p = Path(workdir).expanduser()
        elif os.environ.get("TEFM_WORKDIR"):
            p = Path(os.environ["TEFM_WORKDIR"]).expanduser()
        else:
            local = Path.cwd() / "tefm.json"
            # 只有看起来是完整配置（含 game_dir）才把当前目录当成工作目录，
            # 避免某次 `-w .` 只写了 kernel_label 的残留文件把路径“劫持”。
            if _is_complete_config(local):
                p = Path.cwd()
            else:
                if local.is_file():
                    warn(f"{local} 不完整（缺少 game_dir），已忽略；"
                         f"如需以当前目录为工作目录，运行 tefm init --local --game <目录>")
                p = DEFAULT_WORKDIR
        self.workdir = p.resolve()
        self.as_json = as_json
        self.config_path = self.workdir / "tefm.json"

    def set_workdir(self, path):
        self.workdir = Path(path).expanduser().resolve()
        self.config_path = self.workdir / "tefm.json"

    # ---- 配置 ----
    @property
    def config(self):
        return read_json(self.config_path, {}) or {}

    def save_config(self, cfg):
        write_json(self.config_path, cfg)

    @property
    def game_dir(self):
        g = self.config.get("game_dir")
        if not g:
            raise TeFmError(
                f"当前工作目录 {self.workdir} 还没有配置游戏目录（{self.config_path}）。\n"
                f"先执行: tefm -w {self.workdir} init --game /path/to/game\n"
                f"如果之前用了别的 -w，请对后续命令带上同样的参数，"
                f"或 export TEFM_WORKDIR=<那个目录>")
        return Path(g)

    @property
    def arch(self):
        return self.config.get("arch") or norm_arch()

    # ---- 内核 ----
    @property
    def kernel_dir(self):
        return self.workdir / "tefkernel"

    # ---- 包缓存（既可保留多个版本的 zip，也用于切换） ----
    @property
    def packages_dir(self):
        return self.workdir / "packages"

    def cache_dir(self, kind):
        return self.packages_dir / kind

    def cache_entries(self, kind):
        """返回 [(label, path)]，kind 为 tefkernel / tefloader。"""
        d = self.cache_dir(kind)
        if not d.is_dir():
            return []
        suffixes = {".zip"}
        if kind == "tefkernel":
            # 允许缓存裸内核库，方便直接装构建产物
            suffixes |= {".so", ".dll", ".dylib"}
        out = [(p.stem, p) for p in d.iterdir()
               if p.is_file() and p.suffix.lower() in suffixes]
        if kind == "tefkernel":
            out.sort(key=lambda it: version_key(it[0]))
        else:
            out.sort(key=lambda it: it[1].stat().st_mtime)
        return out

    def cache_path(self, kind, label, ext=".zip"):
        return self.cache_dir(kind) / f"{label}{ext}"

    def ensure_workdir(self):
        try:
            self.workdir.mkdir(parents=True, exist_ok=True)
        except PermissionError:
            raise TeFmError(
                f"没有权限创建 {self.workdir}。若放在 /srv 等系统目录，请用 sudo，"
                f"或改用有权限的目录：-w ~/tefm")

    def active_label(self, kind):
        key = "kernel_label" if kind == "tefkernel" else "loader_label"
        return self.config.get(key)

    def set_active_label(self, kind, label):
        cfg = self.config
        cfg["kernel_label" if kind == "tefkernel" else "loader_label"] = label
        self.save_config(cfg)

    def resolve_cached(self, kind, spec):
        """spec 支持 latest / 版本号 / 标签 / zip 路径。"""
        p = Path(spec).expanduser()
        if p.is_file():
            return p, None
        entries = self.cache_entries(kind)
        if not entries:
            raise TeFmError(f"没有缓存的 {kind} 包，先用 install 添加一个")
        if spec in ("latest", "newest"):
            return entries[-1][1], entries[-1][0]
        for label, path in entries:
            if label == spec:
                return path, label
        raise TeFmError(
            f"找不到 {kind} 版本 {spec!r}；可用: " + ", ".join(l for l, _ in entries))

    def kernel_version(self):
        # 内核包里的 info.json 优先；GUI 版的旧工作目录只把版本记在 app_config.json
        j = read_json(self.kernel_dir / "info.json", {}) or {}
        v = j.get("version")
        if v:
            return v
        app = read_json(self.workdir / "app_config.json", {}) or {}
        return app.get("kernelVersion")

    def kernel_lib(self):
        d = self.kernel_dir
        if not d.is_dir():
            return None
        o, a, ext = os_name(), self.arch, lib_suffix()
        candidates = [
            f"libtefkernel.{o}.{a}{ext}",
            f"tefkernel.{o}.{a}{ext}",
        ]
        for name in candidates:
            p = d / name
            if p.is_file():
                return p
        # 兜底：名称里同时包含 os / arch / 扩展名
        for p in sorted(d.iterdir()):
            n = p.name.lower()
            if p.is_file() and n.endswith(ext.lower()) and "tefkernel" in n \
                    and o in n and a in n:
                return p
        return None

    # ---- 装载器 ----
    @property
    def tefloader_zip(self):
        return self.kernel_dir / "tefloader.zip"

    def loader_bin(self):
        return self.game_dir / f"tefloader.bin.{self.arch}"

    # ---- 附加包 ----
    def addon_root(self, addon_type, parent_loader=None):
        if addon_type == "mod":
            if not parent_loader:
                raise TeFmError("Mod 缺少 parentLoader，无法定位 mods/<loader> 目录")
            return self.workdir / "mods" / parent_loader
        root = TYPE_ROOTS.get(addon_type)
        if not root:
            raise TeFmError(f"未知附加包类型: {addon_type}")
        return self.workdir / root

    def find_addon(self, pkg_id):
        """在四类附加包中查找 pkg_id，返回 (type, parent_loader, root, 文件路径)。"""
        for t in ("modloader", "module", "plugin"):
            root = self.addon_root(t)
            p = root / "pkg" / f"{pkg_id}.tefpkg"
            if p.is_file():
                return t, None, root, p
        mods_dir = self.workdir / "mods"
        if mods_dir.is_dir():
            for loader in sorted(mods_dir.iterdir()):
                p = loader / "mod" / pkg_id
                if p.is_file():
                    return "mod", loader.name, loader, p
        return None


# ---------------------------------------------------------------------------
# enables.txt
# ---------------------------------------------------------------------------

def enabled_ids(root):
    f = Path(root) / "enables.txt"
    if not f.is_file():
        return []
    ids = []
    for line in f.read_text(encoding="utf-8", errors="replace").splitlines():
        s = line.strip()
        if s and not s.startswith("#"):
            ids.append(s)
    return ids


def set_enabled(root, pkg_id, value):
    root = Path(root)
    root.mkdir(parents=True, exist_ok=True)
    f = root / "enables.txt"
    ids = enabled_ids(root)
    if value:
        if pkg_id in ids:
            return False
        ids.append(pkg_id)
    else:
        if pkg_id not in ids:
            return False
        ids = [i for i in ids if i != pkg_id]
    f.write_text("".join(f"{i}\n" for i in ids), encoding="utf-8")
    return True


# ---------------------------------------------------------------------------
# 安装逻辑
# ---------------------------------------------------------------------------

def check_kernel_min(ctx, manifest, force=False):
    req = None
    tk = manifest.get("tefkernel")
    if isinstance(tk, dict):
        req = tk.get("minVersion")
    req = req or manifest.get("tefkernelMinVersion")
    if not req:
        return
    if force:
        warn(f"已跳过内核版本检查（需要 >= {req}）")
        return
    cur = ctx.kernel_version()
    if cur is None:
        warn(f"未检测到已安装内核版本，跳过最低版本检查（需要 >= {req}）")
        return
    # 裸内核库（local-x86_64 等）没有可靠的版本号，一律放行
    if str(cur).lower().startswith("local"):
        warn(f"内核版本为 {cur!r}（本地构建），跳过最低版本检查（需要 >= {req}）")
        return
    if version_lt(cur, req):
        raise TeFmError(
            f"内核版本过低：当前 {cur}，要求 >= {req}（确认无误可加 --force 强制安装）")


def install_zip(ctx, zip_path, auto_enable=True, _depth=0, _seen=None, force=False):
    """安装一个附加包 zip（Manifest.json 在根目录）。返回安装结果列表。"""
    zip_path = Path(zip_path)
    if not zip_path.is_file():
        raise TeFmError(f"文件不存在: {zip_path}")
    if _depth > 8:
        raise TeFmError("依赖嵌套过深，已中止")
    _seen = _seen or set()

    try:
        zf = zipfile.ZipFile(zip_path)
    except zipfile.BadZipFile as e:
        raise TeFmError(f"不是有效的 zip 包: {zip_path} ({e})")

    with zf:
        names = set(zf.namelist())
        if "Manifest.json" not in names:
            raise TeFmError(f"{zip_path} 缺少 Manifest.json")
        manifest = json.loads(zf.read("Manifest.json").decode("utf-8-sig"))
        t = str(manifest.get("type", "")).lower()
        if t not in TYPE_ROOTS:
            raise TeFmError(f"未知附加包类型: {manifest.get('type')!r}")

        check_kernel_min(ctx, manifest, force=force)

        results = []

        # ---- inline 依赖：先装依赖，再装本体 ----
        ml_dep = manifest.get("modloader")
        if isinstance(ml_dep, dict) and str(ml_dep.get("type", "")).lower() == "inline":
            f = ml_dep.get("file")
            if not f:
                raise TeFmError("inline modloader 缺少 file 字段")
            results += _install_inline_zip(ctx, zf, f, auto_enable, _depth, _seen, force)

        pl_dep = manifest.get("plugins")
        if isinstance(pl_dep, dict) and str(pl_dep.get("type", "")).lower() == "inline":
            for f in pl_dep.get("files") or []:
                results += _install_inline_zip(ctx, zf, f, auto_enable, _depth, _seen, force)

        # ---- 读 Info.json ----
        if "Info.json" not in names:
            raise TeFmError(f"{zip_path} 缺少 Info.json")
        info_json = json.loads(zf.read("Info.json").decode("utf-8-sig"))
        pkg_id = info_json.get("pkgId")
        if not pkg_id:
            raise TeFmError("Info.json 缺少 pkgId")

        parent_loader = None
        if t == "mod":
            parent_loader = manifest.get("parentLoader")
            if not parent_loader:
                raise TeFmError("Mod 的 Manifest.json 缺少 parentLoader")
            if not (ctx.workdir / "modloader" / "pkg" / f"{parent_loader}.tefpkg").is_file():
                warn(f"父级 ModLoader {parent_loader} 尚未安装，安装后需要再装它")

        root = ctx.addon_root(t, parent_loader)
        root.mkdir(parents=True, exist_ok=True)

        # ---- 清单文件本体 ----
        src_entry = manifest.get("file")
        if not src_entry:
            raise TeFmError("Manifest.json 缺少 file 字段")
        if src_entry not in names:
            raise TeFmError(f"包内缺少清单文件: {src_entry}")

        if t == "mod":
            dst = root / "mod" / pkg_id
        else:
            dst = root / "pkg" / f"{pkg_id}.tefpkg"
        dst.parent.mkdir(parents=True, exist_ok=True)
        with zf.open(src_entry) as src, open(dst, "wb") as out:
            shutil.copyfileobj(src, out)

        # ---- 图标 ----
        icon_file = manifest.get("iconFile")
        if icon_file and icon_file in names:
            icon_dst = root / "icons" / f"{pkg_id}.icon"
            icon_dst.parent.mkdir(parents=True, exist_ok=True)
            with zf.open(icon_file) as src, open(icon_dst, "wb") as out:
                shutil.copyfileobj(src, out)

        # ---- 资源 ----
        res = manifest.get("resources")
        if res:
            private = root / "private" / pkg_id
            private.mkdir(parents=True, exist_ok=True)
            n = zip_extract_prefix(zf, res, private, strip_prefix=True)
            if n == 0:
                warn(f"{pkg_id}: resources 目录 {res} 中没有文件")

        # ---- 启用 ----
        if auto_enable:
            set_enabled(root, pkg_id, True)

        results.append({
            "pkg_id": pkg_id,
            "type": t,
            "type_name": ADDON_TYPE_NAMES[t],
            "version": info_json.get("version"),
            "version_code": info_json.get("versionCode"),
            "name": info_json.get("name"),
            "parent_loader": parent_loader,
            "path": str(dst),
        })
        return results


def _install_inline_zip(ctx, outer_zf, entry, auto_enable, depth, seen, force=False):
    if entry not in outer_zf.namelist():
        raise TeFmError(f"inline 依赖缺少内嵌文件: {entry}")
    data = outer_zf.read(entry)
    key = hash(data)
    if key in seen:
        return []
    seen.add(key)
    with tempfile.NamedTemporaryFile(suffix=".zip", delete=False) as tmp:
        tmp.write(data)
        tmp_path = tmp.name
    try:
        return install_zip(ctx, tmp_path, auto_enable, depth + 1, seen, force)
    finally:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass


# ---------------------------------------------------------------------------
# 命令实现
# ---------------------------------------------------------------------------

def cmd_init(ctx, args):
    game = Path(args.game).expanduser().resolve()
    if not game.is_dir():
        raise TeFmError(f"游戏目录不存在: {game}")
    arch = args.arch or ctx.config.get("arch") or norm_arch()

    if args.local:
        ctx.set_workdir(Path.cwd())

    # 在“指定目录”初始化工作路径：这里会创建 workdir / packages / tefkernel 骨架。
    ctx.ensure_workdir()
    ctx.kernel_dir.mkdir(parents=True, exist_ok=True)
    ctx.packages_dir.mkdir(parents=True, exist_ok=True)

    cfg = ctx.config
    cfg["game_dir"] = str(game)
    cfg["arch"] = arch
    ctx.save_config(cfg)

    info(f"工作目录 : {ctx.workdir}")
    info(f"游戏目录 : {game}")
    info(f"架构     : {arch}")

    bins = sorted(p.name for p in game.glob("*.bin.*"))
    if bins:
        info(f"发现启动器: {', '.join(bins)}")
        if not any(b in (f"Terraria.bin.{arch}", f"TerrariaServer.bin.{arch}") for b in bins):
            warn(f"没有 {arch} 架构的 Terraria/TerrariaServer 启动器；可用 --arch 指定")
    else:
        warn("游戏目录里没有 *.bin.* 启动器")

    if (game / "TerrariaServer.exe").is_file() or (game / "TerrariaServer").is_file():
        info("检测到服务端可执行文件（TerrariaServer）")

    # 顺带选定内核 / TEFLoader（路径或缓存里的版本标签都可以）
    if args.kernel:
        if Path(args.kernel).expanduser().is_file():
            r = install_kernel_package(ctx, args.kernel)
        else:
            path, label = ctx.resolve_cached("tefkernel", args.kernel)
            r = install_kernel_package(ctx, path, label)
        info(f"内核已选定: {r['label']}")
        if not ctx.kernel_lib():
            warn(f"当前平台 ({os_name()}/{arch}) 没有匹配的内核库")

    if args.loader:
        if Path(args.loader).expanduser().is_file():
            r = install_loader_zip(ctx, args.loader)
        else:
            path, label = ctx.resolve_cached("tefloader", args.loader)
            r = install_loader_zip(ctx, path, label)
        info(f"TEFLoader 已选定: {r['label']}")

    if args.patch:
        cmd_patch(ctx, argparse.Namespace(force=True))

    info("")
    info(f"配置已保存: {ctx.config_path}")
    if Path(ctx.workdir) != DEFAULT_WORKDIR.resolve() and not args.local:
        info(f"后续命令需要带 -w {ctx.workdir}，或先 export TEFM_WORKDIR={ctx.workdir}")
    if args.local:
        info(f"以后在该目录直接运行 tefm 即可（会自动读取 ./tefm.json）")
    info("下一步: tefm status / tefm doctor / tefm run --server")
    return 0


def cmd_where(ctx, args):
    data = {
        "workdir": str(ctx.workdir),
        "config": str(ctx.config_path),
        "config_exists": ctx.config_path.is_file(),
        "game_dir": ctx.config.get("game_dir"),
    }
    if ctx.as_json:
        print(json.dumps(data, ensure_ascii=False, indent=2))
    else:
        info(f"workdir : {data['workdir']}")
        info(f"config  : {data['config']} ({'存在' if data['config_exists'] else '不存在'})")
        info(f"game_dir: {data['game_dir'] or '(未配置)'}")
    return 0


def cmd_status(ctx, args):
    cfg = ctx.config
    data = {
        "workdir": str(ctx.workdir),
        "game_dir": cfg.get("game_dir"),
        "arch": ctx.arch,
        "kernel_version": ctx.kernel_version(),
        "kernel_label": ctx.active_label("tefkernel"),
        "loader_label": ctx.active_label("tefloader"),
        "kernel_cache": [l for l, _ in ctx.cache_entries("tefkernel")],
        "loader_cache": [l for l, _ in ctx.cache_entries("tefloader")],
        "kernel_lib": str(ctx.kernel_lib()) if ctx.kernel_lib() else None,
        "tefloader_zip": ctx.tefloader_zip.is_file(),
    }
    if cfg.get("game_dir"):
        try:
            data["tefloader_bin"] = ctx.loader_bin().is_file()
            data["server_exe"] = (ctx.game_dir / "TerrariaServer.exe").is_file()
        except TeFmError:
            pass

    addons = []
    for t in ("modloader", "module", "plugin"):
        root = ctx.addon_root(t)
        if not root.is_dir():
            continue
        for p in sorted((root / "pkg").glob("*.tefpkg")) if (root / "pkg").is_dir() else []:
            pid = p.stem
            addons.append({
                "pkg_id": pid, "type": t, "type_name": ADDON_TYPE_NAMES[t],
                "enabled": pid in enabled_ids(root), "path": str(p),
            })
    mods_dir = ctx.workdir / "mods"
    if mods_dir.is_dir():
        for loader in sorted(mods_dir.iterdir()):
            if not loader.is_dir():
                continue
            en = enabled_ids(loader)
            mdir = loader / "mod"
            if not mdir.is_dir():
                continue
            for p in sorted(mdir.iterdir()):
                if not p.is_file():
                    continue
                try:
                    meta = read_json(p, {}) or {}
                except TeFmError:
                    meta = {}
                addons.append({
                    "pkg_id": p.name, "type": "mod", "type_name": "Mod",
                    "parent_loader": loader.name,
                    "enabled": p.name in en,
                    "version": meta.get("version"),
                    "version_code": meta.get("version_code", meta.get("versionCode")),
                    "path": str(p),
                })
    data["addons"] = addons

    if ctx.as_json:
        print(json.dumps(data, ensure_ascii=False, indent=2))
        return 0

    info(f"workdir      : {data['workdir']}")
    info(f"game_dir     : {data['game_dir'] or '(未配置)'}")
    info(f"arch         : {data['arch']}")
    kver_display = data['kernel_version'] or ('(未记录版本)' if data['kernel_lib'] else '(未安装)')
    klabel = data['kernel_label'] or ('未记录标签' if data['kernel_lib'] else '-')
    info(f"kernel       : {kver_display} [选用 {klabel}；缓存 {len(data['kernel_cache'])} 个]")
    info(f"kernel lib   : {data['kernel_lib'] or '(未找到)'}")
    llabel = data['loader_label'] or ('未记录标签' if data['tefloader_zip'] else '(未安装)')
    info(f"tefloader    : {llabel} [缓存 {len(data['loader_cache'])} 个]")
    info(f"tefloader.zip: {'有' if data['tefloader_zip'] else '无'}")
    if data.get("game_dir"):
        info(f"tefloader bin: {'有' if data.get('tefloader_bin') else '未 patch'}")
        info(f"server exe   : {'有' if data.get('server_exe') else '无'}")
    if addons:
        info("")
        info(f"{'状态':<4} {'类型':<10} {'包 ID':<34} 版本")
        for a in addons:
            mark = "✓" if a["enabled"] else "-"
            ver = a.get("version") or ""
            extra = f" [{a['parent_loader']}]" if a.get("parent_loader") else ""
            info(f"{mark:<4} {a['type_name']:<10} {a['pkg_id']:<34} {ver}{extra}")
    else:
        info("(没有安装任何附加包)")
    return 0


def _zip_kernel_info(zip_path):
    with zipfile.ZipFile(zip_path) as zf:
        if "info.json" not in zf.namelist():
            raise TeFmError(f"{zip_path} 缺少 info.json，不是 tefkernel 包")
        return json.loads(zf.read("info.json").decode("utf-8-sig"))


def _derive_label(zip_path, prefix_names):
    """给没有版本元数据的包取一个稳定标签：文件名 -> 去前缀 -> 内容哈希兜底。"""
    import hashlib

    zip_path = Path(zip_path)
    orig = zip_path.stem
    stem = orig
    for prefix in prefix_names:
        if stem.lower().startswith(prefix):
            stem = stem[len(prefix):].lstrip("-_. ")
            break
    stem = re.sub(r"[^0-9A-Za-z._+-]+", "-", stem).strip("-._")
    if stem:
        return stem
    # 文件名就是 tefloader / tefkernel 这种通用名时，用 内容哈希 区分不同版本
    h = hashlib.sha256(zip_path.read_bytes()).hexdigest()[:8]
    return f"{orig}-{h}"


LIB_SUFFIXES = (".so", ".dll", ".dylib")


def _kernel_arch_from_name(name):
    m = re.search(r"\.(linux|windows|macos|android|ios)\."
                  r"(x86_64|x86|arm64|arm64-v8a|armeabi-v7a|arm)\.", name)
    if not m:
        return None, None
    os_part = m.group(1)
    arch = {"arm64-v8a": "arm64", "armeabi-v7a": "arm"}.get(m.group(2), m.group(2))
    return os_part, arch


def install_kernel_zip(ctx, zip_path, label=None):
    zip_path = Path(zip_path).expanduser().resolve()
    if not zip_path.is_file():
        raise TeFmError(f"文件不存在: {zip_path}")
    meta = _zip_kernel_info(zip_path)
    ver = meta.get("version") or meta.get("versionCode") or _derive_label(zip_path, ["tefkernel"])
    label = label or str(ver)

    cache = ctx.cache_path("tefkernel", label)
    cache.parent.mkdir(parents=True, exist_ok=True)
    if cache.resolve() != zip_path:
        shutil.copy2(zip_path, cache)

    ctx.kernel_dir.mkdir(parents=True, exist_ok=True)
    picked = 0
    with zipfile.ZipFile(cache) as zf:
        for n in zf.namelist():
            m = safe_member(n)
            if m is None or n.endswith("/"):
                continue
            if m == "info.json" or m.startswith("libtefkernel.") or m.startswith("tefkernel."):
                out = ctx.kernel_dir / m
                out.parent.mkdir(parents=True, exist_ok=True)
                with zf.open(n) as src, open(out, "wb") as dst:
                    shutil.copyfileobj(src, dst)
                picked += 1
    if picked == 0:
        raise TeFmError("内核包里没有可安装的文件")

    ctx.set_active_label("tefkernel", label)
    return {"label": label, "version": meta.get("version"), "version_code": meta.get("versionCode"),
            "cache": str(cache)}


def install_kernel_lib(ctx, lib_path, label=None):
    """直接安装一个裸内核动态库（构建产物），不要求 zip。"""
    lib_path = Path(lib_path).expanduser().resolve()
    if not lib_path.is_file():
        raise TeFmError(f"文件不存在: {lib_path}")
    if lib_path.suffix.lower() not in LIB_SUFFIXES:
        raise TeFmError(f"不认识的内核文件: {lib_path.name}（支持 .zip / .so / .dll / .dylib）")

    # 版本：优先同目录 info.json / --label，否则用 local-<arch> 作为标签
    meta = read_json(lib_path.parent / "info.json", {}) or {}
    lib_os, lib_arch = _kernel_arch_from_name(lib_path.name)
    label = label or meta.get("version") or (f"local-{lib_arch}" if lib_arch else "local")

    cache = ctx.cache_path("tefkernel", label, lib_path.suffix.lower())
    cache.parent.mkdir(parents=True, exist_ok=True)
    if cache.resolve() != lib_path:
        shutil.copy2(lib_path, cache)

    # 文件名里能解析出平台/架构时，顺手清掉旧的同平台库，避免 kernel_lib() 选到旧文件
    if lib_os and lib_arch:
        if lib_os != os_name() or lib_arch != ctx.arch:
            warn(f"这个库是 {lib_os}/{lib_arch}，当前机器是 {os_name()}/{ctx.arch}，"
                 f"启动时可能找不到匹配的内核")
        ctx.kernel_dir.mkdir(parents=True, exist_ok=True)
        for p in ctx.kernel_dir.iterdir():
            if p.is_file() and p.suffix.lower() in LIB_SUFFIXES and "tefkernel" in p.name.lower():
                p_os, p_arch = _kernel_arch_from_name(p.name)
                if p_os == lib_os and p_arch == lib_arch:
                    p.unlink(missing_ok=True)

    ctx.kernel_dir.mkdir(parents=True, exist_ok=True)
    dst = ctx.kernel_dir / lib_path.name
    shutil.copy2(cache, dst)

    # 维护 info.json，保证 status / version 显示的是当前选中的内核
    info_path = ctx.kernel_dir / "info.json"
    existing = read_json(info_path, {}) or {}
    existing["version"] = meta.get("version") or label
    existing.setdefault("versionCode", meta.get("versionCode", 0))
    existing["source"] = lib_path.name
    write_json(info_path, existing)

    ctx.set_active_label("tefkernel", label)
    return {"label": label, "version": meta.get("version") or label,
            "version_code": meta.get("versionCode"), "cache": str(cache),
            "lib": dst.name, "arch": lib_arch, "os": lib_os}


def install_kernel_package(ctx, path, label=None):
    """zip 或裸库都接受。"""
    p = Path(path).expanduser()
    if zipfile.is_zipfile(p):
        return install_kernel_zip(ctx, p, label)
    return install_kernel_lib(ctx, p, label)


def install_loader_zip(ctx, zip_path, label=None):
    zip_path = Path(zip_path).expanduser().resolve()
    if not zip_path.is_file():
        raise TeFmError(f"文件不存在: {zip_path}")
    # tefloader.zip 当前没有版本元数据；优先用包内 info.json，其次文件名，最后哈希。
    ver = None
    try:
        with zipfile.ZipFile(zip_path) as zf:
            for n in ("info.json", "Info.json"):
                if n in zf.namelist():
                    j = json.loads(zf.read(n).decode("utf-8-sig"))
                    ver = j.get("version") or j.get("versionCode")
                    break
    except zipfile.BadZipFile as e:
        raise TeFmError(f"不是有效的 zip 包: {zip_path} ({e})")
    label = label or (str(ver) if ver else _derive_label(zip_path, ["tefloader"]))

    cache = ctx.cache_path("tefloader", label)
    cache.parent.mkdir(parents=True, exist_ok=True)
    if cache.resolve() != zip_path:
        shutil.copy2(zip_path, cache)

    ctx.kernel_dir.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(cache, ctx.tefloader_zip)
    ctx.set_active_label("tefloader", label)

    extracted = None
    if ctx.config.get("game_dir"):
        extracted = extract_loader_runtime(ctx, verbose=True)
    return {"label": label, "version": ver, "cache": str(cache), "extracted": extracted}


def cmd_kernel(ctx, args):
    sub = args.kernel_cmd

    if sub == "install":
        r = install_kernel_package(ctx, args.zip, args.label)
        if ctx.as_json:
            print(json.dumps(r, ensure_ascii=False))
        else:
            info(f"内核已安装: {r['label']} -> {ctx.kernel_dir}")
            lib = ctx.kernel_lib()
            if not lib:
                warn(f"当前平台 ({os_name()}/{ctx.arch}) 没有匹配的内核库，检查内核包内容")
            else:
                info(f"匹配内核库: {lib.name}")
        return 0

    if sub == "list":
        entries = ctx.cache_entries("tefkernel")
        active = ctx.active_label("tefkernel") or ctx.kernel_version()
        rows = [{"label": l, "path": str(p), "active": l == active} for l, p in entries]
        if ctx.as_json:
            print(json.dumps({"active": active, "packages": rows}, ensure_ascii=False, indent=2))
        else:
            if not rows:
                info("(没有缓存的内核包)")
            for r in rows:
                info(f"{'*' if r['active'] else ' '} {r['label']:<16} {r['path']}")
        return 0

    if sub == "use":
        path, label = ctx.resolve_cached("tefkernel", args.label)
        r = install_kernel_package(ctx, path, label)
        info(f"已切换到内核 {r['label']}")
        lib = ctx.kernel_lib()
        if not lib:
            warn(f"当前平台 ({os_name()}/{ctx.arch}) 没有匹配的内核库")
        return 0

    if sub == "version":
        v = ctx.kernel_version()
        if ctx.as_json:
            print(json.dumps({"version": v, "label": ctx.active_label("tefkernel")}))
        else:
            info(v or "(未安装)")
        return 0

    raise TeFmError("用法: tefm kernel install <zip> [--label L] | list | use <label|latest> | version")


def cmd_loader(ctx, args):
    sub = args.loader_cmd

    if sub == "install":
        r = install_loader_zip(ctx, args.zip, args.label)
        if ctx.as_json:
            print(json.dumps(r, ensure_ascii=False))
        else:
            info(f"tefloader 已安装: {r['label']} -> {ctx.tefloader_zip}")
            if r["extracted"] is None:
                info("尚未配置游戏目录，稍后执行 tefm init / tefm patch 时会自动解压")
            else:
                info(f"已解压 {r['extracted']} 个运行时文件到游戏目录")
        return 0

    if sub == "list":
        entries = ctx.cache_entries("tefloader")
        active = ctx.active_label("tefloader")
        rows = [{"label": l, "path": str(p), "active": l == active} for l, p in entries]
        if ctx.as_json:
            print(json.dumps({"active": active, "packages": rows}, ensure_ascii=False, indent=2))
        else:
            if not rows:
                info("(没有缓存的 tefloader 包)")
            for r in rows:
                info(f"{'*' if r['active'] else ' '} {r['label']:<16} {r['path']}")
        return 0

    if sub == "use":
        path, label = ctx.resolve_cached("tefloader", args.label)
        r = install_loader_zip(ctx, path, label)
        info(f"已切换到 tefloader {r['label']}")
        if r["extracted"] is not None:
            info(f"已解压 {r['extracted']} 个运行时文件到游戏目录")
        return 0

    if sub == "runtime":
        n = extract_loader_runtime(ctx, verbose=True)
        info(f"已解压 {n} 个运行时文件到游戏目录")
        return 0

    raise TeFmError("用法: tefm loader install <zip> [--label L] | list | use <label|latest> | runtime")


def loader_runtime_prefix(ctx):
    if is_windows() and ctx.arch == "x86":
        return "net452"
    return "net472"


def extract_loader_runtime(ctx, verbose=False):
    z = ctx.tefloader_zip
    if not z.is_file():
        raise TeFmError(f"没有 tefloader.zip：{z}（先执行 tefm loader install <zip>）")
    game = ctx.game_dir
    prefix = loader_runtime_prefix(ctx)
    with zipfile.ZipFile(z) as zf:
        n = zip_extract_prefix(zf, prefix, game, strip_prefix=True)
    if n == 0:
        warn(f"tefloader.zip 里没有 {prefix}/ 目录")
    if verbose:
        info(f"tefloader 运行时: {prefix}/ -> {game}")
    return n


def cmd_patch(ctx, args):
    game = ctx.game_dir
    arch = ctx.arch
    # 客户端是 Terraria.bin.<arch>，官方 Linux 专用服是 TerrariaServer.bin.<arch>。
    candidates = [
        game / f"Terraria.bin.{arch}",
        game / f"TerrariaServer.bin.{arch}",
    ]
    src = next((p for p in candidates if p.is_file()), None)
    if src is None:
        cands = sorted(list(game.glob("Terraria.bin.*")) + list(game.glob("TerrariaServer.bin.*")))
        if len(cands) == 1 and args.force:
            src = cands[0]
            arch = src.name.split(".bin.", 1)[1]
            warn(f"使用唯一的启动器 {src.name}（架构 {arch}）")
        else:
            raise TeFmError(
                f"找不到 Terraria.bin.{arch} 或 TerrariaServer.bin.{arch}；"
                f"可用的有: {', '.join(p.name for p in cands) or '无'}。"
                f"用 --arch 指定或 --force 自动选择")
    dst = game / f"tefloader.bin.{arch}"
    if dst.exists() and not args.force:
        info(f"已存在: {dst.name}（--force 可覆盖）")
    else:
        shutil.copy2(src, dst)
        st = dst.stat()
        dst.chmod(st.st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
        info(f"已生成: {dst.name}")

    # tefloader 运行时（如果本地已经有 zip）
    if ctx.tefloader_zip.is_file():
        extract_loader_runtime(ctx, verbose=True)
    else:
        warn("还没有 tefloader.zip，服务端启动前需要 tefm loader install <zip>")

    cfg = ctx.config
    if cfg.get("arch") != arch:
        cfg["arch"] = arch
        ctx.save_config(cfg)
    return 0


def cmd_addon(ctx, args):
    sub = args.addon_cmd

    if sub == "install":
        results = []
        for z in args.zips:
            results += install_zip(ctx, z, auto_enable=not args.no_enable, force=args.force)
        if ctx.as_json:
            print(json.dumps(results, ensure_ascii=False, indent=2))
        else:
            for r in results:
                extra = f" (父级 {r['parent_loader']})" if r.get("parent_loader") else ""
                info(f"已安装 {r['type_name']}: {r['pkg_id']} {r.get('version') or ''}{extra}")
        return 0

    if sub == "list":
        return cmd_status(ctx, argparse.Namespace())

    found = ctx.find_addon(args.pkg_id)
    if not found:
        raise TeFmError(f"找不到附加包: {args.pkg_id}")
    t, parent, root, path = found

    if sub == "enable":
        changed = set_enabled(root, args.pkg_id, True)
        info(f"{'已启用' if changed else '已经是启用状态'}: {args.pkg_id}")
    elif sub == "disable":
        changed = set_enabled(root, args.pkg_id, False)
        info(f"{'已禁用' if changed else '已经是禁用状态'}: {args.pkg_id}")
    elif sub == "remove":
        if t == "mod":
            shutil.rmtree(root / "private" / args.pkg_id, ignore_errors=True)
            path.unlink(missing_ok=True)
        else:
            path.unlink(missing_ok=True)
            shutil.rmtree(root / "private" / args.pkg_id, ignore_errors=True)
            (root / "icons" / f"{args.pkg_id}.icon").unlink(missing_ok=True)
            # 删掉 ModLoader 时，连同它下面的 mods 目录一起清理（与 GUI 行为一致）
            if t == "modloader":
                shutil.rmtree(ctx.workdir / "mods" / args.pkg_id, ignore_errors=True)
        set_enabled(root, args.pkg_id, False)
        info(f"已删除: {args.pkg_id}")
    else:
        raise TeFmError(f"未知 addon 子命令: {sub}")
    return 0


def cmd_config(ctx, args):
    found = ctx.find_addon(args.pkg_id)
    if not found:
        raise TeFmError(f"找不到附加包: {args.pkg_id}")
    t, parent, root, path = found
    cfg_path = root / "private" / args.pkg_id / "config.json"
    cfg = read_json(cfg_path, {}) or {}

    def lookup(key):
        if isinstance(cfg.get("values"), dict) and key in cfg["values"]:
            return cfg["values"][key]
        return cfg.get(key)

    if args.config_cmd == "get":
        v = lookup(args.key)
        if v is None:
            raise TeFmError(f"{args.pkg_id} 没有配置项: {args.key}")
        if ctx.as_json:
            print(json.dumps({args.key: v}, ensure_ascii=False))
        else:
            print(v if not isinstance(v, (dict, list)) else json.dumps(v, ensure_ascii=False))
        return 0

    if args.config_cmd == "set":
        raw = args.value
        try:
            val = json.loads(raw)
        except json.JSONDecodeError:
            val = raw
        if isinstance(cfg.get("values"), dict):
            cfg["values"][args.key] = val
        else:
            cfg.setdefault("values", {})[args.key] = val
        cfg.setdefault("schemaVersion", 1)
        write_json(cfg_path, cfg)
        info(f"{args.pkg_id}.{args.key} = {json.dumps(val, ensure_ascii=False)}")
        return 0

    if args.config_cmd == "list":
        if isinstance(cfg.get("values"), dict):
            values = cfg["values"]
        else:
            values = {k: v for k, v in cfg.items() if k != "schemaVersion"}
        for k, v in values.items():
            print(f"{k} = {json.dumps(v, ensure_ascii=False)}")
        return 0

    raise TeFmError("用法: tefm config get|set|list ...")


def cmd_run(ctx, args):
    game = ctx.game_dir
    arch = ctx.arch
    loader = game / f"tefloader.bin.{arch}"
    if not loader.is_file():
        raise TeFmError(f"找不到 {loader.name}，先执行 tefm patch")

    kernel = ctx.kernel_lib()
    if not kernel:
        raise TeFmError(f"找不到当前平台内核库（{os_name()}/{arch}），先执行 tefm kernel install")

    if args.server:
        server = game / "TerrariaServer.exe"
        if not server.is_file():
            server = game / "TerrariaServer"
        if not server.is_file():
            raise TeFmError("游戏目录里没有 TerrariaServer，无法以服务端启动")
        target = server
    else:
        target = game / "Terraria.exe"
        if not target.is_file():
            target = game / "Terraria"
        if not target.is_file():
            raise TeFmError("游戏目录里没有 Terraria，无法启动客户端")

    cmd = [str(loader), "-k", str(kernel), "-w", str(ctx.workdir), "-e", str(target)]
    if args.server:
        cmd.append("-server")
    if args.world:
        cmd += ["-world", str(Path(args.world).expanduser().resolve())]
    cmd += list(args.extra or [])

    if args.dry_run:
        info("cd " + shlex.quote(str(game)))
        info(" ".join(shlex.quote(c) for c in cmd))
        return 0

    info("launch: " + " ".join(shlex.quote(c) for c in cmd))
    sys.stdout.flush()
    sys.stderr.flush()
    # Linux 版官方启动脚本会设置这个，兼容大小写不一致的资源路径。
    os.environ.setdefault("MONO_IOMAP", "all")
    os.chdir(str(game))
    try:
        os.execv(str(loader), cmd)
    except OSError as e:
        raise TeFmError(f"启动失败: {e}")


KERNEL_LOG_RE = re.compile(r"runtime_.*\.log$")


def _run_command_parts(ctx, server=True, world=None, extra=None):
    game = ctx.game_dir
    loader = game / f"tefloader.bin.{ctx.arch}"
    kernel = ctx.kernel_lib()
    if not loader.is_file():
        raise TeFmError(f"找不到 {loader.name}，先执行 tefm patch")
    if not kernel:
        raise TeFmError("找不到当前平台内核库，先执行 tefm kernel install")
    target = game / ("TerrariaServer.exe" if server else "Terraria.exe")
    if not target.is_file():
        target = game / ("TerrariaServer" if server else "Terraria")
    if not target.is_file():
        raise TeFmError("找不到游戏可执行文件")
    cmd = [str(loader), "-k", str(kernel), "-w", str(ctx.workdir), "-e", str(target)]
    if server:
        cmd.append("-server")
    if world:
        cmd += ["-world", str(Path(world).expanduser().resolve())]
    cmd += list(extra or [])
    return game, cmd


def cmd_service(ctx, args):
    game, cmd = _run_command_parts(
        ctx, server=True, world=args.world,
        extra=shlex.split(args.extra) if args.extra else None)

    scope = "user" if args.user else "system"
    wanted = "default.target" if args.user else "multi-user.target"
    unit = f"""[Unit]
Description=TEF Terraria Server ({ctx.workdir})
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory={game}
Environment=MONO_IOMAP=all
ExecStart={" ".join(shlex.quote(c) for c in cmd)}
Restart=on-failure
RestartSec=5
# 首次开服需要选择世界时，先手动跑一次 tefm run --server，或加 --world <路径>
StandardInput=null
StandardOutput=journal
StandardError=journal

[Install]
WantedBy={wanted}
"""

    if not args.output:
        print(unit, end="")
        if not args.output:
            info("")
            info("用法：tefm service --output <路径> [--user] [--world 世界文件]")
            info(f"  系统级: sudo tefm service --output /etc/systemd/system/tef-server.service")
            info(f"  用户级: tefm service --user --output ~/.config/systemd/user/tef-server.service")
            info("  然后: sudo systemctl daemon-reload && sudo systemctl enable --now tef-server")
        return 0

    out = Path(args.output).expanduser()
    out.parent.mkdir(parents=True, exist_ok=True)
    if out.exists() and not args.force:
        raise TeFmError(f"{out} 已存在（--force 覆盖）")
    out.write_text(unit, encoding="utf-8")
    out.chmod(0o644)
    info(f"已写入 {out}（{scope} 服务）")
    if args.user:
        info("启用: systemctl --user daemon-reload && systemctl --user enable --now "
             + out.stem)
    else:
        info("启用: sudo systemctl daemon-reload && sudo systemctl enable --now " + out.stem)
    return 0


def cmd_logs(ctx, args):
    d = ctx.workdir / "logs" / "tefkernel"
    logs = sorted([p for p in d.glob("*.log") if KERNEL_LOG_RE.search(p.name)]) if d.is_dir() else []
    if not logs:
        raise TeFmError(f"没有日志: {d}")
    latest = logs[-1]
    n = args.lines
    with open(latest, "r", encoding="utf-8", errors="replace") as f:
        lines = f.readlines()
    for line in lines[-n:]:
        sys.stdout.write(line)
    if args.follow:
        with open(latest, "r", encoding="utf-8", errors="replace") as f:
            f.seek(0, os.SEEK_END)
            try:
                while True:
                    line = f.readline()
                    if line:
                        sys.stdout.write(line)
                        sys.stdout.flush()
                    else:
                        time.sleep(0.5)
            except KeyboardInterrupt:
                return 0
    return 0


def cmd_doctor(ctx, args):
    ok = True

    def check(cond, msg):
        nonlocal ok
        if cond:
            info(f"  ✓ {msg}")
        else:
            ok = False
            info(f"  ✗ {msg}")

    info("检查部署状态：")
    try:
        game = ctx.game_dir
        check(game.is_dir(), f"游戏目录存在: {game}")
        orig = any((game / n).is_file() for n in
                   (f"Terraria.bin.{ctx.arch}", f"TerrariaServer.bin.{ctx.arch}"))
        check(orig, f"原版启动器 Terraria.bin/TerrariaServer.bin.{ctx.arch}")
        check(ctx.loader_bin().is_file(), f"已补丁启动器 tefloader.bin.{ctx.arch}")
        if ctx.loader_bin().is_file():
            check(os.access(ctx.loader_bin(), os.X_OK), "tefloader 可执行权限")
        check((game / "TerrariaServer.exe").is_file() or (game / "TerrariaServer").is_file(),
              "服务端可执行文件")
        check((game / "tefloader.exe").is_file() or (game / "tefloader").is_file(),
              "tefloader 运行时已解压")
    except TeFmError as e:
        check(False, str(e))
    kver = ctx.kernel_version()
    check(bool(kver or ctx.kernel_lib()),
          f"内核已安装（{kver or ('未记录版本，但库文件存在' if ctx.kernel_lib() else '无')}）")
    check(ctx.kernel_lib() is not None, f"当前平台内核库（{os_name()}/{ctx.arch}）")

    # tefloader.zip 只用于“解压/切换运行时”。只要游戏目录里的运行时已就绪，
    # 缺这个 zip 也能正常启动，所以不算致命问题。
    if ctx.tefloader_zip.is_file():
        check(True, "tefloader.zip 已保存")
    else:
        runtime_ok = False
        try:
            game = ctx.game_dir
            runtime_ok = (game / "tefloader.exe").is_file() or (game / "tefloader").is_file()
        except TeFmError:
            pass
        if runtime_ok:
            info("  ! tefloader.zip 未保存（运行时已就绪，启动不受影响）")
            default_zip = DEFAULT_WORKDIR / "tefkernel" / "tefloader.zip"
            if default_zip.is_file() and default_zip != ctx.tefloader_zip:
                info(f"    可复用默认目录里的包：tefm -w {ctx.workdir} loader install "
                     f"{default_zip}")
        else:
            check(False, "tefloader.zip 已保存（当前工作目录里没有，游戏目录也没有运行时）")

    any_loader = False
    for t in ("modloader", "module", "plugin"):
        root = ctx.addon_root(t)
        if not root.is_dir():
            continue
        for pid in enabled_ids(root):
            if t == "modloader":
                any_loader = True
            p = root / "pkg" / f"{pid}.tefpkg"
            check(p.is_file(), f"{ADDON_TYPE_NAMES[t]} 已启用且文件存在: {pid}")

    if not any_loader:
        info("  ! 当前工作目录没有启用任何 ModLoader（服务端将以无 Mod 状态启动）")

    if not ok:
        eprint("\n有检查项未通过。")
        return 1
    info("\n全部通过。")
    return 0


# ---------------------------------------------------------------------------
# 参数解析
# ---------------------------------------------------------------------------

def build_parser():
    # 允许把 -w / --json 写在子命令后面（argparse 默认只认子命令之前的全局参数）。
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument("-w", "--workdir", default=argparse.SUPPRESS,
                        help="工作目录（也可写在子命令前）")
    common.add_argument("--json", action="store_true", default=argparse.SUPPRESS,
                        help="以 JSON 输出")

    p = argparse.ArgumentParser(
        prog=APP,
        description="无头 TEFManager：在 SSH / 服务器上管理 TEFKernel、tefloader 与各类附加包。",
        epilog="示例:\n"
               "  tefm init -w /srv/tefm --game '/srv/terraria' \\\n"
               "       --kernel tefkernel-1.4.4.zip --loader tefloader.zip --patch\n"
               "  tefm -w /srv/tefm kernel list\n"
               "  tefm -w /srv/tefm kernel use 1.4.4\n"
               "  tefm -w /srv/tefm addon install LuaLoader.zip VeinMiner.zip\n"
               "  tefm -w /srv/tefm run --server --world /srv/terraria/Worlds/MyWorld.wld",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument("-w", "--workdir", default=None,
                   help=f"工作目录。省略时依次尝试 $TEFM_WORKDIR、当前目录的 tefm.json、"
                        f"默认 {DEFAULT_WORKDIR}")
    p.add_argument("--json", action="store_true", help="以 JSON 输出（便于脚本处理）")
    sub = p.add_subparsers(dest="command", required=True)

    sp = sub.add_parser("init", parents=[common], help="在指定目录初始化工作路径，可同时选定内核与 TEFLoader")
    sp.add_argument("--game", required=True, help="游戏目录（含 TerrariaServer.exe 的那一层）")
    sp.add_argument("--arch", choices=["x86_64", "x86", "arm64", "arm"], help="强制指定架构")
    sp.add_argument("--kernel", help="内核 zip 路径，或缓存里的版本标签（如 1.4.4 / latest）")
    sp.add_argument("--loader", help="tefloader zip 路径，或缓存里的标签（如 1.4.0 / latest）")
    sp.add_argument("--patch", action="store_true", help="初始化后立即生成 tefloader.bin.<arch>")
    sp.add_argument("--local", action="store_true",
                    help="把当前目录作为工作目录（写 ./tefm.json，之后在该目录直接运行 tefm 即可）")
    sp.set_defaults(func=cmd_init)

    sp = sub.add_parser("where", parents=[common], help="显示当前使用的工作目录与配置文件")
    sp.set_defaults(func=cmd_where)

    sp = sub.add_parser("status", parents=[common], help="查看内核 / 附加包 / 启用状态")
    sp.set_defaults(func=cmd_status)

    sp = sub.add_parser("doctor", parents=[common], help="检查部署是否完整")
    sp.set_defaults(func=cmd_doctor)

    sp = sub.add_parser("kernel", parents=[common], help="内核管理（多版本缓存 / 切换）")
    ksub = sp.add_subparsers(dest="kernel_cmd", required=True)
    k = ksub.add_parser("install", help="安装 tefkernel.zip 或裸内核库（.so/.dll/.dylib）并加入缓存")
    k.add_argument("zip")
    k.add_argument("--label", help="自定义版本标签（默认取包内 info.json 的 version）")
    k.set_defaults(func=cmd_kernel)
    k = ksub.add_parser("list", help="列出已缓存的内核版本")
    k.set_defaults(func=cmd_kernel)
    k = ksub.add_parser("use", help="切换到缓存中的某个内核版本")
    k.add_argument("label")
    k.set_defaults(func=cmd_kernel)
    k = ksub.add_parser("version", help="显示当前内核版本")
    k.set_defaults(func=cmd_kernel)

    sp = sub.add_parser("loader", parents=[common], help="tefloader 运行时管理（多版本缓存 / 切换）")
    lsub = sp.add_subparsers(dest="loader_cmd", required=True)
    k = lsub.add_parser("install", help="安装 tefloader.zip 并加入缓存")
    k.add_argument("zip")
    k.add_argument("--label", help="自定义标签（默认取文件名或哈希）")
    k.set_defaults(func=cmd_loader)
    k = lsub.add_parser("list", help="列出已缓存的 tefloader 版本")
    k.set_defaults(func=cmd_loader)
    k = lsub.add_parser("use", help="切换到缓存中的某个 tefloader")
    k.add_argument("label")
    k.set_defaults(func=cmd_loader)
    k = lsub.add_parser("runtime", help="重新解压 tefloader 运行时到游戏目录")
    k.set_defaults(func=cmd_loader)

    sp = sub.add_parser("patch", parents=[common], help="生成 tefloader.bin.<arch> 并解压运行时")
    sp.add_argument("--force", action="store_true", help="覆盖已存在的 tefloader.bin")
    sp.set_defaults(func=cmd_patch)

    sp = sub.add_parser("addon", parents=[common], help="附加包管理（ModLoader / Module / Plugin / Mod）")
    asub = sp.add_subparsers(dest="addon_cmd", required=True)
    a = asub.add_parser("install", help="从 zip 安装")
    a.add_argument("zips", nargs="+")
    a.add_argument("--no-enable", action="store_true", help="安装后不自动启用")
    a.add_argument("--force", action="store_true",
                   help="跳过内核最低版本等兼容性检查（强制安装）")
    a.set_defaults(func=cmd_addon)
    a = asub.add_parser("list", help="列出附加包（同 status）")
    a.set_defaults(func=cmd_addon)
    for name, helptext in (("enable", "启用"), ("disable", "禁用"), ("remove", "删除")):
        a = asub.add_parser(name, help=helptext)
        a.add_argument("pkg_id")
        a.set_defaults(func=cmd_addon)

    sp = sub.add_parser("config", parents=[common], help="读写 Mod / 附加包私有目录里的 config.json")
    csub = sp.add_subparsers(dest="config_cmd", required=True)
    c = csub.add_parser("get")
    c.add_argument("pkg_id")
    c.add_argument("key")
    c.set_defaults(func=cmd_config)
    c = csub.add_parser("set")
    c.add_argument("pkg_id")
    c.add_argument("key")
    c.add_argument("value")
    c.set_defaults(func=cmd_config)
    c = csub.add_parser("list")
    c.add_argument("pkg_id")
    c.set_defaults(func=cmd_config)

    sp = sub.add_parser("run", parents=[common], help="启动游戏 / 专用服（前台运行，适合 systemd / ssh）")
    sp.add_argument("--server", action="store_true", help="以服务端启动 TerrariaServer")
    sp.add_argument("--dry-run", action="store_true", help="只打印命令")
    sp.add_argument("-world", "--world", help="世界文件路径（等价于透传 -world）")
    sp.add_argument("extra", nargs=argparse.REMAINDER, help="透传给游戏的参数")
    sp.set_defaults(func=cmd_run)

    sp = sub.add_parser("service", parents=[common], help="生成 systemd 服务单元（便于长期挂在服务器上）")
    sp.add_argument("--output", "-o", help="写入路径；省略则打印到标准输出")
    sp.add_argument("--user", action="store_true", help="生成用户级服务（WantedBy=default.target）")
    sp.add_argument("--world", help="世界文件路径，写入 ExecStart")
    sp.add_argument("--extra", help="追加到 ExecStart 的参数（字符串，按 shell 规则切分）")
    sp.add_argument("--force", action="store_true", help="覆盖已存在的文件")
    sp.set_defaults(func=cmd_service)

    sp = sub.add_parser("logs", parents=[common], help="查看内核日志")
    sp.add_argument("-n", "--lines", type=int, default=80)
    sp.add_argument("-f", "--follow", action="store_true")
    sp.set_defaults(func=cmd_logs)

    return p


def main(argv=None):
    parser = build_parser()
    args = parser.parse_args(argv)
    ctx = Ctx(args.workdir, as_json=args.json)
    try:
        return args.func(ctx, args) or 0
    except TeFmError as e:
        die(str(e))
    except PermissionError as e:
        die(f"没有权限写入 {e.filename or '目标路径'}。\n"
            f"若工作目录在 /srv 等系统路径，请用 sudo，或换到有权限的目录：-w ~/tefm")
    except OSError as e:
        die(f"文件操作失败: {e}")
    except KeyboardInterrupt:
        return 130


if __name__ == "__main__":
    sys.exit(main())
