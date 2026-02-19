use crate::config::{LogRotation, ServerConfig};
use chrono::{TimeZone};
use std::fs;
use std::path::Path;
use std::sync::{Arc, Mutex, Once};
use std::time::Duration;
use tracing::{info, warn, Level};
use tracing_appender::rolling::Rotation;
use tracing_subscriber::reload;
use tracing_subscriber::{
    fmt::{self, format::FmtSpan, time::FormatTime},
    layer::SubscriberExt,
    reload::Handle,
    util::SubscriberInitExt, EnvFilter,
};

// 全局日志重载句柄
static mut FILTER_HANDLE: Option<Arc<Mutex<Handle<EnvFilter, tracing_subscriber::Registry>>>> = None;
// 当前日志级别
static mut CURRENT_LOG_LEVEL: Option<String> = None;
// 日志初始化标志
static INIT_LOGGER: Once = Once::new();

// 自定义时间戳格式器
struct CustomTime;
impl FormatTime for CustomTime {
    fn format_time(&self, w: &mut fmt::format::Writer<'_>) -> std::fmt::Result {
        // 使用东八区（Asia/Shanghai）时区
        let utc_time = chrono::Utc::now().naive_utc();
        let east8 = chrono::FixedOffset::east_opt(8 * 3600).unwrap();
        let east8_time = east8.from_utc_datetime(&utc_time);
        // 使用自定义格式，不包含时区标识符
        write!(w, "{}", east8_time.format("%Y-%m-%d %H:%M:%S%.3f"))
    }
}

/// 获取日志级别
fn get_log_level(level_str: &str) -> Level {
    match level_str.to_lowercase().as_str() {
        "trace" => Level::TRACE,
        "debug" => Level::DEBUG,
        "info" => Level::INFO,
        "warn" => Level::WARN,
        "error" => Level::ERROR,
        _ => Level::INFO,
    }
}

/// 检查字符串是否为简单的日志级别名称
fn is_simple_log_level(level_str: &str) -> bool {
    matches!(level_str.to_lowercase().as_str(), 
        "trace" | "debug" | "info" | "warn" | "error")
}

/// 获取日志滚动配置
fn get_log_rotation(rotation: &Option<LogRotation>) -> Rotation {
    if let Some(rotation_config) = rotation {
        // 按照小时点决定是否使用每日滚动
        if let Some(_) = rotation_config.rotation_hour {
            // 简化实现，使用每日滚动
            return Rotation::DAILY;
        }
    }

    // 默认每天滚动
    Rotation::DAILY
}

/// 清理旧的日志文件
/// 根据配置的max_files保留最新的N个日志文件，删除其他旧文件
fn cleanup_old_log_files(log_path: &str, max_files: u32) {
    let path = Path::new(log_path);
    if !path.exists() {
        return;
    }

    // 读取日志目录中的所有文件
    let entries = match fs::read_dir(path) {
        Ok(entries) => entries,
        Err(e) => {
            warn!("Failed to read log directory for cleanup: {}", e);
            return;
        }
    };

    // 收集所有日志文件及其修改时间
    let mut log_files: Vec<(std::path::PathBuf, std::time::SystemTime)> = Vec::new();

    for entry in entries.flatten() {
        let path = entry.path();

        // 只处理.log文件且文件名以pgraph开头
        if path.is_file() {
            if let Some(filename) = path.file_name().and_then(|n| n.to_str()) {
                if filename.starts_with("pgraph") && filename.ends_with(".log") {
                    // 获取文件修改时间
                    if let Ok(metadata) = entry.metadata() {
                        if let Ok(modified) = metadata.modified() {
                            log_files.push((path, modified));
                        }
                    }
                }
            }
        }
    }

    // 如果文件数量不超过max_files，不需要清理
    if log_files.len() <= max_files as usize {
        return;
    }

    // 按修改时间降序排序（最新的在前面）
    log_files.sort_by(|a, b| b.1.cmp(&a.1));

    // 删除超出max_files的旧文件
    for (file_path, _) in log_files.iter().skip(max_files as usize) {
        match fs::remove_file(file_path) {
            Ok(_) => {
                info!("Removed old log file: {:?}", file_path);
            }
            Err(e) => {
                warn!("Failed to remove old log file {:?}: {}", file_path, e);
            }
        }
    }
}

