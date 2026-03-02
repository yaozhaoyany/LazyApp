#!/bin/bash
# LazyApp 部署脚本 - 从 Windows 同步代码到 WSL 并重新构建
# 用法: 在 WSL 中运行 bash ~/LazyApp/deploy.sh
#   或首次部署: bash ~/deploy.sh

set -e

PROJECT_DIR="$HOME/LazyApp"
WINDOWS_SOURCE="/mnt/c/Users/Tony_Yao1/CascadeProjects/LazyApp"
ENV_FILE="$PROJECT_DIR/.env"
ENV_BACKUP="/tmp/lazyapp-env-backup"

echo "🦥 LazyApp 部署开始..."

# 1. 备份 .env（如果存在）
if [ -f "$ENV_FILE" ]; then
    cp "$ENV_FILE" "$ENV_BACKUP"
    echo "✅ .env 已备份"
else
    echo "⚠️  未找到 .env，稍后需要手动配置"
fi

# 2. 停止现有容器（如果在运行）
if [ -f "$PROJECT_DIR/docker-compose.yml" ]; then
    echo "⏹️  停止现有容器..."
    cd "$PROJECT_DIR"
    docker compose down 2>/dev/null || true
fi

# 3. 同步代码（删除旧代码，复制新代码）
echo "📦 同步代码..."
rm -rf "$PROJECT_DIR"
cp -r "$WINDOWS_SOURCE" "$PROJECT_DIR"

# 4. 恢复 .env
if [ -f "$ENV_BACKUP" ]; then
    cp "$ENV_BACKUP" "$ENV_FILE"
    echo "✅ .env 已恢复"
elif [ ! -f "$ENV_FILE" ]; then
    # 首次部署，从模板创建
    if [ -f "$PROJECT_DIR/.env.example" ]; then
        cp "$PROJECT_DIR/.env.example" "$ENV_FILE"
        echo "⚠️  已从模板创建 .env，请编辑填入你的配置："
        echo "   nano $ENV_FILE"
    fi
fi

# 5. 构建并启动
echo "🔨 构建并启动容器..."
cd "$PROJECT_DIR"
docker compose up -d --build

# 6. 等待启动
echo ""
echo "⏳ 等待服务启动..."
sleep 5

# 7. 检查状态
echo ""
echo "📊 容器状态："
docker compose ps

echo ""
echo "🎉 部署完成！访问 http://localhost 使用 LazyApp"
echo "📋 查看日志: docker compose logs -f"
