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

package org.apache.shardingsphere.core.parse.sql.statement.dml;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.SelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.StarSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.table.Table;
import org.apache.shardingsphere.core.parse.sql.segment.dml.pagination.PaginationValueSegment;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Select statement.
 *
 * @author zhangliang
 * @author panjuan
 */
@Getter
@Setter
@ToString(callSuper = true, exclude = "parentStatement")
public final class SelectStatement extends DMLStatement {
    
    private final Set<SelectItem> items = new LinkedHashSet<>();
    
    private boolean containStar;
    
    private boolean containsSubquery;
    
    private int selectListStopIndex;
    
    private PaginationValueSegment offset;
    
    private PaginationValueSegment rowCount;
    
    private SelectStatement parentStatement;
    
    /**
     * Judge has unqualified star select item.
     * 
     * @return star select item without owner
     */
    public boolean hasUnqualifiedStarSelectItem() {
        for (SelectItem each : items) {
            if (each instanceof StarSelectItem && !((StarSelectItem) each).getOwner().isPresent()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get qualified star select items.
     *
     * @return qualified star select items
     */
    public Collection<StarSelectItem> getQualifiedStarSelectItems() {
        Collection<StarSelectItem> result = new LinkedList<>();
        for (SelectItem each : items) {
            if (each instanceof StarSelectItem && ((StarSelectItem) each).getOwner().isPresent()) {
                result.add((StarSelectItem) each);
            }
        }
        return result;
    }
    
    /**
     * Find star select item via table name or alias.
     *
     * @param tableNameOrAlias table name or alias
     * @return star select item via table name or alias
     */
    public Optional<StarSelectItem> findStarSelectItem(final String tableNameOrAlias) {
        Optional<Table> table = getTables().find(tableNameOrAlias);
        if (!table.isPresent()) {
            return Optional.absent();
        }
        for (SelectItem each : items) {
            if (!(each instanceof StarSelectItem)) {
                continue;
            }
            StarSelectItem starSelectItem = (StarSelectItem) each;
            if (starSelectItem.getOwner().isPresent() && getTables().find(starSelectItem.getOwner().get()).equals(table)) {
                return Optional.of(starSelectItem);
            }
        }
        return Optional.absent();
    }
}
