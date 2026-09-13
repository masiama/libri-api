.PHONY: dev-boot dev-build install-hooks openapi

default:
	make -j3 dev-boot dev-build

dev-boot:
	@./gradlew bootRun

dev-build:
	@./gradlew build --continuous --quiet

install-hooks:
	git config core.hooksPath .githooks

openapi:
	@./gradlew generateOpenApiDocs
