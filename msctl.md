#!/bin/bash
#===============================================================================
# MeterSphere 2.10 控制脚本 (优化版)
# 版本: 2.0
#===============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 基础配置
MS_BASE=${MS_BASE:-/opt}
MS_HOME="${MS_BASE}/metersphere"
COMPOSE_FILES=""

# 加载配置
[[ -f ~/.msrc ]] && source ~/.msrc >/dev/null 2>&1
[[ -f ${MS_HOME}/install.conf ]] && source ${MS_HOME}/install.conf
[[ -f ${MS_HOME}/compose_files ]] && COMPOSE_FILES=$(cat ${MS_HOME}/compose_files 2>/dev/null)

export COMPOSE_HTTP_TIMEOUT=180

#===============================================================================
# 工具函数
#===============================================================================

log_info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()    { echo -e "${CYAN}==>${NC} $1"; }

# 检查服务是否存在
check_service() {
local service=$1
if ! docker-compose ${COMPOSE_FILES} config --services 2>/dev/null | grep -q "^${service}$"; then
log_error "服务 '${service}' 不存在"
echo "可用服务列表："
docker-compose ${COMPOSE_FILES} config --services 2>/dev/null | sed 's/^/  - /'
exit 1
fi
}

# 等待服务健康
wait_healthy() {
local service=$1
local timeout=${2:-120}
local elapsed=0

    log_step "等待 ${service} 启动..."
    while [ $elapsed -lt $timeout ]; do
        local status=$(docker inspect --format='{{.State.Health.Status}}' ${service} 2>/dev/null || echo "unknown")
        case $status in
            healthy)
                log_info "${service} 已就绪 ✓"
                return 0
                ;;
            unhealthy)
                log_error "${service} 启动失败"
                return 1
                ;;
        esac
        sleep 2
        elapsed=$((elapsed + 2))
        echo -ne "\r  已等待 ${elapsed}s / ${timeout}s"
    done
    echo
    log_warn "${service} 启动超时"
    return 1
}

#===============================================================================
# 核心功能
#===============================================================================

usage() {
cat << EOF
${CYAN}MeterSphere 2.10 控制脚本${NC}

${YELLOW}用法:${NC}
msctl <命令> [服务名] [选项]

${YELLOW}服务管理:${NC}
status              查看所有服务状态
start   [服务]      启动服务 (不指定则启动全部)
stop    [服务]      停止服务 (不指定则停止全部)
restart [服务]      重启服务 (不指定则重启全部)
reload              重新加载配置并启动

${YELLOW}日志查看:${NC}
logs    <服务>      查看服务日志 (实时)
logs    <服务> -n50 查看最近50行日志

${YELLOW}调试运维:${NC}
exec    <服务>      进入服务容器
health              检查所有服务健康状态
pull    [服务]      拉取最新镜像

${YELLOW}系统管理:${NC}
version             查看版本信息
upgrade [版本号]    升级到指定版本
uninstall           卸载 MeterSphere
backup              备份数据库
clean               清理无用镜像和容器

${YELLOW}示例:${NC}
msctl status                    # 查看状态
msctl restart test-track        # 重启测试跟踪模块
msctl logs api-test             # 查看 API 测试日志
msctl logs gateway -n 100       # 查看网关最近100行日志
msctl exec mysql                # 进入 MySQL 容器
msctl pull test-track           # 拉取测试跟踪最新镜像

EOF
}

# 状态查看 (增强版)
status() {
echo
log_step "MeterSphere 服务状态"
echo
cd ${MS_HOME}
docker-compose ${COMPOSE_FILES} ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || \
docker-compose ${COMPOSE_FILES} ps
echo

    # 显示资源使用
    log_step "资源使用概览"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" \
        $(docker-compose ${COMPOSE_FILES} ps -q 2>/dev/null) 2>/dev/null | head -20
}

# 启动服务
start() {
local target=$1
cd ${MS_HOME}

    if [ -n "$target" ]; then
        check_service "$target"
        log_step "启动服务: ${target}"
        docker-compose ${COMPOSE_FILES} start ${target}
    else
        log_step "启动所有服务"
        docker-compose ${COMPOSE_FILES} start
    fi
    log_info "启动完成 ✓"
}

# 停止服务
stop() {
local target=$1
cd ${MS_HOME}

    if [ -n "$target" ]; then
        check_service "$target"
        log_step "停止服务: ${target}"
        docker-compose ${COMPOSE_FILES} stop ${target}
    else
        log_step "停止所有服务"
        docker-compose ${COMPOSE_FILES} stop
    fi
    log_info "停止完成 ✓"
}

