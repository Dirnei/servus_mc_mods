package io.github.dirnei.mc.servus.manufacturing.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.research.ResearchNodeRef;
import com.klikli_dev.modonomicon.api.datagen.research.SingleResearchSubProvider;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import net.minecraft.resources.Identifier;

/**
 * The journal's research: Modonomicon 2 unlocks entries by research nodes. The Villach node unlocks
 * when the team's Villach chapter advancement is granted, which opens the Schwaz chapter and the
 * Albedo workshop entries.
 */
public final class JournalResearch extends SingleResearchSubProvider {

    public static final ResearchNodeRef VILLACH =
            ResearchNodeRef.of(Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "journal/villach"));

    public JournalResearch() {
        super("journal", ServusManufacturingIds.MOD_ID);
    }

    @Override
    protected void generateResearch() {
        var villachFinished = ingress()
                .onAdvancementEarned(Journal.advancementId(ServusManufacturingIds.MOD_ID + ":villach"))
                .declareFact("journal/villach_finished");
        node(VILLACH, villachFinished);
        researchNodeName(VILLACH, "The Villach chapter");
    }
}
