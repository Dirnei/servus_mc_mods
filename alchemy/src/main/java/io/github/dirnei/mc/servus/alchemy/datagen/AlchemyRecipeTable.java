package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorRecipe;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinationDimension;
import io.github.dirnei.mc.servus.alchemy.datagen.gamedata.GameDataExportProvider;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinSeedItem;
import io.github.dirnei.mc.servus.alchemy.prelum.PressingRecipe;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

/**
 * The calcination, purification, coagulation and Athanor recipes Alchemy ships, in one table that both
 * the recipe provider and the journal's Workshop read, so the book always lists the shipped
 * recipes. Every row carries an English label for the book. The numbers are datapack examples.
 */
public final class AlchemyRecipeTable {

    /** A recipe input as datagen knows it: one item or an item tag, made into an ingredient with the registries of the run. */
    public record Input(@Nullable Item item, @Nullable TagKey<Item> tag) {

        public static Input of(ItemLike item) {
            return new Input(item.asItem(), null);
        }

        public static Input of(TagKey<Item> tag) {
            return new Input(null, tag);
        }

        public Ingredient ingredient(HolderGetter<Item> items) {
            return tag != null ? Ingredient.of(items.getOrThrow(tag)) : Ingredient.of(item);
        }

        /** The item or {@code #tag} id, for the book's tables. */
        public String id() {
            return tag != null ? "#" + tag.location() : BuiltInRegistries.ITEM.getKey(item).toString();
        }
    }

    /** One calcination: what goes in, the principles it gives and where its matter comes from. */
    public record Calcine(String name, String label, Input input, Principles output, CalcinationDimension dimension) {

        /** An Overworld calcination. */
        public Calcine(String name, String label, Input input, Principles output) {
            this(name, label, input, output, CalcinationDimension.OVERWORLD);
        }
    }

    /** One coagulation: the principles it takes and what it makes. */
    public record Coagulate(String name, String label, Principles input, ItemStackTemplate result) {
    }

    /** One Athanor craft: the item laid on it, the principles it burns, the stage it needs and the result. */
    public record AthanorCraft(String name, String inputLabel, String resultLabel, Input input, Principles principles,
                               int tier, ItemStackTemplate result, int time) {
    }

    /** One purification: an item leached with water into Salt, maybe with a residue; with no Salt it is a wash. */
    public record Purify(String name, String label, Input input, int count, int water, int salt, @Nullable ItemStackTemplate residue,
                         String residueLabel, int time) {

        /** A purification that leaves no residue. */
        public static Purify saltOnly(String name, String label, Input input, int count, int water, int salt, int time) {
            return new Purify(name, label, input, count, water, salt, null, "", time);
        }

        public Optional<ItemStackTemplate> residueTemplate() {
            return Optional.ofNullable(residue);
        }

        public int residueCount() {
            return residue == null ? 0 : residue.count();
        }
    }

    /** One kind of colour wash: a recipe per colour, listed in the journal as one line. */
    public record Wash(String label, String residueLabel, List<Purify> rows) {
    }

    /** One pressing at the Prelum: a counted item pressed into one result. */
    public record Press(String name, String label, Input input, int count, ItemStackTemplate result, int time) {
    }

    private AlchemyRecipeTable() {
    }

