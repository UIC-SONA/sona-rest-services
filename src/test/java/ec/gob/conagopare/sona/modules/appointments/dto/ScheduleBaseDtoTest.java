package ec.gob.conagopare.sona.modules.appointments.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleBaseDtoTest {

    @Test
    void isValidHours_CuandoHoraInicioEsMenorQueHoraFin_DebeRetornarTrue() {
        // Arrange
        var dto = new ScheduleBaseDto();
        dto.setFromHour(8);
        dto.setToHour(12);

        // Act
        var result = dto.isValidHours();

        // Assert
        assertTrue(result, "La hora de inicio debe ser menor que la hora de fin");
    }

}