#!/system/bin/sh
# TEFManager Zygisk module installer

ui_print "- TEFManager Zygisk module"

if [ ! -d "$MODPATH/zygisk" ]; then
    abort "! zygisk directory missing from package"
fi

set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm_recursive "$MODPATH/zygisk" 0 0 0755 0644

APP_FILES=/data/media/0/Android/data/eternal.future.tefmanager/files
APP_DATA=/data/data/eternal.future.tefmanager
if [ -d "$APP_FILES" ]; then
    echo "tefmanager" > "$APP_FILES/zygisk_active"
    if [ -d "$APP_DATA" ]; then
        uid=$(stat -c %u "$APP_DATA" 2>/dev/null)
        [ -n "$uid" ] && chown "$uid:$uid" "$APP_FILES/zygisk_active"
    fi
    chmod 0660 "$APP_FILES/zygisk_active"
fi
