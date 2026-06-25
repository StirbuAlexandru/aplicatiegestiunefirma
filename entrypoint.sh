#!/bin/sh
set -e

mkdir -p /var/lib/tailscale /var/run/tailscale

# Porneste tailscaled in fundal
tailscaled --state=/var/lib/tailscale/tailscaled.state --socket=/var/run/tailscale/tailscaled.sock &

# Asteapta sa porneasca daemonul
sleep 2

# Conectare la tailnet folosind cheia din variabila de mediu TS_AUTHKEY
tailscale up --authkey="${TS_AUTHKEY}" --hostname="render-backend" --accept-routes=true

echo "Tailscale status:"
tailscale status

# Porneste backend-ul FastAPI
exec python main.py
