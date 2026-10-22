package com.oversight.issuance.event;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory stand-in for the outbox table.
 *
 * In Phase 4 this is a real table, and the service inserts into it in the SAME database
 * transaction as the issuance update, so the change and its event are saved together or not
 * at all. That is what stops events from being lost when the app crashes between the two.
 */
public class Outbox {

    private final List<Entry> entries = new ArrayList<>();

    public synchronized void add(IssuanceEvent event) {
        entries.add(new Entry(event));
    }

    /** Oldest-first unsent events, like SELECT ... WHERE sent_at IS NULL ORDER BY id. */
    public synchronized List<Entry> unsent(int limit) {
        return entries.stream().filter(e -> !e.sent).limit(limit).toList();
    }

    public synchronized void markSent(Entry entry) {
        entry.sent = true;
    }

    public synchronized int size() {
        return entries.size();
    }

    public static final class Entry {
        private final IssuanceEvent event;
        private boolean sent;

        private Entry(IssuanceEvent event) {
            this.event = event;
        }

        public IssuanceEvent event() {
            return event;
        }
    }
}
