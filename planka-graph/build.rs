fn main() -> Result<(), Box<dyn std::error::Error>> {
    // proto folder is now in the project root, so we need to go up one level
    let proto_dir = std::env::current_dir()?.parent().unwrap().join("proto").display().to_string();
    
    println!("cargo:rerun-if-changed={}", proto_dir);
    
    // 使用更简单的方法，让tonic-build自动查找并编译所有相关的proto文件
    tonic_build::configure()
        .out_dir("src/proto")
        .compile_well_known_types(true)
        .build_server(true)
        .build_client(true)
        .type_attribute(".", "#[derive(serde::Serialize, serde::Deserialize)]")
        // 直接编译入口proto文件，让protoc自动处理导入关系
        .compile(
            &[
                format!("{}/query/query.proto", proto_dir),
                format!("{}/linkquery/linkquery.proto", proto_dir),
                format!("{}/write/write.proto", proto_dir),
                format!("{}/auth/auth.proto", proto_dir),
                format!("{}/request/request.proto",proto_dir),
                format!("{}/response/response.proto",proto_dir),
            ],
            &[proto_dir],
        )?;
    
    Ok(())
} 