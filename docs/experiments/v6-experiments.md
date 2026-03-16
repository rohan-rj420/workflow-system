# V6 Experiments

This document records experimental validation of the V6 architecture.

------------------------------------------------------------------------

# Experiment 1 -- End-to-End Event Flow

Goal:

Verify that the new event-driven execution pipeline works correctly.

Steps:

1.  Start workflow engine
2.  Create workflow
3.  Observe outbox event creation
4.  Start dispatcher
5.  Observe external call and result processing

Observed logs confirmed:

-   event published
-   dispatcher invoked
-   external call executed
-   result stored
-   workflow completed

Database verification:

outbox_events.status → PROCESSED\
execution_results.processed → true\
steps.status → SUCCESS

------------------------------------------------------------------------

# Experiment 2 -- Dispatcher Crash Recovery

Goal:

Verify reliability of the outbox pattern.

Procedure:

1.  Stop dispatcher
2.  Generate workflow events
3.  Verify outbox events remain PENDING
4.  Restart dispatcher

Result:

Dispatcher processed pending events successfully.

No events were lost.

------------------------------------------------------------------------

# Experiment 3 -- Concurrent Dispatchers

Goal:

Validate horizontal scaling.

Two dispatcher instances were started.

Logs showed:

pool-2-thread-1 ... pool-2-thread-10

on both instances.

Meaning:

10 threads per dispatcher\
2 dispatchers → 20 concurrent executions.

------------------------------------------------------------------------

# Database Observations

Snapshots during load:

PROCESSING 17\
PROCESSING 32\
PROCESSING 56\
PROCESSING 78\
PROCESSING 96

Explanation:

Events are claimed in batches before execution threads start.

PROCESSING therefore represents:

claimed events not strictly running threads.

------------------------------------------------------------------------

# Experiment 4 -- Burst Load Test

k6 load test triggered many workflows.

Observations:

Dispatcher thread pools limited concurrent execution.

System remained stable.

Events gradually drained from:

PENDING → PROCESSING → PROCESSED.

------------------------------------------------------------------------

# Observed Issues

## Late Arriving Results

When steps were moved to DLQ, some results arrived later.

This produced:

NoSuchElementException.

Fix:

Result processor now ignores results for deleted steps.

------------------------------------------------------------------------

## Duplicate Dispatch Attempts

Multiple dispatchers attempted dispatching same event.

External simulator idempotency prevented duplicate execution.

HTTP 409 responses confirmed deduplication.

------------------------------------------------------------------------

# Key Results

The experiments confirm:

-   Outbox pattern reliability
-   Horizontal dispatcher scaling
-   Safe concurrent execution
-   Idempotent external calls
-   Backpressure via thread pools

The V6 architecture successfully decouples workflow orchestration from
execution.
