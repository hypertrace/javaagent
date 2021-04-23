DOCKER_IMAGE ?= hypertrace/javaagent
DOCKER_TAG ?= latest
SMOKE_TEST_SUITE ?= other 

.PHONY: assemble
assemble:
	./gradlew assemble --stacktrace

.PHONY: build
build:
	./gradlew build -x :smoke-tests:test --stacktrace --info

.PHONY: smoke-test
smoke-test:
	./gradlew :smoke-tests:test -PsmokeTestSuite=${SMOKE_TEST_SUITE} --stacktrace

.PHONY: muzzle
muzzle:
	# daemon was causing failure "java.lang.IllegalStateException: Could not locate class file for"
    # for injecting helper classes from the same packages as instrumentations
	./gradlew assemble muzzle --no-daemon

.PHONY: docker
docker: assemble
	docker build -f javaagent/Dockerfile javaagent/ -t ${DOCKER_IMAGE}:${DOCKER_TAG}
	# use also 'latest' tag if the tag is on the main branch
	@git branch -a --contains ${DOCKER_TAG} | grep main && docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest || echo "skipping latest; not on the main branch"

.PHONY: docker-push
docker-push:
	docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
	@git branch -a --contains ${DOCKER_TAG} | grep main && docker push ${DOCKER_IMAGE}:latest || echo "skipping latest; not on the main branch"

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
