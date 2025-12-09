# Trabalho 3ª Unidade: Testes de Mutação e Dublês de Teste

Este projeto consiste na implementação e teste da camada de serviço de um E-commerce (`CompraService`), focando em **Testes de Unidade**, **Cobertura Estrutural**, **Análise de Mutação** e uso de **Dublês de Teste** (Mocks e Fakes).

## Autores
* **Bianca Maciel Medeiros**
* **Nicole Carvalho Nogueira**

---

## Instruções de Execução

### Pré-requisitos
* Java 17 ou superior.
* Maven (o projeto inclui o wrapper `./mvnw` para facilitar).

### Compilar o projeto
Para baixar as dependências e compilar o projeto sem rodar os testes:
```bash
./mvnw clean install -DskipTests
```

### Como Rodar os Testes
O projeto contém três conjuntos principais de testes localizados em src/test/java:

1. Lógica de Cálculo: CompraServiceTest.java

2. Cenário 1 (Fakes Externos): CompraServiceCenario1Test.java

3. Cenário 2 (Fakes de Repositório): CompraServiceCenario2Test.java

Para executar todos os testes de uma vez:
```bash
./mvnw test
```

### Relatórios de Cobertura (JaCoCo)
O projeto utiliza o JaCoCo para garantir que 100% das arestas (branches) do método calcularCustoTotal foram exercitadas.

#### Como gerar o relatório
Execute o comando de verificação do Maven:
```bash
./mvnw verify
```

#### Como visualizar e interpretar
1. Após a execução, abra o arquivo gerado em: target/site/jacoco/index.html
2. Navegue até o pacote ecommerce.service e clique na classe CompraService.
3. Interpretação:
 - Losangos Verdes: Indicam que todas as bifurcações (if/else) foram testadas.
 - Linhas Verdes: Indicam que a linha foi executada.
 - Meta Atingida: O método calcularCustoTotal possui 100% de cobertura de instruções e arestas.

### Relatório de Mutação (PITEST)
Utilizamos o PITEST para avaliar a qualidade dos testes do método calcularCustoTotal. O objetivo foi garantir que nenhum defeito injetado propositalmente (mutante) sobrevivesse aos testes.

```bash
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```
#### Como verificar mutantes sobreviventes

1.  Acesse o diretório: target/pit-reports/.

2.  Abra o arquivo index.html (dentro da pasta com a data/hora da execução).

3.  Verifique a coluna **Mutation Coverage**.

    *   **Verde (100%):** Todos os mutantes foram mortos (detectados).

    *   **Vermelho:** Algum mutante sobreviveu (o teste não falhou quando deveria).


#### Estratégias usadas para matar mutantes

Para atingir 100% de eficácia na mutação, focamos em **Análise de Valor Limite (AVL)**:

1.  **Mutantes de Condicional (Conditionals Boundary Mutator):**

    *   O PITEST altera condições como < para <=.

    *   **Estratégia:** Criamos testes com valores exatos nas bordas das faixas de desconto e frete.

    *   _Exemplo:_ Para a regra "Desconto se > 500", testamos exatamente 500.00 (sem desconto) e 500.01 (com desconto). Se o código mudasse para >= 500, o teste de 500.00 falharia, matando o mutante.

2.  **Mutantes de Matemática (Math Mutator):**

    *   O PITEST altera operações matemáticas (ex: \* para / ou + para -).

    *   **Estratégia:** Utilizamos asserções estritas com BigDecimal (isEqualByComparingTo) verificando o resultado exato do cálculo final. Qualquer alteração na fórmula do frete ou subtotal gera um valor final incorreto, quebrando o teste.

3.  **Mutantes de Retorno (Empty Returns / Null Returns):**

    *   **Estratégia:** Todos os testes validam que o retorno isNotNull() e possui o valor numérico esperado, impedindo que o código retorne null ou 0 indevidamente.


### Cenários de Teste com Dublês

Conforme solicitado no enunciado, implementamos dois cenários para testar o método finalizarCompra():

#### Cenário 1 (CompraServiceCenario1Test.java)

*   **Fakes:** Implementados manualmente para IEstoqueExternal e IPagamentoExternal (no pacote ecommerce.external.fake).

*   **Mocks:** Utilizado Mockito para ClienteService e CarrinhoDeComprasService.

*   **Objetivo:** Validar o fluxo de sucesso, erros de negócio (sem estoque/pagamento recusado) e estorno.


#### Cenário 2 (CompraServiceCenario2Test.java)

*   **Mocks:** Utilizado Mockito para os serviços externos (IEstoqueExternal, IPagamentoExternal).

*   **Fakes:** Implementados manualmente para a camada de persistência (FakeClienteRepository, FakeCarrinhoRepository).

*   **Objetivo:** Simular o teste da camada de serviço integrada a um "banco de dados em memória", garantindo que os dados sejam recuperados corretamente antes de processar a compra.