use anyhow::{anyhow, Result};

/// SQL查询类型
#[derive(Debug, Clone)]
pub enum SqlQueryType {
    /// SELECT查询
    Select {
        fields: Vec<String>,
        is_count: bool,
    },
    /// COUNT查询
    Count,
}

/// SQL查询解析结果
#[derive(Debug, Clone)]
pub struct ParsedSqlQuery {
    /// 查询类型
    pub query_type: SqlQueryType,
    /// 表名（即card_type_id）
    pub table_name: String,
    /// WHERE条件
    pub where_conditions: Vec<WhereCondition>,
    /// ORDER BY条件
    pub order_by: Option<OrderByClause>,
    /// LIMIT条件
    pub limit: Option<u32>,
}

/// WHERE条件
#[derive(Debug, Clone)]
pub struct WhereCondition {
    /// 字段名
    pub field: String,
    /// 操作符
    pub operator: WhereOperator,
    /// 值
    pub values: Vec<String>,
}

/// WHERE操作符
#[derive(Debug, Clone, PartialEq)]
pub enum WhereOperator {
    Equal,        // =
    In,           // IN
    Contains,     // CONTAINS (对于title和desc)
    GreaterThan,  // > (主要用于日期字段)
    LessThan,     // < (主要用于日期字段)
}

/// ORDER BY子句
#[derive(Debug, Clone)]
pub struct OrderByClause {
    /// 排序字段
    pub field: String,
    /// 排序方向 (ASC/DESC)
    pub direction: SortDirection,
}

/// 排序方向
#[derive(Debug, Clone, PartialEq)]
pub enum SortDirection {
    Asc,
    Desc,
}

/// SQL解析器
pub struct SqlParser;

impl SqlParser {
    /// 校验字段名是否支持
    fn validate_field_name(field_name: &str) -> Result<()> {
        // 支持的内置字段
        const SUPPORTED_FIELDS: &[&str] = &[
            "*", "id", "card_id", "org_id", "type_id", "state", "title",
            "code", "code_in_org", "custom_code", "created", "updated",
            "desc", "stream_id", "status_id", "archived_at", "discarded_at"
        ];
        
        if SUPPORTED_FIELDS.contains(&field_name) {
            Ok(())
        } else {
            match field_name {
                 _ => Err(anyhow!("不支持的字段: {}。支持的字段包括: {}", 
                field_name, SUPPORTED_FIELDS.join(", ")))
            }
        }
    }

    /// 校验state字段的值
    fn validate_state_value(value: &str) -> Result<()> {
        const VALID_STATES: &[&str] = &["InProgress", "Archived", "Abandon", "Deleted"];
        
        if VALID_STATES.contains(&value) {
            Ok(())
        } else {
            Err(anyhow!("无效的state值: {}。支持的值: {}", 
                value, VALID_STATES.join(", ")))
        }
    }

    /// 特别校验WHERE条件中的字段使用限制
    fn validate_where_field_usage(_field_name: &str) -> Result<()> {
        // 移除了日期字段的限制，现在支持所有字段在WHERE条件中使用
        Ok(())
    }

    /// 解析SQL查询语句
    pub fn parse(sql: &str) -> Result<ParsedSqlQuery> {
        // 去除SQL语句前后的空格和分号
        let sql = sql.trim().trim_end_matches(';');
        
        // 转换为大写进行关键词匹配，但保留原始字符串用于值解析
        let sql_upper = sql.to_uppercase();
        
        // 检查是否是SELECT语句
        if !sql_upper.starts_with("SELECT") {
            return Err(anyhow!("只支持SELECT语句"));
        }
        
        // 解析SELECT部分
        let (query_type, remaining) = Self::parse_select(&sql, &sql_upper)?;
        
        // 解析FROM部分
        let (table_name, remaining) = Self::parse_from(&remaining)?;
        
        // 解析WHERE部分（可选）
        let (where_conditions, remaining) = Self::parse_where(&remaining)?;
        
        // 解析ORDER BY部分（可选）
        let (order_by, remaining) = Self::parse_order_by(&remaining)?;
        
        // 解析LIMIT部分（可选）
        let (limit, _) = Self::parse_limit(&remaining)?;
        
        Ok(ParsedSqlQuery {
            query_type,
            table_name,
            where_conditions,
            order_by,
            limit,
        })
    }
    
