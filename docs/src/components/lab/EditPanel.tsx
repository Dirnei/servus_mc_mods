import { useState } from 'preact/hooks';
import { gameData } from '../../lib/committed';
import { editFor, getNumber, type Path, withEdit } from '../../lib/draft';
import { type GameData, stackKey } from '../../lib/gamedata';
import { name } from './format';
import type { LabState } from './store';

interface Field {
	path: Path;
	label: string;
}

interface Entry {
	id: string;
	title: string;
	fields: Field[];
}

const MACHINE_FIELDS = ['work_ticks', 'energy_per_tick', 'energy_capacity', 'fuel_divisor', 'recipe_slots'] as const;

/** Every editable number of the committed data, grouped like the datapack files. */
export function editableGroups(data: GameData): { label: string; entries: Entry[] }[] {
	const sorted = <T,>(record: Record<string, T>) => Object.entries(record).sort(([a], [b]) => a.localeCompare(b));
	return [
		{
			label: 'Machines',
			entries: sorted(data.machines).map(([id]) => ({
				id,
				title: name(data, id),
				fields: MACHINE_FIELDS.map((field) => ({ path: ['machines', id, field], label: field })),
			})),
		},
		{
			label: 'Recipes',
			entries: sorted(data.recipes).map(([id, recipe]) => ({
				id,
				title: `${id.slice(id.indexOf(':') + 1)}`,
				fields: [
					{ path: ['recipes', id, 'time'], label: 'time (ticks)' },
					...recipe.inputs.map((stack, i) => ({ path: ['recipes', id, 'inputs', i, 'count'], label: `in: ${name(data, stackKey(stack))}` })),
					...recipe.outputs.map((stack, i) => ({ path: ['recipes', id, 'outputs', i, 'count'], label: `out: ${name(data, stackKey(stack))}` })),
				],
			})),
		},
		{
			label: 'Deposits',
			entries: sorted(data.deposits).map(([id, deposit]) => ({
				id,
				title: name(data, id),
				fields: [{ path: ['deposits', id, 'yield', 'count'], label: `yield: ${name(data, stackKey(deposit.yield))}` }],
			})),
		},
		{
			label: 'Techs',
			entries: sorted(data.techs).map(([id, tech]) => ({
				id,
				title: name(data, id),
				fields: [
					{ path: ['techs', id, 'ticks_per_pack'], label: 'ticks_per_pack' },
					...tech.cost.map((stack, i) => ({ path: ['techs', id, 'cost', i, 'count'], label: `cost: ${name(data, stackKey(stack))}` })),
				],
			})),
		},
		{
			label: 'Fuels',
			entries: sorted(data.fuels).map(([id]) => ({
				id,
				title: name(data, id),
				fields: [{ path: ['fuels', id], label: 'burn time (ticks)' }],
			})),
		},
	];
}

export function EditPanel({ lab }: { lab: LabState }) {
	const { draft, setDraft } = lab;
	const [filter, setFilter] = useState('');
	const needle = filter.trim().toLowerCase();
	const groups = editableGroups(gameData);

	return (
		<div class="lab-edit">
			<div class="lab-controls">
				<label>
					Filter <input type="search" value={filter} placeholder="red_pack, assembler" onInput={(e) => setFilter(e.currentTarget.value)} />
				</label>
				<button type="button" disabled={draft.edits.length === 0} onClick={() => setDraft({ ...draft, edits: [] })}>
					Reset all numbers ({draft.edits.length})
				</button>
			</div>
			{groups.map((group) => {
				const entries = group.entries.filter(
					(entry) => !needle || entry.id.toLowerCase().includes(needle) || entry.title.toLowerCase().includes(needle),
				);
				const edited = entries.filter((entry) => entry.fields.some((field) => editFor(draft, field.path))).length;
				return (
					<details key={group.label} open={needle !== '' || edited > 0}>
						<summary>
							{group.label} ({entries.length}){edited > 0 && <span class="lab-badge">{edited} edited</span>}
						</summary>
						{entries.map((entry) => (
							<fieldset key={entry.id} class="lab-entry">
								<legend title={entry.id}>{entry.title}</legend>
								{entry.fields.map((field) => {
									const baseline = getNumber(gameData, field.path);
									const edit = editFor(draft, field.path);
									return (
										<label key={JSON.stringify(field.path)} class={edit ? 'lab-field lab-changed' : 'lab-field'}>
											<span>{field.label}</span>
											<input
												type="number"
												step="1"
												min="0"
												value={edit?.value ?? baseline}
												onChange={(e) => {
													const value = Number(e.currentTarget.value);
													if (Number.isFinite(value)) {
														setDraft(withEdit(draft, gameData, field.path, value));
													}
												}}
											/>
											{edit && (
												<button
													type="button"
													class="lab-reset"
													title={`Back to ${baseline}`}
													onClick={() => setDraft(withEdit(draft, gameData, field.path, baseline ?? edit.value))}
												>
													was {baseline}
												</button>
											)}
										</label>
									);
								})}
							</fieldset>
						))}
					</details>
				);
			})}
		</div>
	);
}
