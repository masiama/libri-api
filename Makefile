.PHONY: dev-boot dev-build dev-crawler

-include .env
export

default:
	make -j3 dev-boot dev-build dev-crawler

dev-boot:
	@./gradlew bootRun

dev-build:
	@./gradlew build --continuous --quiet

dev-crawler:
	@bash -c '\
		if [ -n "$$DEV_CRAWLER_DIR" ]; then \
			echo "Starting crawler dev environment..."; \
			cd "$$DEV_CRAWLER_DIR" && \
			exec watchexec -c -- make build; \
		else \
			echo "DEV_CRAWLER_DIR not set, skipping crawler watch"; \
		fi; \
	'