    /// 解析SELECT部分
    fn parse_select(sql: &str, sql_upper: &str) -> Result<(SqlQueryType, String)> {
        // 查找FROM关键字
        let from_pos = sql_upper.find(" FROM ")
            .ok_or_else(|| anyhow!("缺少FROM子句"))?;
        
        let select_part = &sql[6..from_pos].trim(); // 跳过"SELECT"
        let remaining = &sql[from_pos + 6..]; // 跳过" FROM "
        
        // 检查是否是COUNT查询
        if select_part.trim().to_uppercase() == "COUNT(*)" {
            return Ok((SqlQueryType::Count, remaining.to_string()));
        }
        
        // 解析字段列表
        let fields = if select_part.trim() == "*" {
            vec!["*".to_string()]
        } else {
            let field_list: Result<Vec<String>> = select_part.split(',')
                .map(|f| {
                    let field_name = f.trim();
                    Self::validate_field_name(field_name)?;
                    Ok(field_name.to_string())
                })
                .collect();
            field_list?
        };
        
        Ok((SqlQueryType::Select { fields, is_count: false }, remaining.to_string()))
    }
    
    /// 解析FROM部分
    fn parse_from(sql: &str) -> Result<(String, String)> {
        let sql = sql.trim();
        let parts: Vec<&str> = sql.split_whitespace().collect();
        
        if parts.is_empty() {
            return Err(anyhow!("FROM子句缺少表名"));
        }
        
        // 解析表名，支持反引号、单引号、双引号
        let table_name = if parts[0].starts_with('`') && parts[0].ends_with('`') {
            parts[0][1..parts[0].len()-1].to_string()
        } else if parts[0].starts_with('\'') && parts[0].ends_with('\'') {
            parts[0][1..parts[0].len()-1].to_string()
        } else if parts[0].starts_with('"') && parts[0].ends_with('"') {
            parts[0][1..parts[0].len()-1].to_string()
        } else {
            return Err(anyhow!("表名必须用引号包围，支持反引号`table`、单引号'table'或双引号\"table\""));
        };
        
        // 返回剩余的SQL
        let remaining = if parts.len() > 1 {
            parts[1..].join(" ")
        } else {
            String::new()
        };
        
        Ok((table_name, remaining))
    }
    
    /// 解析WHERE部分
    fn parse_where(sql: &str) -> Result<(Vec<WhereCondition>, String)> {
        let sql = sql.trim();
        if sql.is_empty() {
            return Ok((vec![], String::new()));
        }
        
        let sql_upper = sql.to_uppercase();
        
        // 查找WHERE关键字
        if !sql_upper.starts_with("WHERE ") {
            return Ok((vec![], sql.to_string()));
        }
        
        // 跳过WHERE关键字
        let where_clause = &sql[6..];
        
        // 查找ORDER BY或LIMIT，确定WHERE子句的结束位置
        let end_pos = Self::find_clause_end(where_clause, &["ORDER BY", "LIMIT"]);
        let (where_part, remaining) = if let Some(pos) = end_pos {
            (&where_clause[..pos], &where_clause[pos..])
        } else {
            (where_clause, "")
        };
        
        // 解析WHERE条件
        let conditions = Self::parse_where_conditions(where_part)?;
        
        Ok((conditions, remaining.to_string()))
    }
    
    /// 解析WHERE条件
    fn parse_where_conditions(where_part: &str) -> Result<Vec<WhereCondition>> {
        let mut conditions = vec![];
        
        // 简单解析，支持AND连接的条件（大小写不敏感）
        let clauses = Self::split_by_and(where_part);
        
        for clause in clauses {
            let condition = Self::parse_single_condition(clause.trim())?;
            conditions.push(condition);
        }
        
        Ok(conditions)
    }

    /// 按AND关键字分割条件（大小写不敏感）
    fn split_by_and(where_part: &str) -> Vec<&str> {
        let mut result = vec![];
        let upper_where = where_part.to_uppercase();
        let mut last_pos = 0;
        
        // 查找所有的 " AND " 出现位置（大小写不敏感）
        while let Some(pos) = upper_where[last_pos..].find(" AND ") {
            let actual_pos = last_pos + pos;
            result.push(&where_part[last_pos..actual_pos]);
            last_pos = actual_pos + 5; // 跳过 " AND "
        }
        
        // 添加最后一个条件
        if last_pos < where_part.len() {
            result.push(&where_part[last_pos..]);
        }
        
        // 如果没有找到AND，返回整个字符串
        if result.is_empty() {
            result.push(where_part);
        }
        
        result
    }
    
    /// 根据操作符分割条件，支持灵活的空白字符匹配
    /// 返回字段名和值部分，如果没有找到操作符则返回None
    fn split_by_operator(clause: &str, operator: &str) -> Result<Option<(String, String)>> {
        use regex::Regex;
        
        // 构建正则表达式模式，支持操作符前后的多个空白字符
        // 使用非贪婪匹配确保正确处理复杂的表达式
        let pattern = format!(r"^(.+?)\s+{}\s+(.+)$", regex::escape(operator));
        
        // 对于CONTAINS和IN操作符，需要进行大小写不敏感的匹配
        let regex = if operator == "CONTAINS" || operator == "IN" {
            Regex::new(&format!("(?i){}", pattern))
                .map_err(|e| anyhow!("正则表达式编译失败: {}", e))?
        } else {
            Regex::new(&pattern)
                .map_err(|e| anyhow!("正则表达式编译失败: {}", e))?
        };
        
        if let Some(captures) = regex.captures(clause.trim()) {
            let field = captures.get(1).unwrap().as_str().trim().to_string();
            let value_part = captures.get(2).unwrap().as_str().trim().to_string();
            Ok(Some((field, value_part)))
        } else {
            Ok(None)
        }
    }
    
