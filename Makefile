
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
