package cn.planka.domain.schema.definition.workflow;

/**
 * 工作流画布上节点的二维坐标（仅用于前端编排展示与持久化）。
 */
public record CanvasNodePosition(double x, double y) {
}
