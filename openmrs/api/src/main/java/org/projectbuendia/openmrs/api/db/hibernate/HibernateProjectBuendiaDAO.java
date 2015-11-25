// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.openmrs.api.db.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Disjunction;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.projectbuendia.openmrs.api.SyncToken;
import org.projectbuendia.openmrs.api.db.ProjectBuendiaDAO;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

import static org.hibernate.criterion.Order.asc;
import static org.hibernate.criterion.Restrictions.disjunction;
import static org.hibernate.criterion.Restrictions.eq;
import static org.hibernate.criterion.Restrictions.ge;
import static org.hibernate.criterion.Restrictions.sqlRestriction;
import static org.projectbuendia.openmrs.api.db.hibernate.SqlOrder.sqlAsc;

/** Default implementation of {@link ProjectBuendiaDAO}. */
public class HibernateProjectBuendiaDAO implements ProjectBuendiaDAO {
    protected final Log log = LogFactory.getLog(this.getClass());

    private SessionFactory sessionFactory;

    /** @return the sessionFactory */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /** @param sessionFactory the sessionFactory to set */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Obs> getObservationsModifiedAtOrAfter(@Nullable Date date, boolean includeVoided) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Obs.class);
        if (!includeVoided) {
            criteria.add(eq("voided", false));
        }

        if (date != null) {
            Disjunction orClause = disjunction();
            orClause.add(ge("dateCreated", date));

            if (includeVoided) {
                orClause.add(ge("dateVoided", date));
            }
            criteria.add(orClause);
        }
        //noinspection unchecked
        return criteria.list();
    }

    @Override
    public List<Patient> getPatientsModifiedAfter(
            SyncToken syncToken, boolean includeVoided, int maxResults) {
        /*
        This is a horrible, horrible, set of workarounds. Here's what's going on:
        - The buendia_date_updated column is a column that we've bolted on to the `person` table.
          The value gets updated by a trigger on insert and update into the `person` table, to the
          latest time that the record was modified, which is a simpler and more reliable version of
          personDateCreated, personDateChanged, and personDateVoided.
        - We do this instead of querying `GREATEST(personDateCreated, personDateChanged,
          personDateVoided)` because you can't create indexes on functions in MySQL [^1], thus
          adding this computation to every query means that MySQL would have to load every `patient`
          for every fetch.
        - We don't want to modify the underlying OpenMRS platform for the sake of our module, so
          we can't bind `buendia_date_updated` to the hibernate model for `Person`.
        - There's three ways of returning data from Hibernate - using HQL (Hibernate Query
          Language), Criteria, and SQL.
        - HQL works entirely with property names, and can't access raw DB fields, so we can't use
          it.
        - SQL doesn't work for our purposes because Patient inherits from Person, and the way
          Hibernate parses aliases in SQL queries prevents inherited tables from working correctly.
          See https://forum.hibernate.org/viewtopic.php?t=978008. Basically, Hibernate adds a
          numerical suffix to table names for superclasses and subclasses, which aren't able to be
          used by the alias syntax. The offending code is in
          org.hibernate.loader.custom.sql.SqlQueryParser#substituteBrackets(). This gives us a
          choice of either hard-coding a mangled table name, which could change if the underlying
          data model changes (bad), or using the strategy we've opted for here (less bad).
        - We use Criteria, which supports adding custom SQL restrictions, but doesn't support
          ordering by custom SQL fields natively.
        - We manage this by creating our own implementation of org.hibernate.criterion.Order, which
          overrides the #toSqlString() method to provide direct access to database columns without
          them being interpreted by Hibernate.

         [^1]: You can create indexes on generated columns in MySQL 5.7.5+, which is functionally
               equivalent, but the Buendia project won't be on 5.7.5+ at least until Debian is.
               Current MySQL version for Debian:
               http://distrowatch.com/table.php?distribution=debian
         */
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Patient.class);
        if (syncToken != null) {
            // (a, b) > (x, y) is equivalent to (a > x) OR ((a = x) AND (b > y)). See
            // http://dev.mysql.com/doc/refman/5.7/en/comparison-operators.html#operator_greater-than
            criteria.add(sqlRestriction(
                    "(buendia_date_updated, uuid) > (?, ?)",
                    new Object[]{syncToken.greaterThanOrEqualToTimestamp, syncToken.greaterThanUuid},
                    new Type[] {StandardBasicTypes.TIMESTAMP, StandardBasicTypes.STRING}));
        }

        if (!includeVoided) {
            criteria.add(eq("personVoided", false));
        }

        criteria.addOrder(sqlAsc("buendia_date_updated"))
                .addOrder(asc("uuid"));

        if (maxResults > 0) {
            criteria.setMaxResults(maxResults);
        }

        //noinspection unchecked
        return criteria.list();
    }

    @Override
    public List<Order> getOrdersModifiedAtOrAfter(@Nullable Date date, boolean includeVoided) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Order.class);
        if (!includeVoided) {
            criteria.add(eq("voided", false));
        }
        if (date != null) {
            Disjunction orClause = disjunction();
            orClause.add(ge("dateCreated", date));

            if (includeVoided) {
                orClause.add(ge("dateVoided", date));
            }
            criteria.add(orClause);
        }
        //noinspection unchecked
        return criteria.list();
    }
}