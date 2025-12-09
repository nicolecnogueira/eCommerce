package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import ecommerce.repository.FakeCarrinhoDeComprasRepository;
import ecommerce.repository.FakeClienteRepository;

@ExtendWith(MockitoExtension.class)
class CompraServiceCenario2Test {

    // Dependências Externas Mockadas (Mockito)
    @Mock private IEstoqueExternal estoqueMock;
    @Mock private IPagamentoExternal pagamentoMock;

    // Dependências Repository Fakes (Manuais)
    private FakeClienteRepository clienteRepositoryFake;
    private FakeCarrinhoDeComprasRepository carrinhoRepositoryFake;

    // Services Reais (SUT - System Under Test e suas dependências internas)
    private ClienteService clienteService;
    private CarrinhoDeComprasService carrinhoService;
    private CompraService compraService;

    @BeforeEach
    void setUp() {
        clienteRepositoryFake = new FakeClienteRepository();
        carrinhoRepositoryFake = new FakeCarrinhoDeComprasRepository();

        clienteService = new ClienteService(clienteRepositoryFake);
        carrinhoService = new CarrinhoDeComprasService(carrinhoRepositoryFake);

        compraService = new CompraService(carrinhoService, clienteService, estoqueMock, pagamentoMock);
    }

    private void prepararDadosNoBancoFake() {
        Cliente cliente = new Cliente(1L, "Maria", Regiao.SUL, TipoCliente.OURO);
        clienteRepositoryFake.save(cliente);

        Produto produto = new Produto(10L, "Notebook", "Desc", new BigDecimal("3000.00"),
                new BigDecimal("2.0"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ELETRONICO);

        ItemCompra item = new ItemCompra(null, produto, 1L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, List.of(item), null);
        carrinhoRepositoryFake.save(carrinho);
    }

    @Test
    @DisplayName("Cenário 2: Sucesso - Compra com Fakes de Repository e Mocks Externos")
    void deveFinalizarCompraComSucesso() {
        prepararDadosNoBancoFake();

        when(estoqueMock.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, List.of()));

        when(pagamentoMock.autorizarPagamento(eq(1L), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 999L));

        when(estoqueMock.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(true));

        CompraDTO resultado = compraService.finalizarCompra(1L, 1L);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.transacaoPagamentoId()).isEqualTo(999L);

        verify(estoqueMock).darBaixa(anyList(), anyList());
    }

    @Test
    @DisplayName("Cenário 2: Erro - Cliente não encontrado no Banco Fake")
    void deveFalharSeClienteNaoExiste() {

        assertThatThrownBy(() -> compraService.finalizarCompra(1L, 99L)) // ID 99 não existe
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cliente não encontrado");
    }

    @Test
    @DisplayName("Cenário 2: Erro - Estoque Indisponível (Mock)")
    void deveFalharSeEstoqueIndisponivel() {
        prepararDadosNoBancoFake();

        when(estoqueMock.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(false, List.of(10L)));

        assertThatThrownBy(() -> compraService.finalizarCompra(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Itens fora de estoque.");
    }
}