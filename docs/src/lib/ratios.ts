import { type GameData, type MachineData, type RecipeData, type Stack, stackKey } from './gamedata';

/**
 * Production ratio math. Pure: no DOM, no framework. The timing rules mirror the machine code:
 * - a machine's cycle is its work_ticks, except recipe types in RECIPE_TIMED_TYPES (and concept machines),
 *   which take the recipe's time (Assembler, Crusher), and the Research Lab, which takes ticks_per_pack;
 * - burners burn fuel only while working: one fuel item lasts burn_time / fuel_divisor working ticks;
 * - powered machines draw energy_per_tick only on working ticks, so draw scales with utilization;
 * - generators run only while energy is needed, so their fuel follows the FE demand.
 */

export const EXTRACTION = 'servus:extraction';
export const RESEARCH = 'servus:research';
export const RECIPE_TIMED_TYPES = new Set(['servus_manufacturing:assembling', 'servus_manufacturing:crushing']);
export const DEFAULT_FUEL = 'minecraft:coal';
const TICKS_PER_SECOND = 20;
const EPSILON = 1e-9;

/** Machine preference for the default route: burner tier first. */
const KIND_ORDER: Record<MachineData['kind'], number> = { burner: 0, crafter: 1, powered: 2, generator: 3 };

export interface Flags {
	items?: ReadonlySet<string>;
	recipes?: ReadonlySet<string>;
	/** Concept machines: they need code and take their cycle from the recipe time. */
	machines?: ReadonlySet<string>;
}

export interface Route {
	/** `deposit:<id>@<machine>` or `recipe:<id>@<machine>`. */
	key: string;
	kind: 'deposit' | 'recipe';
	/** Deposit id or recipe id. */
	source: string;
	machine: string;
	inputs: Stack[];
	/** Count of the produced item per cycle. */
	outputCount: number;
	cycleTicks: number;
	draft: boolean;
	needsCode: boolean;
}

export interface ChainNode {
	item: string;
	/** Items per second needed. */
	rate: number;
	route: Route | null;
	/** Set when the item has no producing recipe or deposit. */
	noSource: boolean;
	/** Set when following this item's route would loop back into the chain. */
	loop: boolean;
	alternatives: Route[];
	perMachine: number;
	count: number;
	utilization: number;
	fePerTick: number;
	/** Fuel items per second burned by this node's burner machines. */
	fuelPerSecond: number;
	inputs: { item: string; rate: number }[];
	draft: boolean;
}

export interface ConsumerNode {
	machine: string;
	tech: string;
	item: string;
	rate: number;
	perMachine: number;
	count: number;
	utilization: number;
	fePerTick: number;
}

export interface ChainTotals {
	machines: Record<string, number>;
	fePerTick: number;
	generators: number;
	generatorMachine: string | null;
	generatorFuelPerSecond: number;
	burnerFuelPerSecond: number;
	fuelItem: string;
	fuelPerSecond: number;
}

export interface Chain {
	target: { item: string; perSecond: number };
	/** In order from the target down to raw resources. */
	nodes: ChainNode[];
	consumer: ConsumerNode | null;
	totals: ChainTotals;
}

export interface ChainOptions {
	/** Chosen route key per item; items without a choice use the default route. */
	routes?: Record<string, string>;
	fuelItem?: string;
	flags?: Flags;
	/** Add a Research Lab consuming the target when it is a research pack. Default true. */
	research?: boolean;
}

/** Cycle length in ticks of a machine running a recipe. */
export function cycleTicks(machine: MachineData, recipe: RecipeData, conceptMachine = false): number {
	if (conceptMachine || RECIPE_TIMED_TYPES.has(recipe.type)) {
		return Math.max(1, recipe.time);
	}
	return Math.max(1, machine.work_ticks);
}

/** Working ticks one fuel item lasts in a burner or generator. */
export function fuelTicks(data: GameData, machine: MachineData, fuelItem: string): number {
	const burn = data.fuels[fuelItem] ?? 0;
	return burn / Math.max(1, machine.fuel_divisor);
}

function sortedMachines(data: GameData, type: string): string[] {
	return Object.entries(data.machines)
		.filter(([, machine]) => machine.recipe_types.includes(type))
		.sort(([a, ma], [b, mb]) => KIND_ORDER[ma.kind] - KIND_ORDER[mb.kind] || a.localeCompare(b))
		.map(([id]) => id);
}

