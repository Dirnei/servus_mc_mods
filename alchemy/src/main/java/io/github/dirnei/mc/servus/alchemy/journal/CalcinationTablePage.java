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

/**
 * One page of the journal's calcination table. The rows come from the recipes the client knows at the time it
 * draws; the page only says which part of the laid out table it shows and whether it is the last.
 */
public class CalcinationTablePage extends BookPage {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "calcination_table");

    public static final MapCodec<CalcinationTablePage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(BookPage::getId),
                    BookCondition.CODEC.optionalFieldOf("condition", new BookNoneCondition()).forGetter(BookPage::getCondition),
                    Codec.INT.fieldOf("part").forGetter(CalcinationTablePage::part),
                    Codec.BOOL.fieldOf("last").forGetter(CalcinationTablePage::last))
            .apply(instance, CalcinationTablePage::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CalcinationTablePage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BookPage::getId,
            BookCondition.STREAM_CODEC, BookPage::getCondition,
            ByteBufCodecs.VAR_INT, CalcinationTablePage::part,
            ByteBufCodecs.BOOL, CalcinationTablePage::last,
            CalcinationTablePage::new);

    private static BookPageType<CalcinationTablePage> type;

    private final int part;
    private final boolean last;

    public CalcinationTablePage(String id, BookCondition condition, int part, boolean last) {
        super(id, condition, List.of());
        this.part = part;
        this.last = last;
    }

    /** Registers the page type with Modonomicon; call while the mod constructs. */
    public static void register() {
        type = BookPageTypeRegistry.register(ID, CODEC, STREAM_CODEC);
    }

    /** Which page of the table this is, from 0; the first carries the title. */
    public int part() {
        return part;
    }

    /** Whether this is the table's last page, which notes the rows that did not fit. */
    public boolean last() {
        return last;
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
