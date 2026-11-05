#!/system/bin/sh
# 生成 TEFManager 可读取的 Zygisk 激活标记（无需 App 再申请一次 root 才能识别）
MODULE_ID=tefmanager
APP_FILES=/data/media/0/Android/data/eternal.future.tefmanager/files
APP_DATA=/data/data/eternal.future.tefmanager

if [ -d "$APP_FILES" ]; then
    echo "$MODULE_ID" > "$APP_FILES/zygisk_active"
    if [ -d "$APP_DATA" ]; then
        uid=$(stat -c %u "$APP_DATA" 2>/dev/null)
        [ -n "$uid" ] && chown "$uid:$uid" "$APP_FILES/zygisk_active"
    fi
    chmod 0660 "$APP_FILES/zygisk_active"
fi

# 模块被 Magisk 禁用时（disable 文件存在），清理标记
if [ -f "/data/adb/modules/$MODULE_ID/disable" ]; then
    rm -f "$APP_FILES/zygisk_active"
fi
