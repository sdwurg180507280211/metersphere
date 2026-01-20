#!/bin/bash

# Docker 镜像仓库管理脚本
# 用于管理阿里云容器镜像服务登录和镜像拉取

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

# 阿里云镜像仓库配置
ALIYUN_REGISTRY="crpi-tysjadjbz5afeai8.cn-beijing.personal.cr.aliyuncs.com"
ALIYUN_USERNAME="aliyun1688079337"

# 显示帮助信息
show_help() {
    echo "Docker 镜像仓库管理脚本"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  login         登录阿里云容器镜像服务"
    echo "  logout        登出镜像仓库"
    echo "  test          测试仓库连接"
    echo "  pull [image]  拉取指定镜像"
    echo "  list          列出本地镜像"
    echo "  clean         清理无用镜像"
    echo "  config        显示当前配置"
    echo "  help          显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 login                    # 登录阿里云镜像仓库"
    echo "  $0 pull mysql:8.0.36        # 拉取 MySQL 镜像"
    echo "  $0 test                     # 测试连接"
}

# 登录阿里云镜像仓库
login_registry() {
    print_message $BLUE "登录阿里云容器镜像服务..."
    echo ""
    print_message $YELLOW "仓库地址: $ALIYUN_REGISTRY"
    print_message $YELLOW "用户名: $ALIYUN_USERNAME"
    echo ""
    
    # 方式一：交互式登录
    print_message $BLUE "方式一：交互式登录"
    echo "docker login --username=$ALIYUN_USERNAME $ALIYUN_REGISTRY"
    echo ""
    
    # 提供故障排除建议
    print_message $YELLOW "如果登录失败，请尝试以下解决方案："
    echo ""
    echo "1. 检查网络连接："
    echo "   ping $ALIYUN_REGISTRY"
    echo ""
    echo "2. 检查 DNS 解析："
    echo "   nslookup $ALIYUN_REGISTRY"
    echo ""
    echo "3. 使用 HTTP 代理（如果在企业网络环境）："
    echo "   export HTTP_PROXY=http://proxy.company.com:8080"
    echo "   export HTTPS_PROXY=http://proxy.company.com:8080"
    echo ""
    echo "4. 清理 Docker 配置："
    echo "   rm -rf ~/.docker/config.json"
    echo ""
    echo "5. 重启 Docker 服务："
    echo "   sudo systemctl restart docker  # Linux"
    echo "   # 或重启 Docker Desktop      # macOS/Windows"
    echo ""
    
    # 询问是否立即尝试登录
    read -p "是否现在尝试登录? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker login --username=$ALIYUN_USERNAME $ALIYUN_REGISTRY
        
        if [ $? -eq 0 ]; then
            print_message $GREEN "✅ 登录成功！"
        else
            print_message $RED "❌ 登录失败！"
            echo ""
            print_message $YELLOW "请检查以下问题："
            echo "1. 密码是否正确"
            echo "2. 网络连接是否正常"
            echo "3. 是否需要配置代理"
            echo "4. Docker 服务是否正常运行"
        fi
    fi
}

# 登出镜像仓库
logout_registry() {
    print_message $BLUE "登出镜像仓库..."
    docker logout $ALIYUN_REGISTRY
    
    if [ $? -eq 0 ]; then
        print_message $GREEN "✅ 登出成功！"
    else
        print_message $RED "❌ 登出失败！"
    fi
}

