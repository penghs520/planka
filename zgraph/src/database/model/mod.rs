/**
 * models模块 - 包含图数据库的核心数据模型
 * 
 * 该模块定义了图数据库中的各种实体和组件，包括节点、边、标识符等
 */

/// 批量插入相关的数据结构
mod bulk_insert;

/// 边的定义及相关操作
pub(crate) mod edges;

/// 唯一标识符相关定义
mod identifiers;

/// 查询相关的数据结构和操作
pub mod queries;

/// 节点的定义及相关操作
mod vertices;


// 重导出主要数据结构，使它们可以直接从models模块访问
pub use self::bulk_insert::BulkInsertItem;
pub use self::edges::*;
pub use self::identifiers::Identifier;
pub use self::queries::*;
pub use self::vertices::*;
