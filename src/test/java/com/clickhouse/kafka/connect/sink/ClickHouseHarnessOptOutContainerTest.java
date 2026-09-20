package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.clickhouse.ClickHouseContainer;

/**
 * [D-02, HARN-03] New fork-only class that deliberately opts out of the shared container in order
 * to exercise the opt-out seam itself, rather than being exercised by it (as the two Toxiproxy
 * classes are). It never asserts an absolute {@code sharedContainerStarts()} value independent of
 * whether the shared container exists in this fork - doing so would be the very order-dependence
 * this class exists to pin against; instead it states the counter rule in a form that holds under
 * every fork assignment. It inherits {@code @Tag("clickhouse")} from {@link ClickHouseBase}, which
 * keeps it out of the Docker-free {@code testUnit} gate.
 */
public class ClickHouseHarnessOptOutContainerTest extends ClickHouseBase {

    @Override
    protected boolean usesSharedContainer() {
        return false;
    }

    @Test
    public void optOutClassGetsItsOwnContainerAndDoesNotMoveTheSharedCounter() {
        Assumptions.assumeFalse(isCloud, "No container to inspect against ClickHouse Cloud");
        Assumptions.assumeFalse(isCluster, "No container to inspect against a ClickHouse cluster");

        ClickHouseContainer ownContainer = getDb();
        assertNotNull(ownContainer, "Opting out must still produce a working container for this class");

        assertNotSame(
                sharedContainer(),
                ownContainer,
                "Opting out must produce a container distinct from the shared one, whether or not a "
                        + "sibling class in this fork has already started the shared container");

        int expectedStarts = sharedContainer() != null ? 1 : 0;
        assertEquals(
                expectedStarts,
                sharedContainerStarts(),
                "A non-matching value means the opt-out branch is incrementing the shared-start counter, "
                        + "which would make the harness canary's shared-container assertion "
                        + "depend on which classes Gradle put in this fork");
    }
}
