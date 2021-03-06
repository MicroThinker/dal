package com.ctrip.framework.dal.cluster.client.cluster;

import com.ctrip.framework.dal.cluster.client.base.Lifecycle;
import com.ctrip.framework.dal.cluster.client.exception.ClusterRuntimeException;
import com.ctrip.framework.dal.cluster.client.sharding.context.DbShardContext;
import com.ctrip.framework.dal.cluster.client.sharding.context.TableShardContext;
import com.ctrip.framework.dal.cluster.client.sharding.strategy.ShardStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author c7ch23en
 */
public class ShardStrategyProxy implements ShardStrategy, Lifecycle {

    private ShardStrategy defaultStrategy;
    private Map<String, ShardStrategy> tableStrategies = new HashMap<>();

    public ShardStrategyProxy(ShardStrategy defaultStrategy) {
        addStrategy(defaultStrategy);
        this.defaultStrategy = defaultStrategy;
    }

    @Override
    public Integer getDbShard(String tableName, DbShardContext context) {
        return getAndCheckTableStrategy(tableName).getDbShard(tableName, context);
    }

    @Override
    public boolean tableShardingEnabled(String tableName) {
        ShardStrategy strategy = getTableStrategy(tableName);
        return strategy != null && strategy.tableShardingEnabled(tableName);
    }

    @Override
    public String getTableShard(String tableName, TableShardContext context) {
        return getAndCheckTableStrategy(tableName).getTableShard(tableName, context);
    }

    @Override
    public Set<String> getAllTableShards(String tableName) {
        return getAndCheckTableStrategy(tableName).getAllTableShards(tableName);
    }

    @Override
    public String getTableShardSeparator(String tableName) {
        return getAndCheckTableStrategy(tableName).getTableShardSeparator(tableName);
    }

    @Override
    public Set<String> getAppliedTables() {
        throw new UnsupportedOperationException("unsupported operation for shard strategy proxy");
    }

    private ShardStrategy getTableStrategy(String tableName) {
        if (tableName == null)
            throw new ClusterRuntimeException("table name is necessary");
        ShardStrategy strategy = tableStrategies.get(tableName);
        if (strategy == null)
            strategy = defaultStrategy;
        return strategy;
    }

    private ShardStrategy getAndCheckTableStrategy(String tableName) {
        ShardStrategy strategy = getTableStrategy(tableName);
        if (strategy == null)
            throw new ClusterRuntimeException(String.format("shard strategy not found for table '%s'", tableName));
        return strategy;
    }

    public void addStrategy(ShardStrategy strategy) {
        if (strategy == null || strategy == defaultStrategy)
            return;
        for (String tableName : strategy.getAppliedTables()) {
            if (tableStrategies.put(tableName, strategy) != null)
                throw new ClusterRuntimeException(String.format("multiple shard strategies defined for table '%s'", tableName));
        }
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

}