# 测试仓库连接
test_connection() {
    print_message $BLUE "测试仓库连接..."
    
    # 测试网络连接
    echo -n "网络连接测试: "
    if ping -c 1 -W 5 $ALIYUN_REGISTRY >/dev/null 2>&1; then
        print_message $GREEN "✅ 网络连接正常"
    else
        print_message $RED "❌ 网络连接失败"
        echo "请检查网络设置或防火墙配置"
    fi
    
    # 测试 DNS 解析
    echo -n "DNS 解析测试: "
    if nslookup $ALIYUN_REGISTRY >/dev/null 2>&1; then
        print_message $GREEN "✅ DNS 解析正常"
    else
        print_message $RED "❌ DNS 解析失败"
        echo "请检查 DNS 设置"
    fi
    
    # 测试 HTTPS 连接
    echo -n "HTTPS 连接测试: "
    if curl -s --connect-timeout 10 https://$ALIYUN_REGISTRY/v2/ >/dev/null 2>&1; then
        print_message $GREEN "✅ HTTPS 连接正常"
    else
        print_message $RED "❌ HTTPS 连接失败"
        echo "可能需要配置代理或检查证书"
    fi
    
    # 检查 Docker 登录状态
    echo -n "Docker 登录状态: "
    if docker info | grep -q "Username"; then
        print_message $GREEN "✅ 已登录"
        docker info | grep "Username"
    else
        print_message $YELLOW "⚠️  未登录"
    fi
}

# 拉取镜像
pull_image() {
    local image=$1
    
    if [ -z "$image" ]; then
        print_message $RED "❌ 请指定要拉取的镜像名称"
        echo "示例: $0 pull mysql:8.0.36"
        exit 1
    fi
    
    print_message $BLUE "拉取镜像: $image"
    
    # 如果镜像名不包含仓库地址，则添加阿里云仓库前缀
    if [[ $image != *"$ALIYUN_REGISTRY"* ]]; then
        full_image="$ALIYUN_REGISTRY/$image"
        print_message $YELLOW "使用完整镜像地址: $full_image"
    else
        full_image=$image
    fi
    
    docker pull $full_image
    
    if [ $? -eq 0 ]; then
        print_message $GREEN "✅ 镜像拉取成功！"
    else
        print_message $RED "❌ 镜像拉取失败！"
        echo ""
        print_message $YELLOW "可能的原因："
        echo "1. 镜像名称或标签不存在"
        echo "2. 没有访问权限"
        echo "3. 网络连接问题"
        echo "4. 未登录或登录已过期"
    fi
}

# 列出本地镜像
list_images() {
    print_message $BLUE "本地镜像列表:"
    docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedAt}}\t{{.Size}}"
}

# 清理无用镜像
clean_images() {
    print_message $YELLOW "清理无用镜像..."
    
    echo "1. 清理悬空镜像 (dangling images):"
    docker image prune -f
    
    echo ""
    echo "2. 清理未使用的镜像:"
    read -p "是否清理所有未使用的镜像? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker image prune -a -f
        print_message $GREEN "✅ 镜像清理完成！"
    else
        print_message $BLUE "跳过清理未使用的镜像。"
    fi
}

# 显示当前配置
show_config() {
    print_message $BLUE "当前配置信息:"
    echo ""
    echo "阿里云镜像仓库:"
    echo "  地址: $ALIYUN_REGISTRY"
    echo "  用户名: $ALIYUN_USERNAME"
    echo ""
    echo "Docker 信息:"
    docker version --format '  版本: {{.Client.Version}}'
    echo ""
    echo "Docker 配置文件:"
    if [ -f ~/.docker/config.json ]; then
        echo "  位置: ~/.docker/config.json"
        echo "  大小: $(ls -lh ~/.docker/config.json | awk '{print $5}')"
    else
        echo "  配置文件不存在"
    fi
    echo ""
    echo "已登录的仓库:"
    if [ -f ~/.docker/config.json ]; then
        cat ~/.docker/config.json | jq -r '.auths | keys[]' 2>/dev/null || echo "  无法解析配置文件"
    else
        echo "  无"
    fi
}

# 主逻辑
case "${1:-help}" in
    login)
        login_registry
        ;;
    logout)
        logout_registry
        ;;
    test)
        test_connection
        ;;
    pull)
        pull_image "$2"
        ;;
    list)
        list_images
        ;;
    clean)
        clean_images
        ;;
    config)
        show_config
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