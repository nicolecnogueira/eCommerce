package ecommerce.external.fake;

import java.util.List;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.external.IEstoqueExternal;

public class FakeEstoque implements IEstoqueExternal {

    private boolean disponivel = true;
    private boolean baixaSucesso = true;

    public void setDisponivel(boolean disponivel) {
        this.disponivel = disponivel;
    }

    public void setBaixaSucesso(boolean baixaSucesso) {
        this.baixaSucesso = baixaSucesso;
    }

    @Override
    public DisponibilidadeDTO verificarDisponibilidade(List<Long> produtosIds, List<Long> produtosQuantidades) {
        return new DisponibilidadeDTO(disponivel, List.of());
    }

    @Override
    public EstoqueBaixaDTO darBaixa(List<Long> produtosIds, List<Long> produtosQuantidades) {
        return new EstoqueBaixaDTO(baixaSucesso);
    }
}