package cn.planka.domain.schema.definition.workflow;

import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.SchemaId;
import cn.planka.domain.schema.SchemaSubType;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.WorkflowId;
import cn.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * 工作流定义（持久化形态需继承 {@link AbstractSchemaDefinition}，与 Schema 通用保存接口一致）
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class WorkflowDefinition extends AbstractSchemaDefinition<WorkflowId> {

    @JsonProperty("cardTypeId")
    private CardTypeId cardTypeId;

    @JsonProperty("trigger")
    private WorkflowTrigger trigger;

    @JsonProperty("nodes")
    private List<NodeDefinition> nodes;

    @JsonProperty("edges")
    private List<EdgeDefinition> edges;

    /**
     * 画布节点坐标（nodeId -&gt; 位置），可选；用于前端编排拖拽后的布局持久化。
     */
    @JsonProperty("canvasLayout")
    private Map<String, CanvasNodePosition> canvasLayout;

    @JsonCreator
    public WorkflowDefinition(
            @JsonProperty("id") WorkflowId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.WORKFLOW_DEFINITION;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.WORKFLOW;
    }

    @Override
    public SchemaId belongTo() {
        return cardTypeId;
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return cardTypeId != null ? Set.of(cardTypeId) : Set.of();
    }

    @Override
    protected WorkflowId newId() {
        return WorkflowId.generate();
    }

    @Override
    public void validate() {
        super.validate();
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("工作流至少需要一个节点");
        }
        if (edges == null) {
            throw new IllegalArgumentException("边列表不能为空");
        }

        long startNodeCount = nodes.stream()
                .filter(n -> n instanceof StartNodeDefinition)
                .count();
        if (startNodeCount != 1) {
            throw new IllegalArgumentException("工作流必须有且仅有一个开始节点");
        }

        long endNodeCount = nodes.stream()
                .filter(n -> n instanceof EndNodeDefinition)
                .count();
        if (endNodeCount != 1) {
            throw new IllegalArgumentException("工作流必须有且仅有一个结束节点");
        }

        if (nodes.size() > 50) {
            throw new IllegalArgumentException("工作流节点数量不能超过50个");
        }

        Set<String> nodeIds = new HashSet<>();
        for (NodeDefinition node : nodes) {
            if (!nodeIds.add(node.id())) {
                throw new IllegalArgumentException("节点ID重复: " + node.id());
            }
        }

        for (EdgeDefinition edge : edges) {
            if (!nodeIds.contains(edge.sourceNodeId())) {
                throw new IllegalArgumentException("边的源节点不存在: " + edge.sourceNodeId());
            }
            if (!nodeIds.contains(edge.targetNodeId())) {
                throw new IllegalArgumentException("边的目标节点不存在: " + edge.targetNodeId());
            }
        }

        if (hasCycle()) {
            throw new IllegalArgumentException("工作流不能包含循环");
        }

        String startNodeId = nodes.stream()
                .filter(n -> n instanceof StartNodeDefinition)
                .findFirst()
                .map(NodeDefinition::id)
                .orElseThrow();

        // 画布编辑允许暂时出现孤岛；保存/启动前必须全图从「开始」可达
        Set<String> reachableNodes = getReachableNodes(startNodeId);
        if (reachableNodes.size() != nodes.size()) {
            throw new IllegalArgumentException("存在无法从开始节点到达的孤立节点");
        }
    }

    private boolean hasCycle() {
        Map<String, List<String>> graph = buildGraph();
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (String nodeId : graph.keySet()) {
            if (hasCycleUtil(nodeId, graph, visited, recStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycleUtil(String nodeId, Map<String, List<String>> graph,
                                 Set<String> visited, Set<String> recStack) {
        if (recStack.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recStack.add(nodeId);

        List<String> neighbors = graph.getOrDefault(nodeId, List.of());
        for (String neighbor : neighbors) {
            if (hasCycleUtil(neighbor, graph, visited, recStack)) {
                return true;
            }
        }

        recStack.remove(nodeId);
        return false;
    }

    private Set<String> getReachableNodes(String startNodeId) {
        Map<String, List<String>> graph = buildGraph();
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(startNodeId);
        reachable.add(startNodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<String> neighbors = graph.getOrDefault(current, List.of());
            for (String neighbor : neighbors) {
                if (reachable.add(neighbor)) {
                    queue.offer(neighbor);
                }
            }
        }

        return reachable;
    }

    private Map<String, List<String>> buildGraph() {
        Map<String, List<String>> graph = new HashMap<>();
        for (NodeDefinition node : nodes) {
            graph.put(node.id(), new ArrayList<>());
        }
        for (EdgeDefinition edge : edges) {
            graph.get(edge.sourceNodeId()).add(edge.targetNodeId());
        }
        return graph;
    }
}
