import { useEffect, useMemo, useState } from 'preact/hooks';
import { gameData } from '../../lib/committed';
import { stackKey, type Stack } from '../../lib/gamedata';
import {
	ancestors,
	buildRecipeGraph,
	edgeId,
	type GraphRecipe,
	itemNode,
	MODS,
	modOf,
	rawCost,
	recipeNode,
	type ServusMod,
} from '../../lib/recipegraph';
import { name, num } from './format';
import { Graph, type GraphEdge, type GraphNode } from './Graph';
import { ItemIcon } from './icons';

const MOD_NAMES: Record<string, string> = {
	foundation: 'Foundation',
	alchemy: 'Alchemy',
	shuttles: 'Shuttles',
	vanilla: 'Minecraft',
};

/** An item's display name, or a tag as `#` plus its path. */
function label(key: string): string {
	return key.startsWith('#') ? `#${key.slice(key.indexOf(':') + 1)}` : name(gameData, key);
}

function stackText(stack: Stack): string {
	return `${stack.count} ${label(stackKey(stack))}`;
}

/** Every Servus recipe as one layered graph, with the selected item's ancestors highlighted and its raw cost. */
export default function RecipeGraph() {
	const [visible, setVisible] = useState<ServusMod[]>([...MODS]);
	const [selected, setSelected] = useState<string | null>(null);
	const [routes, setRoutes] = useState<Record<string, string>>({});
	const graph = useMemo(() => buildRecipeGraph(gameData, visible), [visible]);
	const current = selected && graph.items.includes(selected) ? selected : null;
	const highlight = useMemo(() => (current ? ancestors(graph, current) : null), [graph, current]);
	const focus = useMemo(() => (highlight ? [...highlight.nodes].sort() : undefined), [highlight]);

	useEffect(() => {
		const onKey = (event: KeyboardEvent) => {
			if (event.key === 'Escape') {
				setSelected(null);
			}
		};
		window.addEventListener('keydown', onKey);
		return () => window.removeEventListener('keydown', onKey);
	}, []);

	const select = (key: string) => setSelected((old) => (old === key ? null : key));
	const selectRecipe = (recipe: GraphRecipe) => {
		if (recipe.outputs.length > 0) {
			select(stackKey(recipe.outputs[0]));
		}
	};
	const dim = (id: string) => (highlight && !highlight.nodes.has(id) ? ' lab-dim' : '');

	const nodes: GraphNode[] = [
		...graph.items.map((key) => ({
			id: itemNode(key),
			width: 170,
			height: 34,
			selected: key === current,
			className: `lab-mod-${modOf(key)}${dim(itemNode(key))}`,
			onSelect: () => select(key),
			body: (
				<span class="lab-item-body" title={key}>
					<ItemIcon item={key} />
					<strong>{label(key)}</strong>
				</span>
			),
		})),
		...[...graph.recipes.values()].map((recipe) => ({
			id: recipeNode(recipe.id),
			width: recipe.special ? 150 : 96,
			height: recipe.special ? 40 : 26,
			className: `lab-recipe-node lab-mod-${recipe.mod}${dim(recipeNode(recipe.id))}`,
			onSelect: () => selectRecipe(recipe),
			body: recipe.special ? (
				<>
					<strong>{name(gameData, recipe.id)}</strong>
					<span>special</span>
				</>
			) : (
				<span title={recipe.id}>{recipe.kind}</span>
			),
		})),
	];
	const edges: GraphEdge[] = graph.edges.map((edge) => ({
		from: edge.from,
		to: edge.to,
		label: edge.count > 1 ? String(edge.count) : undefined,
		dimmed: highlight ? !highlight.edges.has(edgeId(edge)) : false,
	}));

	const toggle = (mod: ServusMod) =>
		setVisible((old) => (old.includes(mod) ? old.filter((m) => m !== mod) : MODS.filter((m) => m === mod || old.includes(m))));

	return (
		<div class="lab not-content">
			<div class="lab-controls lab-mod-filter" role="group" aria-label="Mods">
				{MODS.map((mod) => (
					<label key={mod} class={`lab-mod-${mod}`}>
						<input type="checkbox" checked={visible.includes(mod)} onChange={() => toggle(mod)} /> {MOD_NAMES[mod]}
					</label>
				))}
				<span class="lab-hint">Click an item to see what it is made from. Drag to pan, wheel or pinch to zoom, Escape clears.</span>
			</div>
			<div class={`lab-recipes${current ? ' lab-recipes-open' : ''}`}>
				<Graph nodes={nodes} edges={edges} direction="LR" ariaLabel="Recipes of the Servus mods" zoomable focus={focus} />
				{current && <ItemPanel item={current} graph={graph} routes={routes} setRoutes={setRoutes} onSelect={select} />}
			</div>
		</div>
	);
}

