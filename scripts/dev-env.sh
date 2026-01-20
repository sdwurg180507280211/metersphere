#!/bin/bash

# MeterSphere 开发环境管理脚本
# 用于管理 MySQL、Redis、Kafka、MinIO 等中间件容器

COMPOSE_FILE="docker-compose-dev.yml"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# 显示帮助信息
show_help() {
    echo "MeterSphere 开发环境管理脚本"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  start     启动所有中间件服务"
    echo "  stop      停止所有中间件服务"
    echo "  restart   重启所有中间件服务"
    echo "  status    查看服务状态"
    echo "  logs      查看服务日志"
    echo "  clean     清理所有容器和数据卷"
    echo "  test      测试所有服务连接"
    echo "  verify    验证 MySQL 配置"
    echo "  config    管理 MySQL 配置 (init|edit|show|check|backup)"
    echo "  registry  管理 Docker 镜像仓库 (login|test|pull|list|clean)"
    echo "  help      显示此帮助信息"
    echo ""
    echo "服务端口:"
    echo "  MySQL:    3306"
    echo "  Redis:    6379"
    echo "  Kafka:    9092"
    echo "  MinIO:    9000 (API), 9001 (Console)"
}

# 启动服务
start_services() {
    print_message $BLUE "启动 MeterSphere 开发环境..."
    docker-compose -f $COMPOSE_FILE up -d
    
    if [ $? -eq 0 ]; then
        print_message $GREEN "✅ 所有服务启动成功！"
        echo ""
        show_connection_info
    else
        print_message $RED "❌ 服务启动失败！"
        exit 1
    fi
}

# 停止服务
stop_services() {
    print_message $YELLOW "停止 MeterSphere 开发环境..."
    docker-compose -f $COMPOSE_FILE down
    
    if [ $? -eq 0 ]; then
        print_message $GREEN "✅ 所有服务已停止！"
    else
        print_message $RED "❌ 服务停止失败！"
        exit 1
    fi
}

# 重启服务
restart_services() {
    print_message $YELLOW "重启 MeterSphere 开发环境..."
    docker-compose -f $COMPOSE_FILE restart
    
    if [ $? -eq 0 ]; then
        print_message $GREEN "✅ 所有服务重启成功！"
    else
        print_message $RED "❌ 服务重启失败！"
        exit 1
    fi
}

# 查看状态
show_status() {
    print_message $BLUE "MeterSphere 开发环境状态:"
    docker-compose -f $COMPOSE_FILE ps
}

# 查看日志
show_logs() {
    if [ -n "$2" ]; then
        print_message $BLUE "查看 $2 服务日志:"
        docker-compose -f $COMPOSE_FILE logs -f $2
    else
        print_message $BLUE "查看所有服务日志:"
        docker-compose -f $COMPOSE_FILE logs -f
    fi
}

# 清理环境
clean_environment() {
    print_message $RED "⚠️  警告: 此操作将删除所有容器和数据卷！"
    read -p "确认继续? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_message $YELLOW "清理 MeterSphere 开发环境..."
        docker-compose -f $COMPOSE_FILE down -v --remove-orphans
        docker volume prune -f
        print_message $GREEN "✅ 环境清理完成！"
    else
        print_message $BLUE "操作已取消。"
    fi
}

# 测试服务连接
test_connections() {
    print_message $BLUE "测试服务连接..."
    
    # 测试 MySQL
    echo -n "MySQL (3306): "
    if nc -z localhost 3306 2>/dev/null; then
        print_message $GREEN "✅ 连接正常"
    else
        print_message $RED "❌ 连接失败"
    fi
    
    # 测试 Redis
    echo -n "Redis (6379): "
    if nc -z localhost 6379 2>/dev/null; then
        print_message $GREEN "✅ 连接正常"
    else
        print_message $RED "❌ 连接失败"
    fi
    
    # 测试 Kafka
    echo -n "Kafka (9092): "
    if nc -z localhost 9092 2>/dev/null; then
        print_message $GREEN "✅ 连接正常"
    else
        print_message $RED "❌ 连接失败"
    fi
    
    # 测试 MinIO
    echo -n "MinIO (9000): "
    if nc -z localhost 9000 2>/dev/null; then
        print_message $GREEN "✅ 连接正常"
    else
        print_message $RED "❌ 连接失败"
    fi
}

# 验证 MySQL 配置
verify_mysql_config() {
    print_message $BLUE "验证 MySQL 配置..."
    if [ -f "./scripts/verify-mysql-unified-config.sh" ]; then
        ./scripts/verify-mysql-unified-config.sh
    else
        print_message $RED "❌ 配置验证脚本不存在！"
    fi
}

# 管理 MySQL 配置
manage_mysql_config() {
    print_message $BLUE "管理 MySQL 配置..."
    if [ -f "./scripts/mysql-config-manager.sh" ]; then
        ./scripts/mysql-config-manager.sh "$@"
    else
        print_message $RED "❌ 配置管理脚本不存在！"
    fi
}

# 管理 Docker 镜像仓库
manage_docker_registry() {
    print_message $BLUE "管理 Docker 镜像仓库..."
    if [ -f "./scripts/docker-registry-helper.sh" ]; then
        ./scripts/docker-registry-helper.sh "$@"
    else
        print_message $RED "❌ Docker 镜像仓库管理脚本不存在！"
    fi
}

# 显示连接信息
show_connection_info() {
    print_message $BLUE "🔗 服务连接信息:"
    echo ""
    echo "📊 MySQL 数据库:"
    echo "   地址: localhost:3306"
    echo "   用户: root / metersphere"
    echo "   密码: Password123@mysql"
    echo "   数据库: metersphere_test"
    echo ""
    echo "🚀 Redis 缓存:"
    echo "   地址: localhost:6379"
    echo "   密码: Password123@redis"
    echo ""
    echo "📨 Kafka 消息队列:"
    echo "   地址: localhost:9092"
    echo "   无需认证"
    echo ""
    echo "📁 MinIO 对象存储:"
    echo "   API: http://localhost:9000"
    echo "   Console: http://localhost:9001"
    echo "   用户: minioadmin"
    echo "   密码: minioadmin123"
}

# 主逻辑
case "${1:-help}" in
    start)
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        restart_services
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs $@
        ;;
    clean)
        clean_environment
        ;;
    test)
        test_connections
        ;;
    config)
        shift
        manage_mysql_config "$@"
        ;;
    registry)
        shift
        manage_docker_registry "$@"
        ;;
    verify)
        verify_mysql_config
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_message $RED "未知命令: $1"
        echo ""
        show_help
        exit 1
        ;;
esac