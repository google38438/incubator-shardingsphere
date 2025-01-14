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

package org.apache.shardingsphere.core.optimize.statement.sharding.dml.select.item;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.constant.AggregationType;
import org.apache.shardingsphere.core.metadata.table.ShardingTableMetaData;
import org.apache.shardingsphere.core.optimize.statement.sharding.dml.select.groupby.GroupBy;
import org.apache.shardingsphere.core.optimize.statement.sharding.dml.select.orderby.OrderBy;
import org.apache.shardingsphere.core.optimize.statement.sharding.dml.select.orderby.OrderByItem;
import org.apache.shardingsphere.core.parse.constant.DerivedColumn;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.AggregationDistinctSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.AggregationSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.DerivedCommonSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.DistinctSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.SelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.StarSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.table.Table;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.ColumnOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.IndexOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.OrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.TextOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.statement.dml.SelectStatement;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

/**
 * Select items engine.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class SelectItemsEngine {
    
    private final ShardingTableMetaData shardingTableMetaData;
    
    /**
     * Create select items.
     * 
     * @param selectStatement SQL statement
     * @param groupBy group by
     * @param orderBy order by
     * @return select items
     */
    public SelectItems createSelectItems(final SelectStatement selectStatement, final GroupBy groupBy, final OrderBy orderBy) {
        SelectItems result = new SelectItems();
        Set<SelectItem> items = selectStatement.getItems();
        items.addAll(getDerivedColumns(selectStatement, groupBy.getItems(), orderBy.getItems()));
        result.getItems().addAll(appendAverageDerivedColumns(items));
        result.setContainStar(selectStatement.isContainStar());
        result.setSelectListStopIndex(selectStatement.getSelectListStopIndex());
        return result;
    }
    
    private Collection<SelectItem> appendAverageDerivedColumns(final Collection<SelectItem> items) {
        Collection<SelectItem> result = new LinkedList<>(items);
        int derivedColumnOffset = 0;
        for (SelectItem each : items) {
            if (each instanceof AggregationSelectItem && AggregationType.AVG == ((AggregationSelectItem) each).getType()) {
                appendAverageDerivedColumns(derivedColumnOffset, each);
                // TODO replace avg to constant, avoid calculate useless avg
                derivedColumnOffset++;
            }
        }
        return result;
    }
    
    private void appendAverageDerivedColumns(final int derivedColumnOffset, final SelectItem selectItem) {
        if (selectItem instanceof AggregationDistinctSelectItem) {
            appendDerivedAggregationDistinctSelectItems((AggregationDistinctSelectItem) selectItem, derivedColumnOffset);
        } else {
            appendDerivedAggregationSelectItems((AggregationSelectItem) selectItem, derivedColumnOffset);
        }
    }
    
    private void appendDerivedAggregationDistinctSelectItems(final AggregationDistinctSelectItem averageDistinctSelectItem, final int derivedColumnOffset) {
        String countAlias = DerivedColumn.AVG_COUNT_ALIAS.getDerivedColumnAlias(derivedColumnOffset);
        AggregationDistinctSelectItem countDistinctSelectItem = new AggregationDistinctSelectItem(
                AggregationType.COUNT, averageDistinctSelectItem.getInnerExpression(), Optional.of(countAlias), averageDistinctSelectItem.getDistinctColumnName());
        String sumAlias = DerivedColumn.AVG_SUM_ALIAS.getDerivedColumnAlias(derivedColumnOffset);
        AggregationDistinctSelectItem sumDistinctSelectItem = new AggregationDistinctSelectItem(
                AggregationType.SUM, averageDistinctSelectItem.getInnerExpression(), Optional.of(sumAlias), averageDistinctSelectItem.getDistinctColumnName());
        averageDistinctSelectItem.getDerivedAggregationSelectItems().clear();
        averageDistinctSelectItem.getDerivedAggregationSelectItems().add(countDistinctSelectItem);
        averageDistinctSelectItem.getDerivedAggregationSelectItems().add(sumDistinctSelectItem);
    }
    
    private void appendDerivedAggregationSelectItems(final AggregationSelectItem averageSelectItem, final int derivedColumnOffset) {
        String countAlias = DerivedColumn.AVG_COUNT_ALIAS.getDerivedColumnAlias(derivedColumnOffset);
        AggregationSelectItem countSelectItem = new AggregationSelectItem(AggregationType.COUNT, averageSelectItem.getInnerExpression(), Optional.of(countAlias));
        String sumAlias = DerivedColumn.AVG_SUM_ALIAS.getDerivedColumnAlias(derivedColumnOffset);
        AggregationSelectItem sumSelectItem = new AggregationSelectItem(AggregationType.SUM, averageSelectItem.getInnerExpression(), Optional.of(sumAlias));
        averageSelectItem.getDerivedAggregationSelectItems().clear();
        averageSelectItem.getDerivedAggregationSelectItems().add(countSelectItem);
        averageSelectItem.getDerivedAggregationSelectItems().add(sumSelectItem);
    }
    
    private Collection<SelectItem> getDerivedColumns(final SelectStatement selectStatement, final Collection<OrderByItem> groupByItems, final Collection<OrderByItem> orderByItems) {
        Collection<SelectItem> result = new LinkedList<>();
        if (!groupByItems.isEmpty()) {
            result.addAll(appendDerivedGroupColumns(selectStatement, groupByItems));
        }
        if (!orderByItems.isEmpty()) {
            result.addAll(appendDerivedOrderColumns(selectStatement, orderByItems));
        }
        return result;
    }
    
    private Collection<SelectItem> appendDerivedOrderColumns(final SelectStatement selectStatement, final Collection<OrderByItem> orderItems) {
        Collection<SelectItem> result = new LinkedList<>();
        int derivedColumnOffset = 0;
        for (OrderByItem each : orderItems) {
            if (!containsItem(selectStatement, each.getSegment())) {
                String alias = DerivedColumn.ORDER_BY_ALIAS.getDerivedColumnAlias(derivedColumnOffset++);
                result.add(new DerivedCommonSelectItem(((TextOrderByItemSegment) each.getSegment()).getText(), Optional.of(alias)));
            }
        }
        return result;
    }
    
    private Collection<SelectItem> appendDerivedGroupColumns(final SelectStatement selectStatement, final Collection<OrderByItem> orderByItems) {
        Collection<SelectItem> result = new LinkedList<>();
        int derivedColumnOffset = 0;
        for (OrderByItem each : orderByItems) {
            if (!containsItem(selectStatement, each.getSegment())) {
                String alias = DerivedColumn.GROUP_BY_ALIAS.getDerivedColumnAlias(derivedColumnOffset++);
                result.add(new DerivedCommonSelectItem(((TextOrderByItemSegment) each.getSegment()).getText(), Optional.of(alias)));
            }
        }
        return result;
    }
    
    private boolean containsItem(final SelectStatement selectStatement, final OrderByItemSegment orderByItemSegment) {
        return orderByItemSegment instanceof IndexOrderByItemSegment
                || containsItemInStarSelectItems(selectStatement, orderByItemSegment) || containsItemInSelectItems(selectStatement, orderByItemSegment);
    }
    
    private boolean containsItemInStarSelectItems(final SelectStatement selectStatement, final OrderByItemSegment orderByItemSegment) {
        return selectStatement.hasUnqualifiedStarSelectItem()
                || containsItemWithOwnerInStarSelectItems(selectStatement, orderByItemSegment) || containsItemWithoutOwnerInStarSelectItems(selectStatement, orderByItemSegment);
    }
    
    private boolean containsItemWithOwnerInStarSelectItems(final SelectStatement selectStatement, final OrderByItemSegment orderItem) {
        return orderItem instanceof ColumnOrderByItemSegment && ((ColumnOrderByItemSegment) orderItem).getColumn().getOwner().isPresent()
                && selectStatement.findStarSelectItem(((ColumnOrderByItemSegment) orderItem).getColumn().getOwner().get().getName()).isPresent();
    }
    
    private boolean containsItemWithoutOwnerInStarSelectItems(final SelectStatement selectStatement, final OrderByItemSegment orderItem) {
        if (!(orderItem instanceof ColumnOrderByItemSegment)) {
            return false;
        }
        if (!((ColumnOrderByItemSegment) orderItem).getColumn().getOwner().isPresent()) {
            for (StarSelectItem each : selectStatement.getQualifiedStarSelectItems()) {
                if (isSameSelectItem(selectStatement, each, (ColumnOrderByItemSegment) orderItem)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isSameSelectItem(final SelectStatement selectStatement, final StarSelectItem starSelectItem, final ColumnOrderByItemSegment orderItem) {
        Preconditions.checkState(starSelectItem.getOwner().isPresent());
        Optional<Table> table = selectStatement.getTables().find(starSelectItem.getOwner().get());
        return table.isPresent() && shardingTableMetaData.containsColumn(table.get().getName(), orderItem.getColumn().getName());
    }
    
    private boolean containsItemInSelectItems(final SelectStatement selectStatement, final OrderByItemSegment orderItem) {
        for (SelectItem each : selectStatement.getItems()) {
            if (orderItem instanceof IndexOrderByItemSegment) {
                return true;
            }
            if (containsItemInDistinctItems(each, (TextOrderByItemSegment) orderItem)
                    || isSameAlias(each, (TextOrderByItemSegment) orderItem) || isSameQualifiedName(each, (TextOrderByItemSegment) orderItem)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsItemInDistinctItems(final SelectItem selectItem, final TextOrderByItemSegment orderItem) {
        return selectItem instanceof DistinctSelectItem && ((DistinctSelectItem) selectItem).getDistinctColumnNames().contains(orderItem.getText());
    }
    
    private boolean isSameAlias(final SelectItem selectItem, final TextOrderByItemSegment orderItem) {
        return selectItem.getAlias().isPresent() && (orderItem.getText().equalsIgnoreCase(selectItem.getAlias().get()) || orderItem.getText().equalsIgnoreCase(selectItem.getExpression()));
    }
    
    private boolean isSameQualifiedName(final SelectItem selectItem, final TextOrderByItemSegment orderItem) {
        return !selectItem.getAlias().isPresent() && selectItem.getExpression().equalsIgnoreCase(orderItem.getText());
    }
}
