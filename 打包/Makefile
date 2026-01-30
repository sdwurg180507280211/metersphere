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
	@echo ""
	@echo "组合构建:"
	@echo "  make core             - 构建核心模块（gateway + eureka）"
	@echo "  make backend          - 构建后端服务模块"
	@echo ""
	@echo "工具命令:"
	@echo "  make list             - 列出所有可用模块"
	@echo "  make check            - 检查构建环境"
	@echo "  make show-images      - 显示已构建的镜像"
	@echo "  make clean            - 清理构建产物"
	@echo "  make clean-docker     - 清理 Docker 缓存"
	@echo "  make export           - 仅导出已有镜像（不构建）"
	@echo ""
	@echo "配置:"
	@echo "  make init-config      - 初始化配置文件"
	@echo "  make edit-config      - 编辑配置文件"
	@echo ""
	@echo "环境变量:"
	@echo "  VERSION=vx.x.x        - 指定版本号"
	@echo "  JOBS=N                - 并行任务数"
	@echo ""
	@echo "示例:"
	@echo "  make parallel JOBS=4                    # 4 个并行任务"
	@echo "  make gateway api-test VERSION=v2.11.0  # 指定版本构建"
	@echo "  make dev gateway                        # 开发模式构建 gateway"

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
gateway:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) gateway

eureka:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) eureka

test-track:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) test-track

api-test:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) api-test

performance-test:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) performance-test

project-management:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) project-management

report-stat:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) report-stat

system-setting:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) system-setting

workstation:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) workstation

# 组合构建
core:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) gateway eureka

backend:
	$(SCRIPT) $(if $(VERSION),-v $(VERSION)) \
		test-track api-test performance-test \
		project-management report-stat system-setting workstation

# 工具命令
list:
	$(SCRIPT) -l

check:
	@echo "检查构建环境..."
	@command -v java >/dev/null 2>&1 || { echo "需要安装 JDK"; exit 1; }
	@command -v mvn >/dev/null 2>&1 || { echo "需要安装 Maven"; exit 1; }
	@command -v docker >/dev/null 2>&1 || { echo "需要安装 Docker"; exit 1; }
	@echo "环境检查通过 ✓"

show-images:
	@docker images | grep -E "(REPOSITORY|registry.fit2cloud.com/north)" || echo "没有找到镜像"

export:
	@echo "导出镜像到 tar 文件..."
	@if [ -z "$$(docker images -q registry.fit2cloud.com/north)" ]; then \
		echo "错误: 没有找到可导出的镜像"; \
		exit 1; \
	fi
	@docker save $$(docker images -q registry.fit2cloud.com/north | sort -u) \
		> /Users/edy/Desktop/metersphere-$$(date +%y%m%d).tar
	@echo "导出完成"

load:
	@if [ -z "$(FILE)" ]; then \
		echo "用法: make load FILE=/path/to/file.tar"; \
		exit 1; \
	fi
	@docker load < $(FILE)
	@echo "加载完成"

clean:
	@echo "清理构建产物..."
	@cd $(PROJECT_PATH) 2>/dev/null && mvn clean || true
	@rm -f /tmp/build_*.log /tmp/build_*.status
	@echo "清理完成"

clean-docker:
	@echo "清理 Docker 缓存..."
	@docker system prune -f
	@echo "清理完成"

clean-all: clean clean-docker
	@echo "清理所有缓存..."
	@rm -rf ~/.m2/repository
	@echo "清理完成（注意：Maven 仓库已清空）"

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
	$(SCRIPT) -a -p -j 4 --skip-init

ci-test:
	@echo "运行集成测试..."
	# 添加测试命令

# 版本管理
tag:
	@if [ -z "$(VERSION)" ]; then \
		echo "用法: make tag VERSION=v2.10.23"; \
		exit 1; \
	fi
	@echo "构建版本: $(VERSION)"
	$(SCRIPT) -v $(VERSION) -a

release: tag
	@echo "创建发布包..."
	@echo "版本 $(VERSION) 已准备就绪"

# 开发辅助
watch:
	@watch -n 2 'make show-images'

logs:
	@tail -f /tmp/build_*.log 2>/dev/null || echo "没有找到日志文件"

status:
	@echo "=== 构建状态 ==="
	@echo "Docker 镜像:"
	@docker images | grep registry.fit2cloud.com/north | wc -l | xargs echo "  已构建模块数:"
	@echo ""
	@echo "磁盘使用:"
	@df -h /Users/edy/Desktop | tail -1
	@echo ""
	@echo "Docker 资源:"
	@docker system df
