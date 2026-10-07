import { useState } from 'preact/hooks';
import { ChainView, DEFAULT_GOAL, type Goal, goalRate } from './ChainView';
import { DraftPanel } from './DraftPanel';
import { EditPanel } from './EditPanel';
import { ProposalBar } from './ProposalBar';
import { useLab } from './store';

/** The Balance lab chain page: proposal bar, chain diagram, and the edit and draft panels. */
export default function ChainLab() {
	const lab = useLab();
	const [goal, setGoal] = useState<Goal>(DEFAULT_GOAL);
	const [selected, setSelected] = useState<string | null>(null);
	const [tab, setTab] = useState<'edit' | 'draft'>('edit');
	const { draft, applied, notice, setNotice } = lab;
	const changes = draft.edits.length + draft.items.length + draft.recipes.length + draft.machines.length;

	return (
		<div class="lab not-content">
			{notice && (
				<p class="lab-notice" role="status">
					{notice} <button type="button" onClick={() => setNotice(null)}>Dismiss</button>
				</p>
			)}
			{applied.missing.length > 0 && (
				<p class="lab-notice" role="status">
					{applied.missing.length} edit(s) point at values that no longer exist and are ignored.
				</p>
			)}
			<ProposalBar lab={lab} target={goalRate(goal)} />
			<ChainView lab={lab} goal={goal} setGoal={setGoal} selected={selected} onSelect={setSelected} />
			<div class="lab-tabs" role="tablist">
				<button type="button" role="tab" aria-selected={tab === 'edit'} onClick={() => setTab('edit')}>
					Edit numbers{draft.edits.length > 0 && ` (${draft.edits.length})`}
				</button>
				<button type="button" role="tab" aria-selected={tab === 'draft'} onClick={() => setTab('draft')}>
					Draft content{changes - draft.edits.length > 0 && ` (${changes - draft.edits.length})`}
				</button>
			</div>
			<div role="tabpanel">{tab === 'edit' ? <EditPanel lab={lab} /> : <DraftPanel lab={lab} />}</div>
		</div>
	);
}