    public static List<Calcine> calcination() {
        return List.of(
                new Calcine("cobblestone", "Cobblestone", Input.of(Tags.Items.COBBLESTONES), new Principles(100, 0, 10)),
                new Calcine("stone", "Stone", Input.of(Tags.Items.STONES), new Principles(100, 0, 10)),
                new Calcine("dirt", "Dirt", Input.of(ItemTags.DIRT), new Principles(50, 20, 20)),
                new Calcine("sand", "Sand", Input.of(Tags.Items.SANDS), new Principles(80, 0, 10)),
                new Calcine("gravel", "Gravel", Input.of(Tags.Items.GRAVELS), new Principles(80, 0, 10)),
                new Calcine("logs", "Logs", Input.of(ItemTags.LOGS), new Principles(20, 100, 10)),
                new Calcine("planks", "Planks", Input.of(ItemTags.PLANKS), new Principles(5, 25, 0)),
                new Calcine("leaves", "Leaves", Input.of(ItemTags.LEAVES), new Principles(0, 40, 20)),
                new Calcine("saplings", "Saplings", Input.of(ItemTags.SAPLINGS), new Principles(0, 40, 20)),
                new Calcine("crops", "Crops", Input.of(Tags.Items.CROPS), new Principles(10, 30, 30)),
                new Calcine("seeds", "Seeds", Input.of(Tags.Items.SEEDS), new Principles(10, 30, 30)),
                new Calcine("coal", "Coal", Input.of(ItemTags.COALS), new Principles(0, 200, 0)),
                new Calcine("raw_materials", "Raw ore", Input.of(Tags.Items.RAW_MATERIALS), new Principles(150, 0, 50)),
                new Calcine("redstone", "Redstone", Input.of(Tags.Items.DUSTS_REDSTONE), new Principles(0, 0, 100)),
                new Calcine("bones", "Bones", Input.of(Tags.Items.BONES), new Principles(100, 0, 0)),
                nether("netherrack", "Netherrack", Input.of(Tags.Items.NETHERRACKS), new Principles(20, 40, 0)),
                nether("soul_sand", "Soul Sand and Soil", Input.of(ItemTags.SOUL_FIRE_BASE_BLOCKS), new Principles(20, 60, 0)),
                nether("nether_wart", "Nether Wart", Input.of(Tags.Items.CROPS_NETHER_WART), new Principles(0, 60, 20)),
                nether("nether_wart_block", "Nether Wart Block", Input.of(Items.NETHER_WART_BLOCK), new Principles(0, 540, 180)),
                nether("crimson_stems", "Crimson Stems", Input.of(ItemTags.CRIMSON_STEMS), new Principles(0, 150, 0)),
                nether("warped_stems", "Warped Stems", Input.of(ItemTags.WARPED_STEMS), new Principles(0, 150, 0)),
                nether("blaze_rod", "Blaze Rod", Input.of(Tags.Items.RODS_BLAZE), new Principles(0, 400, 0)),
                nether("magma_cream", "Magma Cream", Input.of(Items.MAGMA_CREAM), new Principles(0, 80, 40)),
                nether("glowstone_dust", "Glowstone Dust", Input.of(Tags.Items.DUSTS_GLOWSTONE), new Principles(0, 30, 60)),
                nether("nether_quartz", "Nether Quartz", Input.of(Tags.Items.GEMS_QUARTZ), new Principles(120, 0, 0)),
                end("end_stone", "End Stone", Input.of(Tags.Items.END_STONES), new Principles(40, 0, 40)),
                end("purpur_block", "Purpur Block", Input.of(Items.PURPUR_BLOCK), new Principles(50, 0, 20)),
                end("chorus_fruit", "Chorus Fruit", Input.of(Items.CHORUS_FRUIT), new Principles(0, 20, 60)),
                end("popped_chorus_fruit", "Popped Chorus Fruit", Input.of(Items.POPPED_CHORUS_FRUIT), new Principles(0, 0, 100)),
                end("ender_pearl", "Ender Pearl", Input.of(Tags.Items.ENDER_PEARLS), new Principles(0, 0, 300)),
                end("shulker_shell", "Shulker Shell", Input.of(Items.SHULKER_SHELL), new Principles(200, 0, 80)));
    }

    private static Calcine nether(String name, String label, Input input, Principles output) {
        return new Calcine(name, label, input, output, CalcinationDimension.NETHER);
    }

    private static Calcine end(String name, String label, Input input, Principles output) {
        return new Calcine(name, label, input, output, CalcinationDimension.END);
    }

    /** What the Purifier leaches into Salt: the Ash of the Calciner, the seeds of the Coagulator and everyday matter. */
    public static List<Purify> purification() {
        return List.of(
                purify("ash", "Ash", ServusAlchemyItems.ASH.get(), 4, 400, 400, ServusAlchemyItems.CAPUT_MORTUUM.get(), "Caput Mortuum", 160),
                purify("sugar", "Sugar", Items.SUGAR, 8, 500, 100, ServusAlchemyItems.KANDIS.get(), "Kandis", 160),
                purify("gunpowder", "Gunpowder", Items.GUNPOWDER, 4, 400, 200, ServusAlchemyItems.BRIMSTONE.get(), "Brimstone", 160),
                purify("redstone", "Redstone Dust", Items.REDSTONE, 8, 400, 50, ServusAlchemyItems.CINNABAR.get(), "Cinnabar", 160),
                Purify.saltOnly("dried_kelp", "Dried Kelp", Input.of(Items.DRIED_KELP), 4, 250, 200, 80),
                purify("rotten_flesh", "Rotten Flesh", Items.ROTTEN_FLESH, 4, 250, 100, Items.LEATHER, "Leather", 120),
                purify("soul_sand", "Soul Sand", Items.SOUL_SAND, 1, 250, 25, Items.SAND, "Sand", 80),
                purify("magma_cream", "Magma Cream", Items.MAGMA_CREAM, 1, 250, 25, Items.SLIME_BALL, "Slimeball", 80));
    }

    private static Purify purify(String name, String label, ItemLike input, int count, int water, int salt, ItemLike residue,
                                 String residueLabel, int time) {
        return new Purify(name, label, Input.of(input), count, water, salt, new ItemStackTemplate(residue.asItem()), residueLabel, time);
    }

