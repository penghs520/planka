use std::sync::Arc;

use crate::raft::types::Raft;
use crate::raft::NodeId;
use openraft::Config;

// Representation of an application state. This struct can be shared around to share
// instances of raft, store and more.
pub struct pgraphRaftApp {
    pub node_id: NodeId,
    pub rpc_addr: String,
    pub raft: Raft,
    pub config: Arc<Config>,
}
