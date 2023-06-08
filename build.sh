#!/bin/sh

set -e
cd "$(dirname "$0")" || exit 1
start=$(date +%s)

GRADLE_IMAGE=gradle

if [ -f "app/stop.sh" ]; then
  echo ""
  echo "### Stopping previous installation ###"
  cd app
  ./stop.sh
  cd ../
fi

if [ -d "app" ]; then
  echo ""
  echo "### Cleaning up previous installation ###"
  rm -rf app_data
  mkdir app_data
  mv app/docker-compose.yaml app_data/docker-compose.yaml
  rm -rf app
  echo "App removed"
fi

echo ""
echo "### Building Java Application ###"

CONTAINER_NAME="container_${RANDOM}"

rm -rf cache/maven/build

echo ""
echo "Cleaning up previous image..."
docker stop ${CONTAINER_NAME} || true
docker rm ${CONTAINER_NAME} || true

echo ""
echo "Creating new container..."

MSYS_NO_PATHCONV=1 docker create --tty --name ${CONTAINER_NAME} ${GRADLE_IMAGE} bash -c "cd /tmp && gradle build && chmod -R 777 /tmp/build && chown -R 1000:1000 /tmp/build"

echo ""
echo "Copying build data..."
docker cp "$(pwd)/build.gradle.kts" "${CONTAINER_NAME}:/tmp/build.gradle.kts"
docker cp "$(pwd)/gradlew" "${CONTAINER_NAME}:/tmp/gradlew"
docker cp "$(pwd)/gradlew.bat" "${CONTAINER_NAME}:/tmp/gradlew.bat"
docker cp "$(pwd)/src" "${CONTAINER_NAME}:/tmp/src"
docker cp "$(pwd)/gradle" "${CONTAINER_NAME}:/tmp/gradle"
echo "Data copied"

echo ""
echo "Starting build..."
docker start ${CONTAINER_NAME} -a

echo ""
echo "Copying build result..."
mkdir cache
mkdir cache/maven
docker cp "${CONTAINER_NAME}:/tmp/build" "$(pwd)/cache/maven/build"
ls -la "$(pwd)/cache/maven/build/."

echo ""
echo "Cleaning up previous image..."
docker stop ${CONTAINER_NAME} || true
docker rm ${CONTAINER_NAME} || true

chmod +x cache/maven/build/libs/tmp-1.0.0.jar
echo ""
echo ""
echo "... $(($(date +%s) - start))s passed ..."
echo ""

echo ""
echo "### Building Docker Images ###"

docker build -t gaming_notification_bot --no-cache -f Dockerfile .

docker system prune -f || true

mkdir app

echo ""
echo "### Restoring previous app data ###"
if [ -d "app_data" ]; then
  mv app_data/docker-compose.yaml app/docker-compose.yaml
  ls -la app
fi

cd app
cp ../deploy/install.sh ./install.sh
chmod +x ./install.sh
./install.sh
./start.sh
cd ../

echo ""
echo "Finished in $(($(date +%s) - start))s"