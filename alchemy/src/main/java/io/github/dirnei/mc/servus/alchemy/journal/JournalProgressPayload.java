package io.github.dirnei.mc.servus.alchemy.journal;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.journal.JournalProgress.ChapterProgress;
import io.github.dirnei.mc.servus.alchemy.journal.JournalProgress.TaskProgress;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: the reader's team journal progress, every open or finished chapter with its task counts.
 * Replaces what the client had.
 */
public record JournalProgressPayload(List<ChapterProgress> chapters) implements CustomPacketPayload {

    public static final Type<JournalProgressPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "journal_progress"));

    private static final StreamCodec<FriendlyByteBuf, TaskProgress> TASK = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TaskProgress::id,
            ByteBufCodecs.VAR_INT, TaskProgress::count,
            ByteBufCodecs.VAR_INT, TaskProgress::needed,
            TaskProgress::new);

    private static final StreamCodec<FriendlyByteBuf, ChapterProgress> CHAPTER = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChapterProgress::id,
            ByteBufCodecs.BOOL, ChapterProgress::finished,
            TASK.apply(ByteBufCodecs.list()), ChapterProgress::tasks,
            ChapterProgress::new);

    public static final StreamCodec<FriendlyByteBuf, JournalProgressPayload> STREAM_CODEC =
            CHAPTER.apply(ByteBufCodecs.list()).map(JournalProgressPayload::new, JournalProgressPayload::chapters);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
