import { useMemo } from 'preact/hooks';
import { gameData } from '../../lib/committed';
import type { GameData } from '../../lib/gamedata';
import { type Chain, type ChainNode, computeChain, routesFor } from '../../lib/ratios';
import { name, num, percent, rate } from './format';
import { Graph, type GraphEdge, type GraphNode } from './Graph';
import type { LabState } from './store';

const DEFAULT_TARGET = 'servus_manufacturing:red_pack';
const CONSUMER_ID = '__consumer__';

/** Items with at least one way to make them, products first. */
function products(data: GameData, flags: LabState['applied']['flags']): string[] {
	const items = new Set<string>();
	for (const recipe of Object.values(data.recipes)) {
		for (const output of recipe.outputs) {
			if ('item' in output && routesFor(data, output.item, flags).length > 0) {
				items.add(output.item);
			}
		}
	}
	for (const deposit of Object.values(data.deposits)) {
		if ('item' in deposit.yield) {
			items.add(deposit.yield.item);
		}
	}
	return [...items].sort((a, b) => name(data, a).localeCompare(name(data, b)));
}

function routeLabel(data: GameData, key: string): string {
	const [source, machine] = key.slice(key.indexOf(':') + 1).split('@');
	const kind = key.startsWith('deposit:') ? 'Deposit' : 'Recipe';
	return `${kind} ${source.slice(source.indexOf(':') + 1)} in ${name(data, machine)}`;
}

function delta(now: number, before: number | undefined, unit = ''): string | null {
	if (before === undefined || Math.abs(now - before) < 1e-9) {
		return null;
	}
	return `${num(before)}${unit} to ${num(now)}${unit}`;
}

export interface Goal {
	item: string;
	amount: number;
	unit: 'min' | 's';
}

export const DEFAULT_GOAL: Goal = { item: DEFAULT_TARGET, amount: 12, unit: 'min' };

export function goalRate(goal: Goal): { item: string; perSecond: number } {
	return { item: goal.item, perSecond: goal.unit === 'min' ? goal.amount / 60 : goal.amount };
}

export function ChainView({ lab, goal: wanted, setGoal, selected, onSelect }: {
	lab: LabState;
	goal: Goal;
	setGoal: (goal: Goal) => void;
	selected: string | null;
	onSelect: (item: string | null) => void;
}) {
	const { draft, setDraft, applied } = lab;
	const data = applied.data;
	const item = routesFor(data, wanted.item, applied.flags).length > 0 ? wanted.item : DEFAULT_TARGET;
	const { perSecond } = goalRate(wanted);
	const goal = { item, perSecond };

	const chain: Chain = useMemo(
		() => computeChain(data, goal, { routes: draft.routes, flags: applied.flags }),
		[data, goal.item, perSecond, draft.routes, applied.flags],
	);
	const baseline: Chain = useMemo(
		() => computeChain(gameData, goal, { routes: draft.routes }),
		[goal.item, perSecond, draft.routes],
	);
	const baseNode = (item: string) => baseline.nodes.find((n) => n.item === item);

	const graphNodes: GraphNode[] = chain.nodes.map((node) => nodeView(data, node, baseNode(node.item), node.item === selected, () => onSelect(node.item)));
	const graphEdges: GraphEdge[] = chain.nodes.flatMap((node) =>
		node.inputs.map((input) => ({ from: input.item, to: node.item, label: rate(input.rate), dashed: node.loop })),
	);
	if (chain.consumer) {
		const c = chain.consumer;
		const before = baseline.consumer;
		graphNodes.push({
			id: CONSUMER_ID,
			width: 190,
			height: 92,
			tone: 'muted',
			body: (
				<>
					<strong>{c.count} x {name(data, c.machine)}</strong>
					<span>researching {name(data, c.tech)}</span>
					<span>{percent(c.utilization)} busy, {num(c.fePerTick)} FE/t</span>
					{before && delta(c.count, before.count) && <em class="lab-delta">{delta(c.count, before.count)} labs</em>}
				</>
			),
		});
		graphEdges.push({ from: c.item, to: CONSUMER_ID, label: rate(c.rate) });
	}

	const t = chain.totals;
	const bt = baseline.totals;
	const machineIds = [...new Set([...Object.keys(t.machines), ...Object.keys(bt.machines)])].sort();
	const options = products(data, applied.flags);

	return (
		<section class="lab-chain">
			<div class="lab-controls">
				<label>
					Product{' '}
					<select value={goal.item} onChange={(e) => { setGoal({ ...wanted, item: e.currentTarget.value }); onSelect(null); }}>
						{options.map((id) => (
							<option key={id} value={id}>{name(data, id)}{applied.flags.items?.has(id) ? ' (draft)' : ''}</option>
						))}
					</select>
				</label>
				<label>
					Target{' '}
					<input
						type="number"
						min="0"
						step="any"
						value={wanted.amount}
						onInput={(e) => setGoal({ ...wanted, amount: Math.max(0, Number(e.currentTarget.value) || 0) })}
					/>
				</label>
				<select value={wanted.unit} onChange={(e) => setGoal({ ...wanted, unit: e.currentTarget.value as 'min' | 's' })} aria-label="Rate unit">
					<option value="min">per minute</option>
					<option value="s">per second</option>
				</select>
			</div>

			<Graph nodes={graphNodes} edges={graphEdges} ariaLabel={`Production chain for ${name(data, goal.item)}`} />

			{selected && chain.nodes.some((n) => n.item === selected) && (
				<NodeDetails
					data={data}
					node={chain.nodes.find((n) => n.item === selected)!}
					onRoute={(key) => {
						const routes = { ...draft.routes };
						const alternatives = routesFor(data, selected, applied.flags);
						if (key === alternatives[0]?.key) {
							delete routes[selected];
						} else {
							routes[selected] = key;
						}
						setDraft({ ...draft, routes });
					}}
				/>
			)}

			<table class="lab-totals">
				<caption>Totals for {rate(perSecond)} {name(data, goal.item)}</caption>
				<thead>
					<tr><th scope="col">Machine</th><th scope="col">Count</th><th scope="col">Before edits</th></tr>
				</thead>
				<tbody>
					{machineIds.map((id) => (
						<tr key={id} class={(t.machines[id] ?? 0) !== (bt.machines[id] ?? 0) ? 'lab-changed' : ''}>
							<td>{name(data, id)}{applied.flags.machines?.has(id) && <span class="lab-badge">needs code</span>}</td>
							<td>{t.machines[id] ?? 0}</td>
							<td>{bt.machines[id] ?? 0}</td>
						</tr>
					))}
					<tr><td>Power draw</td><td>{num(t.fePerTick)} FE/t</td><td>{num(bt.fePerTick)} FE/t</td></tr>
					<tr><td>{name(data, t.fuelItem)} for burners</td><td>{rate(t.burnerFuelPerSecond)}</td><td>{rate(bt.burnerFuelPerSecond)}</td></tr>
					<tr><td>{name(data, t.fuelItem)} for generators</td><td>{rate(t.generatorFuelPerSecond)}</td><td>{rate(bt.generatorFuelPerSecond)}</td></tr>
					<tr class={Math.abs(t.fuelPerSecond - bt.fuelPerSecond) > 1e-9 ? 'lab-changed' : ''}>
						<td><strong>{name(data, t.fuelItem)} total</strong></td>
						<td><strong>{rate(t.fuelPerSecond)}</strong></td>
						<td>{rate(bt.fuelPerSecond)}</td>
					</tr>
				</tbody>
			</table>
		</section>
	);
}

