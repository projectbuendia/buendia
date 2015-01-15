package org.openmrs.projectbuendia;

import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Obs;
import org.openmrs.hl7.HL7Constants;

/**
 * Common place for handling the logic in handling observation data of different types
 */
public class VisitObsValue {

    /**
     * Visitor design pattern interface for observations values
     * @param <T>
     */
    public interface ObsValueVisitor<T> {
        /**
         * Visit a coded (Concept) value
         */
        public T visitCoded(Concept value);

        /**
         * Visit a numeric value.
         */
        public T visitNumeric(Double value);

        /**
         * Visit a boolean value.
         */
        public T visitBoolean(Boolean value);

        /**
         * Visit a text value.
         */
        public T visitText(String value);
    }

    /**
     * Use a visitor on an observation (we can't retro fit to Obs).
     */
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
            default:
                throw new IllegalArgumentException("Unexpected HL7 type: " + hl7Type + " for concept " + concept);
        }
    }
}
