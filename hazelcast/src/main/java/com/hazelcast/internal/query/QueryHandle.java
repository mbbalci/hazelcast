package com.hazelcast.internal.query;

import com.hazelcast.internal.query.io.Row;
import com.hazelcast.util.executor.CompletedFuture;

import java.util.List;
import java.util.function.Consumer;

public interface QueryHandle {
    /**
     * @return Unique query ID.
     */
    QueryId getQueryId();

    /**
     * Cancel the query.
     *
     * @param reason Why the query is cancelled.
     * @param errMsg Optional error message.
     * @return {@code True} if cancellation was requested by this call.
     */
    boolean cancel(QueryCancelReason reason, String errMsg);

    /**
     * Consume up to "maxRows" asynchronously.
     *
     * @param consumer Consumer.
     * @param maxRows Maximum number of rows.
     * @return Future completed when some rows are available.
     */
    CompletedFuture<Void> consume(Consumer<List<Row>> consumer, int maxRows);
}