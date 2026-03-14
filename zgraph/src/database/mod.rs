/**
 * 数据库模块 - 提供图数据库的存储和检索功能
 * 
 * 该模块包含:
 * - 数据库抽象接口定义
 * - 错误处理机制
 * - 具体的数据库实现（如RocksDB）
 */

/// 数据库抽象接口和事务定义
pub mod database;

/// 错误处理类型和工具
pub mod errors;

/// 基于RocksDB的具体存储实现
pub mod rdb;

pub mod model;

/// 组织化的数据库测试模块
#[cfg(test)]
pub mod test;

pub mod transaction;