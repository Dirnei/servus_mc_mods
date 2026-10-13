package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The journal's advancements: a root and one per shipped chapter, each with an impossible criterion that only
 * the journal awards when the team finishes the chapter. Modonomicon unlocks entries on these.
 */
final class JournalAdvancements implements AdvancementSubProvider {

    /** A shipped chapter: path, icon. In order; each chapter's parent advancement is the one before it. */
    record Chapter(String path, ItemLike icon) {
    }

    static final List<Chapter> CHAPTERS = List.of(
            new Chapter("villach", ServusManufacturingItems.CALCINER.get()),
            new Chapter("schwaz", Items.IRON_PICKAXE));

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
        AdvancementHolder parent = Advancement.Builder.advancement()
                .display(Items.WRITABLE_BOOK,
                        Component.translatable("advancement.servus_manufacturing.journal.root"),
                        Component.translatable("advancement.servus_manufacturing.journal.root.description"),
                        Identifier.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                        AdvancementType.TASK, false, false, false)
                .addCriterion(Journal.CRITERION, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .save(saver, ServusManufacturingIds.MOD_ID + ":journal/root");
        for (Chapter chapter : CHAPTERS) {
            String key = "journal." + ServusManufacturingIds.MOD_ID + "." + chapter.path();
            parent = Advancement.Builder.advancement()
                    .parent(parent)
                    .display(chapter.icon(),
                            Component.translatable(key),
                            Component.translatable(key + ".finished"),
                            null, AdvancementType.GOAL, true, true, false)
                    .addCriterion(Journal.CRITERION, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(saver, Journal.advancementId(ServusManufacturingIds.MOD_ID + ":" + chapter.path()).toString());
        }
    }
}
