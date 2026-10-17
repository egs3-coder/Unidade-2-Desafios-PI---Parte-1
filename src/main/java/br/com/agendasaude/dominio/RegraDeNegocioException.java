package br.com.agendasaude.dominio;

/** Lançada quando uma regra de negócio é violada. O domínio não conhece HTTP: quem traduz é a camada web. */
public class RegraDeNegocioException extends RuntimeException {

    private final Violacao violacao;

    public RegraDeNegocioException(Violacao violacao) {
        super(violacao.getMensagem());
        this.violacao = violacao;
    }

    public Violacao getViolacao() {
        return violacao;
    }
}
