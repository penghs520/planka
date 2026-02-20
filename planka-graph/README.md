# pgraph 图数据库服务器

pgraph 是一个使用 Rust 编写的专为知微打造的高性能图数据库，支持单机和主从模式部署，提供pgraph-driver for java 以 TCP 协议接口的形式供客户端访问。

## 功能特点

- 使用 RocksDB 作为底层存储引擎
- 支持高效的图数据模型和查询
- 通过 TCP 协议提供服务
- 使用 Protocol Buffers 进行序列化/反序列化
- 使用raft协议保障主从节点的一致性


## 配置文件

pgraph 使用 YAML 格式的配置文件进行配置。默认配置文件路径为 `./pgraph.conf`。

配置项说明：

| 配置项 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| listen_address | 字符串 | 服务器监听地址 | "127.0.0.1" |
| listen_port | 整数 | 服务器监听端口 | 7009 |
| allowed_clients | 字符串数组 | 允许连接的客户端地址列表 | ["127.0.0.1"] |
| db_path | 字符串 | 数据库文件存储路径 | "/tmp/rdb_test" |
| db_snapshot_path | 字符串 | 快照目录路径，建议配置到共享目录下，各服务共享快照文件 | "/tmp/rdb_test/snapshots" |
| max_snapshot_files_to_keep | 整数 | 最大保留快照文件份数，超出此数量时会自动清理旧快照 | 5 |
| db_cache_size_mb | 整数 | 数据库缓存大小（MB） | 1024 |
| db_write_buffer_size_mb | 整数 | RocksDB写入缓冲区大小（MB） | 128 |
| db_max_open_files | 整数 | RocksDB最大打开文件数量 | 64 |
| db_max_background_jobs | 整数 | RocksDB并行压缩线程数 | 4 |
| db_vertex_lru_cache_size | 整数 | 节点LRU缓存大小（条目数） | 1000000 |
| thread_pool_size | 整数 | 线程池大小 | 无 |
| log_level | 字符串 | 日志级别配置，支持简单级别名称或复杂的过滤器格式 | "info" |
| log_rotation | 对象 | 日志滚动配置 | 见下文 |
| authentication | 对象 | 认证配置 | 见下文 |
| cluster_config | 对象 | Raft集群配置 | 见下文 |

### 日志级别配置

`log_level` 配置项支持两种格式：

1. 简单格式：直接指定全局日志级别
   - `"trace"`, `"debug"`, `"info"`, `"warn"`, `"error"`

2. 复杂格式（RUST_LOG格式）：精确控制不同模块的日志级别
   - `crate_name=level`：为特定库设置日志级别
   - `path::to::module=level`：为特定模块设置日志级别
   - `level`：设置全局默认日志级别

常用示例：
- `"pgraph=debug,info"`：pgraph库使用DEBUG级别，其他库使用INFO级别
- `"pgraph=debug,pgraph::database=trace,info"`：pgraph库使用DEBUG级别，database模块使用TRACE级别，其他库使用INFO级别
- `"debug"`：所有库都使用DEBUG级别

### 通过pgraph-cli热更新日志级别
= `"log-level <level>"` - 更新日志级别，支持简单格式(trace/debug/info/warn/error)或复杂格式(pgraph=debug,tokio=info)

### 认证配置

`authentication` 对象包含以下配置项：

| 配置项 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| enabled | 布尔值 | 是否启用认证 | false |
| users | 数组 | 用户列表 | [] |

每个用户对象包含：
- `username`: 用户名
- `password`: 密码

### Raft集群配置

`cluster_config` 对象包含以下配置项：

| 配置项 | 类型 | 说明 | 默认值               |
|--------|------|------|-------------------|
| node_id | 整数 | Raft节点ID，集群中唯一标识 | 1                 |
| rpc_addr | 字符串 | 本节点的RPC服务地址，用于节点间通信 | "127.0.0.1:13897" |
| max_in_snapshot_log_to_keep | 整数 | 保留多少个已经包含在快照中的日志条目 | 1000              |
| snapshot_policy_logs_threshold | 整数 | 快照触发策略，当日志条目达到指定数量时触发快照 | 10000       |
| heartbeat_interval | 整数 | 心跳间隔（毫秒） | 500              |
| election_timeout_min | 整数 | 选举超时最小值（毫秒） | 1500              |
| election_timeout_max | 整数 | 选举超时最大值（毫秒） | 3000              |

### 日志滚动配置

`log_rotation` 对象包含以下配置项：

| 配置项 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| max_size_mb | 整数 | 单个日志文件最大大小（MB）（当前版本仅作记录，未实际使用） | 100 |
| max_files | 整数 | 保留的最大日志文件数量（当前版本仅作记录，未实际使用） | 10 |
| rotation_hour | 整数 | 日志滚动的小时点（0-23）（当前仅支持每日滚动） | 0 |

### 日志文件命名格式

日志文件的命名格式为 `pgraph.YYYY-MM-DD.log`，例如 `pgraph.2025-03-19.log`。

示例配置文件：

