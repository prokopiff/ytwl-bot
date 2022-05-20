docker buildx build . -t vprokopiv/ytbot:arm32v7 --build-arg ARCH=arm32v7 --platform linux/arm/v7 --push
docker buildx build . -t vprokopiv/ytbot:arm64 --build-arg ARCH=arm64v8 --platform linux/arm64 --push
docker buildx build . -t vprokopiv/ytbot:amd64 --build-arg ARCH=amd64 --platform linux/amd64 --push
docker buildx build . -t vprokopiv/ytbot:latest --build-arg ARCH=amd64 --platform linux/amd64 --push
