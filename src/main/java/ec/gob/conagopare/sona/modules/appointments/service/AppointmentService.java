package ec.gob.conagopare.sona.modules.appointments.service;

import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.appointments.repository.AppointmentRepository;
import io.github.luidmidev.springframework.data.crud.core.filters.Filter;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaReadService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService extends JpaReadService<Appointment, Long, AppointmentRepository> {

    protected AppointmentService(AppointmentRepository repository, EntityManager entityManager) {
        super(repository, Appointment.class, entityManager);
    }

    @Override
    protected Page<Appointment> search(String search, Pageable pageable, Filter filter) {
        throw ApiError.badRequest("Filtro no soportado");
    }

}