```yaml
listen_address: 127.0.0.1
listen_port: 7009
allowed_clients:
  - 127.0.0.1
  - 0.0.0.0
db_path: /tmp/rdb_test
# 快照目录请配置到共享目录下，各服务共享快照文件，否则Follower将无法使用快照进行恢复
db_snapshot_path: /tmp/rdb_test/snapshots
# 最大保留快照文件份数，超出此数量时会自动清理旧快照
max_snapshot_files_to_keep: 5
db_cache_size_mb: 1024
db_write_buffer_size_mb: 128
db_max_open_files: 64
db_max_background_jobs: 4
db_vertex_lru_cache_size: 1000000
thread_pool_size: 4
log_level: info

log_rotation:
  max_size_mb: 100
  max_files: 10
  rotation_hour: 0

authentication:
  enabled: false
  users:
    - username: pgraph
      password: pgraph
    - username: readonly
      password: readonly

# Raft集群配置
# 注意：如果不需要启用集群模式，可以完全注释或删除cluster_config部分
cluster_config:
  node_id: 1
  rpc_addr: 127.0.0.1:13897
  max_in_snapshot_log_to_keep: 1000
  snapshot_policy_logs_threshold: 5000
  heartbeat_interval: 500
  election_timeout_min: 1500
  election_timeout_max: 3000
```

## 运行

### 使用默认配置文件

```bash
cargo run --release
```

### 指定配置文件路径

```bash
cargo run --release /path/to/config.conf
```

### 使用制品启动

```bash
./pgraph -c /path/to/config.conf
```

### 集群模式启动
这里以一主二从的三节点集群架构为例，节点1为主节点，节点2、3为从节点。
先准备三份配置文件：
node1.conf
```yaml
listen_address: 127.0.0.1
listen_port: 7009
allowed_clients:
  - 127.0.0.1
  - 0.0.0.0
db_path: /tmp/pgraph/data
db_snapshot_path: /tmp/pgraph/snapshots
max_snapshot_files_to_keep: 5
db_cache_size_mb: 1024
db_write_buffer_size_mb: 128
db_max_open_files: 64
db_max_background_jobs: 4
db_vertex_lru_cache_size: 1000000
log_level: info

log_rotation:
  max_size_mb: 100
  max_files: 10
  rotation_hour: 0
authentication:
  enabled: true
  users:
    - username: pgraph
      password: pgraph
    - username: readonly
      password: readonly 

# Raft集群配置
# 注意：如果不需要启用集群模式，可以完全注释或删除cluster_config部分
cluster_config:
  node_id: 1
  rpc_addr: 127.0.0.1:13897
  max_in_snapshot_log_to_keep: 1000
  snapshot_policy_logs_threshold: 5000
  heartbeat_interval: 500
  election_timeout_min: 1500
  election_timeout_max: 3000

```
node2.conf
```yaml
listen_address: 127.0.0.1
listen_port: 3895
allowed_clients:
  - 127.0.0.1
  - 0.0.0.0
db_path: /tmp/pgraph2/data
db_snapshot_path: /tmp/pgraph/snapshots
max_snapshot_files_to_keep: 5
db_cache_size_mb: 1024
db_write_buffer_size_mb: 128
db_max_open_files: 64
db_max_background_jobs: 4
db_vertex_lru_cache_size: 1000000
log_level: info

log_rotation:
  max_size_mb: 100
  max_files: 10
  rotation_hour: 0
authentication:
  enabled: true
  users:
    - username: pgraph
      password: pgraph
    - username: readonly
      password: readonly 

# Raft集群配置
# 注意：如果不需要启用集群模式，可以完全注释或删除cluster_config部分
cluster_config:
  node_id: 2
  rpc_addr: 127.0.0.1:13895
  max_in_snapshot_log_to_keep: 1000
  snapshot_policy_logs_threshold: 5000
  heartbeat_interval: 500
  election_timeout_min: 1500
  election_timeout_max: 3000

```
node3.conf
```yaml
listen_address: 127.0.0.1
listen_port: 3896
allowed_clients:
  - 127.0.0.1
  - 0.0.0.0
db_path: /tmp/pgraph3/data
db_snapshot_path: /tmp/pgraph/snapshots
max_snapshot_files_to_keep: 5
db_cache_size_mb: 1024
db_write_buffer_size_mb: 128
db_max_open_files: 64
db_max_background_jobs: 4
db_vertex_lru_cache_size: 1000000
log_level: info

log_rotation:
  max_size_mb: 100
  max_files: 10
  rotation_hour: 0
authentication:
  enabled: true
  users:
    - username: pgraph
      password: pgraph
    - username: readonly
      password: readonly 

# Raft集群配置
# 注意：如果不需要启用集群模式，可以完全注释或删除cluster_config部分
cluster_config:
  node_id: 3
  rpc_addr: 127.0.0.1:13896
  max_in_snapshot_log_to_keep: 1000
  snapshot_policy_logs_threshold: 5000
  heartbeat_interval: 500
  election_timeout_min: 1500
  election_timeout_max: 3000

```
分别启动三个实例
```shell
./pgraph -c /path/to/node1.conf
./pgraph -c /path/to/node2.conf
./pgraph -c /path/to/node3.conf
```
使用pgraph-cli登录连接到node1，然后执行集群初始化和添加从节点操作
```shell
#初始化集群，此时只有node1，node1为leader
pgraph>init-cluster
#添加node2为leaner
pgraph>add-learner 2 127.0.0.1:13895
#添加node3为leaner
pgraph>add-learner 3 127.0.0.1:13896
#查看集群状态，此时预期node1为leader，node2、node3为leaner，且只有node1为voter
pgraph>cluster-status
#改变节点间的成员关系，此时预期node1为leader，node1、node2、node3都为voter
pgraph>change-membership 1,2,3
#测试leader故障转移
#杀掉node1进程或者屏蔽网络，再次使用cluster-status查看集群状态，此时预期leader会转移到node2或者node3上
```
### 集群功能验证
1. 集群正常情况下，往Leader节点写入数据，其他两个从节点能够及时读取到更新后的数据
2. 某个从节点宕机后，往Leader节点写入数据，能够写入成功，当这个从节点重启时，仍然能够和主节点同步最新数据
3. 两个从节点都宕机后，往Leader节点写入数据，预期写入失败，因为必须保证多数节点写入成功才算成功
4. Leader节点宕机后，预期另外的两个主节点之一将会通过选举担任新的Leader，旧Leader节点重启后将成为从节点
5. 从节点可以接收读写请求，对于写请求，从节点会将写请求转发给主节点，由主节点负责写数据；
   而读请求，从节点可以直接处理，也可以转发给Leader节点来保证一致性读，但目前未实现一致性读
