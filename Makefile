DOCKER_IMAGE ?= hypertrace/javaagent
DOCKER_TAG ?= latest

.PHONY: assemble
assemble:
	./gradlew assemble --stacktrace

.PHONY: build
build:
	./gradlew build -x :smoke-tests:test --stacktrace

.PHONY: smoke-test
smoke-test:
	./gradlew :smoke-tests:test --stacktrace

.PHONY: muzzle
muzzle:
	# daemon was causing failure "java.lang.IllegalStateException: Could not locate class file for"
    # for injecting helper classes from the same packages as instrumentations
	./gradlew muzzle --no-daemon

.PHONY: docker
docker: assemble
	docker build -f javaagent/Dockerfile javaagent/ -t ${DOCKER_IMAGE}:${DOCKER_TAG}

.PHONY: docker-push
docker-push: assemble
	docker push ${DOCKER_IMAGE}:${DOCKER_TAG}

.PHONY: test
test:
	./gradlew check

.PHONY: format
format:
	./gradlew spotlessApply

.PHONY: clean
clean:
	./gradlew clean

.PHONY: init-submodules
init-submodules:
	git submodule update --init
