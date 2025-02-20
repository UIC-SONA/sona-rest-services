package ec.gob.conagopare.sona.utils;

public @interface WithMockJwt {

    String subject() default "test-user";

    String[] roles() default {"USER"};
}