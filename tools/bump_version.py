#!/usr/bin/env python3
"""版本号递增：每次出包前跑一次。

为什么要有这个脚本：`versionCode` 必须严格递增，否则 `adb install -r` 会装不上去、
应用内「检查更新」也分不清谁新谁旧；而 `versionCode` 靠手改，迟早会忘。
这里把 `versionCode` 与 `versionName` 的 `-test.N` 一起 +1，两者永远同步。

`versionName` 的形态固定为 `<主版本>-test.<N>`（尚未正式上传之前）。
正式发版时才去掉 `-test.N`——那时 `AppVersion` 会把它判定为高于同号测试版，
测试机上的「检查更新」正好能收到正式版提示。

用法：
    python tools/bump_version.py              # 2.1.3-test.1 → 2.1.3-test.2，versionCode +1
    python tools/bump_version.py --base 2.2.0 # 换基础版本 → 2.2.0-test.1
    python tools/bump_version.py --release    # 2.1.3-test.1 → 2.1.3（正式版，去掉后缀）
    python tools/bump_version.py --dry-run    # 只打印，不写文件
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VERSION_FILE = ROOT / "version.properties"

# 只接受 `1.2` / `1.2.3` / `1.2.3.4`；段数超过四段时 AppVersion 也会拒绝解析。
BASE_RE = re.compile(r"^\d+(?:\.\d+){1,3}$")
NAME_RE = re.compile(r"^(?P<base>\d+(?:\.\d+){1,3})(?:-test\.(?P<counter>\d+))?$")

TEMPLATE = """\
# 版本號的唯一來源：`app/build.gradle.kts` 直接讀這裡，不要在 build 檔裡寫死第二份。
# 每次產生安裝包前先跑 `python tools/bump_version.py`：versionCode +1、-test.N 的 N +1。
# 正式上傳商店／發布頁之後才去掉 `-test.N` 後綴；在那之前一律標成測試版。
versionCode={code}
versionName={name}
"""


def read_properties() -> dict[str, str]:
    if not VERSION_FILE.is_file():
        sys.exit(f"找不到 {VERSION_FILE}")
    values: dict[str, str] = {}
    for raw in VERSION_FILE.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        key, _, value = line.partition("=")
        values[key.strip()] = value.strip()
    for key in ("versionCode", "versionName"):
        if not values.get(key):
            sys.exit(f"{VERSION_FILE.name} 缺少 {key}")
    return values


def main() -> int:
    parser = argparse.ArgumentParser(description="递增 versionCode 与 -test.N")
    parser.add_argument("--base", help="更换基础版本，例如 2.2.0；不填则沿用当前基础版本")
    parser.add_argument(
        "--release",
        action="store_true",
        help="去掉 -test.N 后缀，标记为正式版（versionCode 仍然 +1）",
    )
    parser.add_argument("--dry-run", action="store_true", help="只打印结果，不写文件")
    args = parser.parse_args()

    values = read_properties()
    code = int(values["versionCode"])
    current_name = values["versionName"]

    matched = NAME_RE.match(current_name)
    if not matched:
        sys.exit(
            f"versionName 无法识别：{current_name}\n"
            "只接受 1.2 / 1.2.3 或 1.2.3-test.N，请先手工修正 version.properties。"
        )

    base = args.base or matched.group("base")
    if not BASE_RE.match(base):
        sys.exit(f"--base 不是合法版本号：{base}")

    if args.release:
        if matched.group("counter") is None:
            sys.exit(f"当前 {current_name} 已经是正式版，不需要 --release")
        new_name = base
    else:
        # 换了基础版本，测试计数从头开始；否则在原来的计数上 +1。
        counter = 1 if base != matched.group("base") else int(matched.group("counter") or 0) + 1
        new_name = f"{base}-test.{counter}"
    new_code = code + 1

    print(f"versionCode {code} → {new_code}")
    print(f"versionName {current_name} → {new_name}")

    if args.dry_run:
        print("（dry-run，未写入）")
        return 0

    with open(VERSION_FILE, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(TEMPLATE.format(code=new_code, name=new_name))
    print(f"已写入 {VERSION_FILE.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
