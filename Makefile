
.PHONY: build
build:
	./gradlew build

.PHONY: format
format:
	./gradlew spotlessApply

.PHONY: clean
clean:
	./gradlew clean
