#!/bin/bash

# 验证统一配置目录下的 MySQL 配置
# 确保 /opt/metersphere/conf/mysql 目录下的配置正确

MYSQL_CONF_DIR="/opt/metersphere/conf/mysql"
MYSQL_INIT_DIR="/opt/metersphere/conf/mysql/init"
CONTAINER_NAME="metersphere-mysql"
MYSQL_USER="root"
MYSQL_PASSWORD="Password123@mysql"

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

print_message $BLUE "🔍 验证 MySQL 统一配置..."
echo ""

# 检查配置目录
print_message $YELLOW "📁 检查配置目录结构:"
if [ -d "$MYSQL_CONF_DIR" ]; then
    print_message $GREEN "✅ 配置目录存在: $MYSQL_CONF_DIR"
    ls -la "$MYSQL_CONF_DIR"
else
    print_message $RED "❌ 配置目录不存在: $MYSQL_CONF_DIR"
    exit 1
fi

echo ""

# 检查配置文件
print_message $YELLOW "📄 检查配置文件:"
if [ -f "$MYSQL_CONF_DIR/my.cnf" ]; then
    print_message $GREEN "✅ MySQL 配置文件存在"
    echo "文件大小: $(wc -c < "$MYSQL_CONF_DIR/my.cnf") 字节"
    echo "修改时间: $(stat -f "%Sm" "$MYSQL_CONF_DIR/my.cnf")"
else
    print_message $RED "❌ MySQL 配置文件不存在"
fi

echo ""

# 检查初始化脚本
print_message $YELLOW "📜 检查初始化脚本:"
if [ -d "$MYSQL_INIT_DIR" ]; then
    script_count=$(ls -1 "$MYSQL_INIT_DIR"/*.sql 2>/dev/null | wc -l)
    if [ "$script_count" -gt 0 ]; then
        print_message $GREEN "✅ 找到 $script_count 个初始化脚本"
        ls -la "$MYSQL_INIT_DIR"
    else
        print_message $RED "❌ 初始化脚本目录为空"
    fi
else
    print_message $RED "❌ 初始化脚本目录不存在"
fi

echo ""

# 检查 Docker 容器状态
print_message $YELLOW "🐳 检查 Docker 容器状态:"
if docker ps | grep -q "$CONTAINER_NAME"; then
    print_message $GREEN "✅ MySQL 容器正在运行"
    
    # 验证配置文件挂载
    print_message $BLUE "🔗 验证配置文件挂载:"
    if docker exec "$CONTAINER_NAME" test -f /etc/mysql/my.cnf; then
        print_message $GREEN "✅ 配置文件已正确挂载到容器"
        
        # 检查配置文件内容
        config_size=$(docker exec "$CONTAINER_NAME" wc -c < /etc/mysql/my.cnf)
        print_message $GREEN "容器内配置文件大小: $config_size 字节"
        
        # 验证字符集配置
        print_message $BLUE "🔤 验证字符集配置:"
        charset_server=$(docker exec "$CONTAINER_NAME" mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SHOW VARIABLES LIKE 'character_set_server';" 2>/dev/null | tail -n 1 | awk '{print $2}')
        if [ "$charset_server" = "utf8mb4" ]; then
            print_message $GREEN "✅ 服务器字符集: $charset_server"
        else
            print_message $RED "❌ 服务器字符集异常: $charset_server"
        fi
        
        charset_client=$(docker exec "$CONTAINER_NAME" mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SHOW VARIABLES LIKE 'character_set_client';" 2>/dev/null | tail -n 1 | awk '{print $2}')
        if [ "$charset_client" = "utf8mb4" ]; then
            print_message $GREEN "✅ 客户端字符集: $charset_client"
        else
            print_message $RED "❌ 客户端字符集异常: $charset_client"
        fi
        
    else
        print_message $RED "❌ 配置文件未挂载到容器"
    fi
    
    # 验证初始化脚本执行结果
    print_message $BLUE "🗄️  验证数据库初始化:"
    databases=$(docker exec "$CONTAINER_NAME" mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SHOW DATABASES;" 2>/dev/null | grep -E "(metersphere_test|metersphere_workflow|metersphere_audit)" | wc -l)
    if [ "$databases" -eq 3 ]; then
        print_message $GREEN "✅ 所有必需数据库已创建"
    else
        print_message $RED "❌ 缺少必需数据库 (找到 $databases 个，期望 3 个)"
    fi
    
elif docker ps -a | grep -q "$CONTAINER_NAME"; then
    print_message $YELLOW "⚠️  MySQL 容器存在但未运行"
else
    print_message $RED "❌ MySQL 容器不存在"
fi

echo ""

# 显示配置文件内容摘要
print_message $YELLOW "📋 配置文件内容摘要:"
if [ -f "$MYSQL_CONF_DIR/my.cnf" ]; then
    echo "----------------------------------------"
    echo "主要配置项:"
    grep -E "^(character_set_server|collation-server|lower_case_table_names|max_connections|innodb_buffer_pool_size)" "$MYSQL_CONF_DIR/my.cnf" | head -10
    echo "----------------------------------------"
fi

echo ""
print_message $BLUE "✨ 配置验证完成！"

# 提供管理建议
echo ""
print_message $YELLOW "💡 管理建议:"
echo "1. 配置文件位置: $MYSQL_CONF_DIR/my.cnf"
echo "2. 初始化脚本: $MYSQL_INIT_DIR/"
echo "3. 编辑配置: ./scripts/mysql-config-manager.sh edit"
echo "4. 同步配置: ./scripts/mysql-config-manager.sh sync"
echo "5. 重启容器: docker-compose -f docker-compose-dev.yml restart mysql"