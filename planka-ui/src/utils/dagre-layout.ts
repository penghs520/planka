import dagre from '@dagrejs/dagre'
import type { Node, Edge } from '@vue-flow/core'

export interface DagreLayoutOptions {
  direction?: 'TB' | 'LR'
  nodeWidth?: number
  nodeHeight?: number
  rankSep?: number    // 层级间距
  nodeSep?: number    // 同层节点间距
}

export function applyDagreLayout(
  nodes: Node[],
  edges: Edge[],
  options: DagreLayoutOptions = {}
): Node[] {
  const opts = {
    direction: 'LR' as const,   // 左到右，适合 ER 图
    nodeWidth: 280,
    nodeHeight: 200,
    rankSep: 200,                // 层级间距大，给连线留空间
    nodeSep: 60,
    ...options,
  }

  const g = new dagre.graphlib.Graph()
  g.setDefaultEdgeLabel(() => ({}))
  g.setGraph({
    rankdir: opts.direction,
    ranksep: opts.rankSep,
    nodesep: opts.nodeSep,
  })

  const visibleNodes = nodes.filter(n => !n.hidden)
  const visibleNodeIds = new Set(visibleNodes.map(n => n.id))

  for (const node of visibleNodes) {
    g.setNode(node.id, { width: opts.nodeWidth, height: opts.nodeHeight })
  }

  for (const edge of edges) {
    if (edge.source === edge.target) continue  // dagre 不支持自环
    if (!visibleNodeIds.has(edge.source) || !visibleNodeIds.has(edge.target)) continue
    g.setEdge(edge.source, edge.target)
  }

  dagre.layout(g)

  return nodes.map(node => {
    const pos = g.node(node.id)
    if (!pos) return node
    return {
      ...node,
      position: {
        x: pos.x - opts.nodeWidth / 2,  // dagre 返回中心点，转左上角
        y: pos.y - opts.nodeHeight / 2,
      },
    }
  })
}
