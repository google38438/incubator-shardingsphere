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

package org.apache.shardingsphere.core.rewrite.token;

import com.google.common.base.Optional;
import org.apache.shardingsphere.core.optimize.statement.sharding.dml.condition.ShardingCondition;
import org.apache.shardingsphere.core.optimize.statement.sharding.dml.select.ShardingSelectOptimizedStatement;
import org.apache.shardingsphere.core.optimize.statement.sharding.dml.select.groupby.GroupBy;
import org.apache.shardingsphere.core.optimize.statement.sharding.dml.select.orderby.OrderBy;
import org.apache.shardingsphere.core.optimize.statement.sharding.dml.select.orderby.OrderByItem;
import org.apache.shardingsphere.core.parse.sql.context.condition.AndCondition;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.SelectItem;
import org.apache.shardingsphere.core.parse.sql.segment.dml.SelectItemsSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.item.AggregationDistinctSelectItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.item.SelectItemSegment;
import org.apache.shardingsphere.core.parse.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.core.rewrite.token.pojo.SQLToken;
import org.apache.shardingsphere.core.rewrite.token.pojo.SelectItemPrefixToken;
import org.apache.shardingsphere.core.rule.EncryptRule;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class SQLTokenGenerateEngineTest {
    
    private SQLTokenGenerateEngine shardingTokenGenerateEngine = new ShardingTokenGenerateEngine();
    
    private SQLTokenGenerateEngine baseTokenGenerateEngine = new BaseTokenGenerateEngine();
    
    private SQLTokenGenerateEngine encryptTokenGenerateEngine = new EncryptTokenGenerateEngine();
    
    private ShardingSelectOptimizedStatement optimizedStatement;
    
    @Before
    public void setUp() {
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.getSQLSegments().add(createSelectItemsSegment());
        optimizedStatement = new ShardingSelectOptimizedStatement(selectStatement, Collections.<ShardingCondition>emptyList(), new AndCondition(), Collections.<SelectItem>emptyList(), 
                new GroupBy(Collections.<OrderByItem>emptyList(), 1), new OrderBy(Collections.<OrderByItem>emptyList(), false), null);
    }
    
    private SelectItemsSegment createSelectItemsSegment() {
        SelectItemsSegment selectItemsSegment = mock(SelectItemsSegment.class);
        when(selectItemsSegment.getStartIndex()).thenReturn(1);
        when(selectItemsSegment.getSelectItems()).thenReturn(Collections.<SelectItemSegment>emptyList());
        AggregationDistinctSelectItemSegment distinctSelectItemSegment = mock(AggregationDistinctSelectItemSegment.class);
        when(distinctSelectItemSegment.getDistinctExpression()).thenReturn("COUNT(DISTINCT id)");
        when(distinctSelectItemSegment.getAlias()).thenReturn(Optional.of("c"));
        when(distinctSelectItemSegment.getStartIndex()).thenReturn(1);
        when(distinctSelectItemSegment.getStopIndex()).thenReturn(2);
        when(selectItemsSegment.findSelectItemSegments(AggregationDistinctSelectItemSegment.class)).thenReturn(Collections.singletonList(distinctSelectItemSegment));
        return selectItemsSegment;
    }
    
    @Test
    public void assertGenerateSQLTokensWithBaseTokenGenerateEngine() {
        List<SQLToken> actual = baseTokenGenerateEngine.generateSQLTokens(optimizedStatement, null, mock(ShardingRule.class), true);
        assertThat(actual.size(), is(0));
    }
    
    @Test
    public void assertGetSQLTokenGeneratorsWithShardingTokenGenerateEngineWithoutSingleRoute() {
        List<SQLToken> actual = shardingTokenGenerateEngine.generateSQLTokens(optimizedStatement, null, mock(ShardingRule.class), false);
        assertThat(actual.size(), is(2));
        assertThat(actual.get(0), CoreMatchers.<SQLToken>instanceOf(SelectItemPrefixToken.class));
    }
    
    @Test
    public void assertGetSQLTokenGeneratorsWithShardingTokenGenerateEngineWithSingleRoute() {
        List<SQLToken> actual = shardingTokenGenerateEngine.generateSQLTokens(optimizedStatement, null, mock(ShardingRule.class), true);
        assertThat(actual.size(), is(0));
    }
    
    @Test
    public void assertGenerateSQLTokensWithEncryptTokenGenerateEngine() {
        List<SQLToken> actual = encryptTokenGenerateEngine.generateSQLTokens(optimizedStatement, null, mock(EncryptRule.class), true);
        assertThat(actual.size(), is(0));
    }
}
