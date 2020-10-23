
.PHONY: build
build:
	./gradlew build

.PHONY: build
build:
	# daemon was causing failures when a helper class was in the same package as instrumentation
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