    /// 解析单个WHERE条件
    fn parse_single_condition(clause: &str) -> Result<WhereCondition> {
        // 处理CONTAINS操作符 - 使用正则式匹配以支持灵活的空白字符
        if let Some((field, value_part)) = Self::split_by_operator(clause, "CONTAINS")? {
            Self::validate_field_name(&field)?; // 校验字段名
            Self::validate_where_field_usage(&field)?; // 校验WHERE中的字段使用
            let value = Self::parse_string_value(&value_part)?;
            
            return Ok(WhereCondition {
                field,
                operator: WhereOperator::Contains,
                values: vec![value],
            });
        }
        
        // 处理IN操作符 - 使用正则式匹配以支持灵活的空白字符
        if let Some((field, value_part)) = Self::split_by_operator(clause, "IN")? {
            Self::validate_field_name(&field)?; // 校验字段名
            Self::validate_where_field_usage(&field)?; // 校验WHERE中的字段使用
            let values = Self::parse_in_values(&value_part)?;
            
            // 对state字段的值进行特殊校验
            if field == "state" {
                for value in &values {
                    Self::validate_state_value(value)?;
                }
            }
            
            return Ok(WhereCondition {
                field,
                operator: WhereOperator::In,
                values,
            });
        }
        
        // 处理等号操作符 - 使用正则式匹配以支持灵活的空白字符
        if let Some((field, value_part)) = Self::split_by_operator(clause, "=")? {
            Self::validate_field_name(&field)?; // 校验字段名
            Self::validate_where_field_usage(&field)?; // 校验WHERE中的字段使用
            let value = Self::parse_string_value(&value_part)?;
            
            // 对state字段的值进行特殊校验
            if field == "state" {
                Self::validate_state_value(&value)?;
            }
            
            return Ok(WhereCondition {
                field,
                operator: WhereOperator::Equal,
                values: vec![value],
            });
        }
        
        // 处理大于操作符 - 使用正则式匹配以支持灵活的空白字符
        if let Some((field, value_part)) = Self::split_by_operator(clause, ">")? {
            Self::validate_field_name(&field)?; // 校验字段名
            Self::validate_where_field_usage(&field)?; // 校验WHERE中的字段使用
            let value = Self::parse_string_value(&value_part)?;
            
            return Ok(WhereCondition {
                field,
                operator: WhereOperator::GreaterThan,
                values: vec![value],
            });
        }
        
        // 处理小于操作符 - 使用正则式匹配以支持灵活的空白字符
        if let Some((field, value_part)) = Self::split_by_operator(clause, "<")? {
            Self::validate_field_name(&field)?; // 校验字段名
            Self::validate_where_field_usage(&field)?; // 校验WHERE中的字段使用
            let value = Self::parse_string_value(&value_part)?;
            
            return Ok(WhereCondition {
                field,
                operator: WhereOperator::LessThan,
                values: vec![value],
            });
        }
        
        Err(anyhow!("无法解析WHERE条件: {}", clause))
    }
    
    /// 解析字符串值
    fn parse_string_value(value_str: &str) -> Result<String> {
        let value_str = value_str.trim();
        
        // 字符串值必须用单引号或双引号包围
        if value_str.starts_with('\'') && value_str.ends_with('\'') {
            Ok(value_str[1..value_str.len()-1].to_string())
        } else if value_str.starts_with('"') && value_str.ends_with('"') {
            Ok(value_str[1..value_str.len()-1].to_string())
        } else {
            Err(anyhow!("字符串值必须用单引号'value'或双引号\"value\"包围: {}", value_str))
        }
    }
    
    /// 解析IN子句的值列表
    fn parse_in_values(value_part: &str) -> Result<Vec<String>> {
        let value_part = value_part.trim();
        
        // 确保以括号包围
        if !value_part.starts_with('(') || !value_part.ends_with(')') {
            return Err(anyhow!("IN子句的值必须用括号包围"));
        }
        
        let inner = &value_part[1..value_part.len()-1];
        let mut values = vec![];
        
        // 简单的逗号分割解析
        for part in inner.split(',') {
            let value = Self::parse_string_value(part)?;
            values.push(value);
        }
        
        Ok(values)
    }
    
