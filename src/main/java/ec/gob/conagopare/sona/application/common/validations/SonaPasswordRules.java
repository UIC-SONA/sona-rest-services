package ec.gob.conagopare.sona.application.common.validations;

import org.passay.CharacterRule;
import org.passay.LengthRule;
import org.passay.Rule;

import java.util.List;
import java.util.function.Supplier;

import static org.passay.EnglishCharacterData.*;
import static org.passay.EnglishCharacterData.LowerCase;

public class SonaPasswordRules implements Supplier<List<? extends Rule>> {
    @Override
    public List<? extends Rule> get() {
        return List.of(
                new LengthRule(12, 30),
                new CharacterRule(Digit, 1),
                new CharacterRule(Special, 1),
                new CharacterRule(UpperCase, 1),
                new CharacterRule(LowerCase, 1)
        );
    }
}
