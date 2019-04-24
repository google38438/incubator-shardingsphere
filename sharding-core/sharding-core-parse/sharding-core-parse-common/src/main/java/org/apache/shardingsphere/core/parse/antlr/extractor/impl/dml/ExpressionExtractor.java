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

package org.apache.shardingsphere.core.parse.antlr.extractor.impl.dml;

import com.google.common.base.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.shardingsphere.core.parse.antlr.extractor.api.OptionalSQLSegmentExtractor;
import org.apache.shardingsphere.core.parse.antlr.extractor.impl.dml.select.SubqueryExtractor;
import org.apache.shardingsphere.core.parse.antlr.extractor.util.ExtractorUtils;
import org.apache.shardingsphere.core.parse.antlr.extractor.util.RuleName;
import org.apache.shardingsphere.core.parse.antlr.sql.segment.dml.expr.CommonExpressionSegment;
import org.apache.shardingsphere.core.parse.antlr.sql.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.core.parse.antlr.sql.segment.dml.expr.LiteralExpressionSegment;
import org.apache.shardingsphere.core.util.NumberUtil;

import java.util.Map;

/**
 * Expression extractor.
 *
 * @author duhongjun
 */
public final class ExpressionExtractor implements OptionalSQLSegmentExtractor {
    
    @Override
    public Optional<? extends ExpressionSegment> extract(final ParserRuleContext expressionNode, final Map<ParserRuleContext, Integer> parameterMarkerIndexes) {
        Optional<ParserRuleContext> subqueryNode = ExtractorUtils.findFirstChildNode(expressionNode, RuleName.SUBQUERY);
        return subqueryNode.isPresent() ? new SubqueryExtractor().extract(subqueryNode.get(), parameterMarkerIndexes) : Optional.of(extractExpression(expressionNode, parameterMarkerIndexes));
    }
    
    private ExpressionSegment extractExpression(final ParserRuleContext expressionNode, final Map<ParserRuleContext, Integer> parameterMarkerIndexes) {
        if (ExtractorUtils.findFirstChildNode(expressionNode, RuleName.FUNCTION_CALL).isPresent()
                || ExtractorUtils.findFirstChildNodeNoneRecursive(expressionNode, RuleName.COLUMN_NAME).isPresent()) {
            return extractCommonExpressionSegment(expressionNode);
        }
        return extractLiteralExpressionSegment(expressionNode, parameterMarkerIndexes);
    }
    
    // TODO extract column name and value from expression
    private ExpressionSegment extractCommonExpressionSegment(final ParserRuleContext functionNode) {
        return new CommonExpressionSegment(functionNode.getStart().getStartIndex(), functionNode.getStop().getStopIndex());
    }
    
    /**
     * Extract literal expression segment.
     *
     * @param parameterMarkerIndexes parameter marker indexes
     * @param expressionNode expression node
     * @return common expression segment
     */
    public LiteralExpressionSegment extractLiteralExpressionSegment(final ParserRuleContext expressionNode, final Map<ParserRuleContext, Integer> parameterMarkerIndexes) {
        LiteralExpressionSegment result = new LiteralExpressionSegment(expressionNode.getStart().getStartIndex(), expressionNode.getStop().getStopIndex());
        Optional<ParserRuleContext> parameterMarkerNode = ExtractorUtils.findFirstChildNode(expressionNode, RuleName.PARAMETER_MARKER);
        if (parameterMarkerNode.isPresent()) {
            Integer index = parameterMarkerIndexes.get(parameterMarkerNode.get());
            result.setPlaceholderIndex(index);
            return result;
        }
        Optional<ParserRuleContext> bitExprNode = ExtractorUtils.findFirstChildNode(expressionNode, RuleName.BIT_EXPR);
        if (bitExprNode.isPresent() && 1 != bitExprNode.get().getChildCount()) {
            return result;
        }
        Optional<ParserRuleContext> numberNode = ExtractorUtils.findFirstChildNode(expressionNode, RuleName.NUMBER_LITERALS);
        if (numberNode.isPresent()) {
            result.setLiterals(NumberUtil.getExactlyNumber(numberNode.get().getText(), 10));
        }
        Optional<ParserRuleContext> stringNode = ExtractorUtils.findFirstChildNode(expressionNode, RuleName.STRING_LITERALS);
        if (stringNode.isPresent()) {
            String text = stringNode.get().getText();
            result.setLiterals(text.substring(1, text.length() - 1));
        }
        return result;
    }
}
