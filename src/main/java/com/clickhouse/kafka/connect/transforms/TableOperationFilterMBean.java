package com.clickhouse.kafka.connect.transforms;

public interface TableOperationFilterMBean {
    long getProcessedRecords();
    long getFilteredRecords();
}
