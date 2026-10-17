package br.com.agendasaude.dominio;

/** Cada regra de negócio quebrada tem um código estável, devolvido pela API no campo "codigo". */
public enum Violacao {

    FORA_DO_EXPEDIENTE("Consultas só podem ser marcadas de segunda a sexta, das 08:00 às 18:00, fora do almoço (12:00–13:00)."),
    HORARIO_NAO_ALINHADO("Consultas começam em horário cheio ou meia hora (ex.: 09:00, 09:30)."),
    HORARIO_NO_PASSADO("Não é possível agendar consulta em um horário que já passou."),
    ALEM_DA_JANELA("A agenda está aberta apenas para os próximos 60 dias."),
    MEDICO_OCUPADO("O médico já tem uma consulta neste horário."),
    PACIENTE_OCUPADO("O paciente já tem uma consulta neste horário."),
    CANCELAMENTO_TARDIO("Cancelamentos exigem pelo menos 24 horas de antecedência."),
    CONSULTA_JA_CANCELADA("Esta consulta já foi cancelada.");

    private final String mensagem;

    Violacao(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }
}
