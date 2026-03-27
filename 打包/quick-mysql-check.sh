#!/bin/bash

# 快速检查 MySQL 配置状态
# 用于快速验证配置是否正确

MYSQL_CONF_DIR="/opt/metersphere/conf/mysql"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🔍 快速 MySQL 配置检查"
echo "======================="

# 检查配置目录
if [ -d "$MYSQL_CONF_DIR" ]; then
    echo -e "${GREEN}✅ 配置目录存在${NC}"
else
    echo -e "${RED}❌ 配置目录不存在${NC}"
    exit 1
fi

# 检查配置文件
if [ -f "$MYSQL_CONF_DIR/my.cnf" ]; then
    echo -e "${GREEN}✅ 配置文件存在${NC}"
    file_size=$(wc -c < "$MYSQL_CONF_DIR/my.cnf")
    echo "   文件大小: $file_size 字节"
else
    echo -e "${RED}❌ 配置文件缺失${NC}"
fi

# 检查初始化脚本
init_count=$(ls -1 "$MYSQL_CONF_DIR/init"/*.sql 2>/dev/null | wc -l)
if [ "$init_count" -gt 0 ]; then
    echo -e "${GREEN}✅ 初始化脚本: $init_count 个${NC}"
else
    echo -e "${RED}❌ 初始化脚本缺失${NC}"
fi

# 检查 Docker Compose 配置
if grep -q "/opt/metersphere/conf/mysql" docker-compose-dev.yml 2>/dev/null; then
    echo -e "${GREEN}✅ Docker Compose 配置正确${NC}"
else
    echo -e "${YELLOW}⚠️  Docker Compose 配置可能需要更新${NC}"
fi

echo ""
echo "💡 管理提示:"
echo "   编辑配置: ./scripts/dev-env.sh config edit"
echo "   完整验证: ./scripts/dev-env.sh verify"