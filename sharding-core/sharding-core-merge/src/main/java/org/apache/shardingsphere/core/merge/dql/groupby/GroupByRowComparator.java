/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.merge.dql.groupby;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.merge.dql.common.MemoryQueryResultRow;
import org.apache.shardingsphere.core.merge.dql.orderby.CompareUtil;
import org.apache.shardingsphere.core.optimize.statement.sharding.dml.select.ShardingSelectOptimizedStatement;
import org.apache.shardingsphere.core.optimize.statement.sharding.dml.select.orderby.OrderByItem;

import java.util.Collection;
import java.util.Comparator;

/**
 * Group by row comparator.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class GroupByRowComparator implements Comparator<MemoryQueryResultRow> {
    
    private final ShardingSelectOptimizedStatement optimizedStatement;
    
    @Override
    public int compare(final MemoryQueryResultRow o1, final MemoryQueryResultRow o2) {
        if (!optimizedStatement.getOrderBy().getItems().isEmpty()) {
            return compare(o1, o2, optimizedStatement.getOrderBy().getItems());
        }
        return compare(o1, o2, optimizedStatement.getGroupBy().getItems());
    }
    
    private int compare(final MemoryQueryResultRow o1, final MemoryQueryResultRow o2, final Collection<OrderByItem> orderByItems) {
        for (OrderByItem each : orderByItems) {
            Object orderValue1 = o1.getCell(each.getIndex());
            Preconditions.checkState(null == orderValue1 || orderValue1 instanceof Comparable, "Order by value must implements Comparable");
            Object orderValue2 = o2.getCell(each.getIndex());
            Preconditions.checkState(null == orderValue2 || orderValue2 instanceof Comparable, "Order by value must implements Comparable");
            int result = CompareUtil.compareTo((Comparable) orderValue1, (Comparable) orderValue2, each.getSegment().getOrderDirection(), each.getSegment().getNullOrderDirection());
            if (0 != result) {
                return result;
            }
        }
        return 0;
    }
}
