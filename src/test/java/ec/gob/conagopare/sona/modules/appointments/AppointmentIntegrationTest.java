package ec.gob.conagopare.sona.modules.appointments;

import com.jayway.jsonpath.JsonPath;
import ec.gob.conagopare.sona.modules.appointments.dto.CancelAppointment;
import ec.gob.conagopare.sona.modules.appointments.dto.NewAppointment;
import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalScheduleDto;
import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalSchedulesDto;
import ec.gob.conagopare.sona.modules.appointments.models.Appointment.Type;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.modules.user.dto.UserDto;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.test.IntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static ec.gob.conagopare.sona.modules.user.models.User.KEYCLOAK_ID_ATTRIBUTE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppointmentIntegrationTest extends IntegrationTest {


    private static boolean isSetUp = false;
    private static Integer professionalId;
    private static final String PROFESSIONAL_USER_USERNAME = "professional.user";
    private static final String PROFESSIONAL_USER_PASSWORD = "Qwerty1598.1598.";

    private static final String ATTENDANT1_USER_USERNAME = "attendant1.user";
    private static final String ATTENDANT1_USER_PASSWORD = "Qwerty1598.1598.";

    private static final String ATTENDANT2_USER_USERNAME = "attendant2.user";
    private static final String ATTENDANT2_USER_PASSWORD = "Qwerty1598.1598.";
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        if (isSetUp) return;

        var singupProfessional = new SingUpUser();
        singupProfessional.setFirstName("Professional");
        singupProfessional.setLastName("User");
        singupProfessional.setUsername(PROFESSIONAL_USER_USERNAME);
        singupProfessional.setPassword(PROFESSIONAL_USER_PASSWORD);
        singupProfessional.setEmail("profesional@localtest.com");
        singUp(singupProfessional, mockMvc);

        var singupAttendant1 = new SingUpUser();
        singupAttendant1.setFirstName("Attendant1");
        singupAttendant1.setLastName("User");
        singupAttendant1.setUsername(ATTENDANT1_USER_USERNAME);
        singupAttendant1.setPassword(ATTENDANT1_USER_PASSWORD);
        singupAttendant1.setEmail("attendant1@localtest.com");
        singUp(singupAttendant1, mockMvc);

        var singupAttendant2 = new SingUpUser();
        singupAttendant2.setFirstName("Attendant2");
        singupAttendant2.setLastName("User");
        singupAttendant2.setUsername(ATTENDANT2_USER_USERNAME);
        singupAttendant2.setPassword(ATTENDANT2_USER_PASSWORD);
        singupAttendant2.setEmail("attendant2@localtest.com");
        singUp(singupAttendant2, mockMvc);

        var professionalAccessToken = obtainAccessToken(PROFESSIONAL_USER_USERNAME, PROFESSIONAL_USER_PASSWORD);
        professionalId = getUserId(professionalAccessToken, mockMvc);

        var adminBearerToken = obtainAdminBearerToken();

        var authoritiesToAdd = Set.of(Authority.MEDICAL_PROFESSIONAL);
        var authoritiesToRemove = Set.of(Authority.USER);

        var dto = new UserDto();
        dto.setAuthoritiesToAdd(authoritiesToAdd);
        dto.setAuthoritiesToRemove(authoritiesToRemove);
        dto.setFirstName("Professional");
        dto.setLastName("User");
        dto.setEmail("profesional@localtest.com");
        dto.setUsername(PROFESSIONAL_USER_USERNAME);

        mockMvc.perform(put("/user/{id}", professionalId)
                        .contentType("application/json")
                        .header("Authorization", adminBearerToken)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        isSetUp = true;
    }

    @Test
    @Order(1)
    void createProfessionalSchedule() throws Exception {

        var date = LocalDate.now().plusMonths(1);
        var fromHour = 8;
        var toHour = 12;

        var dto = new ProfessionalScheduleDto();
        dto.setDate(date);
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setProfessionalId(Long.valueOf(professionalId));

        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(post("/professional-schedule")
                        .contentType("application/json")
                        .header("Authorization", adminBearerToken)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }


    @Test
    @Order(2)
    void createMultipleProfessionalSchedules() throws Exception {
        var date = LocalDate.now().plusMonths(2);
        var fromHour = 8;
        var toHour = 12;

        var dto = new ProfessionalSchedulesDto();
        dto.setProfessionalId(Long.valueOf(professionalId));
        dto.setFromHour(fromHour);
        dto.setToHour(toHour);
        dto.setDates(List.of(date, date.plusDays(1), date.plusDays(2)));

        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(post("/professional-schedule/all")
                        .header("Authorization", adminBearerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    void programAppointments() throws Exception {

        var userBearerToken = "Bearer " + obtainAccessToken(ATTENDANT1_USER_USERNAME, ATTENDANT1_USER_PASSWORD);

        var date = LocalDate.now().plusMonths(1);
        var newAppointment1 = new NewAppointment();
        newAppointment1.setProfessionalId(Long.valueOf(professionalId));
        newAppointment1.setDate(date);
        newAppointment1.setHour(9);
        newAppointment1.setType(Type.PRESENTIAL);

        var newAppointment2 = new NewAppointment();
        newAppointment2.setProfessionalId(Long.valueOf(professionalId));
        newAppointment2.setDate(date);
        newAppointment2.setHour(10);
        newAppointment2.setType(Type.PRESENTIAL);

        mockMvc.perform(post("/appointment/program")
                        .contentType("application/json")
                        .header("Authorization", userBearerToken)
                        .content(objectMapper.writeValueAsString(newAppointment1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/appointment/program")
                        .contentType("application/json")
                        .header("Authorization", userBearerToken)
                        .content(objectMapper.writeValueAsString(newAppointment2)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void cancelAppointments() throws Exception {
        var userBearerToken = "Bearer " + obtainAccessToken(ATTENDANT2_USER_USERNAME, ATTENDANT2_USER_PASSWORD);

        var date = LocalDate.now().plusMonths(1);
        var newAppointmentToCancel = new NewAppointment();
        newAppointmentToCancel.setProfessionalId(Long.valueOf(professionalId));
        newAppointmentToCancel.setDate(date);
        newAppointmentToCancel.setHour(11);
        newAppointmentToCancel.setType(Type.PRESENTIAL);

        var appointmentJson = mockMvc.perform(post("/appointment/program")
                        .contentType("application/json")
                        .header("Authorization", userBearerToken)
                        .content(objectMapper.writeValueAsString(newAppointmentToCancel)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var appointmentId = JsonPath.<Integer>read(appointmentJson, "$.id");

        var cancelAppointment = new CancelAppointment();
        cancelAppointment.setAppointmentId(Long.valueOf(appointmentId));
        cancelAppointment.setReason("No puedo asistir");

        mockMvc.perform(post("/appointment/cancel")
                        .contentType("application/json")
                        .header("Authorization", userBearerToken)
                        .content(objectMapper.writeValueAsString(cancelAppointment)))
                .andExpect(status().isOk());
    }


    @Test
    @Order(4)
    void selfAppointments() throws Exception {
        var userBearerToken = "Bearer " + obtainAccessToken(ATTENDANT2_USER_USERNAME, ATTENDANT2_USER_PASSWORD);

        mockMvc.perform(get("/appointment/self")
                        .header("Authorization", userBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));
    }

    @Test
    @Order(5)
    void listAllAppointments() throws Exception {
        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(get("/appointment/list")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(3));
    }

    @Test
    @Order(6)
    void professionalAppointmentRanges() throws Exception {
        var adminBearerToken = obtainAdminBearerToken();
        var from = LocalDate.now().plusMonths(1);
        var to = from.plusDays(2);

        mockMvc.perform(get("/appointment/professional/{professionalId}/ranges", professionalId)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(6)
    void searchAndFilterAppointments() throws Exception {

        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(get("/appointment")
                        .param(KEYCLOAK_ID_ATTRIBUTE, "keycloakId")
                        .param("userId", "1")
                        .param("professionalId", "2")
                        .param("canceled", "true")
                        .param("type", "VIRTUAL")
                        .param("from", "2021-01-01")
                        .param("to", "2021-12-31")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}