6. 加入新的从节点，会自动从Leader节点同步全量数据
7. 批量操作大量数据的请求，例如一次性更新1万+节点
8. 日志快照
9. Leader节点写入成功，某个从节点写入失败
10. Leader节点写入失败，从节点预期写入失败或者不会执行写入？暂不确定

## 快照管理

pgraph 支持自动快照管理功能：

### 快照自动清理
- 当快照数量超过 `max_snapshot_files_to_keep` 配置的限制时，系统会自动清理最旧的快照文件
- 清理策略基于文件的修改时间，保留最新创建的快照
- 清理操作在每次构建新快照时自动执行
- 清理过程中如果发生错误，不会影响快照构建，但会记录警告日志

### 快照目录配置
- `db_snapshot_path`: 指定快照文件的存储目录
- 建议将快照目录配置在共享存储上，以便集群中的各个节点可以访问
- 如果Follower节点无法访问快照文件，将无法通过快照进行数据恢复

### 快照策略配置
- `snapshot_policy_logs_threshold`: 当Raft日志条目达到该阈值时自动触发快照构建
- `max_in_snapshot_log_to_keep`: 快照完成后保留多少个已包含在快照中的日志条目

### docker启动
1. 配置文件放在 /etc/pgraph.conf
配置内容参考上面
2. 启动
```shell
docker run -d -p 7009:7009 -p 13897:13897 -v /etc/graph.conf:/etc/graph.conf -v /var/pgraph/data:/app/data -v /var/log/pgraph:/app/logs --name pgraph pgraph:latest-ubuntu-amd64
```

### 使用systemctl管理

pgraph可以配置为系统服务，使用systemd进行管理。步骤如下：

1. 创建系统服务文件

创建文件 `/etc/systemd/system/pgraph.service`，内容如下：

```ini
[Unit]
Description=pgraph Database Server
After=network.target

[Service]
Type=simple
User=pgraph
Group=pgraph
WorkingDirectory=/opt/pgraph
ExecStart=/opt/pgraph/pgraph /opt/pgraph/pgraph_config.json
Restart=on-failure
RestartSec=5
LimitNOFILE=65536

# 确保日志目录存在并有权限
ExecStartPre=/bin/mkdir -p /tmp/pgraph_logs
ExecStartPre=/bin/chown -R pgraph:pgraph /tmp/pgraph_logs

[Install]
WantedBy=multi-user.target
```

> 注意：根据实际情况调整用户、路径和配置文件位置。

2. 重新加载systemd配置并启用服务

```bash
sudo systemctl daemon-reload
sudo systemctl enable pgraph
```

3. 管理服务

- 启动服务：
```bash
sudo systemctl start pgraph
```

- 停止服务：
```bash
sudo systemctl stop pgraph
```

- 重启服务：
```bash
sudo systemctl restart pgraph
```

- 查看服务状态：
```bash
sudo systemctl status pgraph
```

4. 查看日志

- 使用systemd日志：
```bash
sudo journalctl -u pgraph
```

- 查看最近的日志：
```bash
sudo journalctl -u pgraph -f
```

- 直接查看日志文件：
```bash
ls -l /tmp/pgraph_logs/
cat /tmp/pgraph_logs/pgraph.$(date +%Y-%m-%d).log
```

## 客户端访问

pgraph 服务器通过 TCP 协议接受客户端连接，使用 Protocol Buffers 进行数据序列化。请参考 pgraph-driver 项目了解如何使用 Java 客户端进行访问。 