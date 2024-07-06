set -x

docker buildx build . -t vprokopiv/ytbot:arm64$1 --build-arg ARCH=arm64v8 --platform linux/arm64 --push
docker buildx build . -t vprokopiv/ytbot:amd64$1 --build-arg ARCH=amd64 --platform linux/amd64 --push
docker buildx build . -t vprokopiv/ytbot:latest$1 --build-arg ARCH=amd64 --platform linux/amd64 --push
