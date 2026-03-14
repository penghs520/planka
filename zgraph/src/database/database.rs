use crate::database::errors::DbResult;

use super::transaction::Transaction;

/// 动态迭代器类型 - 用于返回数据库操作的结果集
///
/// 这是一个返回包含数据库操作结果的动态迭代器，支持懒加载
pub type DynIter<'a, T> = Box<dyn Iterator<Item=DbResult<T>> + 'a>;

/// 数据库接口 - 定义图数据库的核心操作
///
/// 提供事务创建和同步等基本操作，具体数据操作通过Transaction接口实现
pub trait Database {
    /// 事务类型 - 由实现者定义具体的事务类型
    type Transaction<'a>: Transaction<'a>
    where
        Self: 'a;
    
    /// 开启一个新的事务
    ///
    /// 返回关联的事务对象，可用于执行具体的数据库操作
    fn transaction(&self) -> Self::Transaction<'_>;

    /// 同步数据到持久化存储
    ///
    /// 确保所有修改都已写入持久化存储
    fn sync(&mut self) -> DbResult<()> {
        let txn = self.transaction();
        txn.sync()
    }
}
