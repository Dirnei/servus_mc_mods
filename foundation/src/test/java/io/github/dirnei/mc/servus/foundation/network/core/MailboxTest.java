package io.github.dirnei.mc.servus.foundation.network.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

class MailboxTest {

    @Test
    void drainsInPostingOrder() {
        Mailbox<String> mailbox = new Mailbox<>();
        mailbox.post("a");
        mailbox.post("b");
        mailbox.post("c");
        List<String> seen = new ArrayList<>();

        int handled = mailbox.drain(seen::add);

        assertEquals(3, handled);
        assertEquals(List.of("a", "b", "c"), seen);
        assertTrue(mailbox.isEmpty());
    }

    @Test
    void eventsPostedDuringDrainWaitForNextDrain() {
        Mailbox<String> mailbox = new Mailbox<>();
        mailbox.post("a");
        List<String> seen = new ArrayList<>();

        mailbox.drain(event -> {
            seen.add(event);
            mailbox.post(event + "+");
        });

        assertEquals(List.of("a"), seen);
        assertEquals(1, mailbox.size());
        mailbox.drain(seen::add);
        assertEquals(List.of("a", "a+"), seen);
    }

    @Test
    void concurrentPostsAllArrive() throws InterruptedException {
        Mailbox<Integer> mailbox = new Mailbox<>();
        int threads = 8;
        int perThread = 500;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            int base = t * perThread;
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        mailbox.post(base + i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        done.await();

        List<Integer> seen = new ArrayList<>();
        mailbox.drain(seen::add);

        assertEquals(threads * perThread, seen.size());
        assertEquals(threads * perThread, seen.stream().distinct().count());
    }

    @Test
    void clearDropsEverything() {
        Mailbox<String> mailbox = new Mailbox<>();
        mailbox.post("a");
        mailbox.clear();
        assertTrue(mailbox.isEmpty());
        assertEquals(0, mailbox.drain(e -> { }));
    }
}
