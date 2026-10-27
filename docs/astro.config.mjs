// @ts-check
import starlight from '@astrojs/starlight';
import { defineConfig } from 'astro/config';

export default defineConfig({
	site: 'https://dirnei.github.io',
	base: '/servus_mc_mods',
	integrations: [
		starlight({
			title: 'Servus Mods',
			description:
				'Player guide, datapack reference and data line API for the Servus mods for Minecraft 26.1.2 on NeoForge.',
			social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/Dirnei/servus_mc_mods' }],
			sidebar: [
				{ label: 'Start', items: [{ slug: 'getting-started' }] },
				{
					label: 'Mods',
					items: [{ slug: 'mods/foundation' }, { slug: 'mods/shuttles' }, { slug: 'mods/alchemy' }],
				},
				{
					label: 'Developers',
					items: [
						{ slug: 'developers' },
						{ slug: 'developers/data-line' },
						{ slug: 'developers/join-the-data-line' },
						{ slug: 'developers/deliveries' },
						{ slug: 'developers/machine-adapter' },
					],
				},
				{ label: 'Datapack reference', items: [{ autogenerate: { directory: 'reference/datapack' } }] },
			],
		}),
	],
});