function ItemPanel({ item, graph, routes, setRoutes, onSelect }: {
	item: string;
	graph: ReturnType<typeof buildRecipeGraph>;
	routes: Record<string, string>;
	setRoutes: (routes: Record<string, string>) => void;
	onSelect: (key: string) => void;
}) {
	const made = graph.producers.get(item) ?? [];
	const used = graph.consumers.get(item) ?? [];
	const cost = rawCost(graph, item, routes);
	const recipe = (id: string) => graph.recipes.get(id)!;

	return (
		<aside class="lab-details lab-item-panel" aria-label={`${label(item)} details`}>
			<h3>
				<ItemIcon item={item} size={32} /> {label(item)}
			</h3>
			<p class="lab-hint">
				<span class={`lab-mod-tag lab-mod-${modOf(item)}`}>{MOD_NAMES[modOf(item)]}</span> <code>{item}</code>
			</p>

			<h4>Made from</h4>
			{made.length === 0 ? (
				<p class="lab-hint">No Servus recipe makes this.</p>
			) : (
				made.map((id) => <RecipeCard key={id} recipe={recipe(id)} item={item} />)
			)}

			<h4>Used in</h4>
			{used.length === 0 ? (
				<p class="lab-hint">Nothing.</p>
			) : (
				<ul class="lab-used-in">
					{used.map((id) => {
						const outputs = recipe(id).outputs;
						return (
							<li key={id}>
								{outputs.length > 0 ? (
									<button type="button" class="lab-link" onClick={() => onSelect(stackKey(outputs[0]))}>
										<ItemIcon item={stackKey(outputs[0])} size={16} /> {label(stackKey(outputs[0]))}
									</button>
								) : (
									name(gameData, id)
								)}{' '}
								<span class="lab-hint">{recipe(id).kind}</span>
							</li>
						);
					})}
				</ul>
			)}

			{made.length > 0 && (
				<>
					<h4>Raw cost per item</h4>
					{[...cost.routes].map(([key, route]) => (
						<label key={key} class="lab-route">
							{label(key)} by{' '}
							<select value={route.chosen} onChange={(e) => setRoutes({ ...routes, [key]: e.currentTarget.value })}>
								{route.options.map((id) => (
									<option key={id} value={id}>
										{recipe(id).kind}: {id}
									</option>
								))}
							</select>
						</label>
					))}
					<table class="lab-cost">
						<tbody>
							{[...cost.amounts].map(([key, amount]) => (
								<tr key={key}>
									<td>{num(amount, 3)}</td>
									<td>
										<ItemIcon item={key} size={16} /> {label(key)}
										{cost.noRecipe.has(key) && <span class="lab-flag"> no recipe</span>}
									</td>
								</tr>
							))}
						</tbody>
					</table>
					{cost.cycles.size > 0 && (
						<p class="lab-flag">Cycle at {[...cost.cycles].map(label).join(', ')}: stopped there.</p>
					)}
					<p class="lab-hint">Expanded through Servus items, ending at vanilla items and tags.</p>
				</>
			)}
		</aside>
	);
}

function RecipeCard({ recipe, item }: { recipe: GraphRecipe; item: string }) {
	const output = recipe.outputs.find((stack) => stackKey(stack) === item);
	return (
		<div class="lab-recipe-card">
			<div>
				<strong>{recipe.kind}</strong> <span class="lab-hint">{recipe.id}</span>
			</div>
			{recipe.pattern && recipe.key ? (
				<div class="lab-grid" style={{ gridTemplateColumns: `repeat(${Math.max(...recipe.pattern.map((row) => row.length))}, 1fr)` }}>
					{recipe.pattern.flatMap((row, r) =>
						[...row.padEnd(Math.max(...recipe.pattern!.map((x) => x.length)))].map((symbol, c) => {
							const entry = recipe.key![symbol];
							const key = entry ? ('item' in entry ? entry.item : `#${entry.tag}`) : null;
							return (
								<span key={`${r}-${c}`} class={`lab-grid-cell${key ? ` lab-mod-${modOf(key)}` : ''}`} title={key ?? ''}>
									{key && <ItemIcon item={key} size={20} />}
									{key ? label(key) : ''}
								</span>
							);
						}),
					)}
				</div>
			) : (
				<div>{recipe.inputs.map(stackText).join(', ')}</div>
			)}
			{output && <div>makes {stackText(output)}</div>}
		</div>
	);
}