/** What changed against the baseline, most important first: machine count, then power, then fuel. */
function nodeChange(node: ChainNode, before: ChainNode): string | null {
	const count = delta(node.count, before.count);
	if (count) return `${count} machines`;
	const fe = delta(node.fePerTick, before.fePerTick, ' FE/t');
	if (fe) return fe;
	if (Math.abs(node.fuelPerSecond - before.fuelPerSecond) > 1e-9) return `fuel ${rate(before.fuelPerSecond)} to ${rate(node.fuelPerSecond)}`;
	return null;
}

function nodeView(data: GameData, node: ChainNode, before: ChainNode | undefined, selected: boolean, onSelect: () => void): GraphNode {
	const machine = node.route ? name(data, node.route.machine) : null;
	const change = before ? nodeChange(node, before) : null;
	const tone = node.noSource || node.loop ? 'warning' : node.draft || node.route?.needsCode ? 'draft' : 'normal';
	return {
		id: node.item,
		width: 200,
		height: 122,
		tone,
		selected,
		onSelect,
		body: (
			<>
				<strong>{name(data, node.item)}</strong>
				{node.noSource ? (
					<span class="lab-flag">no source</span>
				) : (
					<span>
						{node.count} x {machine}
						{node.route?.needsCode && <span class="lab-badge">needs code</span>}
					</span>
				)}
				<span>
					{rate(node.rate)}
					{!node.noSource && `, ${percent(node.utilization)} busy`}
				</span>
				{!node.noSource && (
					<span>{node.fuelPerSecond > 0 ? `${rate(node.fuelPerSecond)} fuel` : `${num(node.fePerTick)} FE/t`}</span>
				)}
				{node.loop && <span class="lab-flag">loop</span>}
				{change && <em class="lab-delta">{change}</em>}
			</>
		),
	};
}

function NodeDetails({ data, node, onRoute }: { data: GameData; node: ChainNode; onRoute: (key: string) => void }) {
	return (
		<div class="lab-details">
			<h3>{name(data, node.item)}</h3>
			{node.noSource ? (
				<p>Nothing makes this item: no recipe or deposit produces it. Draft a recipe for it in the panel below.</p>
			) : (
				<>
					<label>
						Route{' '}
						<select value={node.route?.key} onChange={(e) => onRoute(e.currentTarget.value)}>
							{node.alternatives.map((route) => (
								<option key={route.key} value={route.key}>
									{routeLabel(data, route.key)}
									{route.needsCode ? ' (needs code)' : route.draft ? ' (draft)' : ''}
								</option>
							))}
						</select>
					</label>
					<ul>
						<li>One machine makes {rate(node.perMachine)}, one cycle is {num(node.route!.cycleTicks / 20)} s for {node.route!.outputCount}.</li>
						<li>Needed: {rate(node.rate)}, so {node.count} machines at {percent(node.utilization)}.</li>
						{node.inputs.map((input) => (
							<li key={input.item}>Takes {rate(input.rate)} {name(data, input.item)}</li>
						))}
						{node.loop && <li>One input loops back into this chain; its demand is not followed.</li>}
					</ul>
				</>
			)}
		</div>
	);
}
