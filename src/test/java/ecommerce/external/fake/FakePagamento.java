package ecommerce.external.fake;

import ecommerce.dto.PagamentoDTO;
import ecommerce.external.IPagamentoExternal;

public class FakePagamento implements IPagamentoExternal {

    private boolean autorizado = true;

    public void setAutorizado(boolean autorizado) {
        this.autorizado = autorizado;
    }

    @Override
    public PagamentoDTO autorizarPagamento(Long clienteId, Double custoTotal) {
        return new PagamentoDTO(autorizado, autorizado ? 123L : null);
    }

    @Override
    public void cancelarPagamento(Long clienteId, Long pagamentoTransacaoId) {
    }
}