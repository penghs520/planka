/**
 * 错误处理模块 - 定义图数据库操作中可能出现的错误类型
 *
 * 该模块提供了统一的错误类型和结果类型，用于处理数据库操作中的各种错误情况，
 * 包括事务错误、验证错误、底层存储错误等
 */

use std::error::Error as StdError;
use std::fmt;
use std::fmt::Formatter;
use std::result::Result as StdResult;


use bincode::Error as BincodeError;
use rmp_serde::encode::Error as RmpEncodeError;
use rocksdb::Error as RocksDbError;
use serde_json::Error as JsonError;

use super::model::VertexId;

/// 数据库操作结果类型 - 表示可能成功或失败的数据库操作
///
/// 成功时返回泛型T，失败时返回DbError
pub type DbResult<T> = StdResult<T, DbError>;

/// 数据库错误枚举 - 表示数据库操作中可能出现的各种错误
///
/// 包含不同类型的错误情况，每种情况提供适当的错误信息
#[non_exhaustive]
#[derive(Debug)]
pub enum DbError {
    /// 事务处理错误 - 封装底层事务相关的错误
    TransactionError(Box<dyn StdError + Send + Sync>),

    /// RocksDB错误 - 底层存储引擎返回的错误
    RocksDbError(RocksDbError),

    /// 锁错误 - 获取或释放锁时遇到的错误
    LockError(String),

    /// 节点不存在错误 - 当操作一个不存在的节点时发生
    VertexNotExists(VertexId),

    /// 验证错误 - 输入数据验证失败
    ValidationError(ValidationError),

    /// 不支持的操作错误 - 尝试执行不受支持的功能
    Unsupported,

    /// 查询操作错误 - 当查询与操作类型不兼容时发生
    /// 例如，尝试对返回计数的查询执行删除操作
    OperationOnQuery,
}

/// 从RocksDB错误转换为数据库错误
impl From<RocksDbError> for DbError {
    fn from(err: RocksDbError) -> Self {
        DbError::RocksDbError(err)
    }
}

/// 验证错误枚举 - 表示数据验证过程中可能出现的错误
#[derive(Debug)]
pub enum ValidationError {
    /// 无效值错误 - 当提供的值不符合要求时发生
    InvalidValue(String/*错误消息*/),
    
    /// 空输入错误 - 当必需的输入为空时发生
    EmptyInput(String/*错误消息*/),
}

/// 为验证错误实现显示特性
impl fmt::Display for ValidationError {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        match self {
            ValidationError::InvalidValue(msg) => {
                write!(f, "Invalid value error: {}", msg)
            }
            ValidationError::EmptyInput(msg) => {
                write!(f, "Empty input error: {}", msg)
            }
        }
    }
}

/// 为数据库错误实现标准错误特性
impl StdError for DbError {
    fn source(&self) -> Option<&(dyn StdError + 'static)> {
        match *self {
            DbError::TransactionError(ref err) => Some(&**err),
            _ => None,
        }
    }
}

/// 为数据库错误实现显示特性
impl fmt::Display for DbError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            DbError::TransactionError(err) => write!(f, "Underlying storage error: {err}"),
            DbError::Unsupported => write!(f, "Feature not supported"),
            DbError::OperationOnQuery => write!(f, "This operation cannot be used with the given query"),
            DbError::VertexNotExists(vid) => { write!(f, "Node with ID {} does not exist", vid) }
            DbError::ValidationError(err) => { write!(f, "{}", err) }
            DbError::RocksDbError(err) => write!(f, "RocksDB error: {}", err),
            DbError::LockError(err) => write!(f, "Lock error: {}", err),
        }
    }
}

/// 从JSON错误转换为数据库错误
impl From<JsonError> for DbError {
    fn from(err: JsonError) -> Self {
        DbError::TransactionError(Box::new(err))
    }
}

/// 从Bincode错误转换为数据库错误
impl From<BincodeError> for DbError {
    fn from(err: BincodeError) -> Self {
        DbError::TransactionError(Box::new(err))
    }
}

/// 从MessagePack错误转换为数据库错误
impl From<RmpEncodeError> for DbError {
    fn from(err: RmpEncodeError) -> Self {
        DbError::TransactionError(Box::new(err))
    }
}
