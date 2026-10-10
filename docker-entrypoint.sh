#!/bin/sh
# ───────────────────────────────────────────────────────────────
#  Container entrypoint
#
#  1. Makes the JWT key pair available at /app/certs, from whichever
#     source this environment provides:
#       a) JWT_PRIVATE_KEY / JWT_PUBLIC_KEY env vars  — raw PEM or base64.
#          This is the ECS Fargate / Secrets Manager path: no volume needed,
#          and every task shares one key pair so tokens stay valid across tasks.
#       b) a mounted volume (docker compose mounts ./certs:/app/certs:ro).
#       c) nothing — then we refuse to start with a clear message instead of
#          generating a throwaway key pair that would silently invalidate every
#          token the moment the container restarts or scales out.
#
#  2. Starts the JVM with exec, so java becomes PID 1 and receives SIGTERM
#     directly. Without this the container is SIGKILLed on every stop.
# ───────────────────────────────────────────────────────────────
set -e

CERT_DIR="${CERT_DIR:-/app/certs}"

# Write $2 (raw PEM or base64-encoded PEM) to the file $1.
write_key() {
    target="$1"
    value="$2"

    if printf '%s' "$value" | grep -q 'BEGIN'; then
        printf '%s\n' "$value" > "$target"
    else
        # Single-line base64 (how secrets managers usually hand over a PEM)
        printf '%s' "$value" | tr -d ' \n' | base64 -d > "$target"
    fi

    chmod 600 "$target"
}

if [ -n "$JWT_PRIVATE_KEY" ] && [ -n "$JWT_PUBLIC_KEY" ]; then
    echo "entrypoint: writing JWT keys from environment to $CERT_DIR"
    mkdir -p "$CERT_DIR"
    write_key "$CERT_DIR/private_key.pem" "$JWT_PRIVATE_KEY"
    write_key "$CERT_DIR/public_key.pem"  "$JWT_PUBLIC_KEY"
    unset JWT_PRIVATE_KEY JWT_PUBLIC_KEY
elif [ -f "$CERT_DIR/private_key.pem" ] && [ -f "$CERT_DIR/public_key.pem" ]; then
    echo "entrypoint: using JWT keys mounted at $CERT_DIR"
else
    echo "entrypoint: FATAL - no JWT keys found." >&2
    echo "  Provide them either as a mounted volume at $CERT_DIR (docker compose does this" >&2
    echo "  for you), or as JWT_PRIVATE_KEY / JWT_PUBLIC_KEY environment variables" >&2
    echo "  (PEM or base64) — the usual way on ECS Fargate via Secrets Manager." >&2
    echo "  Generate a pair with:" >&2
    echo "    openssl genrsa -out certs/private_key.pem 2048" >&2
    echo "    openssl rsa -in certs/private_key.pem -pubout -out certs/public_key.pem" >&2
    exit 1
fi

# shellcheck disable=SC2086  # JAVA_OPTS is intentionally word-split
exec java $JAVA_OPTS -jar /app/app.jar "$@"
