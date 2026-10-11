package br.com.example.autogestao_backend.model;

public enum StatusOrdemServico {
    RASCUNHO, RECEBIDO, DIAGNOSTICO, AGUARDANDO_APROVACAO, APROVADO,
    EM_SERVICO, AGUARDANDO_PECA, FINALIZADO, ENTREGUE, CANCELADO;

    public boolean permite(StatusOrdemServico destino) {
        if (destino == null) return false;
        if (destino == CANCELADO) return this != FINALIZADO && this != ENTREGUE && this != CANCELADO;
        return switch (this) {
            case RASCUNHO -> destino == RECEBIDO;
            case RECEBIDO -> destino == DIAGNOSTICO;
            case DIAGNOSTICO -> destino == AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> destino == APROVADO;
            case APROVADO -> destino == EM_SERVICO || destino == AGUARDANDO_PECA;
            case EM_SERVICO -> destino == FINALIZADO || destino == AGUARDANDO_PECA;
            case AGUARDANDO_PECA -> destino == EM_SERVICO;
            case FINALIZADO -> destino == ENTREGUE;
            case ENTREGUE, CANCELADO -> false;
        };
    }
}
