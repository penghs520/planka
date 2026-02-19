use rocksdb::{ColumnFamilyRef, OptimisticTransactionDB, ReadOptions, WriteBatchWithTransaction};
use std::io::Cursor;

use crate::{
    database::errors::DbResult,
    database::model::{Edge, EdgeDescriptor, EdgeDirection, VertexId},
    utils::{self, build},
};

use super::memory::EdgeDeltaItem;

/// 空字节数组常量 - 用于存储没有值的键
const EMPTY_U8: [u8; 0] = [];

/// 边管理器 - 负责边数据的底层存储和检索
pub(super) struct EdgeManager<'a> {
    /// RocksDB数据库引用
    db: &'a OptimisticTransactionDB,

    /// 边数据的列族引用
    edge_cf: ColumnFamilyRef<'a>,

    /// 边的增量变更记录
    pub delta: EdgeDelta,
}

/// 边增量更新结构 - 跟踪边的变化
pub(super) struct EdgeDelta {
    /// 已删除的边
    pub delete: Vec<EdgeDeltaItem>,
    
    /// 新创建的边
    pub create: Vec<EdgeDeltaItem>,
    
    /// 已更新的边
    pub update: Vec<EdgeDeltaItem>,
}

impl Default for EdgeDelta {
    fn default() -> Self {
        Self {
            delete: Vec::default(),
            create: Vec::default(),
            update: Vec::default(),
        }
    }
}

