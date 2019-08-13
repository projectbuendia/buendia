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

package org.openmrs.projectbuendia;

import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Obs;
import org.openmrs.hl7.HL7Constants;

import java.util.Date;

/** Visitor design pattern for observation values of various types. */
public class VisitObsValue {
    /**
     * Visitor design pattern interface for observation values.
     * @param <T>
     */
    public interface ObsValueVisitor<T> {
        /** Visits a coded (Concept) value. */
        public T visitCoded(Concept value);

        /** Visits a numeric value. */
        public T visitNumeric(Double value);

        /** Visits a boolean value. */
        public T visitBoolean(Boolean value);

        /** Visits a text value. */
        public T visitText(String value);

        /** Visits a date value. */
        public T visitDate(Date value);

        /** Visits a datetime value. */
        public T visitDatetime(Date value);
    }

    /** Applies a visitor to an observation (we can't retrofit to Obs). */
    public static <T> T visit(Obs obs, ObsValueVisitor<T> visitor) {
        Concept concept = obs.getConcept();
        ConceptDatatype dataType = concept.getDatatype();
        String hl7Type = dataType.getHl7Abbreviation();
        switch (hl7Type) {
            case HL7Constants.HL7_BOOLEAN:
                return visitor.visitBoolean(obs.getValueAsBoolean());
            case HL7Constants.HL7_CODED: // deliberate fall through
            case HL7Constants.HL7_CODED_WITH_EXCEPTIONS:
                return visitor.visitCoded(obs.getValueCoded());
            case HL7Constants.HL7_NUMERIC:
                return visitor.visitNumeric(obs.getValueNumeric());
            case HL7Constants.HL7_TEXT:
                return visitor.visitText(obs.getValueText());
            case HL7Constants.HL7_DATE:
                return visitor.visitDate(obs.getValueDate());
            case HL7Constants.HL7_DATETIME:
                return visitor.visitDatetime(obs.getValueDatetime());
            default:
                throw new IllegalArgumentException("Unexpected HL7 type: " + hl7Type + " for "
                    + "concept " + concept);
        }
    }
}
