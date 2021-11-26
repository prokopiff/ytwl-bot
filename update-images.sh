docker buildx build . -t vprokopiv/ytbot:arm32v7 --build-arg ARCH=arm32v7 --platform linux/arm/v7
docker push vprokopiv/ytbot:arm32v7

docker buildx build . -t vprokopiv/ytbot:amd64 --build-arg ARCH=amd64 --platform linux/amd64
docker push vprokopiv/ytbot:amd64

docker buildx build . -t vprokopiv/ytbot:latest --build-arg ARCH=amd64 --platform linux/amd64
docker push vprokopiv/ytbot:latest
