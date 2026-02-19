# 构建命令 docker build  --platform linux/amd64 -t pgraph:latest-ubuntu-amd64 .
# 不带缓存 docker build  --no-cache --platform linux/amd64 -t pgraph:latest-ubuntu-amd64 .

# 运行命令 docker run -d --name pgraph -p 3897:3897 -v /host/path/to/data:/app/data -v /host/path/to/graph.conf:/etc/graph.conf pgraph:latest-ubuntu-amd64
# 例如 docker run -d --name pgraph -p 3897:3897 -v /tmp/pgraph/data:/app/data -v /tmp/pgraph/logs:/app/logs  -v /tmp/graph.conf:/etc/graph.conf pgraph:latest-ubuntu-amd64


# 第一阶段：构建阶段
FROM cimg/rust:1.91.0-node AS builder

# 安装开发依赖
USER root
RUN apt-get update && apt-get install -y llvm-dev libclang-dev clang

# 设置工作目录
WORKDIR /home/circleci

# 复制源代码
COPY src /home/circleci/pgraph/src
COPY Cargo.toml /home/circleci/pgraph/Cargo.toml
COPY Cargo.lock /home/circleci/pgraph/Cargo.lock
COPY graph.conf /home/circleci/pgraph/pgraph.conf
COPY benches /home/circleci/pgraph/benches
RUN chown -R circleci:circleci /home/circleci/pgraph

# 切换到circleci用户
USER circleci

# 进入pgraph目录并构建
WORKDIR /home/circleci/pgraph
# RUN cargo test
RUN cargo build --release

# 第二阶段：运行阶段
FROM ubuntu:22.04

# 安装运行时可能需要的最小依赖和网络工具
RUN apt-get update && apt-get install -y --no-install-recommends \
    vim \
    ca-certificates \
    libssl-dev \
    curl \
    wget \
    net-tools \
    telnet \
    iproute2 \
    iputils-ping \
    openssh-client \
    && rm -rf /var/lib/apt/lists/*

# 创建必要的目录
WORKDIR /app

# 从构建阶段复制编译好的二进制文件和配置
COPY --from=builder /home/circleci/pgraph/target/release/pgraph /app/
COPY --from=builder /home/circleci/pgraph/target/release/pgraph-cli /app/
COPY --from=builder /home/circleci/pgraph/pgraph.conf /etc

# 创建启动脚本
RUN echo '#!/bin/bash\n\
echo "正在后台启动 pgraph..."\n\
/app/pgraph -c /etc/graph.conf &\n\
# 保持容器运行 如果没有 tail -f /dev/null 保持前台进程运行，容器会立即停止，pgraph也会被终止
tail -f /dev/null' > /app/start.sh && chmod +x /app/start.sh

# 设置默认命令
CMD ["/app/start.sh"]