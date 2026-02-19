use std::collections::HashMap;
use std::future::Future;
use std::sync::Arc;

use crate::raft::types::Node;
use crate::raft::types::{Snapshot, SnapshotResponse, StreamingError, Vote};
use crate::raft::{NodeId, TypeConfig};
use openraft::error::NetworkError;
use openraft::error::RPCError;
use openraft::error::ReplicationClosed;
use openraft::error::Unreachable;
use openraft::network::RPCOption;
use openraft::raft::AppendEntriesRequest;
use openraft::raft::AppendEntriesResponse;
use openraft::raft::VoteRequest;
use openraft::raft::VoteResponse;
use openraft::{OptionalSend, RaftNetworkV2};
use openraft::RaftNetworkFactory;
use serde::de::DeserializeOwned;
use serde::Serialize;

pub struct pgraphRaftNetwork {
    pub clients: Arc<HashMap<String, reqwest::Client>>,
}

impl pgraphRaftNetwork {
    pub fn new() -> Self {
        Self {
            clients: Arc::new(HashMap::new()),
        }
    }

    pub async fn send_rpc<Req, Resp>(
        &mut self,
        _target: NodeId,
        target_node: Option<&Node>,
        uri: &str,
        req: Req,
    ) -> Result<Resp, RPCError<TypeConfig>>
    where
        Req: Serialize,
        Resp: DeserializeOwned,
    {
        let addr = target_node.map(|x| &x.rpc_addr).unwrap();

        let url = format!("http://{}/{}", addr, uri);

        let clients = Arc::get_mut(&mut self.clients).unwrap();

        let client = clients.entry(url.clone()).or_insert(reqwest::Client::new());

        let resp = client
            .post(url)
            .json(&req)
            .send()
            .await
            .map_err(|e| {
                // 检查是否是连接错误，如果是则返回Unreachable错误以触发退避机制
                if e.is_connect() || e.is_timeout() {
                    RPCError::Unreachable(Unreachable::new(&e))
                } else {
                    RPCError::Network(NetworkError::new(&e))
                }
            })?;

        // Check HTTP status code first
        if !resp.status().is_success() {
            let status = resp.status();
            let body = resp.text().await.unwrap_or_default();
            return Err(RPCError::Network(NetworkError::new(&std::io::Error::new(
                std::io::ErrorKind::Other,
                format!("HTTP error {}: {}", status, body),
            ))));
        }

        resp.json()
            .await
            .map_err(|e| RPCError::Network(NetworkError::new(&e)))
    }
}

impl RaftNetworkFactory<TypeConfig> for pgraphRaftNetwork {
    type Network = ExampleNetworkConnection;

    async fn new_client(&mut self, target: NodeId, node: &Node) -> Self::Network {
        ExampleNetworkConnection {
            owner: pgraphRaftNetwork::new(),
            target,
            target_node: Some(node.clone()),
        }
    }
}

pub struct ExampleNetworkConnection {
    owner: pgraphRaftNetwork,
    target: NodeId,
    target_node: Option<Node>,
}

impl RaftNetworkV2<TypeConfig> for ExampleNetworkConnection {
    async fn append_entries(
        &mut self,
        req: AppendEntriesRequest<TypeConfig>,
        _option: RPCOption,
    ) -> Result<AppendEntriesResponse<TypeConfig>, RPCError<TypeConfig>> {
        self.owner
            .send_rpc(self.target, self.target_node.as_ref(), "raft-append", req)
            .await
    }

    async fn full_snapshot(
        &mut self,
        vote: Vote,
        snapshot: Snapshot,
        _cancel: impl Future<Output = ReplicationClosed> + OptionalSend + 'static,
        _option: RPCOption,
    ) -> Result<SnapshotResponse, StreamingError> {
        // Extract inner Vec<u8> from Cursor for serialization
        let data: Vec<u8> = snapshot.snapshot.into_inner();
        self.owner
            .send_rpc(
                self.target,
                self.target_node.as_ref(),
                "raft-snapshot",
                (vote, snapshot.meta, data),
            )
            .await
            .map_err(|e| StreamingError::from(e))
    }

    async fn vote(
        &mut self,
        req: VoteRequest<TypeConfig>,
        _option: RPCOption,
    ) -> Result<VoteResponse<TypeConfig>, RPCError<TypeConfig>> {
        self.owner
            .send_rpc(self.target, self.target_node.as_ref(), "raft-vote", req)
            .await
    }
}
