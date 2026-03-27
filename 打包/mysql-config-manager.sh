#!/bin/bash

# MeterSphere MySQL 配置管理脚本
# 用于管理 /opt/metersphere/conf/mysql 目录下的配置文件

MYSQL_CONF_DIR="/opt/metersphere/conf/mysql"
MYSQL_INIT_DIR="/opt/metersphere/conf/mysql/init"

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
    echo "MeterSphere MySQL 配置管理脚本"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  init      初始化配置目录"
    echo "  edit      编辑 MySQL 配置文件"
    echo "  show      显示当前配置"
    echo "  check     检查配置文件权限"
    echo "  backup    备份当前配置"
    echo "  help      显示此帮助信息"
    echo ""
    echo "配置目录:"
    echo "  系统配置: $MYSQL_CONF_DIR"
}

# 初始化配置目录
init_config() {
    print_message $BLUE "初始化 MySQL 配置目录..."
    
    # 创建目录
    sudo mkdir -p "$MYSQL_CONF_DIR"
    sudo mkdir -p "$MYSQL_INIT_DIR"
    
    # 创建默认配置文件
    if [ ! -f "$MYSQL_CONF_DIR/my.cnf" ]; then
        create_default_config
        print_message $GREEN "✅ 创建默认配置文件"
    else
        print_message $YELLOW "⚠️  配置文件已存在，跳过创建"
    fi
    
    # 创建默认初始化脚本
    if [ ! -f "$MYSQL_INIT_DIR/01-init-databases.sql" ]; then
        create_default_init_script
        print_message $GREEN "✅ 创建默认初始化脚本"
    else
        print_message $YELLOW "⚠️  初始化脚本已存在，跳过创建"
    fi
    
    # 设置权限
    sudo chown -R $(whoami):$(id -gn) "$MYSQL_CONF_DIR"
    sudo chmod -R 755 "$MYSQL_CONF_DIR"
    
    print_message $GREEN "✅ 配置目录初始化完成！"
}

# 创建默认配置文件
create_default_config() {
    sudo tee "$MYSQL_CONF_DIR/my.cnf" > /dev/null << 'EOF'
[mysqld]
# 数据目录
datadir=/var/lib/mysql

# 存储引擎
default-storage-engine=INNODB

# 字符集配置
character_set_server=utf8mb4
character-set-client-handshake=FALSE
collation-server=utf8mb4_general_ci
init_connect='SET NAMES utf8mb4'

# 表名大小写不敏感
lower_case_table_names=1

# 性能优化
performance_schema=off
table_open_cache=128

# 事务隔离级别
transaction_isolation=READ-COMMITTED

# 连接配置
max_connections=1000
max_connect_errors=6000
max_allowed_packet=64M

# InnoDB 配置
innodb_file_per_table=1
innodb_buffer_pool_size=512M
innodb_flush_method=O_DIRECT
innodb_lock_wait_timeout=1800

# 二进制日志配置
server-id=1
log-bin=mysql-bin
expire_logs_days=2
binlog_format=mixed

# SQL 模式
sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION

# 跳过域名解析
skip-name-resolve

[mysql]
default-character-set=utf8mb4

[mysqldump]
default-character-set=utf8mb4

[client]
default-character-set=utf8mb4
EOF
}

# 创建默认初始化脚本
create_default_init_script() {
    sudo tee "$MYSQL_INIT_DIR/01-init-databases.sql" > /dev/null << 'EOF'
-- MeterSphere 数据库初始化脚本
-- 创建开发环境所需的数据库

-- 创建主数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `metersphere_test` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 创建工作流数据库
CREATE DATABASE IF NOT EXISTS `metersphere_workflow` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 创建审计日志数据库
CREATE DATABASE IF NOT EXISTS `metersphere_audit` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 授权给 metersphere 用户
GRANT ALL PRIVILEGES ON `metersphere_test`.* TO 'metersphere'@'%';
GRANT ALL PRIVILEGES ON `metersphere_workflow`.* TO 'metersphere'@'%';
GRANT ALL PRIVILEGES ON `metersphere_audit`.* TO 'metersphere'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 显示创建的数据库
SHOW DATABASES;
EOF
}

