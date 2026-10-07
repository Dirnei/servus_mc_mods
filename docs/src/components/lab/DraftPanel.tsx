import { useState } from 'preact/hooks';
import { gameData } from '../../lib/committed';
import type { Draft, DraftMachine, DraftRecipe } from '../../lib/draft';
import { type GameData, type Stack, stackKey } from '../../lib/gamedata';
import { name } from './format';
import type { LabState } from './store';

function slug(text: string): string {
	return text.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
}

function itemOptions(data: GameData): string[] {
	return Object.keys(data.items).sort((a, b) => name(data, a).localeCompare(name(data, b)));
}

function recipeTypes(data: GameData): string[] {
	const types = new Set<string>();
	for (const machine of Object.values(data.machines)) {
		for (const type of machine.recipe_types) {
			if (!type.startsWith('servus:') && type !== 'minecraft:crafting') {
				types.add(type);
			}
		}
	}
	return [...types].sort();
}

function StackRows({ data, stacks, onChange, label }: {
	data: GameData;
	stacks: Stack[];
	onChange: (stacks: Stack[]) => void;
	label: string;
}) {
	const items = itemOptions(data);
	return (
		<fieldset class="lab-stacks">
			<legend>{label}</legend>
			{stacks.map((stack, i) => (
				<div key={i} class="lab-row">
					<select
						aria-label={`${label} ${i + 1} item`}
						value={stackKey(stack)}
						onChange={(e) => onChange(stacks.map((s, j) => (j === i ? { item: e.currentTarget.value, count: s.count } : s)))}
					>
						{items.map((id) => <option key={id} value={id}>{name(data, id)}</option>)}
					</select>
					<input
						aria-label={`${label} ${i + 1} count`}
						type="number"
						min="1"
						value={stack.count}
						onInput={(e) => onChange(stacks.map((s, j) => (j === i ? { ...s, count: Math.max(1, Number(e.currentTarget.value) || 1) } : s)))}
					/>
					<button type="button" onClick={() => onChange(stacks.filter((_, j) => j !== i))}>Remove</button>
				</div>
			))}
			<button type="button" onClick={() => onChange([...stacks, { item: items[0], count: 1 }])}>Add {label.toLowerCase()}</button>
		</fieldset>
	);
}

function ItemForm({ draft, setDraft }: { draft: Draft; setDraft: (d: Draft) => void }) {
	const [itemName, setItemName] = useState('');
	const [note, setNote] = useState('');
	const id = `draft:${slug(itemName)}`;
	const taken = draft.items.some((item) => item.id === id) || Boolean(gameData.items[id]);
	return (
		<form
			class="lab-form"
			onSubmit={(e) => {
				e.preventDefault();
				if (slug(itemName) && !taken) {
					setDraft({ ...draft, items: [...draft.items, { id, name: itemName.trim(), note }] });
					setItemName('');
					setNote('');
				}
			}}
		>
			<h4>New item</h4>
			<label>Name <input required value={itemName} placeholder="Copper Coil" onInput={(e) => setItemName(e.currentTarget.value)} /></label>
			<label>Note <input value={note} placeholder="Tier 1 intermediate" onInput={(e) => setNote(e.currentTarget.value)} /></label>
			<p class="lab-hint">Id: <code>{slug(itemName) ? id : 'draft:...'}</code>{taken && ' is taken'}</p>
			<button type="submit" disabled={!slug(itemName) || taken}>Add item</button>
		</form>
	);
}

