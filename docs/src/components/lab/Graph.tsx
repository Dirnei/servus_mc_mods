import dagre from '@dagrejs/dagre';
import type { ComponentChildren } from 'preact';
import { useMemo } from 'preact/hooks';

/** A node of a layered diagram. Its body is drawn as HTML inside the SVG. */
export interface GraphNode {
	id: string;
	width: number;
	height: number;
	tone?: 'normal' | 'draft' | 'warning' | 'muted';
	selected?: boolean;
	/** Extra classes on the node button, for example a colour. */
	className?: string;
	body: ComponentChildren;
	onSelect?: () => void;
}

export interface GraphEdge {
	from: string;
	to: string;
	label?: string;
	dashed?: boolean;
	dimmed?: boolean;
}

/** Lays out nodes with dagre (top to bottom by default: raw resources flow down into products) and draws them as SVG. */
export function Graph({ nodes, edges, direction = 'TB', ariaLabel }: {
	nodes: GraphNode[];
	edges: GraphEdge[];
	direction?: 'LR' | 'TB';
	ariaLabel: string;
}) {
	const layout = useMemo(() => {
		const g = new dagre.graphlib.Graph();
		g.setGraph({ rankdir: direction, nodesep: 20, ranksep: 48, marginx: 12, marginy: 12 });
		g.setDefaultEdgeLabel(() => ({}));
		for (const node of nodes) {
			g.setNode(node.id, { width: node.width, height: node.height });
		}
		for (const edge of edges) {
			if (g.hasNode(edge.from) && g.hasNode(edge.to)) {
				g.setEdge(edge.from, edge.to);
			}
		}
		dagre.layout(g);
		const graph = g.graph();
		return {
			width: graph.width ?? 0,
			height: graph.height ?? 0,
			position: (id: string) => g.node(id),
			points: (from: string, to: string) => g.edge({ v: from, w: to })?.points ?? [],
		};
	}, [nodes, edges, direction]);

	return (
		<div class="lab-graph" role="figure" aria-label={ariaLabel}>
			<svg width={layout.width} height={layout.height} viewBox={`0 0 ${layout.width} ${layout.height}`}>
				<defs>
					<marker id="lab-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
						<path d="M 0 0 L 10 5 L 0 10 z" class="lab-arrow" />
					</marker>
				</defs>
				{edges.map((edge) => {
					const points = layout.points(edge.from, edge.to);
					if (points.length === 0) {
						return null;
					}
					const d = points.map((p: { x: number; y: number }, i: number) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');
					const mid = points[Math.floor(points.length / 2)];
					return (
						<g key={`${edge.from}>${edge.to}`} class={edge.dimmed ? 'lab-dim' : undefined}>
							<path d={d} class={`lab-edge${edge.dashed ? ' lab-edge-dashed' : ''}`} marker-end="url(#lab-arrow)" />
							{edge.label && (
								<text x={mid.x} y={mid.y - 4} class="lab-edge-label" text-anchor="middle">
									{edge.label}
								</text>
							)}
						</g>
					);
				})}
				{nodes.map((node) => {
					const p = layout.position(node.id);
					return (
						<foreignObject key={node.id} x={p.x - node.width / 2} y={p.y - node.height / 2} width={node.width} height={node.height}>
							<button
								type="button"
								class={`lab-node lab-node-${node.tone ?? 'normal'}${node.selected ? ' lab-node-selected' : ''}${node.className ? ` ${node.className}` : ''}`}
								onClick={node.onSelect}
							>
								{node.body}
							</button>
						</foreignObject>
					);
				})}
			</svg>
		</div>
	);
}
