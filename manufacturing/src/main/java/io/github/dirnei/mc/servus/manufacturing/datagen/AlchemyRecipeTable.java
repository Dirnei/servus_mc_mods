package io.github.dirnei.mc.servus.manufacturing.datagen;

import net.minecraft.world.level.block.Block;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.List;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;

/**
 * The calcination, coagulation and Athanor recipes Manufacturing ships, in one table that both
 * the recipe provider and the journal's Workshop read, so the book always lists the shipped
 * recipes. Every row carries an English label for the book. The numbers are datapack examples.
 */
public final class AlchemyRecipeTable {

    /** One calcination: what goes in, and the principles it gives. */
    public record Calcine(String name, String label, Ingredient input, Principles output) {
    }

    /** One coagulation: the principles it takes and what it makes. */
    public record Coagulate(String name, String label, Principles input, ItemStack result) {
    }

    /** One Athanor craft: the item laid on it, the principles it burns, the stage it needs and the result. */
    public record AthanorCraft(String name, String inputLabel, String resultLabel, Ingredient input, Principles principles,
                               int tier, ItemStack result, int time) {
    }

    /** One purification: an item leached with water into Salt. */
    public record Purify(String name, String label, Ingredient input, int count, int water, int salt, ItemStack residue, int time) {
    }

    private AlchemyRecipeTable() {
    }

    public static List<Calcine> calcination() {
        return List.of(
                new Calcine("cobblestone", "Cobblestone", Ingredient.of(Tags.Items.COBBLESTONES), new Principles(100, 0, 10)),
                new Calcine("stone", "Stone", Ingredient.of(Tags.Items.STONES), new Principles(100, 0, 10)),
                new Calcine("dirt", "Dirt", Ingredient.of(ItemTags.DIRT), new Principles(50, 20, 20)),
                new Calcine("sand", "Sand", Ingredient.of(Tags.Items.SANDS), new Principles(80, 0, 10)),
                new Calcine("gravel", "Gravel", Ingredient.of(Tags.Items.GRAVELS), new Principles(80, 0, 10)),
                new Calcine("logs", "Logs", Ingredient.of(ItemTags.LOGS), new Principles(20, 100, 10)),
                new Calcine("planks", "Planks", Ingredient.of(ItemTags.PLANKS), new Principles(5, 25, 0)),
                new Calcine("leaves", "Leaves", Ingredient.of(ItemTags.LEAVES), new Principles(0, 40, 20)),
                new Calcine("saplings", "Saplings", Ingredient.of(ItemTags.SAPLINGS), new Principles(0, 40, 20)),
                new Calcine("crops", "Crops", Ingredient.of(Tags.Items.CROPS), new Principles(10, 30, 30)),
                new Calcine("seeds", "Seeds", Ingredient.of(Tags.Items.SEEDS), new Principles(10, 30, 30)),
                new Calcine("coal", "Coal", Ingredient.of(ItemTags.COALS), new Principles(0, 200, 0)),
                new Calcine("raw_materials", "Raw ore", Ingredient.of(Tags.Items.RAW_MATERIALS), new Principles(150, 0, 50)),
                new Calcine("redstone", "Redstone", Ingredient.of(Tags.Items.DUSTS_REDSTONE), new Principles(0, 0, 100)),
                new Calcine("bones", "Bones", Ingredient.of(Tags.Items.BONES), new Principles(100, 0, 0)));
    }

    /** What the Purifier leaches into Salt: the Ash of the Calciner. */
    public static List<Purify> purification() {
        return List.of(new Purify("ash", "Ash", Ingredient.of(ServusManufacturingItems.ASH.get()), 4, 400, 400,
                new ItemStack(ServusManufacturingItems.CAPUT_MORTUUM.get()), 160));
    }

    /** What the Coagulator makes from the principles; no metal in Nigredo. */
    public static List<Coagulate> coagulation() {
        return List.of(
                new Coagulate("salt_crystal", "Salt Crystal", new Principles(250, 0, 0), new ItemStack(ServusManufacturingItems.SALT_CRYSTAL.get())),
                new Coagulate("sulfur", "Sulfur", new Principles(0, 250, 0), new ItemStack(ServusManufacturingItems.SULFUR.get())),
                new Coagulate("quicksilver", "Quicksilver", new Principles(0, 0, 250), new ItemStack(ServusManufacturingItems.QUICKSILVER.get())),
                new Coagulate("clay_ball", "Clay Ball", new Principles(100, 0, 50), new ItemStack(Items.CLAY_BALL)),
                new Coagulate("flint", "Flint", new Principles(150, 0, 0), new ItemStack(Items.FLINT)),
                new Coagulate("gunpowder", "Gunpowder", new Principles(50, 150, 0), new ItemStack(Items.GUNPOWDER)),
                new Coagulate("redstone", "Redstone", new Principles(50, 0, 150), new ItemStack(Items.REDSTONE)),
                new Coagulate("glowstone_dust", "Glowstone Dust", new Principles(0, 100, 100), new ItemStack(Items.GLOWSTONE_DUST)),
                new Coagulate("bone_meal", "Bone Meal", new Principles(60, 20, 0), new ItemStack(Items.BONE_MEAL)),
                new Coagulate("slime_ball", "Slimeball", new Principles(0, 50, 150), new ItemStack(Items.SLIME_BALL)));
    }

    /** Crafts on the Athanor. */
    public static List<AthanorCraft> athanor() {
        return List.of(
                new AthanorCraft("salt_crystal_block", "Salt Crystal", "Salt Crystal Block", Ingredient.of(ServusManufacturingItems.SALT_CRYSTAL.get()),
                        new Principles(1000, 0, 0), 0, new ItemStack(ServusManufacturingItems.SALT_CRYSTAL_BLOCK.get()), AthanorRecipe.DEFAULT_TIME),
                new AthanorCraft("blaze_powder", "Gunpowder", "Blaze Powder", Ingredient.of(Items.GUNPOWDER),
                        new Principles(0, 200, 0), 0, new ItemStack(Items.BLAZE_POWDER), AthanorRecipe.DEFAULT_TIME),
                new AthanorCraft("experience_bottle", "Glass Bottle", "Bottle o' Enchanting", Ingredient.of(Items.GLASS_BOTTLE),
                        new Principles(50, 0, 100), 1, new ItemStack(Items.EXPERIENCE_BOTTLE), AthanorRecipe.DEFAULT_TIME),
                new AthanorCraft("elixir_of_sight", "Quicksilver", "Elixir of Sight", Ingredient.of(ServusManufacturingItems.QUICKSILVER.get()),
                        new Principles(100, 100, 250), 1, new ItemStack(ElementalSight.ELIXIR.get()), 200),
                new AthanorCraft("gnome_hollow", "Cracked Stone Bricks", "Gnome Hollow", Ingredient.of(Items.CRACKED_STONE_BRICKS),
                        new Principles(500, 0, 250), 1, new ItemStack(ServusManufacturingItems.GNOME_HOLLOW.get()), 200));
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
}