function RecipeForm({ data, draft, setDraft }: { data: GameData; draft: Draft; setDraft: (d: Draft) => void }) {
	const types = recipeTypes(data);
	const [type, setType] = useState(types[0] ?? '');
	const [inputs, setInputs] = useState<Stack[]>([]);
	const [outputs, setOutputs] = useState<Stack[]>([]);
	const [time, setTime] = useState(20);
	const [replaces, setReplaces] = useState('');
	const [note, setNote] = useState('');
	const output = outputs[0] && 'item' in outputs[0] ? outputs[0].item : '';
	const replaceable = Object.keys(gameData.recipes).filter((id) =>
		gameData.recipes[id].outputs.some((stack) => stackKey(stack) === output),
	);
	const id = replaces || `draft:${slug(type.slice(type.indexOf(':') + 1))}/${slug(output.slice(output.indexOf(':') + 1))}`;
	const valid = type && outputs.length > 0 && time > 0;

	return (
		<form
			class="lab-form"
			onSubmit={(e) => {
				e.preventDefault();
				if (!valid) return;
				let unique = id;
				for (let n = 2; !replaces && (draft.recipes.some((r) => r.id === unique) || data.recipes[unique]); n++) {
					unique = `${id}_${n}`;
				}
				const recipe: DraftRecipe = { id: unique, type, inputs, outputs, time, note };
				setDraft({ ...draft, recipes: [...draft.recipes.filter((r) => r.id !== unique), recipe] });
				setInputs([]);
				setOutputs([]);
				setReplaces('');
				setNote('');
			}}
		>
			<h4>New recipe</h4>
			<label>
				Type{' '}
				<select value={type} onChange={(e) => setType(e.currentTarget.value)}>
					{types.map((t) => <option key={t} value={t}>{t}</option>)}
				</select>
			</label>
			<StackRows data={data} stacks={inputs} onChange={setInputs} label="Inputs" />
			<StackRows data={data} stacks={outputs} onChange={setOutputs} label="Outputs" />
			<label>Time (ticks, 20 per second) <input type="number" min="1" value={time} onInput={(e) => setTime(Math.max(1, Number(e.currentTarget.value) || 1))} /></label>
			{replaceable.length > 0 && (
				<label>
					Replaces{' '}
					<select value={replaces} onChange={(e) => setReplaces(e.currentTarget.value)}>
						<option value="">nothing, an extra recipe</option>
						{replaceable.map((r) => <option key={r} value={r}>{r}</option>)}
					</select>
				</label>
			)}
			<label>Note <input value={note} onInput={(e) => setNote(e.currentTarget.value)} /></label>
			<p class="lab-hint">Assembling and crushing use this time; smelting uses the smelter's work ticks.</p>
			<button type="submit" disabled={!valid}>Add recipe</button>
		</form>
	);
}

function MachineForm({ draft, setDraft }: { draft: Draft; setDraft: (d: Draft) => void }) {
	const [machineName, setMachineName] = useState('');
	const [kind, setKind] = useState<DraftMachine['kind']>('powered');
	const [workTicks, setWorkTicks] = useState(20);
	const [energy, setEnergy] = useState(20);
	const [divisor, setDivisor] = useState(1);
	const [note, setNote] = useState('');
	const base = slug(machineName);
	const id = `draft:${base}`;
	const type = `draft:${base}`;
	const taken = draft.machines.some((m) => m.id === id);
	return (
		<form
			class="lab-form"
			onSubmit={(e) => {
				e.preventDefault();
				if (!base || taken) return;
				const machine: DraftMachine = {
					id,
					name: machineName.trim(),
					kind,
					recipe_types: [type],
					work_ticks: workTicks,
					energy_per_tick: kind === 'burner' ? 0 : energy,
					fuel_divisor: divisor,
					note,
				};
				setDraft({ ...draft, machines: [...draft.machines, machine] });
				setMachineName('');
				setNote('');
			}}
		>
			<h4>Concept machine</h4>
			<label>Name <input required value={machineName} placeholder="Wire Drawer" onInput={(e) => setMachineName(e.currentTarget.value)} /></label>
			<label>
				Kind{' '}
				<select value={kind} onChange={(e) => setKind(e.currentTarget.value as DraftMachine['kind'])}>
					<option value="powered">powered (FE)</option>
					<option value="burner">burner (fuel)</option>
					<option value="crafter">crafter (FE)</option>
				</select>
			</label>
			<label>work_ticks <input type="number" min="1" value={workTicks} onInput={(e) => setWorkTicks(Math.max(1, Number(e.currentTarget.value) || 1))} /></label>
			{kind === 'burner' ? (
				<label>fuel_divisor <input type="number" min="1" value={divisor} onInput={(e) => setDivisor(Math.max(1, Number(e.currentTarget.value) || 1))} /></label>
			) : (
				<label>energy_per_tick <input type="number" min="0" value={energy} onInput={(e) => setEnergy(Math.max(0, Number(e.currentTarget.value) || 0))} /></label>
			)}
			<label>Note <input value={note} onInput={(e) => setNote(e.currentTarget.value)} /></label>
			<p class="lab-hint">
				It runs the new recipe type <code>{base ? type : 'draft:...'}</code>. Add recipes of that type, then pick the machine as a
				route in the chain. Concept machines take their cycle from the recipe time and need code to exist in game.
			</p>
			<button type="submit" disabled={!base || taken}>Add machine</button>
		</form>
	);
}

