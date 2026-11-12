package br.com.fiap.feedback.api;

import br.com.fiap.feedback.dominio.Avaliacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do {@code POST /avaliacao}, validado com Bean Validation (extensão quarkus-hibernate-validator).
 *
 * <pre>{"descricao": "string", "nota": 0..10}</pre>
 *
 * <p>{@code nota} é {@link Integer} (e não {@code int}) para distinguir "não informado" de zero.</p>
 */
public record AvaliacaoRequest(

        @NotBlank(message = "descricao é obrigatória")
        @Size(max = Avaliacao.TAMANHO_MAXIMO_DESCRICAO, message = "descricao deve ter no máximo 2000 caracteres")
        String descricao,

        @NotNull(message = "nota é obrigatória")
        @Min(value = 0, message = "nota deve estar entre 0 e 10")
        @Max(value = 10, message = "nota deve estar entre 0 e 10")
        Integer nota) {
}
