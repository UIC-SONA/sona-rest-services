package ec.gob.conagopare.sona.application.configuration;

import ec.gob.conagopare.sona.modules.appointments.service.AppointmentService;
import ec.gob.conagopare.sona.modules.appointments.service.ProfessionalScheduleService;
import ec.gob.conagopare.sona.modules.content.services.DidacticContentService;
import ec.gob.conagopare.sona.modules.content.services.TipService;
import ec.gob.conagopare.sona.modules.forum.service.ForumService;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.security.AuthorizeCrudConfigurer.AuthorizationManagerCrudMatcherRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;

@Slf4j
@Configuration
public class CrudConfig {

    @Bean
    public Customizer<AuthorizationManagerCrudMatcherRegistry> authorizationCrudConfigurer() {
        return registry ->
                registry
                        .crudsReadOnly(UserService.class).authenticated()
                        .crudsReadOnly(TipService.class).authenticated()
                        .crudsReadOnly(DidacticContentService.class).authenticated()
                        .crudsReadOnly(ProfessionalScheduleService.class).authenticated()
                        .cruds(ForumService.class).authenticated()
                        .cruds(
                                UserService.class,
                                TipService.class,
                                DidacticContentService.class,
                                ProfessionalScheduleService.class,
                                AppointmentService.class
                        ).hasAuthority(Authority.ADMIN)
                        .anyOperation().permitAll();
    }

}
