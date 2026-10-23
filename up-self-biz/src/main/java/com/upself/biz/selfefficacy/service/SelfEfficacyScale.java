package com.upself.biz.selfefficacy.service;

import com.upself.biz.selfefficacy.dto.SelfEfficacyViews.Instrument;
import com.upself.common.exception.ServiceException;
import java.math.*;
import java.util.*;

/** GSE-3 English content: Doll et al. (2021), https://doi.org/10.6102/zis294.
 * Scale content licensed CC BY-NC-SA 4.0; wording and scoring are unchanged.
 * UP-SELF changes presentation to one statement per screen, with no timed completion.
 */
public final class SelfEfficacyScale {
    public static final String VERSION = "GSE-3-EN-2021-v1";
    private SelfEfficacyScale() {}

    public static Instrument instrument() {
        Instrument i = new Instrument();
        i.setVersion(VERSION);
        i.setName("General Self-Efficacy Short Scale-3 (GSE-3)");
        i.setInstruction("The following statements may apply more or less to you. To what extent do you think each statement applies to you personally?");
        i.setQuestions(Arrays.asList(
            "I can rely on my own abilities in difficult situations.",
            "I am able to solve most problems on my own.",
            "I can usually solve even challenging and complex tasks well."));
        i.setResponses(Arrays.asList("does not apply at all", "applies a bit", "applies somewhat", "applies mostly", "applies completely"));
        i.setSourceUrl("https://doi.org/10.6102/zis294");
        i.setAttribution("Doll, E. S., Nießen, D., Schmidt, I., Rammstedt, B., & Lechner, C. M. (2021).");
        i.setLicense("CC BY-NC-SA 4.0");
        i.setLicenseUrl("https://creativecommons.org/licenses/by-nc-sa/4.0/");
        return i;
    }

    public static BigDecimal score(List<? extends Number> answers) {
        if (answers == null || answers.size() != 3 || answers.stream().anyMatch(v -> !valid(v))) {
            throw new ServiceException(400, "Choose one of the five options for all three statements.");
        }
        return BigDecimal.valueOf(answers.stream().mapToInt(Number::intValue).sum()).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
    }
    private static boolean valid(Number value) {
        if (value == null) return false;
        try {
            BigDecimal v = new BigDecimal(value.toString());
            return v.compareTo(BigDecimal.ONE) >= 0 && v.compareTo(BigDecimal.valueOf(5)) <= 0 && v.stripTrailingZeros().scale() <= 0;
        } catch (NumberFormatException ex) { return false; }
    }
}
