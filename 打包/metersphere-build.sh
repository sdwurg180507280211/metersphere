#!/usr/bin/env bash

# ============================================================================
# MeterSphere 模块化打包脚本
# 支持指定模块打包、并行构建、错误处理、增量构建
# ============================================================================

set -e  # 遇到错误立即退出
# set -x  # 取消注释以显示详细执行过程

# ============================================================================
# 配置区域
# ============================================================================

# 项目路径
PROJECT_PATH=${PROJECT_PATH:-"/Users/zhaozhiwei/IdeaProjects/metersphere"}

# 镜像版本
IMAGE_VERSION=${IMAGE_VERSION:-"v2.10.23.02-lts"}

# 镜像仓库地址
REGISTRY=${REGISTRY:-"registry.fit2cloud.com/north"}

# 打包输出路径
# 使用 $HOME 自动适配当前用户的桌面路径
PACKAGE_PATH=${PACKAGE_PATH:-"$HOME/Desktop/metersphere-$(date +%y%m%d).tar"}

# 是否跳过 Maven 依赖安装（如果已经安装过）
SKIP_INIT=${SKIP_INIT:-false}

# 是否并行构建（实验性功能）
PARALLEL_BUILD=${PARALLEL_BUILD:-false}

# 并行任务数
MAX_JOBS=${MAX_JOBS:-4}

# 是否只构建镜像不保存 tar
BUILD_ONLY=${BUILD_ONLY:-false}

# ============================================================================
# 颜色输出
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================================
# 日志函数
# ============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# ============================================================================
# 模块定义
# ============================================================================

# 定义所有可用模块及其属性
declare -A MODULES=(
    ["gateway"]="framework/gateway"
    ["eureka"]="framework/eureka"
    ["test-track"]="test-track"
    ["api-test"]="api-test"
    ["performance-test"]="performance-test"
    ["project-management"]="project-management"
    ["report-stat"]="report-stat"
    ["system-setting"]="system-setting"
    ["workstation"]="workstation"
)

# 定义需要特殊处理的模块（没有 backend 子目录）
declare -A SIMPLE_MODULES=(
    ["gateway"]=1
    ["eureka"]=1
)

# 保存构建的镜像列表
BUILT_IMAGES=()

# ============================================================================
# 辅助函数
# ============================================================================

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# 检查环境
check_environment() {
    log_info "检查构建环境..."
    check_command "mvn"
    check_command "docker"
    check_command "java"
    
    # 检查 Java 版本
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        log_error "需要 JDK 17+，当前版本: $java_version"
        exit 1
    fi
    
    # 检查项目路径
    if [ ! -d "$PROJECT_PATH" ]; then
        log_error "项目路径不存在: $PROJECT_PATH"
        exit 1
    fi
    
    log_success "环境检查通过"
}

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项] [模块...]

选项:
    -h, --help              显示此帮助信息
    -a, --all               构建所有模块（默认）
    -l, --list              列出所有可用模块
    -v, --version VERSION   指定镜像版本（默认: $IMAGE_VERSION）
    -o, --output PATH       指定输出 tar 文件路径（默认: $PACKAGE_PATH）
    -p, --parallel          启用并行构建（实验性）
    -j, --jobs N            并行任务数（默认: $MAX_JOBS）
    -s, --skip-init         跳过 Maven 依赖初始化
    -b, --build-only        只构建镜像，不导出 tar 文件
    --registry URL          指定镜像仓库地址（默认: $REGISTRY）

模块:
    gateway                 网关服务
    eureka                  注册中心
    test-track              测试跟踪
    api-test                API 测试
    performance-test        性能测试
    project-management      项目管理
    report-stat             报告统计
    system-setting          系统设置
    workstation             工作台

示例:
    # 构建所有模块
    $0 -a
    
    # 构建指定模块
    $0 gateway eureka test-track
    
    # 并行构建所有模块
    $0 -a -p -j 4
    
    # 只构建镜像不导出
    $0 -a -b
    
    # 指定版本和输出路径
    $0 -v v2.10.23.03-lts -o /tmp/ms.tar gateway api-test

环境变量:
    PROJECT_PATH            项目路径
    IMAGE_VERSION           镜像版本
    REGISTRY                镜像仓库地址
    PACKAGE_PATH            输出 tar 文件路径
    SKIP_INIT               跳过初始化（true/false）
    PARALLEL_BUILD          并行构建（true/false）
    MAX_JOBS                并行任务数
    BUILD_ONLY              只构建不导出（true/false）

