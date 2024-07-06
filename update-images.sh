set -x

suffix="-$1"

if [[ "$1" == "main" ]]; then
  suffix=""
fi

docker buildx build . -t vprokopiv/ytbot:arm64${suffix} --build-arg ARCH=arm64v8 --platform linux/arm64 --push
docker buildx build . -t vprokopiv/ytbot:amd64${suffix} --build-arg ARCH=amd64 --platform linux/amd64 --push
docker buildx build . -t vprokopiv/ytbot:latest${suffix} --build-arg ARCH=amd64 --platform linux/amd64 --push
