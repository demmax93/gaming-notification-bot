#!/bin/sh

set -e
cd "$(dirname "$0")" || exit 1

# WINDOWS FIX
MSYS_NO_PATHCONV=1

echo ""
echo "### Writing configs ###"

if [ ! -f "docker-compose.yaml" ]; then
  cat <<EOF >> docker-compose.yaml
version: '3'
services:
  app:
    image: gaming_notification_bot
    environment:
      - TOKEN=test
    ports:
      - 8080
      - 5555
EOF
  ls -la docker-compose.yaml
fi

cat <<EOF >> start.sh
#!/bin/bash
cd "\$(dirname "\$0")" || exit 1
docker-compose up -d
EOF
ls -la start.sh

cat <<EOF >> stop.sh
#!/bin/bash
cd "\$(dirname "\$0")" || exit 1
docker-compose down
EOF
ls -la stop.sh

cat <<EOF >> restart.sh
#!/bin/bash
cd "\$(dirname "\$0")" || exit 1
docker-compose down && docker-compose up -d
EOF
ls -la restart.sh

chmod +x start.sh
chmod +x stop.sh
chmod +x restart.sh

echo ""
echo "Installation finished!"
