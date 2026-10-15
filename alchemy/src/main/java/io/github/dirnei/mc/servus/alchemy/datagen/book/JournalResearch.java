package io.github.dirnei.mc.servus.alchemy.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.research.ResearchNodeRef;
import com.klikli_dev.modonomicon.api.datagen.research.SingleResearchSubProvider;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import net.minecraft.resources.Identifier;

/**
 * The journal's research: Modonomicon 2 unlocks entries by research nodes. Each node unlocks when the
 * team's chapter advancement is granted and opens the next chapter with its workshop entries: Villach
 * opens Schwaz and Albedo, Schwaz opens Wandering years and Citrinitas, Wandering years opens Basel and
 * Salzburg and Rubedo.
 */
public final class JournalResearch extends SingleResearchSubProvider {

    public static final ResearchNodeRef VILLACH = chapterNode("villach");
    public static final ResearchNodeRef SCHWAZ = chapterNode("schwaz");
    public static final ResearchNodeRef WANDERING_YEARS = chapterNode("wandering_years");

    public JournalResearch() {
        super("journal", ServusAlchemyIds.MOD_ID);
    }

    @Override
    protected void generateResearch() {
        chapter(VILLACH, "villach", "The Villach chapter");
        chapter(SCHWAZ, "schwaz", "The Schwaz chapter");
        chapter(WANDERING_YEARS, "wandering_years", "The Wandering years chapter");
    }

    private static ResearchNodeRef chapterNode(String chapter) {
        return ResearchNodeRef.of(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "journal/" + chapter));
    }

    /** A node that unlocks with the chapter's advancement. */
    private void chapter(ResearchNodeRef node, String chapter, String name) {
        var finished = ingress()
                .onAdvancementEarned(Journal.advancementId(ServusAlchemyIds.MOD_ID + ":" + chapter))
                .declareFact("journal/" + chapter + "_finished");
        node(node, finished);
        researchNodeName(node, name);
    }
}