impl<'a> EdgeManager<'a> {
    /// 创建新的边管理器
    ///
    /// # 参数
    /// * `db` - RocksDB数据库引用
    ///
    /// # 返回
    /// 新的EdgeManager实例
    pub(super) fn new(db: &'a OptimisticTransactionDB) -> EdgeManager<'a> {
        EdgeManager {
            db,
            edge_cf: db.cf_handle("edge_cf").unwrap(),
            delta: EdgeDelta {
                delete: Default::default(),
                create: Default::default(),
                update: Default::default(),
            },
        }
    }

    /// 生成边的键
    ///
    /// 将边的源节点ID、边描述符和目标节点ID组合成键
    fn key(
        &self,
        src_vertex_id: &VertexId,
        edge_descriptor: &EdgeDescriptor,
        dest_vertex_id: &VertexId,
    ) -> Vec<u8> {
        build(&[
            utils::Component::Identifier(&edge_descriptor.t),
            utils::Component::EdgeDirection(&edge_descriptor.direction),
            utils::Component::VertexId(src_vertex_id),
            utils::Component::VertexId(dest_vertex_id),
        ])
    }


    /// 创建边
    ///
    /// 一条边会创建两条记录，因为关系是双向的:
    /// * A -> B: key: edge_type:inbound:a_id:b_id，值为边属性
    /// * B <- A: key: edge_type:outbound:b_id:a_id，值为边属性
    ///
    /// 查询示例: 要查询A的关联节点B，使用edge_type:inbound:a_id前缀进行查询，
    /// 通过对key进行剪裁可以得到关联的B节点id
    pub fn create(
        &mut self,
        batch: &mut WriteBatchWithTransaction<true>,
        edge: &Edge,
    ) -> DbResult<bool> {
        // 如果边已存在，则返回false
        let key = self.key(
            &edge.src_id,
            &EdgeDescriptor {
                t: edge.t,
                direction: EdgeDirection::Src,
            },
            &edge.dest_id,
        );

        // 如果边有属性，则序列化并存储
        if let Some(props) = &edge.props {
            let serialized = bincode::serialize(props)?;
            batch.put_cf(&self.edge_cf, &key, &serialized);
        } else {
            batch.put_cf(&self.edge_cf, &key, &EMPTY_U8);
        }

        // 创建反向边的键
        let dest_to_src_key = self.key(
            &edge.dest_id,
            &EdgeDescriptor {
                t: edge.t,
                direction: EdgeDirection::Dest,
            },
            &edge.src_id,
        );
        batch.put_cf(&self.edge_cf, &dest_to_src_key, &EMPTY_U8);
        self.delta.create.push(EdgeDeltaItem {
            src_vertex_id: edge.src_id,
            edge_descriptor: EdgeDescriptor {
                t: edge.t,
                direction: EdgeDirection::Src,
            },
            dest_vertex_id: edge.dest_id,
            props: edge.props.clone(),
        });

        // 更新内存缓存 - 反向边
        self.delta.create.push(EdgeDeltaItem {
            src_vertex_id: edge.dest_id,
            edge_descriptor: EdgeDescriptor {
                t: edge.t,
                direction: EdgeDirection::Dest,
            },
            dest_vertex_id: edge.src_id,
            props: None, //反向边属性为空，要想设置反方向的边，需要使用update方法再保存一次反方向的边
        });

        Ok(true)
    }

    /// 删除一条边
    ///
    /// 根据边对象删除正向和反向边
    pub fn delete(
        &mut self,
        batch: &mut WriteBatchWithTransaction<true>,
        edge: &Edge,
    ) -> DbResult<()> {
        // 删除源节点到目标节点的边
        let src_to_dest_key = self.key(
            &edge.src_id,
            &EdgeDescriptor {
                t: edge.t,
                direction: EdgeDirection::Src,
            },
            &edge.dest_id,
        );
        batch.delete_cf(&self.edge_cf, &src_to_dest_key);

        // 删除目标节点到源节点的反向边
        let dest_to_src_key = self.key(
            &edge.dest_id,
            &EdgeDescriptor {
                t: edge.t,
                direction: EdgeDirection::Dest,
            },
            &edge.src_id,
        );
        batch.delete_cf(&self.edge_cf, &dest_to_src_key);

        // 更新内存中的delta记录 - 正向边
        self.delta.delete.push(EdgeDeltaItem {
            src_vertex_id: edge.src_id,
            edge_descriptor: EdgeDescriptor {
                t: edge.t,
                direction: EdgeDirection::Src,
            },
            dest_vertex_id: edge.dest_id,
            props: edge.props.clone(),
        });

        // 更新内存中的delta记录 - 反向边
        self.delta.delete.push(EdgeDeltaItem {
            src_vertex_id: edge.dest_id,
            edge_descriptor: EdgeDescriptor {
                t: edge.t,
                direction: EdgeDirection::Dest,
            },
            dest_vertex_id: edge.src_id,
            props: None,
        });

        Ok(())
    }

    /// 根据边描述符获取所有匹配的边
    ///
    /// 返回匹配指定描述符的所有边
    pub fn get_edges_by_descriptor(&self, descriptor: &EdgeDescriptor) -> DbResult<Vec<Edge>> {
        let mut edges = Vec::new();

        // 获取边描述符对应的所有前缀匹配项
        let mut iter_opts = ReadOptions::default();
        iter_opts.set_prefix_same_as_start(true);

        // 构建前缀用于搜索
        let prefix = build(&[
            utils::Component::Identifier(&descriptor.t),
            utils::Component::EdgeDirection(&descriptor.direction),
        ]);

        // 使用前缀进行查询
        let iter = self.db.iterator_cf_opt(
            &self.edge_cf,
            iter_opts,
            rocksdb::IteratorMode::From(&prefix, rocksdb::Direction::Forward),
        );

        // 遍历结果，找出所有匹配的边
        for item in iter {
            let (key, _) = item?;

            // 检查是否仍然在前缀范围内
            if !key.starts_with(&prefix) {
                break;
            }

            // 从键中提取源节点ID和目标节点ID
            // 键的格式应该是：edge_type + direction + src_id + dest_id
            let mut cursor = Cursor::new(&key);
            let _ = utils::read_identifier(&mut cursor); // 跳过边类型
            let _ = utils::read_edge_direction(&mut cursor); // 跳过边方向
            let src_id = utils::read_vertex_id(&mut cursor); // 读取源节点ID
            let dest_id = utils::read_vertex_id(&mut cursor); // 读取目标节点ID

            // 创建边对象
            let edge = Edge::new(src_id, descriptor.t.clone(), dest_id, Some(vec![]));
            edges.push(edge);
        }

        Ok(edges)
    }

   /// 更新边的属性
    ///
    /// 更新现有边的属性值，如果边不存在，返回错误
    pub fn update(
        &mut self,
        batch: &mut WriteBatchWithTransaction<true>,
        edge: &Edge,
    ) -> DbResult<bool> {
        let key = self.key(
            &edge.src_id,
            &EdgeDescriptor {
                t: edge.t,
                direction: EdgeDirection::Src,
            },
            &edge.dest_id,
        );

        // 如果边有属性，则序列化并存储
        if let Some(props) = &edge.props {
            let serialized = bincode::serialize(props)?;
            batch.put_cf(&self.edge_cf, &key, &serialized);
        } else {
            batch.put_cf(&self.edge_cf, &key, &EMPTY_U8);
        }

        self.delta.update.push(EdgeDeltaItem {
            src_vertex_id: edge.src_id,
            edge_descriptor: EdgeDescriptor {
                t: edge.t,
                direction: EdgeDirection::Src,
            },
            dest_vertex_id: edge.dest_id,
            props: edge.props.clone(),
        });

        Ok(true)
    }
} 