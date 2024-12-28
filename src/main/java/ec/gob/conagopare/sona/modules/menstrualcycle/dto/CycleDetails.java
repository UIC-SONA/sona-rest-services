package ec.gob.conagopare.sona.modules.menstrualcycle.dto;

import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class CycleDetails {

    /**
     * The duration of the period in days average is (4-7) days
     */
    @Min(1)
    @Max(15)
    @NotNull
    private int periodDuration;

    /**
     * The duration of the cycle in days average is (23-35) days
     */
    @Min(16)
    @Max(99)
    @NotNull
    private int cycleLength;

}
