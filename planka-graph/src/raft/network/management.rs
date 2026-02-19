use std::collections::BTreeMap;
use std::collections::BTreeSet;

use crate::raft::app::pgraphRaftApp;
use crate::raft::{NodeId, TypeConfig};
use actix_web::get;
use actix_web::post;
use actix_web::web;
use actix_web::web::Data;
use actix_web::Responder;
use openraft::async_runtime::WatchReceiver;
use openraft::error::Infallible;
use openraft::RaftMetrics;
use web::Json;

// --- Cluster management

/// Add a node as **Learner**.
///
/// A Learner receives log replication from the leader but does not vote.
/// This should be done before adding a node as a member into the cluster
/// (by calling `change-membership`)
#[post("/cluster/add-learner")]
pub async fn add_learner(
    app: Data<pgraphRaftApp>,
    req: Json<(NodeId, String)>,
) -> actix_web::Result<impl Responder> {
    let node_id = req.0.0;
    let rpr_addr = req.0.1;
    let node = crate::raft::Node {
        rpc_addr: rpr_addr.clone(),
    };
    let res = app.raft.add_learner(node_id, node, true).await;
    Ok(Json(res))
}

/// Changes specified learners to members, or remove members.
#[post("/cluster/change-membership")]
pub async fn change_membership(
    app: Data<pgraphRaftApp>,
    req: Json<BTreeSet<NodeId>>,
) -> actix_web::Result<impl Responder> {
    let res = app.raft.change_membership(req.0, false).await;
    Ok(Json(res))
}

/// Initialize a single-node cluster.
#[get("/cluster/init")]
pub async fn init(app: Data<pgraphRaftApp>) -> actix_web::Result<impl Responder> {
    let mut nodes = BTreeMap::new();
    let node = crate::raft::Node {
        rpc_addr: app.rpc_addr.clone(),
    };
    nodes.insert(app.node_id, node);
    let res = app.raft.initialize(nodes).await;
    Ok(Json(res))
}

/// Get the latest metrics of the cluster
#[get("/cluster/metrics")]
pub async fn metrics(app: Data<pgraphRaftApp>) -> actix_web::Result<impl Responder> {
    let metrics = app.raft.metrics().borrow_watched().clone();

    let res: Result<RaftMetrics<TypeConfig>, Infallible> = Ok(metrics);
    Ok(Json(res))
}

/// 触发节点创建快照
///
/// 可以在需要时主动触发节点创建快照，防止日志过长
#[get("/cluster/trigger-snapshot")]
pub async fn trigger_snapshot(app: Data<pgraphRaftApp>) -> actix_web::Result<impl Responder> {
    let res = app.raft.trigger().snapshot().await;
    Ok(Json(res))
}
