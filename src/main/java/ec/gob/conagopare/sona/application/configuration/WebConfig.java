package ec.gob.conagopare.sona.application.configuration;

import ec.gob.conagopare.sona.application.common.converters.http.ArrayHttpMessageConverterDelegate;
import ec.gob.conagopare.sona.application.common.converters.http.CollectionHttpMessageConverter;
import ec.gob.conagopare.sona.application.common.converters.http.FromStringMessageConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new CollectionHttpMessageConverter());
        converters.add(new ArrayHttpMessageConverterDelegate());
        converters.add(new FromStringMessageConverter());
    }
}
