// @ts-check
import preact from '@astrojs/preact';
import starlight from '@astrojs/starlight';
import { defineConfig } from 'astro/config';

export default defineConfig({
	site: 'https://dirnei.github.io',
	base: '/servus_mc_mods',
	integrations: [
		starlight({
			title: 'Servus Mods',
			description: 'Documentation and Balance lab for the Servus mod family for Minecraft 1.21.1 on NeoForge.',
			social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/Dirnei/servus_mc_mods' }],
			customCss: ['./src/styles/lab.css'],
			sidebar: [
				{ label: 'Start', items: [{ slug: 'getting-started' }] },
				{
					label: 'Mods',
					items: [
						{ slug: 'mods/lib' },
						{ slug: 'mods/manufacturing' },
						{ slug: 'mods/logistics' },
						{ slug: 'mods/storage' },
					],
				},
				{ label: 'Datapack reference', items: [{ autogenerate: { directory: 'reference/datapack' } }] },
				{
					label: 'Balance lab',
					items: [{ slug: 'lab' }, { slug: 'lab/chains' }, { slug: 'lab/recipes' }, { slug: 'lab/research' }],
				},
			],
		}),
		preact(),
	],
});
