use crate::raft::app::pgraphRaftApp;
use crate::raft::types::{SnapshotMeta, Vote, VoteRequest};
use crate::raft::TypeConfig;
use actix_web::post;
use actix_web::web;
use actix_web::web::Data;
use actix_web::Responder;
use openraft::raft::AppendEntriesRequest;
use openraft::storage::Snapshot;
use std::io::Cursor;
use web::Json;

// --- Raft communication
#[post("/raft-vote")]
pub async fn vote(app: Data<pgraphRaftApp>, req: Json<VoteRequest>) -> actix_web::Result<impl Responder> {
    let res = app.raft.vote(req.0).await;
    Ok(Json(res))
}

#[post("/raft-append")]
pub async fn append(
    app: Data<pgraphRaftApp>,
    req: Json<AppendEntriesRequest<TypeConfig>>,
) -> actix_web::Result<impl Responder> {
    let res = app.raft.append_entries(req.0).await;
    Ok(Json(res))
}

#[post("/raft-snapshot")]
pub async fn snapshot(
    app: Data<pgraphRaftApp>,
    req: Json<(Vote, SnapshotMeta, Vec<u8>)>,
) -> actix_web::Result<impl Responder> {
    let (req_vote, meta, data) = req.0;
    let snapshot = Snapshot {
        meta,
        snapshot: Cursor::new(data),
    };
    let res = app.raft.install_full_snapshot(req_vote, snapshot).await;
    Ok(Json(res))
}
