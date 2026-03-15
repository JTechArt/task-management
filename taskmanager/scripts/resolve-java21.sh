#!/usr/bin/env bash

set -euo pipefail

is_java21_home() {
    local candidate="${1:-}"
    [[ -n "$candidate" ]] || return 1
    [[ -x "$candidate/bin/java" ]] || return 1
    "$candidate/bin/java" -version 2>&1 | head -n 1 | grep -q '"21\.'
}

emit_java_home() {
    local candidate="$1"
    if is_java21_home "$candidate"; then
        printf '%s\n' "$candidate"
        return 0
    fi
    return 1
}

if emit_java_home "${TASKMANAGER_JAVA_HOME:-}"; then
    exit 0
fi

if emit_java_home "${JAVA_HOME:-}"; then
    exit 0
fi

if [[ "$(uname -s)" == "Darwin" ]] && [[ -x /usr/libexec/java_home ]]; then
    candidate="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if emit_java_home "$candidate"; then
        exit 0
    fi
fi

if [[ -d "${HOME}/.sdkman/candidates/java" ]]; then
    while IFS= read -r candidate; do
        if emit_java_home "$candidate"; then
            exit 0
        fi
    done < <(find "${HOME}/.sdkman/candidates/java" -maxdepth 1 -mindepth 1 -type d | sort -r)
fi

echo "Unable to find a Java 21 installation." >&2
echo "Set TASKMANAGER_JAVA_HOME or JAVA_HOME to a JDK 21 home directory." >&2
exit 1
