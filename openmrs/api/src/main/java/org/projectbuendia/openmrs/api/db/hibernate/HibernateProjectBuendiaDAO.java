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
import org.hibernate.criterion.Restrictions;
import org.openmrs.Obs;
import org.projectbuendia.openmrs.api.db.ProjectBuendiaDAO;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

import static org.hibernate.criterion.Restrictions.eq;
import static org.hibernate.criterion.Restrictions.ge;

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
            Disjunction orClause = Restrictions.disjunction();
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