import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

/**
 * Released mod versions, read from the repository so a release needs no docs edits.
 * A mod's version is the first heading of its CHANGELOG.md that is not "Unreleased".
 * Pages write them as tokens such as %foundation_version%, filled in at build time.
 */
// The build runs in docs/; import.meta.url would point into dist/ once Astro bundles a component.
const repo = new URL('../', pathToFileURL(`${process.cwd()}/`));

function released(mod) {
	const changelog = readFileSync(new URL(`${mod}/CHANGELOG.md`, repo), 'utf8');
	const heading = changelog.match(/^## (?!Unreleased\b)(\S+)/m);
	if (!heading) throw new Error(`${mod}/CHANGELOG.md has no released version heading`);
	return heading[1];
}

function gradleProperty(name) {
	const properties = readFileSync(new URL('gradle.properties', repo), 'utf8');
	const line = properties.match(new RegExp(`^${name}=(.+)$`, 'm'));
	if (!line) throw new Error(`gradle.properties has no ${name}`);
	return line[1].trim();
}

export const versions = {
	foundation_version: released('foundation'),
	shuttles_version: released('shuttles'),
	alchemy_version: released('alchemy'),
	foundation_min_version: gradleProperty('foundation_min_version'),
};

/** Replaces every known %token% in the text; unknown tokens stay as written. */
export function fillVersions(text) {
	return text.replace(/%(\w+)%/g, (token, key) => (Object.hasOwn(versions, key) ? versions[key] : token));
}

/** Remark plugin that fills version tokens in text, inline code, code blocks, HTML and JSX string attributes. */
export function remarkVersions() {
	const fill = (node) => {
		if (typeof node.value === 'string' && ['text', 'inlineCode', 'code', 'html'].includes(node.type)) {
			node.value = fillVersions(node.value);
		}
		if (node.type?.startsWith('mdxJsx')) {
			for (const attribute of node.attributes) {
				if (typeof attribute.value === 'string') attribute.value = fillVersions(attribute.value);
			}
		}
		node.children?.forEach(fill);
	};
	return fill;
}
