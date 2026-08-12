#!/bin/bash
# AssistsX Android 一键调试辅助脚本（供 VSCode tasks 调用）
# 用法:
#   android-debug.sh build-install-launch   编译 + 安装 + 启动(等待调试器) + JDWP 转发
#   android-debug.sh launch                 跳过编译, 直接重启应用(等待调试器) + JDWP 转发
#   android-debug.sh build-install-run      编译 + 安装 + 正常启动 (不调试)
#   android-debug.sh build-install-run-attach  编译 + 安装 + 正常启动 + JDWP 转发 (供调试面板 Run 配置使用)
#   android-debug.sh assemble               仅编译 assembleDebug
set -e

PKG="com.ven.assistsx"
ACTIVITY=".MainActivity"
JDWP_PORT=8700

cd "$(dirname "$0")/.."
MODE="${1:-build-install-launch}"

# --- 定位 java (VSCode GUI 进程可能不继承终端 PATH) ---
if ! command -v java >/dev/null 2>&1; then
    DETECTED_JDK="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    if [ -n "$DETECTED_JDK" ]; then
        export JAVA_HOME="$DETECTED_JDK"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

# --- 定位 adb ---
ADB_BIN="$(command -v adb || true)"
if [ -z "$ADB_BIN" ]; then
    SDK_DIR="$(grep -E '^sdk.dir=' local.properties 2>/dev/null | cut -d= -f2- | tr -d '\r')"
    SDK_DIR="${SDK_DIR:-$HOME/Library/Android/sdk}"
    ADB_BIN="$SDK_DIR/platform-tools/adb"
fi
if [ ! -x "$ADB_BIN" ]; then
    echo "[ERROR] adb 未找到, 请检查 local.properties 中的 sdk.dir"
    exit 1
fi
echo "[INFO] adb: $ADB_BIN"

# --- 检查设备 ---
DEVICE_LIST="$("$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1}')"
if [ -z "$DEVICE_LIST" ]; then
    echo "[ERROR] 没有已连接的 Android 设备"
    exit 1
fi
DEVICE_COUNT="$(echo "$DEVICE_LIST" | wc -l | tr -d ' ')"
SERIAL="$(echo "$DEVICE_LIST" | head -1)"
if [ "$DEVICE_COUNT" -gt 1 ]; then
    echo "[WARN] 检测到多台设备, 使用第一台: $SERIAL"
fi
ADB="$ADB_BIN -s $SERIAL"

# --- 编译 / 安装 ---
case "$MODE" in
    assemble)
        echo "[INFO] ./gradlew :app:assembleDebug"
        ./gradlew :app:assembleDebug
        exit 0
        ;;
    build-install-launch)
        echo "[INFO] ./gradlew :app:installDebug (多台设备时此步骤会失败)"
        ./gradlew :app:installDebug
        ;;
    build-install-run)
        echo "[INFO] ./gradlew :app:installDebug (多台设备时此步骤会失败)"
        ./gradlew :app:installDebug
        ;;
    build-install-run-attach)
        echo "[INFO] ./gradlew :app:installDebug (多台设备时此步骤会失败)"
        ./gradlew :app:installDebug
        ;;
    launch)
        echo "[INFO] 跳过编译, 直接启动应用"
        ;;
    *)
        echo "[ERROR] 未知模式: $MODE"
        exit 1
        ;;
esac

# --- 普通启动 (不等待调试器) ---
if [ "$MODE" = "build-install-run" ] || [ "$MODE" = "build-install-run-attach" ]; then
    echo "[INFO] 重启 $PKG ..."
    $ADB shell am force-stop "$PKG"
    $ADB shell am start -n "$PKG/$ACTIVITY" >/dev/null
    if [ "$MODE" = "build-install-run" ]; then
        echo "[OK] $PKG 已启动"
        exit 0
    fi
else
    # --- 以「等待调试器」方式重启应用 ---
    echo "[INFO] 重启 $PKG (wait-for-debugger) ..."
    $ADB shell am force-stop "$PKG"
    $ADB shell am start -D -n "$PKG/$ACTIVITY" >/dev/null
fi

# --- 等待进程出现 ---
PID=""
for _ in $(seq 1 60); do
    PID="$($ADB shell pidof -s "$PKG" 2>/dev/null | tr -d '\r ' || true)"
    [ -n "$PID" ] && break
    sleep 0.5
done
if [ -z "$PID" ]; then
    echo "[ERROR] 启动后未找到进程 $PKG"
    exit 1
fi

# --- 转发 JDWP 端口 ---
$ADB forward --remove "tcp:$JDWP_PORT" 2>/dev/null || true
$ADB forward "tcp:$JDWP_PORT" "jdwp:$PID"
echo "[OK] $PKG (pid $PID) 已就绪, 调试器可连接 127.0.0.1:$JDWP_PORT"
