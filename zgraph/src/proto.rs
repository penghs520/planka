// 引入从proto文件生成的模块
// 根据实际生成的模块名调整


// zgraph.query 对应包
pub mod zgraph {
    pub mod query {
       include!("proto/zgraph.query.rs");
    }

    pub mod linkquery {
        include!("proto/zgraph.linkquery.rs");
    }
    
    pub mod write {
        include!("proto/zgraph.write.rs");
    }
    
    pub mod model {
        include!("proto/zgraph.model.rs");
    }
    
    pub mod common {
        include!("proto/zgraph.common.rs");
    }
    
    pub mod field {
        include!("proto/zgraph.field.rs");
    }
    pub mod auth {
        include!("proto/zgraph.auth.rs");
    }

    pub mod request {
        include!("proto/zgraph.request.rs");
    }

    pub mod response {
        include!("proto/zgraph.response.rs");
    }   
    
    pub mod admin {
        include!("proto/zgraph.admin.rs");
    }
}

pub use zgraph::admin::*;
pub use zgraph::auth::*;
pub use zgraph::common::*;
pub use zgraph::field::*;
pub use zgraph::linkquery::*;
pub use zgraph::model::*;
// 重新导出常用的类型
pub use zgraph::query::*;
pub use zgraph::request::*;
pub use zgraph::response::*;
pub use zgraph::write::*;
