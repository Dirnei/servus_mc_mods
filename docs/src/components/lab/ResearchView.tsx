import { useState } from 'preact/hooks';
import { gameData } from '../../lib/committed';
import { stackKey } from '../../lib/gamedata';
import { researchTime, techOrder } from '../../lib/research';
import { name, num, rate, seconds } from './format';
import { Graph, type GraphNode } from './Graph';
import { useLab } from './store';

/** Tech graph with costs and unlocks, and time to research for a number of labs and a pack supply rate. */
export default function ResearchView() {
	const lab = useLab();
	const data = lab.applied.data;
	const [labs, setLabs] = useState(1);
	const [packsPerMinute, setPacksPerMinute] = useState(12);
	const order = techOrder(data);
	const [selected, setSelected] = useState<string | null>(order[0] ?? null);
	const packs = [...new Set(order.flatMap((id) => data.techs[id].cost.map(stackKey)))];
	const supply = Object.fromEntries(packs.map((pack) => [pack, packsPerMinute / 60]));

	const techName = (id: string) => name(data, id);
	const nodes: GraphNode[] = order.map((id) => {
		const tech = data.techs[id];
		const base = gameData.techs[id];
		const time = researchTime(data, id, labs, supply);
		const changed = JSON.stringify(tech) !== JSON.stringify(base);
		return {
			id,
			width: 200,
			height: 96,
			tone: changed ? 'draft' : 'normal',
			selected: id === selected,
			onSelect: () => setSelected(id),
			body: (
				<>
					<strong>{techName(id)}</strong>
					<span>{tech.cost.map((s) => `${s.count} ${name(data, stackKey(s))}`).join(', ')}</span>
					<span>{num(tech.ticks_per_pack / 20)} s per pack</span>
					<span>{seconds(time.seconds)} to research</span>
				</>
			),
		};
	});
	const edges = order.flatMap((id) => data.techs[id].parents.map((parent) => ({ from: parent, to: id })));
	const tech = selected ? data.techs[selected] : null;
	const time = selected ? researchTime(data, selected, labs, supply) : null;

	return (
		<div class="lab not-content">
			<div class="lab-controls lab-research-controls">
				<label>
					Labs <input type="number" min="1" value={labs} onInput={(e) => setLabs(Math.max(1, Number(e.currentTarget.value) || 1))} />
				</label>
				<label>
					Packs supplied per minute (each pack type){' '}
					<input type="number" min="0" step="any" value={packsPerMinute} onInput={(e) => setPacksPerMinute(Math.max(0, Number(e.currentTarget.value) || 0))} />
				</label>
			</div>
			<Graph nodes={nodes} edges={edges} ariaLabel="Tech tree" />
			{selected && tech && time && (
				<div class="lab-details">
					<h3>{techName(selected)}</h3>
					<ul>
						<li>Needs: {tech.parents.length > 0 ? tech.parents.map(techName).join(', ') : 'nothing, available from the start'}</li>
						<li>Costs: {tech.cost.map((s) => `${s.count} ${name(data, stackKey(s))}`).join(', ')}, {num(tech.ticks_per_pack / 20)} s per pack in a lab</li>
						<li>Unlocks: {tech.unlocks.length > 0 ? tech.unlocks.map((u) => <code key={u}>{u}</code>) : 'nothing'}</li>
						<li>
							{labs} lab(s) need {seconds(time.labSeconds)}; packs at {rate(packsPerMinute / 60)} arrive in {seconds(time.supplySeconds)}.
							Research takes <strong>{seconds(time.seconds)}</strong>, limited by {time.limitedBy === 'labs' ? 'the labs' : 'pack supply'}.
						</li>
					</ul>
					<p class="lab-hint">Tech costs and lab times can be edited on the Production chains page; the edits apply here too.</p>
				</div>
			)}
		</div>
	);
}