/// 启动日志清理后台任务
fn start_log_cleanup_task(log_path: String, log_rotation: Option<LogRotation>) {
    std::thread::spawn(move || {
        loop {
            // 每12小时检查一次
            std::thread::sleep(Duration::from_secs(3600 * 24));

            if let Some(rotation_config) = &log_rotation {
                if let Some(max_files) = rotation_config.max_files {
                    cleanup_old_log_files(&log_path, max_files);
                }
            }
        }
    });
}

/// 创建EnvFilter，根据配置的日志级别
fn create_env_filter(log_level: &Option<String>) -> EnvFilter {
    let mut env_filter = EnvFilter::from_default_env();
    
    // 处理日志级别配置
    if let Some(level_config) = log_level {
        if is_simple_log_level(level_config) {
            // 简单格式：直接设置全局日志级别
            let log_level = get_log_level(level_config);
            env_filter = env_filter.add_directive(log_level.into());
        } else {
            // 复杂格式：解析过滤器字符串
            for directive in level_config.split(',') {
                if let Ok(parsed) = directive.parse() {
                    env_filter = env_filter.add_directive(parsed);
                } else {
                    eprintln!("Warning: Invalid log filter directive: {}", directive);
                }
            }
        }
    } else {
        // 默认使用INFO级别
        env_filter = env_filter.add_directive(Level::INFO.into());
    }
    
    env_filter
}

/// 初始化日志系统
pub fn init_logger(config: &ServerConfig) {
    INIT_LOGGER.call_once(|| {
        // 确保日志目录存在
        let log_path = config.get_db_log_path();
        let path = Path::new(&log_path);
        if !path.exists() {
            if let Err(e) = fs::create_dir_all(path) {
                // 在日志系统初始化前，只能使用标准错误输出
                eprintln!("Failed to create log directory: {}", e);
                return;
            }
        }
        
        // 保存当前日志级别
        unsafe {
            CURRENT_LOG_LEVEL = config.log_level.clone();
        }
        
        // 配置日志滚动
        let rotation = get_log_rotation(&config.log_rotation);
        
        // 创建文件输出器，使用日期格式化的文件名：pgraph.2025-03-19.log
        let file_appender = tracing_appender::rolling::Builder::new()
            .rotation(rotation)
            .filename_prefix("pgraph")
            .filename_suffix("log")
            .build(&log_path)
            .expect("Failed to create log file appender");
        let (non_blocking_appender, _guard) = tracing_appender::non_blocking(file_appender);
        
        // 文件日志格式
        let file_layer = fmt::layer()
            .with_ansi(false)
            .with_timer(CustomTime)
            .with_span_events(FmtSpan::NONE)
            .with_target(true)
            .with_thread_ids(true)
            .with_thread_names(true)
            .with_file(true)
            .with_line_number(true)
            .with_writer(non_blocking_appender);
            
        // 控制台日志格式
        let console_layer = fmt::layer()
            .with_ansi(true)
            .with_timer(CustomTime)
            .with_span_events(FmtSpan::NONE)
            .with_target(true)
            .with_thread_ids(true)
            .with_thread_names(true)
            .with_file(true)
            .with_line_number(true);
            
        // 创建可重载的环境过滤器
        let env_filter = create_env_filter(&config.log_level);
        let (env_filter_reloadable, reload_handle) = reload::Layer::new(env_filter);
        
        // 保存重载句柄到全局变量
        unsafe {
            FILTER_HANDLE = Some(Arc::new(Mutex::new(reload_handle)));
        }
        
        // 注册订阅器
        tracing_subscriber::registry()
            .with(env_filter_reloadable)
            .with(file_layer)
            .with(console_layer)
            .init();
            
        // 日志初始化成功信息
        info!(
            "Logger initialization completed - Path: {}, Level: {:?}",
            &log_path, config.log_level
        );

        // 启动日志清理后台任务
        if let Some(rotation_config) = &config.log_rotation {
            if let Some(max_files) = rotation_config.max_files {
                info!("Starting log cleanup task - max_files: {}", max_files);
                start_log_cleanup_task(log_path.clone(), config.log_rotation.clone());

                // 立即执行一次清理
                cleanup_old_log_files(&log_path, max_files);
            }
        }

        // 保存 _guard 以确保程序退出前日志被正确刷新
        Box::leak(Box::new(_guard));
    });
}

