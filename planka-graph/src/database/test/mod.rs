/**
 * 图数据库测试模块
 * 
 * 这个模块组织了所有与图数据库相关的测试，包括：
 * - 基本功能测试
 * - 性能测试
 * - 复杂查询测试
 * 
 * 测试使用TestDb和test_utils中的辅助函数来减少重复代码。
 */

// 导入RocksDB测试模块
mod rdb_tests;

// 导入基础查询测试模块
mod basic_query_tests;

// 导入性能测试模块
#[cfg(test)]
mod performance_tests;

// 可以在此处添加更多测试模块/// 测试工具模块，仅在测试环境下可用
#[cfg(test)]
pub mod test_utils; 