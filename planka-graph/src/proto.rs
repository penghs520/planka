// 引入从proto文件生成的模块
// 根据实际生成的模块名调整


// pgraph.query 对应包
pub mod pgraph {
    pub mod query {
       include!("proto/pgraph.query.rs");
    }

    pub mod linkquery {
        include!("proto/pgraph.linkquery.rs");
    }
    
    pub mod write {
        include!("proto/pgraph.write.rs");
    }
    
    pub mod model {
        include!("proto/pgraph.model.rs");
    }
    
    pub mod common {
        include!("proto/pgraph.common.rs");
    }
    
    pub mod field {
        include!("proto/pgraph.field.rs");
    }
    pub mod auth {
        include!("proto/pgraph.auth.rs");
    }

    pub mod request {
        include!("proto/pgraph.request.rs");
    }

    pub mod response {
        include!("proto/pgraph.response.rs");
    }   
    
    pub mod admin {
        include!("proto/pgraph.admin.rs");
    }
}

pub use pgraph::admin::*;
pub use pgraph::auth::*;
pub use pgraph::common::*;
pub use pgraph::field::*;
pub use pgraph::linkquery::*;
pub use pgraph::model::*;
// 重新导出常用的类型
pub use pgraph::query::*;
pub use pgraph::request::*;
pub use pgraph::response::*;
pub use pgraph::write::*;
