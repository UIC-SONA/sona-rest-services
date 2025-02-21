package ec.gob.conagopare.sona.modules.appointments.service;

import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalScheduleDto;
import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalSchedulesDto;
import ec.gob.conagopare.sona.modules.appointments.models.ProfessionalSchedule;
import ec.gob.conagopare.sona.modules.appointments.repository.ProfessionalScheduleRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.exceptions.NotFoundEntityException;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetailsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestProfessionalScheduleService {

    @InjectMocks
    private ProfessionalScheduleService service;

    @Mock
    private ProfessionalScheduleRepository repository;

    @Mock
    private UserService userService;

    @Test
    void create_CuandoDatosSonCorrectos_EntoncesDebeCrearHorarioDeAtencion() {

        // Arrange
        var date = LocalDate.now().plusMonths(1);
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;

        var dto = new ProfessionalScheduleDto();
        dto.setDate(date);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(professionalId);

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.LEGAL_PROFESSIONAL))
                .build();

        //Mock
        when(userService.find(professionalId)).thenReturn(professional);
        when(repository.existsOverlappingSchedule(professionalId, date, fromHour, toHour)).thenReturn(false);

        // Act
        var result = service.create(dto);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(date, result.getDate());
        assertEquals(fromHour, result.getFromHour());
        assertEquals(toHour, result.getToHour());
        assertEquals(professional, result.getProfessional());

    }

    @Test
    void create_CuandoFechaEsPasado_EntoncesDebeLanzarExcepcion() {

        // Arrange
        var date = LocalDate.now().minusMonths(1);
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;

        var dto = new ProfessionalScheduleDto();
        dto.setDate(date);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(professionalId);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.create(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado debe ser 400");
    }

    @Test
    void create_CuandoProfesionalNoEsProfesionalEntoncesDebeLanzarExcepcion() {

        // Arrange
        var date = LocalDate.now().plusMonths(1);
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;

        var dto = new ProfessionalScheduleDto();
        dto.setDate(date);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(professionalId);

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.USER))
                .build();

        //Mock
        when(userService.find(professionalId)).thenReturn(professional);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.create(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado debe ser 400");
    }

    @Test
    void create_CuandoLaFechaSeSolapaConUnHorarioExistenteDelProfesional_EntoncesDebeLanzarExcepcion() {

        // Arrange
        var date = LocalDate.now().plusMonths(1);
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;

        var dto = new ProfessionalScheduleDto();
        dto.setDate(date);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(professionalId);

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.LEGAL_PROFESSIONAL))
                .build();

        //Mock
        when(userService.find(professionalId)).thenReturn(professional);
        when(repository.existsOverlappingSchedule(professionalId, date, fromHour, toHour)).thenReturn(true);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.create(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado debe ser 400");
    }

    @Test
    void update_CuandoDatosSonCorrectos_EntoncesDebeActualizarHorarioDeAtencion() {

        // Arrange
        var date = LocalDate.now().plusMonths(1);
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;
        var scheduleId = 1L;

        var dto = new ProfessionalScheduleDto();
        dto.setDate(date);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(professionalId);

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.LEGAL_PROFESSIONAL))
                .build();

        var schedule = ProfessionalSchedule.builder()
                .id(scheduleId)
                .date(date)
                .fromHour(fromHour)
                .toHour(toHour)
                .professional(professional)
                .build();

        //Mock
        when(userService.find(professionalId)).thenReturn(professional);
        when(repository.existsOverlappingScheduleExcludingId(professionalId, date, fromHour, toHour, scheduleId)).thenReturn(false);
        when(repository.findById(scheduleId)).thenReturn(Optional.of(schedule));

        // Act
        var result = service.update(scheduleId, dto);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(scheduleId, result.getId());
        assertEquals(date, result.getDate());
        assertEquals(fromHour, result.getFromHour());
        assertEquals(toHour, result.getToHour());
        assertEquals(professional, result.getProfessional());
    }

    @Test
    void update_CuandoYaExistenCitasReservadasDentroDelHorario_EntoncesDebeLanzarExcepcion() {

        // Arrange
        var date = LocalDate.now().plusMonths(1);
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;
        var scheduleId = 1L;

        var dto = new ProfessionalScheduleDto();
        dto.setDate(date);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(professionalId);

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.LEGAL_PROFESSIONAL))
                .build();

        var schedule = ProfessionalSchedule.builder()
                .id(scheduleId)
                .date(date)
                .fromHour(fromHour)
                .toHour(toHour)
                .professional(professional)
                .build();

        //Mock
        when(userService.find(professionalId)).thenReturn(professional);
        when(repository.existsOverlappingScheduleExcludingId(professionalId, date, fromHour, toHour, scheduleId)).thenReturn(false);
        when(repository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(repository.existsActiveAppointmentsInSchedule(professionalId, date, fromHour, toHour)).thenReturn(true);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.update(scheduleId, dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado debe ser 400");
    }

    @Test
    void delete_CuandoNoExistenCitasActivasDentroDelHorario_EntoncesDebeEliminarHorarioDeAtencion() {

        var deleted = new AtomicBoolean(false);

        // Arrange
        var date = LocalDate.now().plusMonths(1);
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;
        var scheduleId = 1L;

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.LEGAL_PROFESSIONAL))
                .build();

        var schedule = ProfessionalSchedule.builder()
                .id(scheduleId)
                .date(date)
                .fromHour(fromHour)
                .toHour(toHour)
                .professional(professional)
                .build();

        //Mock
        when(repository.findById(scheduleId)).then(invocation -> deleted.get() ? Optional.empty() : Optional.of(schedule));
        doAnswer(invocation -> {
            deleted.set(true);
            return null;
        }).when(repository).delete(schedule);
        when(repository.existsActiveAppointmentsInSchedule(professionalId, date, fromHour, toHour)).thenReturn(false);

        // Act
        service.delete(scheduleId);

        // Assert
        assertThrows(NotFoundEntityException.class, () -> service.find(scheduleId));
    }

    @Test
    void delete_CuandoExistenCitasActivasDentroDelHorario_EntoncesDebeLanzarExcepcion() {

        // Arrange
        var date = LocalDate.now().plusMonths(1);
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;
        var scheduleId = 1L;

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.LEGAL_PROFESSIONAL))
                .build();

        var schedule = ProfessionalSchedule.builder()
                .id(scheduleId)
                .date(date)
                .fromHour(fromHour)
                .toHour(toHour)
                .professional(professional)
                .build();

        //Mock
        when(repository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(repository.existsActiveAppointmentsInSchedule(professionalId, date, fromHour, toHour)).thenReturn(true);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.delete(scheduleId));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado debe ser 400");
    }

    @Test
    void createAll_CuandoDatosSonCorrectos_EntoncesDebeCrearMultiplesHorariosDeAtencion() {

        // Arrange
        var dates = List.of(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2));
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;

        var dto = new ProfessionalSchedulesDto();
        dto.setDates(dates);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(professionalId);

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.LEGAL_PROFESSIONAL))
                .build();

        //Mock
        when(userService.find(professionalId)).thenReturn(professional);
        when(repository.existsOverlappingSchedule(professionalId, dates.get(0), fromHour, toHour)).thenReturn(false);
        when(repository.existsOverlappingSchedule(professionalId, dates.get(1), fromHour, toHour)).thenReturn(false);
        when(repository.save(any(ProfessionalSchedule.class))).then(invocation -> invocation.getArgument(0));

        // Act
        var result = service.createAll(dto);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(2, result.size(), "Se espera que se creen 2 horarios de atención");
    }

    @Test
    void createAll_CuandoAlgunoDeLosHorariosSeSolapaConOtroHorarioExistenteDelProfesional_EntoncesDebeLanzarExcepcion() {

        // Arrange
        var dates = List.of(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2));
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;

        var dto = new ProfessionalSchedulesDto();
        dto.setDates(dates);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(professionalId);

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.LEGAL_PROFESSIONAL))
                .build();

        //Mock
        when(userService.find(professionalId)).thenReturn(professional);
        when(repository.existsOverlappingSchedule(professionalId, dates.get(0), fromHour, toHour)).thenReturn(false);
        when(repository.existsOverlappingSchedule(professionalId, dates.get(1), fromHour, toHour)).thenReturn(true);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.createAll(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado debe ser 400");
    }

    @Test
    void createAll_CuandoLaFechaDeAlgunoDeLosHorariosEsPasado_EntoncesDebeLanzarExcepcion() {

        // Arrange
        var dates = List.of(LocalDate.now().plusMonths(1), LocalDate.now().minusMonths(1));
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;

        var dto = new ProfessionalSchedulesDto();
        dto.setDates(dates);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(professionalId);

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.LEGAL_PROFESSIONAL))
                .build();

        //Mock
        when(userService.find(professionalId)).thenReturn(professional);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.createAll(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado debe ser 400");
    }

    @Test
    void createAll_CuandoProfesionalNoEsProfesionalEntoncesDebeLanzarExcepcion() {

        // Arrange
        var dates = List.of(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2));
        var fromHour = 8;
        var toHour = 12;
        var professionalId = 1L;

        var dto = new ProfessionalSchedulesDto();
        dto.setDates(dates);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(professionalId);

        var professional = User.builder()
                .id(professionalId)
                .authorities(Set.of(Authority.USER))
                .build();

        //Mock
        when(userService.find(professionalId)).thenReturn(professional);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.createAll(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado debe ser 400");
    }
}