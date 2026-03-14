# 构建命令 docker build  --platform linux/amd64 -t zgraph:latest-ubuntu-amd64 .
# 不带缓存 docker build  --no-cache --platform linux/amd64 -t zgraph:latest-ubuntu-amd64 .

# 运行命令 docker run -d --name zgraph -p 3897:3897 -v /host/path/to/data:/app/data -v /host/path/to/zgraph.conf:/etc/zgraph.conf zgraph:latest-ubuntu-amd64
# 例如 docker run -d --name zgraph -p 3897:3897 -v /tmp/zgraph/data:/app/data -v /tmp/zgraph/logs:/app/logs  -v /tmp/zgraph.conf:/etc/zgraph.conf zgraph:latest-ubuntu-amd64


# 第一阶段：构建阶段
FROM cimg/rust:1.91.0-node AS builder

# 安装开发依赖
USER root
RUN apt-get update && apt-get install -y llvm-dev libclang-dev clang

# 设置工作目录
WORKDIR /home/circleci

# 复制源代码
COPY src /home/circleci/zgraph/src
COPY Cargo.toml /home/circleci/zgraph/Cargo.toml
COPY Cargo.lock /home/circleci/zgraph/Cargo.lock
COPY zgraph.conf /home/circleci/zgraph/zgraph.conf
COPY benches /home/circleci/zgraph/benches
RUN chown -R circleci:circleci /home/circleci/zgraph

# 切换到circleci用户
USER circleci

# 进入zgraph目录并构建
WORKDIR /home/circleci/zgraph
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
COPY --from=builder /home/circleci/zgraph/target/release/zgraph /app/
COPY --from=builder /home/circleci/zgraph/target/release/zgraph-cli /app/
COPY --from=builder /home/circleci/zgraph/zgraph.conf /etc

# 创建启动脚本
RUN echo '#!/bin/bash\n\
echo "正在后台启动 zgraph..."\n\
/app/zgraph -c /etc/zgraph.conf &\n\
# 保持容器运行 如果没有 tail -f /dev/null 保持前台进程运行，容器会立即停止，zgraph也会被终止
tail -f /dev/null' > /app/start.sh && chmod +x /app/start.sh

# 设置默认命令
CMD ["/app/start.sh"]