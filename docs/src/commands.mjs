import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import Ajv2020 from 'ajv/dist/2020.js';
import { parse } from 'yaml';
import { versions } from './versions.mjs';

/**
 * The command catalogs of the released mods, read as they are at each mod's release tag, never
 * from the working tree, so the Commands page shows only released commands. A release without a
 * catalog has no section; a catalog that breaks the Edict schema fails the build.
 */
// The build runs in docs/; import.meta.url would point into dist/ once Astro bundles a component.
const docs = pathToFileURL(`${process.cwd()}/`);
const repo = new URL('../', docs);

/** The mods with commands, in page order. */
export const MODS = [
	{ mod: 'foundation', modId: 'servus_foundation', version: versions.foundation_version },
	{ mod: 'shuttles', modId: 'servus_shuttles', version: versions.shuttles_version },
	{ mod: 'alchemy', modId: 'servus_alchemy', version: versions.alchemy_version },
];

const schema = JSON.parse(readFileSync(new URL('src/edict/catalog.schema.json', docs), 'utf8'));
const validate = new Ajv2020({ allErrors: true, validateFormats: false }).compile(schema);

/** The catalog file of a mod at a git ref, or null when the ref or the file does not exist. */
function catalogText(ref, mod, modId) {
	try {
		return execFileSync('git', ['show', `${ref}:${mod}/src/main/resources/edict/${modId}.edict.yaml`], {
			cwd: repo,
			encoding: 'utf8',
			stdio: ['ignore', 'pipe', 'ignore'],
		});
	} catch {
		return null;
	}
}

/** Parses and checks one catalog; throws naming every schema error. */
export function checkedCatalog(text, source) {
	const catalog = parse(text);
	if (!validate(catalog)) {
		const errors = validate.errors.map((error) => `${error.instancePath || '/'} ${error.message}`).join('\n  ');
		throw new Error(`${source} is not a valid Edict catalog:\n  ${errors}`);
	}
	return catalog;
}

/**
 * The display signature by the Edict rules: the prefix and the command key, then positional
 * parameters as <name> or [name], named ones as --name <value> or [--name <value>], flags as [--name].
 */
export function signature(catalog, key, command) {
	const prefix = command.prefix ?? catalog.prefix ?? '';
	const params = command.params ?? [];
	const kind = (param) => param.kind ?? 'positional';
	const named = (param) => (param.short ? `-${param.short}, --${param.name}` : `--${param.name}`);
	const parts = [`${prefix}${key}`];
	for (const param of params.filter((p) => kind(p) === 'positional')) {
		parts.push(param.required ? `<${param.name}>` : `[${param.name}]`);
	}
	for (const param of params.filter((p) => kind(p) === 'named')) {
		parts.push(param.required ? `${named(param)} <value>` : `[${named(param)} <value>]`);
	}
	for (const param of params.filter((p) => kind(p) === 'flag')) {
		parts.push(`[${named(param)}]`);
	}
	return parts.join(' ');
}

/** One mod's section: its commands grouped by category in catalog order. */
export function section(entry, catalog) {
	const categories = new Map();
	for (const [key, command] of Object.entries(catalog.commands)) {
		if (!categories.has(command.category)) categories.set(command.category, []);
		categories.get(command.category).push({
			key,
			signature: signature(catalog, key, command),
			description: command.description,
			riskLevel: command.riskLevel,
			remote: command.remoteExecutable ?? true,
			params: command.params ?? [],
			examples: command.examples ?? [],
		});
	}
	return {
		...entry,
		name: catalog.name,
		categories: [...categories].map(([name, commands]) => ({ name, commands })),
	};
}

/**
 * The sections of every released mod whose release ships a catalog. SERVUS_DOCS_CATALOG_REF, when
 * set, reads every catalog at that git ref instead of the release tags, to preview unreleased catalogs.
 */
export function commandSections() {
	const override = process.env.SERVUS_DOCS_CATALOG_REF;
	const sections = [];
	for (const entry of MODS) {
		const ref = override || `${entry.mod}-v${entry.version}`;
		const text = catalogText(ref, entry.mod, entry.modId);
		if (text !== null) sections.push(section(entry, checkedCatalog(text, `${entry.modId} catalog at ${ref}`)));
	}
	return sections;
}
