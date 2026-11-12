package br.com.fiap.feedback.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/** Ajusta o {@link ObjectMapper} gerenciado pelo Quarkus (extensão quarkus-jackson). */
@Singleton
public class JsonConfig implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper mapper) {
        configurar(mapper);
    }

    /** Aplica as regras de serialização. Público para que os testes usem exatamente a mesma configuração. */
    public static ObjectMapper configurar(ObjectMapper mapper) {
        return mapper
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // "nota": 7.5 deve ser rejeitado, e não truncado silenciosamente para 7
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }
}