    /// 解析ORDER BY部分
    fn parse_order_by(sql: &str) -> Result<(Option<OrderByClause>, String)> {
        let sql = sql.trim();
        if sql.is_empty() {
            return Ok((None, String::new()));
        }
        
        let sql_upper = sql.to_uppercase();
        
        // 查找ORDER BY关键字
        if !sql_upper.starts_with("ORDER BY ") {
            return Ok((None, sql.to_string()));
        }
        
        // 跳过ORDER BY关键字
        let order_clause = &sql[9..];
        
        // 查找LIMIT，确定ORDER BY子句的结束位置
        let end_pos = Self::find_clause_end(order_clause, &["LIMIT"]);
        let (order_part, remaining) = if let Some(pos) = end_pos {
            (&order_clause[..pos], &order_clause[pos..])
        } else {
            (order_clause, "")
        };
        
        // 解析ORDER BY条件
        let order_by = Self::parse_order_by_clause(order_part)?;
        
        Ok((Some(order_by), remaining.to_string()))
    }
    
    /// 解析ORDER BY子句
    fn parse_order_by_clause(order_part: &str) -> Result<OrderByClause> {
        let parts: Vec<&str> = order_part.trim().split_whitespace().collect();
        
        if parts.is_empty() {
            return Err(anyhow!("ORDER BY子句缺少字段名"));
        }
        
        let field = parts[0].to_string();
        Self::validate_field_name(&field)?; // 校验字段名
        
        let direction = if parts.len() > 1 {
            match parts[1].to_uppercase().as_str() {
                "ASC" => SortDirection::Asc,
                "DESC" => SortDirection::Desc,
                _ => return Err(anyhow!("无效的排序方向: {}", parts[1])),
            }
        } else {
            SortDirection::Asc // 默认升序
        };
        
        Ok(OrderByClause { field, direction })
    }
    
    /// 解析LIMIT部分
    fn parse_limit(sql: &str) -> Result<(Option<u32>, String)> {
        let sql = sql.trim();
        if sql.is_empty() {
            return Ok((None, String::new()));
        }
        
        let sql_upper = sql.to_uppercase();
        
        // 查找LIMIT关键字
        if !sql_upper.starts_with("LIMIT ") {
            return Ok((None, sql.to_string()));
        }
        
        // 跳过LIMIT关键字
        let limit_clause = &sql[6..];
        
        // 解析限制数量
        let parts: Vec<&str> = limit_clause.split_whitespace().collect();
        if parts.is_empty() {
            return Err(anyhow!("LIMIT子句缺少数量"));
        }
        
        let limit_num = parts[0].parse::<u32>()
            .map_err(|_| anyhow!("无效的LIMIT数量: {}", parts[0]))?;
        
        // 返回剩余SQL（如果有的话）
        let remaining = if parts.len() > 1 {
            parts[1..].join(" ")
        } else {
            String::new()
        };
        
        Ok((Some(limit_num), remaining))
    }
    
    /// 查找子句结束位置
    fn find_clause_end(sql: &str, keywords: &[&str]) -> Option<usize> {
        let sql_upper = sql.to_uppercase();
        
        keywords.iter()
            .filter_map(|keyword| {
                sql_upper.find(&format!(" {} ", keyword.to_uppercase()))
                    .or_else(|| {
                        // 也检查SQL结尾是否是该关键字
                        if sql_upper.ends_with(&format!(" {}", keyword.to_uppercase())) {
                            Some(sql.len() - keyword.len() - 1)
                        } else {
                            None
                        }
                    })
            })
            .min()
    }
}