# 编辑配置文件
edit_config() {
    print_message $BLUE "编辑 MySQL 配置文件..."
    
    if [ ! -f "$MYSQL_CONF_DIR/my.cnf" ]; then
        print_message $RED "❌ 配置文件不存在: $MYSQL_CONF_DIR/my.cnf"
        print_message $YELLOW "💡 请先运行 init 命令初始化配置"
        exit 1
    fi
    
    # 使用默认编辑器编辑
    sudo ${EDITOR:-nano} "$MYSQL_CONF_DIR/my.cnf"
    
    print_message $GREEN "✅ 配置文件编辑完成！"
    print_message $YELLOW "⚠️  请重启 MySQL 容器以应用更改"
}

# 显示当前配置
show_config() {
    print_message $BLUE "📋 当前 MySQL 配置:"
    echo ""
    
    if [ -f "$MYSQL_CONF_DIR/my.cnf" ]; then
        print_message $GREEN "配置文件: $MYSQL_CONF_DIR/my.cnf"
        echo "----------------------------------------"
        cat "$MYSQL_CONF_DIR/my.cnf"
        echo "----------------------------------------"
    else
        print_message $RED "❌ 配置文件不存在"
    fi
    
    echo ""
    print_message $BLUE "📋 初始化脚本:"
    if [ -d "$MYSQL_INIT_DIR" ] && [ "$(ls -A $MYSQL_INIT_DIR)" ]; then
        ls -la "$MYSQL_INIT_DIR"
    else
        print_message $RED "❌ 初始化脚本目录为空"
    fi
}

# 检查配置文件权限
check_permissions() {
    print_message $BLUE "🔍 检查配置文件权限..."
    echo ""
    
    if [ -d "$MYSQL_CONF_DIR" ]; then
        print_message $GREEN "配置目录权限:"
        ls -la "$MYSQL_CONF_DIR"
        echo ""
        
        if [ -d "$MYSQL_INIT_DIR" ]; then
            print_message $GREEN "初始化脚本权限:"
            ls -la "$MYSQL_INIT_DIR"
        fi
    else
        print_message $RED "❌ 配置目录不存在"
    fi
}

# 备份当前配置
backup_config() {
    print_message $BLUE "备份当前配置..."
    
    if [ ! -d "$MYSQL_CONF_DIR" ]; then
        print_message $RED "❌ 配置目录不存在"
        exit 1
    fi
    
    local backup_dir="$MYSQL_CONF_DIR/backup"
    local timestamp=$(date +%Y%m%d_%H%M%S)
    
    sudo mkdir -p "$backup_dir"
    
    # 备份配置文件
    if [ -f "$MYSQL_CONF_DIR/my.cnf" ]; then
        sudo cp "$MYSQL_CONF_DIR/my.cnf" "$backup_dir/my.cnf.$timestamp"
        print_message $GREEN "✅ 备份配置文件: my.cnf.$timestamp"
    fi
    
    # 备份初始化脚本
    if [ -d "$MYSQL_INIT_DIR" ] && [ "$(ls -A $MYSQL_INIT_DIR)" ]; then
        sudo mkdir -p "$backup_dir/init_$timestamp"
        sudo cp "$MYSQL_INIT_DIR"/*.sql "$backup_dir/init_$timestamp/" 2>/dev/null || true
        print_message $GREEN "✅ 备份初始化脚本: init_$timestamp/"
    fi
    
    print_message $GREEN "✅ 配置备份完成！备份位置: $backup_dir"
}

# 主逻辑
case "${1:-help}" in
    init)
        init_config
        ;;
    edit)
        edit_config
        ;;
    show)
        show_config
        ;;
    check)
        check_permissions
        ;;
    backup)
        backup_config
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