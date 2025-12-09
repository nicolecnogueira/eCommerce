package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;

public class CompraServiceTest
{
	private CompraService serviceAux;
	private CarrinhoDeCompras carrinhoAux;

	@BeforeEach
	public void setup() {
		serviceAux = new CompraService(null, null, null, null);
		carrinhoAux = new CarrinhoDeCompras();
		carrinhoAux.setItens(new ArrayList<>());
	}

	@Test
	public void calcularCustoTotal()
	{
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();

		List<ItemCompra> itens = new ArrayList<>();

		ItemCompra item1 = new ItemCompra();
		ItemCompra item2 = new ItemCompra();
		ItemCompra item3 = new ItemCompra();

		// Sem desconto, pois 100 + 100 + 100 = 300 < 500)
		// Peso 1kg cada = 3kg total (Frete Isento faixa A)
		configurarItem(item1, new BigDecimal("100.00"), BigDecimal.ONE);
		configurarItem(item2, new BigDecimal("100.00"), BigDecimal.ONE);
		configurarItem(item3, new BigDecimal("100.00"), BigDecimal.ONE);

		itens.add(item1);
		itens.add(item2);
		itens.add(item3);
		carrinho.setItens(itens);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.NORDESTE, TipoCliente.OURO);

		// O valor esperado foi atualizado para 300.00 (3 * 100 + 0 frete) pois 0.00 seria impossível com itens reais.
		BigDecimal esperado = new BigDecimal("300.00");
		assertEquals(0, custoTotal.compareTo(esperado), "Valor calculado incorreto: " + custoTotal);

		assertThat(custoTotal).as("Custo Total da Compra").isEqualByComparingTo("300.00");
	}

	private void configurarItem(ItemCompra item, BigDecimal preco, BigDecimal peso) {
		Produto p = new Produto();
		p.setPreco(preco);
		p.setPesoFisico(peso);
		p.setFragil(false);
		p.setNome("Produto Teste");
		item.setProduto(p);
		item.setQuantidade(1L);
	}

	private void adicionarItem(BigDecimal preco, BigDecimal peso, boolean fragil) {
		Produto p = new Produto();
		p.setPreco(preco);
		p.setPesoFisico(peso);
		p.setFragil(fragil);
		p.setNome("Produto Teste");

		ItemCompra item = new ItemCompra();
		item.setProduto(p);
		item.setQuantidade(1L);
		carrinhoAux.getItens().add(item);
	}

	@Test
	@DisplayName("Deve aplicar 20% de desconto se subtotal for EXATAMENTE 1000.00")
	void desconto_exatamente1000() {
		// Valor: 1000.00 -> Desconto 20% (200.00) -> Total 800.00
		adicionarItem(new BigDecimal("1000.00"), BigDecimal.ONE, false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("800.00");
	}

	@Test
	@DisplayName("NÃO deve aplicar desconto de 20% se subtotal for 1000.01 (acima do limite exato)")
	void desconto_acima1000() {
		// Valor: 1000.01 -> Sem desconto. Total 1000.01.
		adicionarItem(new BigDecimal("1000.01"), BigDecimal.ONE, false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("1000.01");
	}

	@Test
	@DisplayName("Deve aplicar 10% desconto se subtotal for 999.99 (limite superior da faixa de 10%)")
	void desconto_limiteSuperior10Porcento() {
		// Valor: 999.99 -> Desconto 10% (99.999) -> Total 899.99
		adicionarItem(new BigDecimal("999.99"), BigDecimal.ONE, false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("899.99");
	}

	@Test
	@DisplayName("Deve aplicar 10% desconto se subtotal for 500.01 (limite inferior da faixa de 10%)")
	void desconto_limiteInferior10Porcento() {
		// Valor: 500.01 > 500 -> Desconto 10% (50.001) -> Total 450.01
		adicionarItem(new BigDecimal("500.01"), BigDecimal.ONE, false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("450.01");
	}

	@Test
	@DisplayName("NÃO deve aplicar desconto se subtotal for EXATAMENTE 500.00")
	void desconto_exatamente500() {
		// Valor: 500.00 -> Sem desconto.
		adicionarItem(new BigDecimal("500.00"), BigDecimal.ONE, false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("500.00");
	}

	@Test
	@DisplayName("Frete Isento (Faixa A): Peso EXATAMENTE 5.00kg")
	void frete_limiteFaixaA() {
		// Peso 5.00kg -> Isento
		adicionarItem(new BigDecimal("100.00"), new BigDecimal("5.00"), false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("Frete Faixa B: Peso 5.01kg (R$ 2.00/kg)")
	void frete_limiteInferiorFaixaB() {
		// Peso 5.01kg -> Faixa B. Frete = 5.01 * 2.00 = 10.02. Total 110.02.
		adicionarItem(new BigDecimal("100.00"), new BigDecimal("5.01"), false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("110.02");
	}

	@Test
	@DisplayName("Frete Faixa B: Peso EXATAMENTE 10.00kg (R$ 2.00/kg)")
	void frete_limiteSuperiorFaixaB() {
		// Peso 10.00kg -> Faixa B. Frete = 10.00 * 2.00 = 20.00. Total 120.00.
		adicionarItem(new BigDecimal("100.00"), new BigDecimal("10.00"), false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("120.00");
	}

	@Test
	@DisplayName("Frete Faixa C: Peso 10.01kg (R$ 4.00/kg)")
	void frete_limiteInferiorFaixaC() {
		// Peso 10.01kg -> Faixa C. Frete = 10.01 * 4.00 = 40.04. Total 140.04.
		adicionarItem(new BigDecimal("100.00"), new BigDecimal("10.01"), false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("140.04");
	}

	@Test
	@DisplayName("Frete Faixa C: Peso EXATAMENTE 50.00kg (R$ 4.00/kg)")
	void frete_limiteSuperiorFaixaC() {
		// Peso 50.00kg -> Faixa C. Frete = 50.00 * 4.00 = 200.00. Total 300.00.
		adicionarItem(new BigDecimal("100.00"), new BigDecimal("50.00"), false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("300.00");
	}

	@Test
	@DisplayName("Frete Faixa D: Peso 50.01kg (R$ 7.00/kg)")
	void frete_limiteInferiorFaixaD() {
		// Peso 50.01kg -> Faixa D. Frete = 50.01 * 7.00 = 350.07. Total 450.07.
		adicionarItem(new BigDecimal("100.00"), new BigDecimal("50.01"), false);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("450.07");
	}

	@Test
	@DisplayName("Taxa Frágil: Item frágil adiciona R$ 5.00 ao frete")
	void frete_itemFragil() {
		// 1 Item Frágil. Peso 1kg (isento). Taxa: 1 * 5.00 = 5.00. Total 105.00.
		adicionarItem(new BigDecimal("100.00"), BigDecimal.ONE, true);
		BigDecimal total = serviceAux.calcularCustoTotal(carrinhoAux, Regiao.SUL, TipoCliente.OURO);
		assertThat(total).isEqualByComparingTo("105.00");
	}

	@Test
	@DisplayName("Deve lançar exceção se carrinho for nulo")
	void robustez_carrinhoNulo() {
		assertThatThrownBy(() -> serviceAux.calcularCustoTotal(null, Regiao.SUL, TipoCliente.OURO))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Carrinho inválido");
	}
}