/*
 * Copyright 2015 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distrib-
 * uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
 * specific language governing permissions and limitations under the License.
 */

package org.projectbuendia.openmrs.api.db.hibernate;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Order;

import java.io.Serializable;

/**
 * An {@link Order} that circumvents Hibernate's parameter-to-column name interpretation, such that
 * it's possible to directly order by an arbitrary SQL column or function.
 */
public class SqlOrder extends Order implements Serializable {
    private final String columnName;
    private final boolean ascending;

    protected SqlOrder(String columnName, boolean ascending) {
        // Use null values for the super constructor, we're assuming super methods never get called.
        super(null, false);

        assert columnName != null;
        this.columnName = columnName;
        this.ascending = ascending;
    }

    @Override
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
            throws HibernateException {
        return columnName + " " + (ascending ? "ASC" : "DESC");
    }

    /**
     * Returns a new {@link SqlOrder}, which orders by the provided {@code columnName} from smallest
     * to largest.
     */
    public static SqlOrder sqlAsc(String columnName) {
        return new SqlOrder(columnName, true /* ascending */);
    }

    /**
     * Returns a new {@link SqlOrder}, which orders by the provided {@code columnName} from largest
     * to smallest.
     */
    public static SqlOrder sqlDesc(String columnName) {
        assert columnName != null;
        return new SqlOrder(columnName, false /* ascending */);
    }
}