# 重启服务 (优化: 先stop再start，避免restart可能的问题)
restart() {
local target=$1
cd ${MS_HOME}

    if [ -n "$target" ]; then
        check_service "$target"
        log_step "重启服务: ${target}"
        docker-compose ${COMPOSE_FILES} stop ${target}
        docker-compose ${COMPOSE_FILES} start ${target}
        wait_healthy ${target} 60 || true
    else
        log_step "重启所有服务"
        docker-compose ${COMPOSE_FILES} stop
        docker-compose ${COMPOSE_FILES} start
    fi
    log_info "重启完成 ✓"
}

# 重新加载 (支持指定服务)
reload() {
local target=$1
cd ${MS_HOME}

    if [ -n "$target" ]; then
        check_service "$target"
        log_step "重载服务: ${target}"
        docker-compose ${COMPOSE_FILES} pull ${target}
        docker-compose ${COMPOSE_FILES} up -d --no-deps ${target}
        wait_healthy ${target} 60 || true
    else
        log_step "重新生成 compose 配置..."
        generate_compose_files
        
        log_step "拉取所有镜像..."
        docker-compose ${COMPOSE_FILES} pull
        
        log_step "重新创建容器..."
        docker-compose ${COMPOSE_FILES} up -d --remove-orphans
    fi
    
    log_info "重载完成 ✓"
}

# 查看日志 (新增)
logs() {
local target=$1
shift
local args="$@"

    if [ -z "$target" ]; then
        log_error "请指定服务名"
        echo "用法: msctl logs <服务名> [-n 行数]"
        echo "示例: msctl logs test-track -n 100"
        exit 1
    fi
    
    cd ${MS_HOME}
    check_service "$target"
    
    # 默认 follow 模式，除非指定了 -n
    if echo "$args" | grep -q "\-n"; then
        docker-compose ${COMPOSE_FILES} logs --tail=${args#*-n } ${target}
    else
        docker-compose ${COMPOSE_FILES} logs -f --tail=100 ${target}
    fi
}

# 进入容器 (新增)
exec_container() {
local target=$1

    if [ -z "$target" ]; then
        log_error "请指定服务名"
        echo "用法: msctl exec <服务名>"
        exit 1
    fi
    
    cd ${MS_HOME}
    check_service "$target"
    
    log_info "进入容器: ${target}"
    docker-compose ${COMPOSE_FILES} exec ${target} /bin/bash 2>/dev/null || \
    docker-compose ${COMPOSE_FILES} exec ${target} /bin/sh
}

# 健康检查 (新增)
health() {
echo
log_step "服务健康检查"
echo
printf "%-25s %-15s %-10s\n" "服务" "状态" "健康"
printf "%-25s %-15s %-10s\n" "------------------------" "--------------" "---------"

    cd ${MS_HOME}
    for container in $(docker-compose ${COMPOSE_FILES} ps -q 2>/dev/null); do
        local name=$(docker inspect --format='{{.Name}}' $container | sed 's/\///')
        local status=$(docker inspect --format='{{.State.Status}}' $container)
        local health=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}N/A{{end}}' $container)
        
        # 颜色标记
        local health_color=$NC
        case $health in
            healthy)   health_color=$GREEN ;;
            unhealthy) health_color=$RED ;;
            starting)  health_color=$YELLOW ;;
        esac
        
        printf "%-25s %-15s ${health_color}%-10s${NC}\n" "$name" "$status" "$health"
    done
    echo
}

# 拉取镜像 (新增)
pull() {
local target=$1
cd ${MS_HOME}

    if [ -n "$target" ]; then
        check_service "$target"
        log_step "拉取镜像: ${target}"
        docker-compose ${COMPOSE_FILES} pull ${target}
    else
        log_step "拉取所有镜像"
        docker-compose ${COMPOSE_FILES} pull
    fi
    log_info "拉取完成 ✓"
}

# 备份数据库 (新增)
backup() {
local backup_dir="${MS_HOME}/backup"
local backup_file="metersphere_$(date +%Y%m%d_%H%M%S).sql"

    mkdir -p ${backup_dir}
    
    log_step "备份数据库..."
    
    if [ "${MS_EXTERNAL_MYSQL}" = "true" ]; then
        log_warn "使用外部数据库，请手动备份"
        exit 1
    fi
    
    docker-compose ${COMPOSE_FILES} exec -T mysql mysqldump \
        -u${MS_MYSQL_USER:-root} \
        -p${MS_MYSQL_PASSWORD:-Password123@mysql} \
        --all-databases > ${backup_dir}/${backup_file}
    
    log_info "备份完成: ${backup_dir}/${backup_file}"
    ls -lh ${backup_dir}/${backup_file}
}

