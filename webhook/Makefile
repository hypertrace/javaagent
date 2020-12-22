NAME = javaagent-webhook
DOCKER_NAMESPACE ?= hypertrace
DOCKER_TAG ?= $$(git log --abbrev-commit --format=%h -s | head -n 1)

app: deps
	go build -v -o $(NAME) ./cmd/

deps:
	go get -v ./...

test: deps
	go test -v ./... -cover

docker:
	docker build --no-cache -t $(DOCKER_NAMESPACE)/$(NAME):$(DOCKER_TAG) .

.PHONY: docker