/// 更新日志级别
pub fn update_log_level(log_level: &str) -> Result<String, String> {
    // 获取重载句柄
    let handle = unsafe {
        match &*std::ptr::addr_of!(FILTER_HANDLE) {
            Some(h) => h.clone(),
            None => return Err("Logger not initialized".into()),
        }
    };
    
    // 创建新的环境过滤器
    let env_filter = match create_env_filter_from_str(log_level) {
        Ok(filter) => filter,
        Err(e) => return Err(format!("Invalid log level: {}", e)),
    };
    
    // 更新环境过滤器
    let handle_lock = match handle.lock() {
        Ok(lock) => lock,
        Err(_) => return Err("Failed to acquire lock for log level update".into()),
    };
    
    if let Err(e) = handle_lock.reload(env_filter) {
        return Err(format!("Failed to reload log level: {}", e));
    }
    
    // 更新当前日志级别
    unsafe {
        CURRENT_LOG_LEVEL = Some(log_level.to_string());
    }
    
    info!("Log level updated to: {}", log_level);
    
    // 返回成功消息
    Ok(format!("Log level updated to: {}", log_level))
}

/// 获取当前日志级别
pub fn get_current_log_level() -> Option<String> {
    unsafe { (&*std::ptr::addr_of!(CURRENT_LOG_LEVEL)).clone() }
}

