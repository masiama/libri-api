.PHONY: dev-boot dev-build

default:
	make -j3 dev-boot dev-build

dev-boot:
	@./gradlew bootRun

dev-build:
	@./gradlew build --continuous --quiet
