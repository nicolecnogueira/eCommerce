package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ecommerce.dto.CompraDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;
import ecommerce.external.fake.FakeEstoque;
import ecommerce.external.fake.FakePagamento;

@ExtendWith(MockitoExtension.class)
class CompraServiceCenario1Test {

    @Mock
    private ClienteService clienteService;

    @Mock
    private CarrinhoDeComprasService carrinhoService;

    private FakeEstoque fakeEstoque;
    private FakePagamento fakePagamento;
    private CompraService compraService;

    private Cliente cliente;
    private CarrinhoDeCompras carrinho;

    @BeforeEach
    void setUp() {
        // Inicializa os Fakes
        fakeEstoque = new FakeEstoque();
        fakePagamento = new FakePagamento();

        compraService = new CompraService(carrinhoService, clienteService, fakeEstoque, fakePagamento);

        cliente = new Cliente(1L, "João", Regiao.SUDESTE, TipoCliente.BRONZE);

        Produto produto = new Produto();
        produto.setId(10L);
        produto.setPreco(new BigDecimal("100.00"));
        produto.setPesoFisico(BigDecimal.ONE);
        produto.setTipo(TipoProduto.LIVRO);
        produto.setFragil(false);

        ItemCompra item = new ItemCompra(null, produto, 1L);

        carrinho = new CarrinhoDeCompras();
        carrinho.setId(1L);
        carrinho.setCliente(cliente);
        carrinho.setItens(List.of(item));
    }

    @Test
    @DisplayName("Cenário 1: Sucesso - Compra finalizada corretamente")
    void deveFinalizarCompraComSucesso() {
        when(clienteService.buscarPorId(1L)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, cliente)).thenReturn(carrinho);

        CompraDTO resultado = compraService.finalizarCompra(1L, 1L);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.mensagem()).isEqualTo("Compra finalizada com sucesso.");
        assertThat(resultado.transacaoPagamentoId()).isNotNull();

        verify(clienteService).buscarPorId(1L);
    }

    @Test
    @DisplayName("Cenário 1: Erro - Falha na verificação de estoque")
    void deveFalharQuandoEstoqueIndisponivel() {
        when(clienteService.buscarPorId(1L)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, cliente)).thenReturn(carrinho);

        fakeEstoque.setDisponivel(false);

        assertThatThrownBy(() -> compraService.finalizarCompra(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Itens fora de estoque.");
    }

    @Test
    @DisplayName("Cenário 1: Erro - Pagamento não autorizado")
    void deveFalharQuandoPagamentoRecusado() {
        when(clienteService.buscarPorId(1L)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, cliente)).thenReturn(carrinho);

        fakePagamento.setAutorizado(false);

        assertThatThrownBy(() -> compraService.finalizarCompra(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Pagamento não autorizado.");
    }

    @Test
    @DisplayName("Cenário 1: Erro - Falha na baixa do estoque (Estorno)")
    void deveEstornarPagamentoQuandoBaixaFalhar() {
        when(clienteService.buscarPorId(1L)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, cliente)).thenReturn(carrinho);

        fakeEstoque.setBaixaSucesso(false);

        assertThatThrownBy(() -> compraService.finalizarCompra(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Erro ao dar baixa no estoque.");

    }
}