EOF
}

# 列出所有模块
list_modules() {
    log_info "可用模块列表:"
    echo ""
    printf "%-25s %s\n" "模块名称" "路径"
    printf "%-25s %s\n" "----------" "----------"
    for module in "${!MODULES[@]}"; do
        printf "%-25s %s\n" "$module" "${MODULES[$module]}"
    done | sort
    echo ""
}

# ============================================================================
# 核心构建函数
# ============================================================================

# 初始化项目依赖
init_dependencies() {
    if [ "$SKIP_INIT" = true ]; then
        log_warn "跳过依赖初始化"
        return 0
    fi
    
    log_info "初始化项目依赖..."
    cd "$PROJECT_PATH"
    
    # 安装父 POM
    if ! mvn install -N; then
        log_error "父 POM 安装失败"
        return 1
    fi
    
    # 编译核心框架模块
    log_info "编译核心框架模块..."
    if ! mvn clean install -pl framework,framework/sdk-parent,framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter; then
        log_error "核心框架模块编译失败"
        return 1
    fi
    
    log_success "依赖初始化完成"
}

# 构建单个模块
build_module() {
    local module_name=$1
    local module_path="${MODULES[$module_name]}"
    local image_name="${REGISTRY}/${module_name}:${IMAGE_VERSION}"
    
    if [ -z "$module_path" ]; then
        log_error "未知模块: $module_name"
        return 1
    fi
    
    log_info "开始构建模块: $module_name"
    
    # 进入模块目录
    cd "${PROJECT_PATH}/${module_path}"
    
    # Maven 编译
    log_info "[$module_name] Maven 编译中..."
    if ! mvn clean install -DskipTests 2>&1 | grep -E "(BUILD SUCCESS|BUILD FAILURE|ERROR)"; then
        log_error "[$module_name] Maven 编译失败"
        return 1
    fi
    
    # 解压依赖
    log_info "[$module_name] 解压依赖..."
    if [ -n "${SIMPLE_MODULES[$module_name]}" ]; then
        # 简单模块（gateway, eureka）
        mkdir -p target/dependency
        (cd target/dependency && jar -xf ../*.jar)
    else
        # 复杂模块（有 backend 子目录）
        mkdir -p backend/target/dependency
        (cd backend/target/dependency && jar -xf ../*.jar)
    fi
    
    # 构建 Docker 镜像
    log_info "[$module_name] 构建 Docker 镜像..."
    
    # 根据模块类型选择 Dockerfile 位置
    if [ -n "${SIMPLE_MODULES[$module_name]}" ]; then
        # 简单模块在当前目录构建
        if ! docker build --build-arg MS_VERSION="${IMAGE_VERSION}" \
            -t "${image_name}" .; then
            log_error "[$module_name] Docker 镜像构建失败"
            return 1
        fi
    else
        # 复杂模块需要返回项目根目录构建
        cd "$PROJECT_PATH"
        if ! docker build --build-arg MS_VERSION="${IMAGE_VERSION}" \
            -t "${image_name}" \
            -f "${module_path}/Dockerfile" .; then
            log_error "[$module_name] Docker 镜像构建失败"
            return 1
        fi
    fi
    
    # 添加到已构建镜像列表
    BUILT_IMAGES+=("$image_name")

    # 我在做：将并行构建子进程里构建出的镜像名落盘
    # 目的：并行构建时 build_module 在子 shell 中执行，父进程无法拿到子进程内对 BUILT_IMAGES 的修改
    # 如果不这样做：父进程看到 BUILT_IMAGES 为空，export_images 会直接提示“没有构建的镜像需要导出”，从而不会生成 tar
    echo "$image_name" > "/tmp/build_${module_name}.image"
    
    log_success "[$module_name] 构建完成: $image_name"
    return 0
}

# 并行构建模块
build_modules_parallel() {
    local modules=("$@")
    local pids=()
    local failed=()

    # 我在做：建立 pid -> module 的映射
    # 目的：并行任务完成时，不能用 pids 下标去反推 modules 下标（数组会被 unset/重排），否则会读错 status/log/image 文件
    # 如果不这样做：会出现“模块状态/日志/镜像归属错乱”，进而导致 BUILT_IMAGES 汇总失败
    declare -A PID_TO_MODULE=()
    
    log_info "并行构建 ${#modules[@]} 个模块（最大并行数: $MAX_JOBS）"
    
    local running=0
    local index=0
    
    while [ $index -lt ${#modules[@]} ] || [ ${#pids[@]} -gt 0 ]; do
        # 启动新任务
        while [ $running -lt $MAX_JOBS ] && [ $index -lt ${#modules[@]} ]; do
            local module="${modules[$index]}"
            log_info "启动构建任务: $module"
            
            # 在子shell中构建，输出重定向到日志文件
            (
                build_module "$module" > "/tmp/build_${module}.log" 2>&1
                echo $? > "/tmp/build_${module}.status"
            ) &
            
            pids+=($!)
            PID_TO_MODULE[$!]="$module"
            ((running++))
            ((index++))
        done
        
        # 等待任意一个任务完成
        if [ ${#pids[@]} -gt 0 ]; then
            wait -n
            # 检查完成的任务
            for i in "${!pids[@]}"; do
                if ! kill -0 "${pids[$i]}" 2>/dev/null; then
                    # 获取对应的模块名（通过 pid 映射，避免数组下标错乱）
                    local pid="${pids[$i]}"
                    local module="${PID_TO_MODULE[$pid]}"
                    
                    # 检查状态
                    if [ -f "/tmp/build_${module}.status" ]; then
                        local status=$(cat "/tmp/build_${module}.status")
                        if [ "$status" -ne 0 ]; then
                            failed+=("$module")
                            log_error "模块构建失败: $module (查看日志: /tmp/build_${module}.log)"
                        else
                            log_success "模块构建成功: $module"
                        fi
                        rm -f "/tmp/build_${module}.status"
                    fi
                    
                    unset 'pids[$i]'
                    ((running--))
                fi
            done
            # 重建 pids 数组（移除已完成的）
            pids=("${pids[@]}")
        fi
    done
    
    # 检查是否有失败的模块
    if [ ${#failed[@]} -gt 0 ]; then
        log_error "以下模块构建失败: ${failed[*]}"
        return 1
    fi

    # 我在做：汇总并行构建产生的镜像列表到父进程 BUILT_IMAGES
    # 目的：父进程负责 export_images（docker save），需要拿到所有成功构建的镜像名
    # 如果不这样做：BUILT_IMAGES 仍为空，export_images 将跳过导出，导致桌面上没有 tar 包
    for module in "${modules[@]}"; do
        if [ -f "/tmp/build_${module}.image" ]; then
            BUILT_IMAGES+=("$(cat "/tmp/build_${module}.image")")
            rm -f "/tmp/build_${module}.image"
        fi
    done

    log_success "所有模块并行构建完成"
    return 0
}

# 串行构建模块
build_modules_serial() {
    local modules=("$@")
    local failed=()
    
    log_info "串行构建 ${#modules[@]} 个模块"
    
    for module in "${modules[@]}"; do
        if ! build_module "$module"; then
            failed+=("$module")
            log_error "模块构建失败: $module"
            # 根据需要决定是否继续构建其他模块
            # return 1  # 取消注释以在第一个失败时停止
        fi
    done
    
    if [ ${#failed[@]} -gt 0 ]; then
        log_error "以下模块构建失败: ${failed[*]}"
        return 1
    fi
    
    log_success "所有模块串行构建完成"
    return 0
}

# 导出镜像
export_images() {
    if [ "$BUILD_ONLY" = true ]; then
        log_warn "跳过镜像导出（BUILD_ONLY=true）"
        return 0
    fi
    
    if [ ${#BUILT_IMAGES[@]} -eq 0 ]; then
        log_warn "没有构建的镜像需要导出"
        return 0
    fi
    
    # 确保输出目录存在
    local output_dir
    output_dir="$(dirname "$PACKAGE_PATH")"
    if [ ! -d "$output_dir" ]; then
        log_info "创建输出目录: $output_dir"
        mkdir -p "$output_dir" || { log_error "无法创建输出目录: $output_dir"; return 1; }
    fi

    log_info "导出 ${#BUILT_IMAGES[@]} 个镜像到: $PACKAGE_PATH"
    
    # 检查磁盘空间
    local required_space=10000000  # 10GB in KB
    local available_space
    available_space=$(df -k "$output_dir" 2>/dev/null | tail -1 | awk '{print $4}')
    available_space=${available_space:-0}
    
    if [ "$available_space" -lt "$required_space" ]; then
        log_warn "磁盘空间可能不足（可用: $((available_space/1024/1024))GB）"
    fi
    
    # 构建 docker save 命令
    if docker save "${BUILT_IMAGES[@]}" > "$PACKAGE_PATH"; then
        local file_size=$(du -h "$PACKAGE_PATH" | cut -f1)
        log_success "镜像导出完成: $PACKAGE_PATH (大小: $file_size)"
        
        # 显示导出的镜像列表
        log_info "已导出的镜像:"
        for image in "${BUILT_IMAGES[@]}"; do
            echo "  - $image"
        done
    else
        log_error "镜像导出失败"
        return 1
    fi
}

# ============================================================================
# 主函数
# ============================================================================

main() {
    local build_all=false
    local modules_to_build=()
    
    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -a|--all)
                build_all=true
                shift
                ;;
            -l|--list)
                list_modules
                exit 0
                ;;
            -v|--version)
                IMAGE_VERSION="$2"
                shift 2
                ;;
            -o|--output)
                PACKAGE_PATH="$2"
                shift 2
                ;;
            -p|--parallel)
                PARALLEL_BUILD=true
                shift
                ;;
            -j|--jobs)
                MAX_JOBS="$2"
                shift 2
                ;;
            -s|--skip-init)
                SKIP_INIT=true
                shift
                ;;
            -b|--build-only)
                BUILD_ONLY=true
                shift
                ;;
            --registry)
                REGISTRY="$2"
                shift 2
                ;;
            -*)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
            *)
                # 作为模块名处理
                if [ -n "${MODULES[$1]}" ]; then
                    modules_to_build+=("$1")
                else
                    log_error "未知模块: $1"
                    exit 1
                fi
                shift
                ;;
        esac
    done
    
    # 确定要构建的模块
    if [ "$build_all" = true ] || [ ${#modules_to_build[@]} -eq 0 ]; then
        log_info "构建所有模块"
        modules_to_build=($(printf '%s\n' "${!MODULES[@]}" | sort))
    fi
    
    # 显示构建配置
    echo ""
    log_info "=========================================="
    log_info "MeterSphere 构建配置"
    log_info "=========================================="
    log_info "项目路径: $PROJECT_PATH"
    log_info "镜像版本: $IMAGE_VERSION"
    log_info "镜像仓库: $REGISTRY"
    log_info "输出路径: $PACKAGE_PATH"
    log_info "并行构建: $PARALLEL_BUILD"
    [ "$PARALLEL_BUILD" = true ] && log_info "并行任务数: $MAX_JOBS"
    log_info "跳过初始化: $SKIP_INIT"
    log_info "只构建不导出: $BUILD_ONLY"
    log_info "构建模块: ${modules_to_build[*]}"
    log_info "=========================================="
    echo ""
    
    # 开始构建流程
    local start_time=$(date +%s)
    
    # 1. 检查环境
    check_environment
    
    # 2. 初始化依赖
    if ! init_dependencies; then
        log_error "依赖初始化失败"
        exit 1
    fi
    
    # 3. 构建模块
    if [ "$PARALLEL_BUILD" = true ]; then
        if ! build_modules_parallel "${modules_to_build[@]}"; then
            log_error "模块构建失败"
            exit 1
        fi
    else
        if ! build_modules_serial "${modules_to_build[@]}"; then
            log_error "模块构建失败"
            exit 1
        fi
    fi
    
    # 4. 导出镜像
    if ! export_images; then
        log_error "镜像导出失败"
        exit 1
    fi
    
    # 显示构建总结
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    local minutes=$((duration / 60))
    local seconds=$((duration % 60))
    
    echo ""
    log_info "=========================================="
    log_success "构建完成！"
    log_info "=========================================="
    log_info "构建时间: ${minutes}分${seconds}秒"
    log_info "构建模块数: ${#modules_to_build[@]}"
    log_info "成功镜像数: ${#BUILT_IMAGES[@]}"
    [ "$BUILD_ONLY" = false ] && log_info "导出文件: $PACKAGE_PATH"
    log_info "=========================================="
    echo ""
    
    # 显示后续操作提示
    if [ "$BUILD_ONLY" = false ]; then
        echo "后续操作:"
        echo "  1. 在其他机器上加载镜像:"
        echo "     docker load < $PACKAGE_PATH"
        echo ""
        echo "  2. 查看已导出的镜像:"
        echo "     docker images | grep $REGISTRY"
        echo ""
    fi
}

# 执行主函数
main "$@"
