package ec.gob.conagopare.sona.modules.appointments.service;

import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.appointments.repository.AppointmentRepository;
import ec.gob.conagopare.sona.modules.user.models.User;
import io.github.luidmidev.springframework.data.crud.core.filters.Filter;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaReadService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AppointmentService extends JpaReadService<Appointment, UUID, AppointmentRepository> {


    protected AppointmentService(AppointmentRepository repository, Class<Appointment> domainClass, EntityManager entityManager) {
        super(repository, domainClass, entityManager);
    }

    @Override
    protected Page<Appointment> search(String search, Pageable pageable, Filter filter) {
        throw ApiError.badRequest("Filtro no soportado");
    }

    public boolean hasAppointments(Long profesionalId, User.ProfessionalSchedule schedule) {
        return false;
    }


}
