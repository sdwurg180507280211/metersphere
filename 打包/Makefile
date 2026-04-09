# MeterSphere 构建 Makefile
# 提供便捷的构建命令接口

.PHONY: help all init gateway eureka test-track api-test performance-test \
        project-management report-stat system-setting workstation \
        build-all build-parallel build-dev clean clean-docker \
        list show-images export load check

# 默认目标
.DEFAULT_GOAL := help

# 脚本路径
SCRIPT := ./metersphere-build.sh

# 配置文件
CONFIG := .build.config

# 加载配置文件（如果存在）
-include $(CONFIG)

# 帮助信息
help:
	@echo "MeterSphere 构建系统"
	@echo ""
	@echo "快速命令:"
	@echo "  make all              - 构建所有模块（串行）"
	@echo "  make parallel         - 并行构建所有模块（推荐）"
	@echo "  make dev              - 开发模式（跳过初始化，只构建镜像）"
	@echo "  make quick            - 快速构建（跳过初始化）"
	@echo ""
	@echo "单模块构建:"
	@echo "  make gateway          - 构建 Gateway 模块"
	@echo "  make eureka           - 构建 Eureka 模块"
	@echo "  make test-track       - 构建 Test-Track 模块"
	@echo "  make api-test         - 构建 API-Test 模块"
	@echo "  make performance-test - 构建 Performance-Test 模块"
	@echo "  make project-management - 构建 Project-Management 模块"
	@echo "  make report-stat      - 构建 Report-Stat 模块"
	@echo "  make system-setting   - 构建 System-Setting 模块"
	@echo "  make workstation      - 构建 Workstation 模块"
	@echo "  make workflow-service - 构建 Workflow-Service 模块"
	@echo "  make analytics-stat   - 构建 Analytics-Stat 模块"
	@echo ""
	@echo "组合构建:"
	@echo "  make core             - 构建核心模块（gateway + eureka）"
	@echo "  make backend          - 构建后端服务模块"
	@echo ""
	@echo "工具命令:"
	@echo "  make list             - 列出所有可用模块及版本"
	@echo "  make check            - 检查构建环境"
	@echo "  make show-images      - 显示已构建的镜像"
	@echo "  make clean            - 清理构建产物"
	@echo "  make clean-docker     - 清理 Docker 缓存"
	@echo "  make export           - 仅导出已有镜像（不构建）"
	@echo ""
	@echo "环境变量:"
	@echo "  JOBS=N                                 - 并行任务数"
	@echo "  SERVICE_VERSION_<MOD>=vx.x.x-lts      - 每服务独立版本"
	@echo ""
	@echo "示例:"
	@echo "  make parallel JOBS=4"
	@echo "  make api-test SERVICE_VERSION_API_TEST=v2.10.26.05-lts"

# 完整构建（串行）
all:
	$(SCRIPT) -a

# 并行构建
parallel:
	$(SCRIPT) -a -p $(if $(JOBS),-j $(JOBS),-j 4)

# 快速构建（跳过初始化）
quick:
	$(SCRIPT) -a -s

# 开发模式（跳过初始化，只构建镜像）
dev:
	$(SCRIPT) -a -s -b

# 单模块构建
gateway:          ; $(SCRIPT) gateway
eureka:           ; $(SCRIPT) eureka
test-track:       ; $(SCRIPT) test-track
api-test:         ; $(SCRIPT) api-test
performance-test: ; $(SCRIPT) performance-test
project-management: ; $(SCRIPT) project-management
report-stat:      ; $(SCRIPT) report-stat
system-setting:   ; $(SCRIPT) system-setting
workstation:      ; $(SCRIPT) workstation

# 组合构建
core:
	$(SCRIPT) gateway eureka

backend:
	$(SCRIPT) test-track api-test performance-test project-management report-stat system-setting workstation
list:
	$(SCRIPT) -l

check:
	@echo "检查构建环境..."
	@command -v java >/dev/null 2>&1 || { echo "需要安装 JDK"; exit 1; }
	@command -v mvn >/dev/null 2>&1 || { echo "需要安装 Maven"; exit 1; }
	@command -v docker >/dev/null 2>&1 || { echo "需要安装 Docker"; exit 1; }
	java_version=$$(java -version 2>&1 | awk -F '"' '/version/ {print $$2}' | cut -d'.' -f1); \
	[ "$$java_version" -lt 17 ] && { echo "需要 JDK 17+，当前: $$java_version"; exit 1; }
	@echo "环境检查通过"

show-images:
	@docker images | grep -E "(REPOSITORY|$(REGISTRY))" || echo "没有找到镜像"

export:
	@echo "导出镜像到 tar 文件..."
	@if [ -z "$$(docker images -q $(REGISTRY))" ]; then \
		echo "错误: 没有找到可导出的镜像"; \
		exit 1; \
	fi
	@docker save $$(docker images -q $(REGISTRY) | sort -u) \
		> $${HOME}/Desktop/metersphere-$$(date +%y%m%d).tar
	@echo "导出完成"

load:
	@if [ -z "$(FILE)" ]; then \
		echo "用法: make load FILE=/path/to/file.tar"; \
		exit 1; \
	fi
	@docker load -i $(FILE)
	@echo "加载完成"

clean:
	@echo "清理构建产物..."
	@cd $(PROJECT_PATH) 2>/dev/null && mvn clean || true
	@rm -rf "${TMPDIR:-/tmp}/ms-build."*
	@echo "清理完成"

clean-docker:
	@echo "清理 Docker 缓存..."
	@docker system prune -f
	@echo "清理完成"

# 配置管理
init-config:
	@if [ -f "$(CONFIG)" ]; then \
		echo "配置文件已存在: $(CONFIG)"; \
	else \
		cp build.config.example $(CONFIG); \
		echo "已创建配置文件: $(CONFIG)"; \
		echo "请编辑配置文件: make edit-config"; \
	fi

edit-config:
	@if [ -f "$(CONFIG)" ]; then \
		$${EDITOR:-vi} $(CONFIG); \
	else \
		echo "配置文件不存在，运行 'make init-config' 创建"; \
	fi

# CI/CD 相关
ci-build:
	$(SCRIPT) -a -p -j 4 -s

# 版本管理
tag:
	@if [ -z "$(VERSION)" ]; then \
		echo "用法: make tag VERSION=v2.10.26"; \
		exit 1; \
	fi
	@echo "构建版本: $(VERSION)"
	$(SCRIPT) -a

release: tag
	@echo "创建发布包..."
	@echo "版本 $(VERSION) 已准备就绪"

# 开发辅助
logs:
	@ls -t ${TMPDIR:-/tmp}/ms-build.* 2>/dev/null | head -1 | xargs -I{} tail -f {}/$$(ls {} | grep '.log' | head -1) 2>/dev/null || echo "没有找到日志文件"

status:
	@echo "=== 构建状态 ==="
	@echo "Docker 镜像:"
	@docker images | grep $(REGISTRY) | wc -l | xargs printf "  已构建模块数: %s\n"
	@echo ""
	@echo "Docker 资源:"
	@docker system df