function DraftList({ data, draft, setDraft }: { data: GameData; draft: Draft; setDraft: (d: Draft) => void }) {
	const noteInput = (value: string, onInput: (v: string) => void) => (
		<input class="lab-note" value={value} placeholder="Note" aria-label="Note" onInput={(e) => onInput(e.currentTarget.value)} />
	);
	if (draft.items.length + draft.recipes.length + draft.machines.length === 0) {
		return <p>No drafted content yet.</p>;
	}
	return (
		<ul class="lab-draft-list">
			{draft.items.map((item) => (
				<li key={item.id}>
					<strong>Item</strong> {item.name} <code>{item.id}</code>
					{noteInput(item.note, (note) => setDraft({ ...draft, items: draft.items.map((i) => (i.id === item.id ? { ...i, note } : i)) }))}
					<button type="button" onClick={() => setDraft({ ...draft, items: draft.items.filter((i) => i.id !== item.id) })}>Remove</button>
				</li>
			))}
			{draft.recipes.map((recipe) => (
				<li key={recipe.id}>
					<strong>{gameData.recipes[recipe.id] ? 'Changed recipe' : 'Recipe'}</strong>{' '}
					{recipe.inputs.map((s) => `${s.count} ${name(data, stackKey(s))}`).join(' + ') || 'nothing'} to{' '}
					{recipe.outputs.map((s) => `${s.count} ${name(data, stackKey(s))}`).join(' + ')} in {recipe.time} ticks <code>{recipe.id}</code>
					{noteInput(recipe.note, (note) => setDraft({ ...draft, recipes: draft.recipes.map((r) => (r.id === recipe.id ? { ...r, note } : r)) }))}
					<button type="button" onClick={() => setDraft({ ...draft, recipes: draft.recipes.filter((r) => r.id !== recipe.id) })}>Remove</button>
				</li>
			))}
			{draft.machines.map((machine) => (
				<li key={machine.id}>
					<strong>Machine</strong> {machine.name} <span class="lab-badge">needs code</span> runs <code>{machine.recipe_types.join(', ')}</code>
					{noteInput(machine.note, (note) => setDraft({ ...draft, machines: draft.machines.map((m) => (m.id === machine.id ? { ...m, note } : m)) }))}
					<button type="button" onClick={() => setDraft({ ...draft, machines: draft.machines.filter((m) => m.id !== machine.id) })}>Remove</button>
				</li>
			))}
		</ul>
	);
}

export function DraftPanel({ lab }: { lab: LabState }) {
	const { draft, setDraft, applied } = lab;
	return (
		<div class="lab-drafts">
			<DraftList data={applied.data} draft={draft} setDraft={setDraft} />
			<div class="lab-forms">
				<ItemForm draft={draft} setDraft={setDraft} />
				<RecipeForm data={applied.data} draft={draft} setDraft={setDraft} />
				<MachineForm draft={draft} setDraft={setDraft} />
			</div>
		</div>
	);
}
