use actix_web::post;
use actix_web::web;
use actix_web::web::Data;
use actix_web::Responder;
use openraft::error::decompose::DecomposeResult;
use web::Json;

use crate::raft::app::pgraphRaftApp;
use crate::raft::store::Request;

/**
 * Application API
 *
 */
#[post("/api/write")]
pub async fn write(app: Data<pgraphRaftApp>, req: Json<Request>) -> actix_web::Result<impl Responder> {
    let response = app.raft.client_write(req.0).await.decompose().unwrap();
    Ok(Json(response))
}