// #[cfg(test)]
// mod tests {
//     use super::*;
//
//     #[test]
//     fn test_parse_simple_select() {
//         let sql = "SELECT * FROM `123456`";
//         let result = SqlParser::parse(sql).unwrap();
//
//         match result.query_type {
//             SqlQueryType::Select { fields, is_count } => {
//                 assert_eq!(fields, vec!["*"]);
//                 assert!(!is_count);
//             }
//             _ => panic!("Expected Select query type"),
//         }
//
//         assert_eq!(result.table_name, "123456");
//         assert!(result.where_conditions.is_empty());
//         assert!(result.order_by.is_none());
//         assert!(result.limit.is_none());
//     }
//
//     #[test]
//     fn test_parse_with_different_quotes() {
//         // 测试单引号
//         let sql1 = "SELECT * FROM '123456'";
//         let result1 = SqlParser::parse(sql1).unwrap();
//         assert_eq!(result1.table_name, "123456");
//
//         // 测试双引号
//         let sql2 = "SELECT * FROM \"123456\"";
//         let result2 = SqlParser::parse(sql2).unwrap();
//         assert_eq!(result2.table_name, "123456");
//
//         // 测试反引号
//         let sql3 = "SELECT * FROM `123456`";
//         let result3 = SqlParser::parse(sql3).unwrap();
//         assert_eq!(result3.table_name, "123456");
//     }
//
//     #[test]
//     fn test_field_validation() {
//         // 测试有效字段
//         let sql1 = "SELECT id, title FROM `123456`";
//         assert!(SqlParser::parse(sql1).is_ok());
//
//         // 测试无效字段
//         let sql2 = "SELECT invalid_field FROM `123456`";
//         assert!(SqlParser::parse(sql2).is_err());
//
//         // 测试混合字段（有效和无效）
//         let sql3 = "SELECT id, invalid_field FROM `123456`";
//         assert!(SqlParser::parse(sql3).is_err());
//     }
//
//     #[test]
//     fn test_string_value_validation() {
//         // 测试有效的字符串值
//         let sql1 = "SELECT * FROM `123456` WHERE id = 'test'";
//         assert!(SqlParser::parse(sql1).is_ok());
//
//         let sql2 = "SELECT * FROM `123456` WHERE id = \"test\"";
//         assert!(SqlParser::parse(sql2).is_ok());
//
//         // 测试无效的字符串值（没有引号）
//         let sql3 = "SELECT * FROM `123456` WHERE id = test";
//         assert!(SqlParser::parse(sql3).is_err());
//     }
//
//     #[test]
//     fn test_table_name_validation() {
//         // 测试有效的表名
//         assert!(SqlParser::parse("SELECT * FROM 'test'").is_ok());
//         assert!(SqlParser::parse("SELECT * FROM \"test\"").is_ok());
//         assert!(SqlParser::parse("SELECT * FROM `test`").is_ok());
//
//         // 测试无效的表名（没有引号）
//         assert!(SqlParser::parse("SELECT * FROM test").is_err());
//     }
//
//     #[test]
//     fn test_unsupported_fields() {
//         // 测试不支持的字段 tags
//         let sql1 = "SELECT tags FROM `123456`";
//         let result1 = SqlParser::parse(sql1);
//         assert!(result1.is_err());
//         assert!(result1.unwrap_err().to_string().contains("tags"));
//
//         // 测试不支持的字段 position
//         let sql2 = "SELECT position FROM `123456`";
//         let result2 = SqlParser::parse(sql2);
//         assert!(result2.is_err());
//         assert!(result2.unwrap_err().to_string().contains("position"));
//
//         // 测试WHERE条件中的不支持字段
//         let sql3 = "SELECT * FROM `123456` WHERE tags = 'test'";
//         assert!(SqlParser::parse(sql3).is_err());
//     }
//
//     #[test]
//     fn test_state_value_validation() {
//         // 测试有效的state值
//         let sql1 = "SELECT * FROM `123456` WHERE state = 'InProgress'";
//         assert!(SqlParser::parse(sql1).is_ok());
//
//         let sql2 = "SELECT * FROM `123456` WHERE state IN ('InProgress', 'Archived')";
//         assert!(SqlParser::parse(sql2).is_ok());
//
//         // 测试无效的state值
//         let sql3 = "SELECT * FROM `123456` WHERE state = 'InvalidState'";
//         let result3 = SqlParser::parse(sql3);
//         assert!(result3.is_err());
//         assert!(result3.unwrap_err().to_string().contains("InvalidState"));
//
//         // 测试IN中的无效state值
//         let sql4 = "SELECT * FROM `123456` WHERE state IN ('InProgress', 'InvalidState')";
//         let result4 = SqlParser::parse(sql4);
//         assert!(result4.is_err());
//         assert!(result4.unwrap_err().to_string().contains("InvalidState"));
//     }
//
//     #[test]
//     fn test_parse_count_query() {
//         let sql = "SELECT COUNT(*) FROM `123456`";
//         let result = SqlParser::parse(sql).unwrap();
//
//         match result.query_type {
//             SqlQueryType::Count => {}
//             _ => panic!("Expected Count query type"),
//         }
//     }
//
//     #[test]
//     fn test_parse_with_where() {
//         let sql = "SELECT * FROM `123456` WHERE id = 'test123'";
//         let result = SqlParser::parse(sql).unwrap();
//
//         assert_eq!(result.where_conditions.len(), 1);
//         let condition = &result.where_conditions[0];
//         assert_eq!(condition.field, "id");
//         assert_eq!(condition.operator, WhereOperator::Equal);
//         assert_eq!(condition.values, vec!["test123"]);
//     }
//
//     #[test]
//     fn test_parse_with_in_condition() {
//         let sql = "SELECT * FROM `123456` WHERE card_id IN ('id1', 'id2', 'id3')";
//         let result = SqlParser::parse(sql).unwrap();
//
//         assert_eq!(result.where_conditions.len(), 1);
//         let condition = &result.where_conditions[0];
//         assert_eq!(condition.field, "card_id");
//         assert_eq!(condition.operator, WhereOperator::In);
//         assert_eq!(condition.values, vec!["id1", "id2", "id3"]);
//     }
//
//     #[test]
//     fn test_parse_with_order_by() {
//         let sql = "SELECT * FROM `123456` ORDER BY created DESC";
//         let result = SqlParser::parse(sql).unwrap();
//
//         assert!(result.order_by.is_some());
//         let order_by = result.order_by.unwrap();
//         assert_eq!(order_by.field, "created");
//         assert_eq!(order_by.direction, SortDirection::Desc);
//     }
//
//     #[test]
//     fn test_parse_with_limit() {
//         let sql = "SELECT * FROM `123456` LIMIT 10";
//         let result = SqlParser::parse(sql).unwrap();
//
//         assert_eq!(result.limit, Some(10));
//     }
//
//     #[test]
//     fn test_parse_complex_query() {
//         let sql = "SELECT id, title FROM `123456` WHERE state IN ('InProgress', 'Archived') ORDER BY created DESC LIMIT 5";
//         let result = SqlParser::parse(sql).unwrap();
//
//         // 检查SELECT字段
//         match result.query_type {
//             SqlQueryType::Select { fields, is_count } => {
//                 assert_eq!(fields, vec!["id", "title"]);
//                 assert!(!is_count);
//             }
//             _ => panic!("Expected Select query type"),
//         }
//
//         // 检查WHERE条件
//         assert_eq!(result.where_conditions.len(), 1);
//         let condition = &result.where_conditions[0];
//         assert_eq!(condition.field, "state");
//         assert_eq!(condition.operator, WhereOperator::In);
//         assert_eq!(condition.values, vec!["InProgress", "Archived"]);
//
//         // 检查ORDER BY
//         assert!(result.order_by.is_some());
//         let order_by = result.order_by.unwrap();
//         assert_eq!(order_by.field, "created");
//         assert_eq!(order_by.direction, SortDirection::Desc);
//
//         // 检查LIMIT
//         assert_eq!(result.limit, Some(5));
//     }
//
//     #[test]
//     fn test_parse_multiple_and_conditions() {
//         // 测试多个 AND 条件
//         let sql = "SELECT * FROM `123456` WHERE state = 'InProgress' AND container_id = 'container1' AND title CONTAINS 'test'";
//         let result = SqlParser::parse(sql).unwrap();
//
//         // 检查WHERE条件
//         assert_eq!(result.where_conditions.len(), 3);
//
//         // 检查第一个条件: state = 'InProgress'
//         let condition1 = &result.where_conditions[0];
//         assert_eq!(condition1.field, "state");
//         assert_eq!(condition1.operator, WhereOperator::Equal);
//         assert_eq!(condition1.values, vec!["InProgress"]);
//
//         // 检查第二个条件: container_id = 'container1'
//         let condition2 = &result.where_conditions[1];
//         assert_eq!(condition2.field, "container_id");
//         assert_eq!(condition2.operator, WhereOperator::Equal);
//         assert_eq!(condition2.values, vec!["container1"]);
//
//         // 检查第三个条件: title CONTAINS 'test'
//         let condition3 = &result.where_conditions[2];
//         assert_eq!(condition3.field, "title");
//         assert_eq!(condition3.operator, WhereOperator::Contains);
//         assert_eq!(condition3.values, vec!["test"]);
//     }
//
//     #[test]
//     fn test_parse_mixed_conditions_with_order_limit() {
//         // 测试混合条件类型加上ORDER BY和LIMIT
//         let sql = "SELECT id, title, state FROM `cardtype123` WHERE state IN ('InProgress', 'Archived') AND container_id = 'container1' AND title CONTAINS 'project' AND title CONTAINS 'project2' ORDER BY updated DESC LIMIT 10";
//         let result = SqlParser::parse(sql).unwrap();
//
//         // 检查SELECT字段
//         match result.query_type {
//             SqlQueryType::Select { fields, is_count } => {
//                 assert_eq!(fields, vec!["id", "title", "state"]);
//                 assert!(!is_count);
//             }
//             _ => panic!("Expected Select query type"),
//         }
//
//         // 检查WHERE条件
//         assert_eq!(result.where_conditions.len(), 4);
//
//         // 检查条件1: state IN ('InProgress', 'Archived')
//         let condition1 = &result.where_conditions[0];
//         assert_eq!(condition1.field, "state");
//         assert_eq!(condition1.operator, WhereOperator::In);
//         assert_eq!(condition1.values, vec!["InProgress", "Archived"]);
//
//         // 检查条件2: container_id = 'container1'
//         let condition2 = &result.where_conditions[1];
//         assert_eq!(condition2.field, "container_id");
//         assert_eq!(condition2.operator, WhereOperator::Equal);
//         assert_eq!(condition2.values, vec!["container1"]);
//
//         // 检查条件3: title CONTAINS 'project'
//         let condition3 = &result.where_conditions[2];
//         assert_eq!(condition3.field, "title");
//         assert_eq!(condition3.operator, WhereOperator::Contains);
//         assert_eq!(condition3.values, vec!["project"]);
//
//         // 检查ORDER BY
//         assert!(result.order_by.is_some());
//         let order_by = result.order_by.unwrap();
//         assert_eq!(order_by.field, "updated");
//         assert_eq!(order_by.direction, SortDirection::Desc);
//
//         // 检查LIMIT
//         assert_eq!(result.limit, Some(10));
//     }
//
//     #[test]
//     fn test_case_insensitive_keywords() {
//         // 测试各种关键字的大小写不敏感
//         let test_cases = vec![
//             // 全大写
//             "SELECT * FROM `123456` WHERE state = 'InProgress' AND container_id = 'container1' ORDER BY created DESC LIMIT 5",
//             // 全小写
//             "select * from `123456` where state = 'InProgress' and container_id = 'container1' order by created desc limit 5",
//             // 混合大小写
//             "Select * From `123456` Where state = 'InProgress' And container_id = 'container1' Order By created Desc Limit 5",
//             // 不规则大小写
//             "sElEcT * fRoM `123456` wHeRe state = 'InProgress' AnD container_id = 'container1' oRdEr By created DeSc LiMiT 5",
//         ];
//
//         for sql in test_cases {
//             let result = SqlParser::parse(sql);
//             assert!(result.is_ok(), "Failed to parse: {}", sql);
//
//             let parsed = result.unwrap();
//
//             // 检查基本结构
//             match parsed.query_type {
//                 SqlQueryType::Select { fields, is_count } => {
//                     assert_eq!(fields, vec!["*"]);
//                     assert!(!is_count);
//                 }
//                 _ => panic!("Expected Select query type for: {}", sql),
//             }
//
//             assert_eq!(parsed.table_name, "123456");
//             assert_eq!(parsed.where_conditions.len(), 2);
//             assert!(parsed.order_by.is_some());
//             assert_eq!(parsed.limit, Some(5));
//         }
//     }
//
//     #[test]
//     fn test_case_insensitive_operators() {
//         // 测试操作符的大小写不敏感
//         let test_cases = vec![
//             // CONTAINS 不同大小写
//             ("WHERE title CONTAINS 'test'", WhereOperator::Contains),
//             ("WHERE title contains 'test'", WhereOperator::Contains),
//             ("WHERE title Contains 'test'", WhereOperator::Contains),
//             ("WHERE title CoNtAiNs 'test'", WhereOperator::Contains),
//
//             // IN 不同大小写
//             ("WHERE state IN ('InProgress', 'Archived')", WhereOperator::In),
//             ("WHERE state in ('InProgress', 'Archived')", WhereOperator::In),
//             ("WHERE state In ('InProgress', 'Archived')", WhereOperator::In),
//             ("WHERE state iN ('InProgress', 'Archived')", WhereOperator::In),
//         ];
//
//         for (where_clause, expected_op) in test_cases {
//             let sql = format!("SELECT * FROM `123456` {}", where_clause);
//             let result = SqlParser::parse(&sql);
//             assert!(result.is_ok(), "Failed to parse: {}", sql);
//
//             let parsed = result.unwrap();
//             assert_eq!(parsed.where_conditions.len(), 1);
//             assert_eq!(parsed.where_conditions[0].operator, expected_op);
//         }
//     }
//
//     #[test]
//     fn test_flexible_whitespace_operators() {
//         // 测试操作符周围的灵活空白字符支持
//         let test_cases = vec![
//             // 等号操作符的各种空格组合
//             ("WHERE id = 'test'", WhereOperator::Equal, "test"),              // 单个空格
//             ("WHERE id  =  'test'", WhereOperator::Equal, "test"),            // 多个空格
//             ("WHERE id   =   'test'", WhereOperator::Equal, "test"),          // 更多空格
//             ("WHERE id\t=\t'test'", WhereOperator::Equal, "test"),            // 制表符
//             ("WHERE id \t = \t 'test'", WhereOperator::Equal, "test"),        // 混合空格和制表符
//
//             // 大于操作符的各种空格组合
//             ("WHERE created > '2023-01-01'", WhereOperator::GreaterThan, "2023-01-01"),
//             ("WHERE created  >  '2023-01-01'", WhereOperator::GreaterThan, "2023-01-01"),
//             ("WHERE created   >   '2023-01-01'", WhereOperator::GreaterThan, "2023-01-01"),
//
//             // 小于操作符的各种空格组合
//             ("WHERE updated < '2023-12-31'", WhereOperator::LessThan, "2023-12-31"),
//             ("WHERE updated  <  '2023-12-31'", WhereOperator::LessThan, "2023-12-31"),
//             ("WHERE updated   <   '2023-12-31'", WhereOperator::LessThan, "2023-12-31"),
//
//             // CONTAINS操作符的各种空格组合（大小写不敏感）
//             ("WHERE title CONTAINS 'test'", WhereOperator::Contains, "test"),
//             ("WHERE title  CONTAINS  'test'", WhereOperator::Contains, "test"),
//             ("WHERE title   CONTAINS   'test'", WhereOperator::Contains, "test"),
//             ("WHERE title contains 'test'", WhereOperator::Contains, "test"),
//             ("WHERE title  contains  'test'", WhereOperator::Contains, "test"),
//             ("WHERE desc\tCONTAINS\t'important'", WhereOperator::Contains, "important"),
//         ];
//
//         for (where_clause, expected_op, expected_value) in test_cases {
//             let sql = format!("SELECT * FROM `123456` {}", where_clause);
//             let result = SqlParser::parse(&sql);
//             assert!(result.is_ok(), "Failed to parse: {}", sql);
//
//             let parsed = result.unwrap();
//             assert_eq!(parsed.where_conditions.len(), 1, "Expected 1 condition for: {}", sql);
//
//             let condition = &parsed.where_conditions[0];
//             assert_eq!(condition.operator, expected_op, "Operator mismatch for: {}", sql);
//             assert_eq!(condition.values.len(), 1, "Expected 1 value for: {}", sql);
//             assert_eq!(condition.values[0], expected_value, "Value mismatch for: {}", sql);
//         }
//     }
//
//     #[test]
//     fn test_flexible_whitespace_in_operator() {
//         // 测试IN操作符的灵活空白字符支持
//         let test_cases = vec![
//             // 标准格式
//             "WHERE state IN ('InProgress', 'Archived')",
//             // 多个空格
//             "WHERE state  IN  ('InProgress', 'Archived')",
//             // 更多空格
//             "WHERE state   IN   ('InProgress', 'Archived')",
//             // 制表符
//             "WHERE state\tIN\t('InProgress', 'Archived')",
//             // 混合空格和制表符
//             "WHERE state \t IN \t ('InProgress', 'Archived')",
//             // 大小写变化 + 多空格
//             "WHERE state  in  ('InProgress', 'Archived')",
//             "WHERE state   In   ('InProgress', 'Archived')",
//             "WHERE card_id  IN  ('id1', 'id2', 'id3')",
//         ];
//
//         for where_clause in test_cases {
//             let sql = format!("SELECT * FROM `123456` {}", where_clause);
//             let result = SqlParser::parse(&sql);
//             assert!(result.is_ok(), "Failed to parse: {}", sql);
//
//             let parsed = result.unwrap();
//             assert_eq!(parsed.where_conditions.len(), 1, "Expected 1 condition for: {}", sql);
//
//             let condition = &parsed.where_conditions[0];
//             assert_eq!(condition.operator, WhereOperator::In, "Expected IN operator for: {}", sql);
//             assert!(condition.values.len() >= 2, "Expected multiple values for IN operator: {}", sql);
//         }
//     }
//
//     #[test]
//     fn test_flexible_whitespace_complex_conditions() {
//         // 测试复杂条件中的灵活空白字符支持
//         let sql = "SELECT * FROM `123456` WHERE state  =  'InProgress' AND container_id   =   'container1' AND title    CONTAINS    'test'";
//         let result = SqlParser::parse(sql).unwrap();
//
//         assert_eq!(result.where_conditions.len(), 3);
//
//         // 检查第一个条件: state = 'InProgress'
//         let condition1 = &result.where_conditions[0];
//         assert_eq!(condition1.field, "state");
//         assert_eq!(condition1.operator, WhereOperator::Equal);
//         assert_eq!(condition1.values, vec!["InProgress"]);
//
//         // 检查第二个条件: container_id = 'container1'
//         let condition2 = &result.where_conditions[1];
//         assert_eq!(condition2.field, "container_id");
//         assert_eq!(condition2.operator, WhereOperator::Equal);
//         assert_eq!(condition2.values, vec!["container1"]);
//
//         // 检查第三个条件: title CONTAINS 'test'
//         let condition3 = &result.where_conditions[2];
//         assert_eq!(condition3.field, "title");
//         assert_eq!(condition3.operator, WhereOperator::Contains);
//         assert_eq!(condition3.values, vec!["test"]);
//     }
//
//     #[test]
//     fn test_mixed_whitespace_characters() {
//         // 测试混合的空白字符（空格、制表符、多个空格等）
//         let test_cases = vec![
//             "SELECT * FROM `123456` WHERE title\t=\t'test'",           // 制表符
//             "SELECT * FROM `123456` WHERE title \t = \t 'test'",       // 混合空格和制表符
//             "SELECT * FROM `123456` WHERE title    =    'test'",       // 多个空格
//             "SELECT * FROM `123456` WHERE title\tCONTAINS\t'search'",  // 制表符 + CONTAINS
//             "SELECT * FROM `123456` WHERE state    IN    ('InProgress', 'Archived')", // 多空格 + IN
//         ];
//
//         for sql in test_cases {
//             let result = SqlParser::parse(sql);
//             assert!(result.is_ok(), "Failed to parse SQL with mixed whitespace: {}", sql);
//
//             let parsed = result.unwrap();
//             assert_eq!(parsed.where_conditions.len(), 1, "Expected 1 condition for: {}", sql);
//         }
//     }
// }
