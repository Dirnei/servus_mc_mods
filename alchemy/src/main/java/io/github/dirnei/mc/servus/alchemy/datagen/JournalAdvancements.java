package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            new Chapter("villach", ServusAlchemyItems.CALCINER.get()),
            new Chapter("schwaz", Items.IRON_PICKAXE),
            new Chapter("wandering_years", Items.RAW_GOLD),
            new Chapter("basel_salzburg", Items.GHAST_TEAR));

    /** A side chapter: path, icon and the main chapter it hangs from. No main chapter waits for it. */
    record SideChapter(String path, ItemLike icon, String parent) {
    }

    static final List<SideChapter> SIDE_CHAPTERS = List.of(
            new SideChapter("old_gnome", ServusAlchemyItems.TINY_PICKAXE.get(), "schwaz"));

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
        AdvancementHolder parent = Advancement.Builder.advancement()
                .display(Items.WRITABLE_BOOK,
                        Component.translatable("advancement.servus_alchemy.journal.root"),
                        Component.translatable("advancement.servus_alchemy.journal.root.description"),
                        Identifier.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                        AdvancementType.TASK, false, false, false)
                .addCriterion(Journal.CRITERION, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .save(saver, ServusAlchemyIds.MOD_ID + ":journal/root");
        Map<String, AdvancementHolder> saved = new HashMap<>();
        for (Chapter chapter : CHAPTERS) {
            parent = chapter(parent, chapter.path(), chapter.icon(), saver);
            saved.put(chapter.path(), parent);
        }
        for (SideChapter side : SIDE_CHAPTERS) {
            chapter(saved.get(side.parent()), side.path(), side.icon(), saver);
        }
    }

    private static AdvancementHolder chapter(AdvancementHolder parent, String path, ItemLike icon, Consumer<AdvancementHolder> saver) {
        String key = "journal." + ServusAlchemyIds.MOD_ID + "." + path;
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(icon,
                        Component.translatable(key),
                        Component.translatable(key + ".finished"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion(Journal.CRITERION, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .save(saver, Journal.advancementId(ServusAlchemyIds.MOD_ID + ":" + path).toString());
    }
}
