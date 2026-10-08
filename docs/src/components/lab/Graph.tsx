import dagre from '@dagrejs/dagre';
import type { ComponentChildren } from 'preact';
import { useEffect, useMemo, useRef, useState } from 'preact/hooks';

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

interface View {
	x: number;
	y: number;
	k: number;
}

const MIN_ZOOM = 0.1;
const MAX_ZOOM = 3;
/** Pointer travel in pixels after which a press is a drag, not a click. */
const DRAG_SLOP = 4;

const clampZoom = (k: number) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, k));

/**
 * Lays out nodes with dagre (top to bottom by default: raw resources flow down into products) and draws them as SVG.
 * With `zoomable` the graph sits in a fixed box with wheel and pinch zoom, drag to pan and +, - and Fit buttons; it
 * starts fitted and fits the `focus` nodes whenever they change.
 */
export function Graph({ nodes, edges, direction = 'TB', ariaLabel, zoomable = false, focus }: {
	nodes: GraphNode[];
	edges: GraphEdge[];
	direction?: 'LR' | 'TB';
	ariaLabel: string;
	zoomable?: boolean;
	focus?: string[];
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

	const box = useRef<HTMLDivElement>(null);
	const [view, setView] = useState<View>({ x: 0, y: 0, k: 1 });
	const pointers = useRef(new Map<number, { x: number; y: number }>());
	const drag = useRef({ moved: 0, pinch: 0 });

	/** The view that fits the given nodes (all nodes when empty) into the box. */
	const fit = (ids: string[] = []) => {
		const el = box.current;
		if (!el) {
			return;
		}
		const sizes = new Map(nodes.map((node) => [node.id, node]));
		let minX = 0;
		let minY = 0;
		let maxX = layout.width;
		let maxY = layout.height;
		const known = ids.filter((id) => sizes.has(id));
		if (known.length > 0) {
			minX = Infinity;
			minY = Infinity;
			maxX = -Infinity;
			maxY = -Infinity;
			for (const id of known) {
				const p = layout.position(id);
				const node = sizes.get(id)!;
				minX = Math.min(minX, p.x - node.width / 2);
				maxX = Math.max(maxX, p.x + node.width / 2);
				minY = Math.min(minY, p.y - node.height / 2);
				maxY = Math.max(maxY, p.y + node.height / 2);
			}
		}
		const pad = 24;
		const w = el.clientWidth;
		const h = el.clientHeight;
		const k = clampZoom(Math.min((w - 2 * pad) / Math.max(1, maxX - minX), (h - 2 * pad) / Math.max(1, maxY - minY), 1.5));
		setView({ k, x: (w - (maxX - minX) * k) / 2 - minX * k, y: (h - (maxY - minY) * k) / 2 - minY * k });
	};

	const focusKey = focus?.join('|') ?? '';
	useEffect(() => {
		if (zoomable) {
			fit(focus);
		}
	}, [zoomable, layout, focusKey]);

	/** Zoom by `factor` around the box point (px, py). */
	const zoomAt = (factor: number, px: number, py: number) =>
		setView((v) => {
			const k = clampZoom(v.k * factor);
			return { k, x: px - ((px - v.x) * k) / v.k, y: py - ((py - v.y) * k) / v.k };
		});

	useEffect(() => {
		const el = box.current;
		if (!zoomable || !el) {
			return;
		}
		const onWheel = (event: WheelEvent) => {
			event.preventDefault();
			const rect = el.getBoundingClientRect();
			zoomAt(Math.exp(-event.deltaY * 0.0015), event.clientX - rect.left, event.clientY - rect.top);
		};
		el.addEventListener('wheel', onWheel, { passive: false });
		return () => el.removeEventListener('wheel', onWheel);
	}, [zoomable]);

	const local = (event: PointerEvent) => {
		const rect = box.current!.getBoundingClientRect();
		return { x: event.clientX - rect.left, y: event.clientY - rect.top };
	};
	const spread = () => {
		const [a, b] = [...pointers.current.values()];
		return { d: Math.hypot(a.x - b.x, a.y - b.y), x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 };
	};
	const onPointerDown = (event: PointerEvent) => {
		pointers.current.set(event.pointerId, local(event));
		if (pointers.current.size === 1) {
			drag.current.moved = 0;
		} else if (pointers.current.size === 2) {
			drag.current.pinch = spread().d;
		}
	};
	const onPointerMove = (event: PointerEvent) => {
		const last = pointers.current.get(event.pointerId);
		if (!last) {
			return;
		}
		if (event.pointerType === 'mouse' && event.buttons === 0) {
			// Released outside the box: forget the press instead of panning on hover.
			pointers.current.delete(event.pointerId);
			return;
		}
		const now = local(event);
		if (pointers.current.size === 2) {
			pointers.current.set(event.pointerId, now);
			const s = spread();
			if (drag.current.pinch > 0) {
				zoomAt(s.d / drag.current.pinch, s.x, s.y);
			}
			drag.current.pinch = s.d;
			drag.current.moved = DRAG_SLOP + 1;
			return;
		}
		const dx = now.x - last.x;
		const dy = now.y - last.y;
		drag.current.moved += Math.abs(dx) + Math.abs(dy);
		pointers.current.set(event.pointerId, now);
		if (drag.current.moved > DRAG_SLOP) {
			box.current!.setPointerCapture(event.pointerId);
			setView((v) => ({ ...v, x: v.x + dx, y: v.y + dy }));
		}
	};
	const onPointerUp = (event: PointerEvent) => {
		pointers.current.delete(event.pointerId);
		if (pointers.current.size < 2) {
			drag.current.pinch = 0;
		}
	};
	/** A drag ends with a click on whatever is under the pointer; swallow it so panning never selects. */
	const onClickCapture = (event: MouseEvent) => {
		if (drag.current.moved > DRAG_SLOP) {
			event.stopPropagation();
			event.preventDefault();
			drag.current.moved = 0;
		}
	};

	/** Zoom around the middle of the box. */
	const zoomButton = (factor: number) => {
		const el = box.current;
		if (el) {
			zoomAt(factor, el.clientWidth / 2, el.clientHeight / 2);
		}
	};

	const content = (
		<>
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
		</>
	);

	if (!zoomable) {
		return (
			<div class="lab-graph" role="figure" aria-label={ariaLabel}>
				<svg width={layout.width} height={layout.height} viewBox={`0 0 ${layout.width} ${layout.height}`}>
					{content}
				</svg>
			</div>
		);
	}
	return (
		<div
			class="lab-graph lab-graph-zoom"
			role="figure"
			aria-label={ariaLabel}
			ref={box}
			onPointerDown={onPointerDown}
			onPointerMove={onPointerMove}
			onPointerUp={onPointerUp}
			onPointerCancel={onPointerUp}
			onClickCapture={onClickCapture}
		>
			<svg width="100%" height="100%">
				<g transform={`translate(${view.x} ${view.y}) scale(${view.k})`}>{content}</g>
			</svg>
			<div class="lab-zoom-buttons">
				<button type="button" aria-label="Zoom in" onClick={() => zoomButton(1.25)}>+</button>
				<button type="button" aria-label="Zoom out" onClick={() => zoomButton(0.8)}>-</button>
				<button type="button" onClick={() => fit()}>Fit</button>
			</div>
		</div>
	);
}
