package ec.gob.conagopare.sona.utils;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;

public class WithMockJwtSecurityContextFactory implements WithSecurityContextFactory<WithMockJwt> {
    @Override
    public SecurityContext createSecurityContext(WithMockJwt annotation) {
        return JwtTestUtil.createJwtContext(annotation.subject(), Arrays.asList(annotation.roles()));
    }
}