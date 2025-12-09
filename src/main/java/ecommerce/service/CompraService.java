package ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import ecommerce.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import jakarta.transaction.Transactional;

@Service
public class CompraService
{

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;

	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
			IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal)
	{
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;

		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId)
	{
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel())
		{
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado())
		{
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso())
		{
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	}

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {
		if (carrinho == null || carrinho.getItens() == null) {
			throw new IllegalArgumentException("Carrinho inválido");
		}

		BigDecimal subtotal = BigDecimal.ZERO;
		BigDecimal pesoTotal = BigDecimal.ZERO;
		int itensFrageis = 0;

		for (ItemCompra item : carrinho.getItens()) {
			BigDecimal totalItem = item.getProduto().getPreco().multiply(new BigDecimal(item.getQuantidade()));
			subtotal = subtotal.add(totalItem);

			pesoTotal = pesoTotal.add(item.getProduto().getPesoFisico().multiply(new BigDecimal(item.getQuantidade())));

			if (Boolean.TRUE.equals(item.getProduto().isFragil())) {
				itensFrageis += item.getQuantidade();
			}
		}

		// Se S = 1000 -> 20%
		// Se S > 500 && S < 1000 -> 10%
		BigDecimal desconto = BigDecimal.ZERO;
		if (subtotal.compareTo(new BigDecimal("1000.00")) == 0) {
			desconto = subtotal.multiply(new BigDecimal("0.20"));
		} else if (subtotal.compareTo(new BigDecimal("500.00")) > 0 && subtotal.compareTo(new BigDecimal("1000.00")) < 0) {
			desconto = subtotal.multiply(new BigDecimal("0.10"));
		}

		BigDecimal subtotalComDesconto = subtotal.subtract(desconto);

		BigDecimal frete = BigDecimal.ZERO;

		if (pesoTotal.compareTo(new BigDecimal("5.00")) <= 0) {
			frete = BigDecimal.ZERO;
		} else if (pesoTotal.compareTo(new BigDecimal("10.00")) <= 0) {
			frete = pesoTotal.multiply(new BigDecimal("2.00"));
		} else if (pesoTotal.compareTo(new BigDecimal("50.00")) <= 0) {
			frete = pesoTotal.multiply(new BigDecimal("4.00"));
		} else {
			frete = pesoTotal.multiply(new BigDecimal("7.00"));
		}

		if (itensFrageis > 0) {
			frete = frete.add(new BigDecimal(itensFrageis).multiply(new BigDecimal("5.00")));
		}

		return subtotalComDesconto.add(frete).setScale(2, RoundingMode.HALF_UP);
	}
}
