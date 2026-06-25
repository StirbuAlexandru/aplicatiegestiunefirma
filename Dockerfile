FROM python:3.12-slim

# Tailscale - ca backend-ul sa poata ajunge la PostgreSQL de pe serverul Ubuntu
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates iptables \
    && curl -fsSL https://tailscale.com/install.sh | sh \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py entrypoint.sh ./
RUN chmod +x entrypoint.sh

EXPOSE 8000
CMD ["./entrypoint.sh"]
