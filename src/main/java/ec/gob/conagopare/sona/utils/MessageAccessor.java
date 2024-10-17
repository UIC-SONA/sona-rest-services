package ec.gob.conagopare.sona.utils;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Log4j2
@Component
public class MessageAccessor implements MessageSourceAware {


    private MessageSourceAccessor messageSource;


    public String getMessage(String code, String defaultMessage) {
        return this.messageSource.getMessage(code, defaultMessage);
    }


    public String getMessage(String code, @Nullable Object[] args, String defaultMessage) {
        return this.messageSource.getMessage(code, args, defaultMessage);
    }


    public String getMessage(String code) throws NoSuchMessageException {
        return this.messageSource.getMessage(code);
    }


    public String getMessage(String code, @Nullable Object[] args) throws NoSuchMessageException {
        return this.messageSource.getMessage(code, args);
    }


    public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
        return this.messageSource.getMessage(resolvable);
    }


    @Override
    public void setMessageSource(@NotNull MessageSource messageSource) {
        this.messageSource = new MessageSourceAccessor(messageSource);
    }
}
