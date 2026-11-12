package br.com.fiap.feedback.config;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Fuso horário e formatos de data usados em toda a aplicação. */
public final class Fuso {

    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    /** Formato exibido para pessoas (e-mails e relatórios): 12/11/2026 14:42. */
    public static final DateTimeFormatter DATA_HORA_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(SAO_PAULO);

    public static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Fuso() {
    }
}