/// 从字符串创建EnvFilter
fn create_env_filter_from_str(log_level: &str) -> Result<EnvFilter, String> {
    let mut env_filter = EnvFilter::from_default_env();
    
    if is_simple_log_level(log_level) {
        // 简单格式：直接设置全局日志级别
        let level = get_log_level(log_level);
        env_filter = env_filter.add_directive(level.into());
    } else {
        // 复杂格式：解析过滤器字符串
        for directive in log_level.split(',') {
            match directive.parse() {
                Ok(parsed) => {
                    env_filter = env_filter.add_directive(parsed);
                }
                Err(e) => {
                    return Err(format!("Invalid log filter directive: {}: {}", directive, e));
                }
            }
        }
    }
    
    Ok(env_filter)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::Instant;
    use tracing::{debug, error, info, span, warn, Level};

    #[test]
    #[ignore]
    pub fn test_logger_request_span() {
        // 配置测试日志
        let config = ServerConfig {
            log_level: Some(String::from("trace")),
            log_rotation: None,
            // 其他字段以默认值初始化
            ..Default::default()
        };
        
        // 初始化日志系统
        init_logger(&config);
        
        // 模拟请求处理中的日志打印
        let request_id = "req-9527";
        let span = span!(Level::DEBUG, "request", request_id = request_id);
        let _enter = span.enter();
        
        // 模拟处理请求并记录耗时
        let start = Instant::now();
        // 假装做了一些工作
        std::thread::sleep(std::time::Duration::from_micros(409));
        let elapsed = start.elapsed();
        
        // 打印类似于目标格式的日志
        debug!(
            target: "pgraph::server::querys",
            "filter 1 vertices, remaining 1, cost: {:.1}µs",
            elapsed.as_micros() as f64
        );
        
        println!("已模拟一条请求处理日志，请查看上方输出");
    }

    #[test]
    #[ignore]
    pub fn test_logger_with_span_performance() {
        // 配置测试日志
        let config = ServerConfig {
            log_level: Some(String::from("trace")),
            log_rotation: None,
            // 其他字段以默认值初始化
            ..Default::default()
        };
        
        // 初始化日志系统
        init_logger(&config);
        
        // 测试迭代次数
        let iterations = 1000;
        
        // 不带span的普通日志打印性能测试
        let start = Instant::now();
        for i in 0..iterations {
            info!("普通日志，不带span: {}", i);
        }
        let no_span_duration = start.elapsed();
        println!("打印 {} 条不带span的日志耗时: {:?}, 平均每条: {:?}", 
            iterations, no_span_duration, no_span_duration / iterations as u32);
        
        // 带span的日志打印性能测试
        let start = Instant::now();
        for i in 0..iterations {
            // 创建span
            let span = span!(Level::INFO, "test_span", request_id = format!("req-{}", i));
            // 进入span作用域
            let _guard = span.enter();
            // 在span内打印日志
            info!("带span的日志: {}", i);
        }
        let with_span_duration = start.elapsed();
        println!("打印 {} 条带span的日志耗时: {:?}, 平均每条: {:?}", 
            iterations, with_span_duration, with_span_duration / iterations as u32);
        
        // 带嵌套span的日志打印性能测试
        let start = Instant::now();
        for i in 0..iterations {
            // 创建父span
            let parent_span = span!(Level::INFO, "parent_span", parent_id = format!("parent-{}", i));
            let _parent_guard = parent_span.enter();
            
            // 在父span内创建子span
            let child_span = span!(Level::INFO, "child_span", child_id = format!("child-{}", i));
            let _child_guard = child_span.enter();
            
            // 在嵌套span内打印日志
            info!("带嵌套span的日志: {}", i);
        }
        let nested_span_duration = start.elapsed();
        println!("打印 {} 条带嵌套span的日志耗时: {:?}, 平均每条: {:?}", 
            iterations, nested_span_duration, nested_span_duration / iterations as u32);
        
        // 性能比较
        let span_overhead = with_span_duration.as_nanos() as f64 / no_span_duration.as_nanos() as f64;
        let nested_span_overhead = nested_span_duration.as_nanos() as f64 / no_span_duration.as_nanos() as f64;
        
        println!("单层span开销是普通日志的 {:.2} 倍", span_overhead);
        println!("嵌套span开销是普通日志的 {:.2} 倍", nested_span_overhead);
    }

    #[test]
    #[ignore]
    pub fn test_logger_performance() {
        // 配置测试日志
        let config = ServerConfig {

            log_level: Some(String::from("trace")),
            log_rotation: None,
            // 其他字段以默认值初始化
            ..Default::default()
        };
        
        // 初始化日志系统
        init_logger(&config);
        
        // 测试不同级别的日志打印耗时
        let iterations = 1000;
        
        // 简单日志打印耗时测试
        let start = Instant::now();
        for i in 0..iterations {
            info!("这是一条简单的日志消息, 迭代: {}", i);
        }
        let info_duration = start.elapsed();
        println!("打印 {} 条 info 级别简单日志耗时: {:?}, 平均每条: {:?}", 
            iterations, info_duration, info_duration / iterations as u32);
        
        // 复杂日志在 debug 级别下的打印耗时测试
        let start = Instant::now();
        for i in 0..iterations {
            debug!("这是一条复杂的日志消息，包含多个参数: iter={}, val1={}, val2={}, val3={}, val4={:?}", 
                i, "test_value", 123456, 3.14159, vec![1, 2, 3, 4, 5]);
        }
        let debug_duration = start.elapsed();
        println!("打印 {} 条 debug 级别复杂日志耗时: {:?}, 平均每条: {:?}", 
            iterations, debug_duration, debug_duration / iterations as u32);
        
        // 测试不同日志级别在过滤条件下的耗时差异
        let config = ServerConfig {

            log_level: Some(String::from("info")), // 设置为info级别，低于此级别的日志不输出
            log_rotation: None,
            ..Default::default()
        };
        
        // 尝试更新日志级别（注意：初始化完成后不会再更新）
        update_log_level("info").unwrap();
        
        // 测试被过滤掉的debug日志的处理耗时
        let start = Instant::now();
        for i in 0..iterations {
            debug!("这条日志应该被过滤掉，测试其处理耗时: {}", i);
        }
        let filtered_duration = start.elapsed();
        println!("处理 {} 条被过滤的 debug 级别日志耗时: {:?}, 平均每条: {:?}", 
            iterations, filtered_duration, filtered_duration / iterations as u32);
    }

    #[test]
    #[ignore]
    pub fn test_update_log_level() {
        // 配置测试日志
        let config = ServerConfig {

            log_level: Some(String::from("info")),
            log_rotation: None,
            ..Default::default()
        };
        
        // 初始化日志系统
        init_logger(&config);
        
        // 初始状态下，debug日志应该被过滤
        debug!("这条DEBUG日志应该被过滤掉");
        info!("这条INFO日志应该可以看到");
        
        // 更新日志级别为debug
        match update_log_level("debug") {
            Ok(msg) => println!("{}", msg),
            Err(e) => println!("更新失败: {}", e),
        }
        
        // 现在debug日志应该可见
        debug!("更新后，这条DEBUG日志应该可以看到");
        info!("这条INFO日志仍然可以看到");
        
        // 更新为复杂格式
        match update_log_level("pgraph=debug,tokio=info,info") {
            Ok(msg) => println!("{}", msg),
            Err(e) => println!("更新失败: {}", e),
        }
        
        // 测试复杂格式下的日志过滤
        debug!("这是一条pgraph的DEBUG日志，应该可以看到");
        debug!(target: "tokio::runtime", "这是一条tokio的DEBUG日志，应该被过滤掉");
        info!(target: "tokio::runtime", "这是一条tokio的INFO日志，应该可以看到");
        
        // 获取当前日志级别
        let current_level = get_current_log_level();
        println!("当前日志级别: {:?}", current_level);
    }

    #[test]
    pub fn test_logger() {
        // 保留原有空测试方法
    }

    #[test]
    #[ignore]
    pub fn test_logger_filter_debug() {
        // 配置测试日志，使用复杂过滤器格式
        let config = ServerConfig {

            log_level: Some(String::from("pgraph=debug,tokio=info,info")),
            log_rotation: None,
            // 其他字段以默认值初始化
            ..Default::default()
        };
        
        // 初始化日志系统
        init_logger(&config);
        
        // 打印测试日志
        info!("这是一条pgraph的INFO日志，应该可以看到");
        debug!("这是一条pgraph的DEBUG日志，应该可以看到");
        
        // 模拟来自其他库的日志（通过设置target）
        debug!(target: "tokio::runtime", "这是一条tokio的DEBUG日志，应该被过滤掉");
        info!(target: "tokio::runtime", "这是一条tokio的INFO日志，应该可以看到");
        
        // 测试更多第三方库
        debug!(target: "hyper::client", "这是一条hyper的DEBUG日志，应该被过滤掉");
        debug!(target: "tonic::transport", "这是一条tonic的DEBUG日志，应该被过滤掉");
        debug!(target: "some_random_library", "这是一条未知库的DEBUG日志，应该被过滤掉");
        
        // 测试pgraph的子模块
        debug!(target: "pgraph::database", "这是一条pgraph子模块的DEBUG日志，应该可以看到");
        
        // 更新日志级别
        update_log_level("debug").unwrap();
        
        // 现在所有的debug日志都应该可见
        debug!(target: "tokio::runtime", "更新后，这条tokio的DEBUG日志应该可以看到");
        debug!(target: "hyper::client", "更新后，这条hyper的DEBUG日志应该可以看到");
        
        println!("测试完成，请查看日志文件：/tmp/pgraph_test_logs/pgraph.*.log");
    }
}