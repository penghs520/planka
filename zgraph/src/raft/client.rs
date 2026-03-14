use std::collections::BTreeSet;
use std::sync::Arc;
use std::sync::Mutex;

use openraft::error::ForwardToLeader;
use openraft::error::Infallible;
use openraft::error::InitializeError;
use openraft::error::NetworkError;
use openraft::error::RPCError;
use openraft::error::{ClientWriteError, RemoteError, Unreachable};
use openraft::raft::ClientWriteResponse;
use openraft::{RaftMetrics, TryAsRef};
use reqwest::Client;
use serde::de::DeserializeOwned;
use serde::Serialize;
use tracing::debug;
use crate::raft::store::Request;
use crate::raft::{NodeId, TypeConfig};

pub struct RaftClient
{
    /// The leader node to send request to.
    ///
    /// All traffic should be sent to the leader in a cluster.
    pub leader: Arc<Mutex<(NodeId, String)>>,

    pub inner: Client,
}

impl RaftClient
{
    /// Create a client with a leader node id and a node manager to get node address by node id.
    pub fn new(leader_id: NodeId, leader_addr: String) -> Self {
        Self {
            leader: Arc::new(Mutex::new((leader_id, leader_addr))),
            inner: Client::new(),
        }
    }

    // --- Application API

    /// Submit a write request to the raft cluster.
    ///
    /// The request will be processed by raft protocol: it will be replicated to a quorum and then will be applied to
    /// state machine.
    ///
    /// The result of applying the request will be returned.
    pub async fn write(
        &self,
        req: &Request,
    ) -> Result<ClientWriteResponse<TypeConfig>, RPCError<TypeConfig, ClientWriteError<TypeConfig>>>
    {
        match self.send_with_forwarding("api/write", Some(req), 3).await {
            Ok(Ok(resp)) => Ok(resp),
            Ok(Err(client_err)) => Err(RPCError::RemoteError(RemoteError::new(NodeId::default(), client_err))),
            Err(RPCError::Network(e)) => Err(RPCError::Network(e)),
            Err(RPCError::Timeout(e)) => Err(RPCError::Timeout(e)),
            Err(RPCError::Unreachable(e)) => Err(RPCError::Unreachable(e)),
            Err(RPCError::RemoteError(_)) => unreachable!("send_with_forwarding should not return RemoteError with Infallible"),
        }
    }

    // --- Cluster management API

    /// Initialize a cluster of only the node that receives this request.
    ///
    /// This is the first step to initialize a cluster.
    /// With a initialized cluster, new node can be added with [`write`].
    /// Then setup replication with [`add_learner`].
    /// Then make the new node a member with [`change_membership`].
    pub async fn init(&self) -> Result<Result<(), InitializeError<TypeConfig>>, RPCError<TypeConfig>> {
        self.send("cluster/init", None::<&()>).await
    }

    /// Add a node as learner.
    ///
    /// The node to add has to exist, i.e., being added with `write(ExampleRequest::AddNode{})`
    pub async fn add_learner(
        &self,
        req: (NodeId, String),
    ) -> Result<Result<ClientWriteResponse<TypeConfig>, ClientWriteError<TypeConfig>>, RPCError<TypeConfig>>
    {
        self.send_with_forwarding("cluster/add-learner", Some(&req), 3).await
    }

    /// Change membership to the specified set of nodes.
    ///
    /// All nodes in `req` have to be already added as learner with [`add_learner`],
    /// or an error [`LearnerNotFound`] will be returned.
    pub async fn change_membership(
        &self,
        req: &BTreeSet<NodeId>,
    ) -> Result<Result<ClientWriteResponse<TypeConfig>, ClientWriteError<TypeConfig>>, RPCError<TypeConfig>> {
        self.send_with_forwarding("cluster/change-membership", Some(req), 3).await
    }

    /// Get the metrics about the cluster.
    ///
    /// Metrics contains various information about the cluster, such as current leader,
    /// membership config, replication status etc.
    /// See [`RaftMetrics`].
    pub async fn metrics(&self) -> Result<RaftMetrics<TypeConfig>, RPCError<TypeConfig>> {
        let res = self.send::<_, _, Infallible>("cluster/metrics", None::<&()>).await?;
        Ok(res.unwrap())
    }

    /// 触发节点创建快照
    ///
    pub async fn trigger_snapshot(&self) -> Result<(), RPCError<TypeConfig, Infallible>> {
        let res: Result<(), Infallible> = self.send("cluster/trigger-snapshot", None::<&()>).await?;
        Ok(res.unwrap())
    }

    async fn send_with_forwarding<Req, Resp, Err>(
        &self,
        uri: &str,
        req: Option<&Req>,
        mut retry: usize,
    ) -> Result<Result<Resp, Err>, RPCError<TypeConfig>>
    where
        Req: Serialize + 'static,
        Resp: Serialize + DeserializeOwned,
        Err:
        std::error::Error + Serialize + DeserializeOwned + TryAsRef<ForwardToLeader<TypeConfig>> + Clone,
    {
        loop {
            let res: Result<Resp, Err> = self.send(uri, req).await?;

            let rpc_err = match res {
                Ok(x) => return Ok(Ok(x)),
                Err(rpc_err) => rpc_err,
            };

            if let Some(ForwardToLeader {
                            leader_id: Some(leader_id),
                            leader_node: Some(leader_node),
                            ..
                        }) = rpc_err.try_as_ref()
            {
                // Update target to the new leader.
                {
                    let mut t = self.leader.lock().unwrap();
                    let api_addr = leader_node.rpc_addr.clone();
                    *t = (*leader_id, api_addr);
                }

                if retry > 0 {
                    retry -= 1;
                    continue;
                }
            }

            return Ok(Err(rpc_err));
        }
    }

    async fn send<Req, Resp, Err>(&self, uri: &str, req: Option<&Req>) -> Result<Result<Resp, Err>, RPCError<TypeConfig>>
    where
        Req: Serialize + 'static,
        Resp: Serialize + DeserializeOwned,
        Err: std::error::Error + Serialize + DeserializeOwned,
    {
        let (_leader_id, url) = {
            let t = self.leader.lock().unwrap();
            let target_addr = &t.1;
            (t.0.clone(), format!("http://{}/{}", target_addr, uri))
        };

        let resp = if let Some(r) = req {
            debug!(
                ">>> client send request to {}: {}",
                url,
                serde_json::to_string_pretty(&r).unwrap()
            );
            self.inner.post(url.clone()).json(r)
        } else {
            debug!(">>> client send request to {}", url, );
            self.inner.get(url.clone())
        }
            .send()
            .await
            .map_err(|e| {
                if e.is_connect() {
                    // `Unreachable` informs the caller to backoff for a short while to avoid error log flush.
                    RPCError::Unreachable(Unreachable::new(&e))
                } else {
                    RPCError::Network(NetworkError::new(&e))
                }
            })?;

        let res: Result<Resp, Err> = resp.json().await.map_err(|e| {
            debug!(">>> client send response error: {}", e);
            RPCError::Network(NetworkError::new(&e))
        })?;
        debug!(
            "<<< client recv reply from {}: {}",
            url,
            serde_json::to_string_pretty(&res).unwrap()
        );

        Ok(res)
    }
}