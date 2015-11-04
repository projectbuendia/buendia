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
import org.hibernate.criterion.Restrictions;
import org.openmrs.Encounter;
import org.projectbuendia.openmrs.api.db.ProjectBuendiaDAO;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

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
    public List<Encounter> getEncountersCreatedAtOrAfter(@Nullable Date date) {
        // NOTES:
        // - this code relies on the assumption that observations can't be modified independently of
        // encounters.
        // - This doesn't actually return encounters "modified on or after", it's currently
        // encounters "created on or after". This is ok for our purposes because we don't have
        // the ability to modify Encounters in Buendia.

        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Encounter.class);
        if (date != null) {
            criteria.add(Restrictions.ge("dateCreated", date));
        }
        //noinspection unchecked
        return criteria.list();
    }
}