/** Every way to make an item: deposits first, then recipes by id, each with every machine able to run it. */
export function routesFor(data: GameData, item: string, flags: Flags = {}): Route[] {
	const routes: Route[] = [];
	for (const [depositId, deposit] of Object.entries(data.deposits).sort(([a], [b]) => a.localeCompare(b))) {
		if (stackKey(deposit.yield) !== item) {
			continue;
		}
		for (const machineId of sortedMachines(data, EXTRACTION)) {
			const machine = data.machines[machineId];
			routes.push({
				key: `deposit:${depositId}@${machineId}`,
				kind: 'deposit',
				source: depositId,
				machine: machineId,
				inputs: [],
				outputCount: deposit.yield.count,
				cycleTicks: Math.max(1, machine.work_ticks),
				draft: false,
				needsCode: flags.machines?.has(machineId) ?? false,
			});
		}
	}
	const recipeEntries = Object.entries(data.recipes).sort(
		([a], [b]) => Number(flags.recipes?.has(a) ?? false) - Number(flags.recipes?.has(b) ?? false) || a.localeCompare(b),
	);
	for (const [recipeId, recipe] of recipeEntries) {
		const outputCount = recipe.outputs.filter((stack) => stackKey(stack) === item).reduce((sum, s) => sum + s.count, 0);
		if (outputCount === 0) {
			continue;
		}
		for (const machineId of sortedMachines(data, recipe.type)) {
			const concept = flags.machines?.has(machineId) ?? false;
			routes.push({
				key: `recipe:${recipeId}@${machineId}`,
				kind: 'recipe',
				source: recipeId,
				machine: machineId,
				inputs: recipe.inputs,
				outputCount,
				cycleTicks: cycleTicks(data.machines[machineId], recipe, concept),
				draft: flags.recipes?.has(recipeId) ?? false,
				needsCode: concept,
			});
		}
	}
	return routes;
}

/** The research tech whose lab time applies when the target is a pack: the first tech by id that costs it. */
export function researchTechFor(data: GameData, item: string): string | null {
	const techs = Object.keys(data.techs).sort();
	return techs.find((id) => data.techs[id].cost.some((stack) => stackKey(stack) === item)) ?? null;
}

interface Plan {
	order: string[];
	routes: Map<string, { route: Route | null; alternatives: Route[] }>;
	/** Edges skipped because they close a loop, as consumer to input. */
	loops: Set<string>;
}

function plan(data: GameData, roots: string[], options: ChainOptions): Plan {
	const routes = new Map<string, { route: Route | null; alternatives: Route[] }>();
	const loops = new Set<string>();
	const order: string[] = [];
	const state = new Map<string, 'visiting' | 'done'>();

	const visit = (item: string) => {
		state.set(item, 'visiting');
		const alternatives = routesFor(data, item, options.flags);
		const chosen = options.routes?.[item];
		const route = alternatives.find((r) => r.key === chosen) ?? alternatives[0] ?? null;
		routes.set(item, { route, alternatives });
		for (const input of route?.inputs ?? []) {
			const key = stackKey(input);
			const seen = state.get(key);
			if (seen === 'visiting') {
				loops.add(`${item}>${key}`);
			} else if (seen === undefined) {
				visit(key);
			}
		}
		state.set(item, 'done');
		order.push(item);
	};
	for (const root of roots) {
		if (!state.has(root)) {
			visit(root);
		}
	}
	// Post order lists inputs before consumers; consumers first is the order demand flows in.
	order.reverse();
	return { order, routes, loops };
}

