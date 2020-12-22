NAME = javaagent-webhook
IMAGE_PREFIX = hypertrace

export GO111MODULE=on

app: deps
	go build -v -o $(NAME) ./cmd/

deps:
	go get -v ./...

test: deps
	go test -v ./... -cover

docker:
	docker build --no-cache -t $(IMAGE_PREFIX)/$(NAME):latest .

.PHONY: docker
