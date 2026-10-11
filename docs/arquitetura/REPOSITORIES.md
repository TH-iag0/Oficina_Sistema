# Repositories relacionais

Os 15 repositories estão em `br.com.example.autogestao_backend.repository`. São interfaces Spring Data JPA, com identificadores UUID e operações de persistência herdadas de `JpaRepository`. A aplicação foi configurada para PostgreSQL 17 local, conforme [POSTGRESQL.md](POSTGRESQL.md). Nenhum armazenamento não relacional ou upload de arquivos foi acrescentado.

## Consultas disponíveis

- Cliente: documento, nome e telefone; buscas paginadas de clientes ativos.
- Veículo: placa, proprietário e revisões vencidas/próximas por data ou km registrado.
- Funcionário: matrícula, ativos paginados e leitura com bloqueio para atualização.
- Serviço: nome e categoria, filtrando os ativos.
- OS: número, cliente, veículo, estado e conclusão por período; faturamento, custos diretos, quantidade concluída e clientes distintos atendidos.
- Item da OS: itens por ordem e ranking paginado de serviços concluídos, com quantidade, receita e custo direto. Retrabalhos ficam fora desse ranking comercial.
- Histórico de OS: estados por ordem, paginados por instante crescente.
- Pagamento: confirmação, OS, somatório por OS e recebimentos por período; bloqueio para atualização.
- Estorno: confirmação, pagamento, total estornado por pagamento e devoluções por período.
- Despesa: pendências por vencimento, despesas por competência e desembolsos por pagamento efetivo.
- Ponto: confirmação por funcionário, última marcação e histórico paginado por período.
- Jornada: dia da semana e vigência, com resultado paginado. Resultados sobrepostos devem ser tratados como inconsistência pelo serviço.
- Execução: períodos por item, execução aberta e períodos sobrepostos ao filtro de tempo, inclusive os iniciados antes dele.
- Remap: item da OS e categoria ECU/TCU.
- Diagnóstico: item da OS e busca pela causa identificada.

Os filtros de período usam início inclusivo e fim exclusivo. O serviço deve validar `inicio < fim`, converter datas locais para `Instant` com o fuso da oficina e limitar tamanho das páginas. Campos armazenados normalizados, como placa e documento, devem receber parâmetros igualmente normalizados. Os somatórios monetários vazios retornam zero; ticket médio, margem e variação percentual continuam responsabilidade do serviço.

## Transações e regras

`findParaAtualizacao` usa `PESSIMISTIC_WRITE` e exige transação no serviço. O teste verifica que a consulta com lock pode ser executada dentro de uma transação; não simula a concorrência entre duas conexões nem garante o comportamento dos locks do PostgreSQL.

Os repositories não autenticam PIN, não validam sequência de ponto, não autorizam usuários e não impedem todos os conflitos de negócio por conta própria. Estornos acumulados, saldo da OS, reserva de números, sobreposição de jornadas e execução simultânea exigem validações transacionais nos serviços. `existsByNumero` não deve ser usado como gerador de número nem substituir uma reserva atômica.

O CRUD herdado inclui exclusão física; não deve ser exposto diretamente pela API. Cadastro com desativação e registros auditáveis precisam de políticas específicas no serviço. Consultas de ranking usam projeção para não carregar objetos completos. O nome histórico do serviço participa do agrupamento; versões com descrições diferentes podem aparecer como grupos separados.

## Testes executados

Foram executados 70 testes em 11/10/2026: 56 unitários dos models e 14 de integração dos repositories, todos aprovados. A integração usa Hibernate 7.4.5, Spring Data JPA 4.1.1 e H2 2.3.232 em memória no modo de compatibilidade MySQL, com transações desfeitas após cada teste. A geração das tabelas ocorre exclusivamente nesse banco de teste.

Os testes verificam persistência real, inicialização das consultas, paginação, filtros, limites de períodos, clientes distintos, ranking, somatórios vazios, separação entre faturamento/recebimento/estorno, ponto duplicado e períodos de execução sobrepostos. Não usam mocks de `JpaRepository`.

Execução local com as bibliotecas em cache:

```powershell
.\scripts\test-models-offline.ps1 -JdkHome 'C:\Users\Pichau\.jdks\ms-21.0.12.1' -IncludeRepositories
```

O fallback usa launcher JUnit 5.12.2. Com as dependências Maven disponíveis, a execução normal usa as versões gerenciadas no `pom.xml`:

```powershell
$env:JAVA_HOME = 'C:\Users\Pichau\.jdks\ms-21.0.12.1'
.\mvnw.cmd '-Dtest=br.com.example.autogestao_backend.model.*Test,RepositoriesTest' test
```

H2 foi acrescentado apenas como dependência de teste. A conexão local foi posteriormente configurada para PostgreSQL, e o modo H2 atualizado para PostgreSQL: 75 testes aprovados, incluindo cinco testes de configuração. Ainda faltam autenticação com a senha local, migrations PostgreSQL e teste no banco real. O fallback não substitui o build completo do Spring Boot. O acesso público do ponto e as telas também não foram implementados nesta etapa.