function evaluate(
	data: GameData,
	target: { item: string; perSecond: number },
	fuelDemand: number,
	options: ChainOptions,
): Chain {
	const fuelItem = options.fuelItem ?? DEFAULT_FUEL;
	const flags = options.flags ?? {};
	// The fuel root goes first so that, after reversing, the target leads the node order.
	const roots = fuelDemand > 0 && fuelItem !== target.item ? [fuelItem, target.item] : [target.item];
	const { order, routes, loops } = plan(data, roots, options);

	const demand = new Map<string, number>([[target.item, target.perSecond]]);
	if (fuelDemand > 0) {
		demand.set(fuelItem, (demand.get(fuelItem) ?? 0) + fuelDemand);
	}

	const nodes: ChainNode[] = [];
	const totals: ChainTotals = {
		machines: {},
		fePerTick: 0,
		generators: 0,
		generatorMachine: null,
		generatorFuelPerSecond: 0,
		burnerFuelPerSecond: 0,
		fuelItem,
		fuelPerSecond: 0,
	};

	for (const item of order) {
		const rate = demand.get(item) ?? 0;
		const { route, alternatives } = routes.get(item)!;
		const node: ChainNode = {
			item,
			rate,
			route,
			noSource: route === null,
			loop: false,
			alternatives,
			perMachine: 0,
			count: 0,
			utilization: 0,
			fePerTick: 0,
			fuelPerSecond: 0,
			inputs: [],
			draft: (flags.items?.has(item) ?? false) || (route?.draft ?? false),
		};
		if (route !== null) {
			const machine = data.machines[route.machine];
			node.perMachine = (route.outputCount * TICKS_PER_SECOND) / route.cycleTicks;
			node.count = rate > EPSILON ? Math.ceil(rate / node.perMachine - EPSILON) : 0;
			node.utilization = node.count > 0 ? rate / (node.count * node.perMachine) : 0;
			const working = node.count * node.utilization;
			if (machine.kind === 'burner') {
				const ticks = fuelTicks(data, machine, fuelItem);
				node.fuelPerSecond = ticks > 0 ? (working * TICKS_PER_SECOND) / ticks : 0;
			} else {
				node.fePerTick = working * machine.energy_per_tick;
			}
			const cyclesPerSecond = rate / route.outputCount;
			for (const input of route.inputs) {
				const key = stackKey(input);
				const inputRate = cyclesPerSecond * input.count;
				node.inputs.push({ item: key, rate: inputRate });
				if (loops.has(`${item}>${key}`)) {
					node.loop = true;
				} else {
					demand.set(key, (demand.get(key) ?? 0) + inputRate);
				}
			}
			if (node.count > 0) {
				totals.machines[route.machine] = (totals.machines[route.machine] ?? 0) + node.count;
			}
		}
		totals.fePerTick += node.fePerTick;
		totals.burnerFuelPerSecond += node.fuelPerSecond;
		nodes.push(node);
	}

	let consumer: ConsumerNode | null = null;
	const tech = options.research === false ? null : researchTechFor(data, target.item);
	const labId = tech ? sortedMachines(data, RESEARCH)[0] : undefined;
	if (tech && labId) {
		const lab = data.machines[labId];
		const perMachine = TICKS_PER_SECOND / data.techs[tech].ticks_per_pack;
		const count = target.perSecond > EPSILON ? Math.ceil(target.perSecond / perMachine - EPSILON) : 0;
		const utilization = count > 0 ? target.perSecond / (count * perMachine) : 0;
		consumer = {
			machine: labId,
			tech,
			item: target.item,
			rate: target.perSecond,
			perMachine,
			count,
			utilization,
			fePerTick: count * utilization * lab.energy_per_tick,
		};
		totals.machines[labId] = (totals.machines[labId] ?? 0) + count;
		totals.fePerTick += consumer.fePerTick;
	}

	const generatorId = Object.keys(data.machines)
		.filter((id) => data.machines[id].kind === 'generator')
		.sort()[0];
	if (generatorId && totals.fePerTick > EPSILON) {
		const generator = data.machines[generatorId];
		totals.generatorMachine = generatorId;
		totals.generators = Math.ceil(totals.fePerTick / generator.energy_per_tick - EPSILON);
		const ticks = fuelTicks(data, generator, fuelItem);
		const workingGenerators = totals.fePerTick / generator.energy_per_tick;
		totals.generatorFuelPerSecond = ticks > 0 ? (workingGenerators * TICKS_PER_SECOND) / ticks : 0;
		totals.machines[generatorId] = (totals.machines[generatorId] ?? 0) + totals.generators;
	}
	totals.fuelPerSecond = totals.burnerFuelPerSecond + totals.generatorFuelPerSecond;
	return { target, nodes, consumer, totals };
}

/**
 * The production chain for a target rate. Fuel for burners and generators is fed back as demand for the fuel
 * item (coal by default), solved by iterating until the fuel rate settles.
 */
export function computeChain(
	data: GameData,
	target: { item: string; perSecond: number },
	options: ChainOptions = {},
): Chain {
	let fuel = 0;
	let chain = evaluate(data, target, fuel, options);
	for (let round = 0; round < 20; round++) {
		const next = chain.totals.fuelPerSecond;
		if (Math.abs(next - fuel) < EPSILON) {
			break;
		}
		fuel = next;
		chain = evaluate(data, target, fuel, options);
	}
	return chain;
}
