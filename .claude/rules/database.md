# 数据库规范

## 核心原则

**禁止使用 Flyway 等数据库迁移工具**

## 表结构维护

所有表结构统一在 `docker/mysql/init/` 初始化脚本中维护：

```
docker/mysql/init/
├── 01-init-schema.sql      # 基础库表
├── 02-init-data.sql        # 基础数据
└── 03-init-xxx.sql         # 其他模块
```

## 新增/修改表结构

1. 直接更新对应的初始化 SQL 文件
2. 如果是新增模块，创建新的 SQL 文件（按序号命名）
3. 本地开发环境需要重新初始化数据库：

```bash
# 停止并删除现有容器
docker-compose down -v

# 重新启动（会自动执行初始化脚本）
docker-compose up -d mysql
```

## SQL 编写规范

- 表名使用下划线命名，小写
- 必须包含 `id`, `created_at`, `updated_at` 字段
- 外键字段命名：`{table}_id`
- 使用 `BIGINT` 作为主键类型

```sql
CREATE TABLE IF NOT EXISTS `card_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL COMMENT '类型名称',
  `org_id` BIGINT NOT NULL COMMENT '组织ID',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='__PLANKA_EINST__';
```
