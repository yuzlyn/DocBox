#!/usr/bin/env python3
"""安装闸门：只允许「严格更高」的 versionCode 覆盖安装。

为什么要单独写一个脚本，而不是直接 `adb install -r`：
`adb install -r` 允许**同版**重装，只拦降版（且能用 `-d` 强行绕过）。同版重装会让
`lastUpdateTime` 变了、版本号没变，事后分不清手机上跑的是哪一次构建。这里把两件事
一起拦掉：同版不装、降版不装，并把判断依据（APK 与手机上的 versionCode）打出来。

用法：
    python tools/install_apk.py                 # 默认装 release，自动选唯一设备
    python tools/install_apk.py --debug         # 装 debug
    python tools/install_apk.py --serial ebf505b2
    python tools/install_apk.py --check         # 只比较版本，不安装

规则：
- 手机上没装过 → 允许装。
- APK 的 versionCode < 手机上 → 拒绝（降版）。
- 两者相等 → 拒绝（同版重装没有意义，也违反了「每次出包版本号都不同」）。
- 想装上去，就跑 `python tools/bump_version.py` 再重新打包。
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APPLICATION_ID = "com.yuzlyn.mdeditor"
RELEASE_APK = ROOT / "app/build/outputs/apk/release/app-release.apk"
DEBUG_APK = ROOT / "app/build/outputs/apk/debug/app-debug.apk"


def sdk_root() -> Path | None:
    for name in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        value = os.environ.get(name)
        if value:
            return Path(value)
    for candidate in (
        Path.home() / "AppData/Local/Android/Sdk",
        Path.home() / "Library/Android/sdk",
        Path.home() / "Android/Sdk",
    ):
        if candidate.is_dir():
            return candidate
    return None


def tool(name: str) -> str:
    """优先用 SDK 里的可执行文件，找不到就退回 PATH。"""
    root = sdk_root()
    if root:
        for folder in ("platform-tools",):
            for suffix in (".exe", ""):
                candidate = root / folder / f"{name}{suffix}"
                if candidate.is_file():
                    return str(candidate)
    return name


def find_aapt2() -> str:
    root = sdk_root()
    if root:
        build_tools = root / "build-tools"
        if build_tools.is_dir():
            def key(path: Path) -> tuple[int, ...]:
                return tuple(int(part) for part in re.findall(r"\d+", path.parent.name))

            candidates = sorted(
                list(build_tools.glob("*/aapt2.exe")) + list(build_tools.glob("*/aapt2")),
                key=key,
            )
            if candidates:
                return str(candidates[-1])
    return "aapt2"


def run(command: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        command,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )


def apk_version(aapt2: str, apk: Path) -> tuple[int, str]:
    if not apk.is_file():
        sys.exit(f"找不到安装包：{apk}\n先跑 ./gradlew assembleRelease（或 assembleDebug）")
    result = run([aapt2, "dump", "badging", str(apk)])
    if result.returncode != 0:
        sys.exit(f"aapt2 读取失败：{result.stderr.strip()}")
    code = re.search(r"versionCode='(\d+)'", result.stdout)
    name = re.search(r"versionName='([^']*)'", result.stdout)
    if not code:
        sys.exit("APK 里读不到 versionCode")
    return int(code.group(1)), (name.group(1) if name else "")


def pick_device(adb: str, requested: str | None) -> tuple[str, str]:
    if requested:
        return requested, requested
    result = run([adb, "devices"])
    devices: list[tuple[str, str]] = []
    for line in result.stdout.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append((parts[0], parts[0]))
    if not devices:
        state = [line.strip() for line in result.stdout.splitlines()[1:] if line.strip()]
        detail = f"\n当前状态：{'; '.join(state)}" if state else ""
        sys.exit(f"没有处于 device 状态的设备，未安装。{detail}")
    if len(devices) > 1:
        listing = "\n".join(f"  - {serial}" for serial, _ in devices)
        sys.exit(f"连接了多台设备，请用 --serial 指定：\n{listing}")
    return devices[0]


def installed_version(adb: str, serial: str) -> tuple[int | None, str]:
    result = run([adb, "-s", serial, "shell", "dumpsys", "package", APPLICATION_ID])
    if "Unable to find package" in result.stdout:
        return None, ""
    code = re.search(r"versionCode=(\d+)", result.stdout)
    if not code:
        return None, ""
    name = re.search(r"versionName=(\S+)", result.stdout)
    return int(code.group(1)), (name.group(1) if name else "")


def main() -> int:
    parser = argparse.ArgumentParser(description="只允许更高版本覆盖安装")
    parser.add_argument("--debug", action="store_true", help="装 debug 包（默认 release）")
    parser.add_argument("--apk", type=Path, help="显式指定 APK 路径")
    parser.add_argument("--serial", help="多台设备时指定目标")
    parser.add_argument("--check", action="store_true", help="只比较版本，不安装")
    args = parser.parse_args()

    apk = args.apk or (DEBUG_APK if args.debug else RELEASE_APK)
    adb = tool("adb")
    built_code, built_name = apk_version(find_aapt2(), apk)
    serial, _ = pick_device(adb, args.serial)

    size_mb = apk.stat().st_size / (1024 * 1024)
    print(f"APK      : {apk.relative_to(ROOT)}（{size_mb:.1f} MB）")
    print(f"待装版本 : versionCode {built_code} / {built_name}")
    if "-test." not in built_name:
        print("注意     : 这个包没有 -test.N 标记；只有正式发布时才应该这样。")

    current_code, current_name = installed_version(adb, serial)
    if current_code is None:
        print(f"设备     : {serial}（未安装过，允许全新安装）")
    else:
        print(f"设备     : {serial}（现有 versionCode {current_code} / {current_name}）")
        if built_code < current_code:
            print(
                f"\n拒绝安装：{built_code} < {current_code}，这是降级覆盖。\n"
                "先跑 python tools/bump_version.py 再重新打包；不要用 adb install -d 绕过。"
            )
            return 1
        if built_code == current_code:
            print(
                f"\n拒绝安装：{built_code} == {current_code}，同版本重装。\n"
                "每次出包版本号都必须不同：先跑 python tools/bump_version.py 再重新打包。"
            )
            return 1

    if args.check:
        print("\n--check：版本满足覆盖条件，未执行安装。")
        return 0

    print("\n开始安装…")
    result = run([adb, "-s", serial, "install", "-r", str(apk)])
    output = (result.stdout + result.stderr).strip()
    if result.returncode != 0 or "Success" not in output:
        print(f"安装失败：\n{output}")
        return 2

    after_code, after_name = installed_version(adb, serial)
    print(f"安装成功 : versionCode {after_code} / {after_name}")
    if after_code != built_code:
        print(f"警告     : 安装后的 versionCode 与安装包不一致（{after_code} vs {built_code}）")
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