    /** Items washed per cycle of a colour wash. */
    private static final int WASH_COUNT = 8;
    private static final int WASH_WATER = 250;
    private static final int WASH_TIME = 40;

    /** The colour washes: dye washed out of Concrete Powder (into Concrete), Wool, Carpet and Terracotta, one recipe per colour. */
    public static List<Wash> washes() {
        return List.of(
                wash("Concrete Powder of any colour", "its Concrete", colour -> colour + "_concrete_powder", colour -> colour + "_concrete", true),
                wash("dyed Wool", "White Wool", colour -> colour + "_wool", colour -> "white_wool", false),
                wash("dyed Carpet", "White Carpet", colour -> colour + "_carpet", colour -> "white_carpet", false),
                wash("dyed Terracotta", "Terracotta", colour -> colour + "_terracotta", colour -> "terracotta", true));
    }

    private static Wash wash(String label, String residueLabel, UnaryOperator<String> input, UnaryOperator<String> residue,
                             boolean withWhite) {
        List<Purify> rows = new ArrayList<>();
        for (DyeColor colour : DyeColor.values()) {
            if (colour == DyeColor.WHITE && !withWhite) {
                continue;
            }
            Item in = vanilla(input.apply(colour.getSerializedName()));
            Item out = vanilla(residue.apply(colour.getSerializedName()));
            String inPath = BuiltInRegistries.ITEM.getKey(in).getPath();
            rows.add(new Purify("wash_" + inPath, GameDataExportProvider.titleCase(inPath), Input.of(in), WASH_COUNT, WASH_WATER, 0,
                    new ItemStackTemplate(out, WASH_COUNT), GameDataExportProvider.titleCase(BuiltInRegistries.ITEM.getKey(out).getPath()),
                    WASH_TIME));
        }
        return new Wash(label, residueLabel, List.copyOf(rows));
    }

