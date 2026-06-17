#!/bin/bash

set -e

patch_init_appdata() {
    local file="/usr/local/bin/init-appdata.sh"

    if [[ ! -f "${file}" ]]; then
        return
    fi

    if grep -q 'DATABASE_TABLE_PREFIX:=apitable_} \\' "${file}"; then
        awk '
            /-Dtable\.prefix=.*DATABASE_TABLE_PREFIX:=apitable_.*\\$/ {
                print "    -Dtable.prefix=\"${DATABASE_TABLE_PREFIX:=apitable_}\" \\"
                getline
                print "    -DDB_ENGINE=\"${DB_ENGINE:=mysql}\""
                next
            }
            { print }
        ' "${file}" > /tmp/init-appdata.sh
        cp /tmp/init-appdata.sh "${file}"
        chmod +x "${file}"
    fi
}

init-dataenv.sh
add-host.sh
patch_init_appdata

exec pm2-runtime start ecosystem.config.js