# 清理 (新增)
clean() {
log_step "清理无用镜像..."
docker image prune -f

    log_step "清理无用容器..."
    docker container prune -f
    
    log_step "清理无用网络..."
    docker network prune -f
    
    log_info "清理完成 ✓"
    
    echo
    log_step "磁盘使用情况"
    docker system df
}

# 版本信息
version() {
echo
cat ${MS_HOME}/version 2>/dev/null || echo "未知版本"
echo
log_step "各服务镜像版本:"
cd ${MS_HOME}
docker-compose ${COMPOSE_FILES} images 2>/dev/null | head -20
}

# 卸载
uninstall() {
echo
log_warn "即将卸载 MeterSphere，数据将被保留在 ${MS_HOME}/data"
read -p "确认卸载? (yes/no): " confirm

    if [ "$confirm" != "yes" ]; then
        log_info "取消卸载"
        exit 0
    fi
    
    cd ${MS_HOME}
    docker-compose ${COMPOSE_FILES} down
    rm -f ~/.msrc
    rm -f /usr/local/bin/msctl
    
    log_info "卸载完成"
    log_warn "数据目录 ${MS_HOME}/data 已保留，如需删除请手动执行"
}

# 生成 compose 文件 (保持原逻辑，略作优化)
generate_compose_files() {
compose_files="-f docker-compose-base.yml"

    mkdir -p ${MS_HOME}/data/{jmeter,body,api-folder,node}
    
    case ${MS_INSTALL_MODE} in
        allinone)
            compose_files="${compose_files} -f docker-compose-data-streaming.yml -f docker-compose-node-controller.yml -f docker-compose-eureka.yml -f docker-compose-gateway.yml -f docker-compose-api-test.yml -f docker-compose-performance-test.yml -f docker-compose-project-management.yml -f docker-compose-system-setting.yml -f docker-compose-report-stat.yml -f docker-compose-test-track.yml -f docker-compose-workstation.yml"
            ;;
        server)
            compose_files="${compose_files} -f docker-compose-eureka.yml -f docker-compose-gateway.yml -f docker-compose-api-test.yml -f docker-compose-performance-test.yml -f docker-compose-project-management.yml -f docker-compose-system-setting.yml -f docker-compose-report-stat.yml -f docker-compose-test-track.yml -f docker-compose-workstation.yml"
            ;;
        node-controller)
            compose_files="${compose_files} -f docker-compose-node-controller.yml"
            ;;
        *)
            log_error "不支持的安装模式: ${MS_INSTALL_MODE}"
            exit 1
            ;;
    esac
    
    # 添加中间件
    [[ "${MS_EXTERNAL_MYSQL}" != "true" ]] && compose_files="${compose_files} -f docker-compose-mysql.yml"
    [[ "${MS_EXTERNAL_KAFKA}" != "true" ]] && compose_files="${compose_files} -f docker-compose-kafka.yml"
    [[ "${MS_EXTERNAL_PROM}" != "true" ]] && compose_files="${compose_files} -f docker-compose-prometheus.yml"
    [[ "${MS_EXTERNAL_REDIS}" != "true" ]] && compose_files="${compose_files} -f docker-compose-redis.yml"
    [[ "${MS_EXTERNAL_MINIO}" != "true" ]] && compose_files="${compose_files} -f docker-compose-minio.yml"
    
    echo ${compose_files} > ${MS_HOME}/compose_files
    COMPOSE_FILES=${compose_files}
    
    # 设置权限
    chmod 777 -R ${MS_HOME}/logs ${MS_HOME}/data/body ${MS_HOME}/data/api-folder ${MS_HOME}/data/node 2>/dev/null || true
}

#===============================================================================
# 主入口
#===============================================================================

main() {
local action=$1
local target=$2
shift 2 2>/dev/null || true
local args="$@"

    # 检查目录
    if [ ! -d "${MS_HOME}" ]; then
        log_error "MeterSphere 目录不存在: ${MS_HOME}"
        exit 1
    fi
    
    case "${action}" in
        status)     status ;;
        start)      start "$target" ;;
        stop)       stop "$target" ;;
        restart)    restart "$target" ;;
        reload)     reload "$target" ;;
        logs)       logs "$target" $args ;;
        exec)       exec_container "$target" ;;
        health)     health ;;
        pull)       pull "$target" ;;
        backup)     backup ;;
        clean)      clean ;;
        version)    version ;;
        uninstall)  uninstall ;;
        help|--help|-h) usage ;;
        "")         usage ;;
        *)
            # 透传其他 docker-compose 命令
            cd ${MS_HOME}
            docker-compose ${COMPOSE_FILES} ${action} ${target} ${args}
            ;;
    esac
}

main "$@"