    private static Item vanilla(String path) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(path))
                .orElseThrow(() -> new IllegalStateException("no vanilla item " + path));
    }

    /** Every shipped purification recipe: {@link #purification()} and each colour of {@link #washes()}. */
    public static List<Purify> allPurification() {
        List<Purify> all = new ArrayList<>(purification());
        washes().forEach(wash -> all.addAll(wash.rows()));
        return List.copyOf(all);
    }

    /** What the Prelum presses: Caput Mortuum into Netherite Scrap, the end of the Work. */
    public static List<Press> pressing() {
        return List.of(new Press("netherite_scrap", "Caput Mortuum", Input.of(ServusAlchemyItems.CAPUT_MORTUUM.get()), 16,
                new ItemStackTemplate(Items.NETHERITE_SCRAP), PressingRecipe.DEFAULT_TIME));
    }

    /** What the Coagulator makes from the principles; no metal in Nigredo. */
    public static List<Coagulate> coagulation() {
        return List.of(
                new Coagulate("salt_crystal", "Salt Crystal", new Principles(250, 0, 0), new ItemStackTemplate(ServusAlchemyItems.SALT_CRYSTAL.get())),
                new Coagulate("sulfur", "Sulfur", new Principles(0, 250, 0), new ItemStackTemplate(ServusAlchemyItems.SULFUR.get())),
                new Coagulate("quicksilver", "Quicksilver", new Principles(0, 0, 250), new ItemStackTemplate(ServusAlchemyItems.QUICKSILVER.get())),
                new Coagulate("clay_ball", "Clay Ball", new Principles(100, 0, 50), new ItemStackTemplate(Items.CLAY_BALL)),
                new Coagulate("flint", "Flint", new Principles(150, 0, 0), new ItemStackTemplate(Items.FLINT)),
                new Coagulate("gunpowder", "Gunpowder", new Principles(50, 150, 0), new ItemStackTemplate(Items.GUNPOWDER)),
                new Coagulate("redstone", "Redstone", new Principles(50, 0, 150), new ItemStackTemplate(Items.REDSTONE)),
                new Coagulate("glowstone_dust", "Glowstone Dust", new Principles(0, 100, 100), new ItemStackTemplate(Items.GLOWSTONE_DUST)),
                new Coagulate("bone_meal", "Bone Meal", new Principles(60, 20, 0), new ItemStackTemplate(Items.BONE_MEAL)),
                new Coagulate("slime_ball", "Slimeball", new Principles(0, 50, 150), new ItemStackTemplate(Items.SLIME_BALL)));
    }

    /** Crafts on the Athanor. */
    public static List<AthanorCraft> athanor() {
        return List.of(
                new AthanorCraft("salt_crystal_block", "Salt Crystal", "Salt Crystal Block", Input.of(ServusAlchemyItems.SALT_CRYSTAL.get()),
                        new Principles(1000, 0, 0), 0, new ItemStackTemplate(ServusAlchemyItems.SALT_CRYSTAL_BLOCK.get()), AthanorRecipe.DEFAULT_TIME),
                new AthanorCraft("blaze_powder", "Gunpowder", "Blaze Powder", Input.of(Items.GUNPOWDER),
                        new Principles(0, 200, 0), 0, new ItemStackTemplate(Items.BLAZE_POWDER), AthanorRecipe.DEFAULT_TIME),
                new AthanorCraft("experience_bottle", "Glass Bottle", "Bottle o' Enchanting", Input.of(Items.GLASS_BOTTLE),
                        new Principles(50, 0, 100), 1, new ItemStackTemplate(Items.EXPERIENCE_BOTTLE), AthanorRecipe.DEFAULT_TIME),
                new AthanorCraft("elixir_of_sight", "Quicksilver", "Elixir of Sight", Input.of(ServusAlchemyItems.QUICKSILVER.get()),
                        new Principles(100, 100, 250), 1, new ItemStackTemplate(ElementalSight.ELIXIR.get()), 200),
                new AthanorCraft("gnome_hollow", "Cracked Stone Bricks", "Gnome Hollow", Input.of(Items.CRACKED_STONE_BRICKS),
                        new Principles(500, 0, 250), 1, new ItemStackTemplate(ServusAlchemyItems.GNOME_HOLLOW.get()), 200),
                new AthanorCraft("principle_dropper", "Principle Vat", "Principle Dropper", Input.of(ServusAlchemyItems.VAT.get()),
                        new Principles(250, 250, 250), 2, new ItemStackTemplate(ServusAlchemyItems.PRINCIPLE_DROPPER.get()), 200),
                new AthanorCraft("ignis_perpetuus", "Sal Ammoniac", "Ignis Perpetuus", Input.of(ServusAlchemyItems.SAL_AMMONIAC.get()),
                        new Principles(0, 500, 0), 2, new ItemStackTemplate(ServusAlchemyItems.IGNIS_PERPETUUS.get()), 200),
                new AthanorCraft("sealed_flask", "Crying Obsidian", "Sealed Flask", Input.of(Items.CRYING_OBSIDIAN),
                        new Principles(250, 0, 500), 3, new ItemStackTemplate(ServusAlchemyItems.SEALED_FLASK.get()), 200),
                new AthanorCraft("prelum_screw", "Dragon's Breath", "Prelum Screw", Input.of(Items.DRAGON_BREATH),
                        new Principles(0, 500, 500), 3, new ItemStackTemplate(ServusAlchemyItems.PRELUM_SCREW.get()), 200),
                seed("coal", "Coal", Input.of(Items.COAL), new Principles(150, 150, 0), 1),
                seed("iron", "Raw Iron", Input.of(Tags.Items.RAW_MATERIALS_IRON), new Principles(250, 0, 100), 1),
                seed("copper", "Raw Copper", Input.of(Tags.Items.RAW_MATERIALS_COPPER), new Principles(250, 0, 100), 1),
                seed("gold", "Raw Gold", Input.of(Tags.Items.RAW_MATERIALS_GOLD), new Principles(250, 250, 250), 2),
                seed("redstone", "Redstone Dust", Input.of(Tags.Items.DUSTS_REDSTONE), new Principles(0, 250, 250), 2),
                seed("quartz", "Nether Quartz", Input.of(Tags.Items.GEMS_QUARTZ), new Principles(250, 0, 250), 2),
                seed("diamond", "Diamond", Input.of(Tags.Items.GEMS_DIAMOND), new Principles(500, 500, 500), 3),
                seed("emerald", "Emerald", Input.of(Tags.Items.GEMS_EMERALD), new Principles(500, 500, 500), 3));
    }

    /** The Seed of a shipped vein, made from its raw material at the stage of that vein. */
    private static AthanorCraft seed(String vein, String inputLabel, Input input, Principles principles, int tier) {
        return new AthanorCraft("seed_of_" + vein, inputLabel, "Seed of " + GameDataExportProvider.titleCase(vein), input, principles, tier,
                VeinSeedItem.template(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, vein)), 200);
    }

    /** The Athanor crafts that make Seeds, in the order of their stages. */
    public static List<AthanorCraft> seeds() {
        return athanor().stream().filter(craft -> craft.name().startsWith("seed_of_")).toList();
    }

    /** The English name of an Athanor stage. */
    public static String stageName(int tier) {
        return switch (tier) {
            case 0 -> "Nigredo";
            case 1 -> "Albedo";
            case 2 -> "Citrinitas";
            default -> "Rubedo";
        };
    }

    /** An item's English name from its id path, for the journal's tables: caput_mortuum gives Caput Mortuum. */
    public static String label(ItemStackTemplate stack) {
        return GameDataExportProvider.titleCase(stack.item().unwrapKey().orElseThrow().identifier().getPath());
    }
}
