package io.github.dirnei.mc.servus.alchemy.journal;

import com.klikli_dev.modonomicon.book.conditions.BookCondition;
import com.klikli_dev.modonomicon.book.conditions.BookNoneCondition;
import com.klikli_dev.modonomicon.book.page.BookPage;
import com.klikli_dev.modonomicon.data.BookPageType;
import com.klikli_dev.modonomicon.registry.BookPageTypeRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/** A journal book page that lists a chapter's tasks with the reader's team progress. */
public class JournalTasksPage extends BookPage {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "journal_tasks");

    public static final MapCodec<JournalTasksPage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(BookPage::getId),
                    BookCondition.CODEC.optionalFieldOf("condition", new BookNoneCondition()).forGetter(BookPage::getCondition),
                    Codec.STRING.fieldOf("chapter").forGetter(JournalTasksPage::chapter))
            .apply(instance, JournalTasksPage::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, JournalTasksPage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BookPage::getId,
            BookCondition.STREAM_CODEC, BookPage::getCondition,
            ByteBufCodecs.STRING_UTF8, JournalTasksPage::chapter,
            JournalTasksPage::new);

    private static BookPageType<JournalTasksPage> type;

    private final String chapter;

    public JournalTasksPage(String id, BookCondition condition, String chapter) {
        super(id, condition, List.of());
        this.chapter = chapter;
    }

    /** Registers the page type with Modonomicon; call while the mod constructs. */
    public static void register() {
        type = BookPageTypeRegistry.register(ID, CODEC, STREAM_CODEC);
    }

    /** The namespaced id of the chapter whose tasks the page lists. */
    public String chapter() {
        return chapter;
    }

    @Override
    public BookPageType<?> type() {
        return type;
    }

    @Override
    public boolean matchesQuery(String query, Level level) {
        return false;
    }
}
