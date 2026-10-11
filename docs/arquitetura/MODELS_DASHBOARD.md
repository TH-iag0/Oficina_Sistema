# Models para os dashboards

Os models estão no pacote `br.com.example.autogestao_backend.model`. Não há tabela para armazenar totais do dashboard: eles devem ser calculados pelo serviço a partir dos registros reais.

| Model | Finalidade |
|---|---|
| Cliente | Cadastro PF/PJ, contato, data de cadastro e desativação lógica. |
| Veiculo | Cliente proprietário, placa, marca/modelo, km e próxima revisão. |
| Servico | Catálogo por categoria, preço sugerido e custo direto estimado. |
| OrdemServico | Cliente/veículo, número, estado, consentimento, datas e totais de conclusão. |
| HistoricoStatusOs | Evolução da OS para medir espera e conclusão. |
| ItemOrdemServico | Serviço contratado, quantidade, preço histórico, custo direto, conclusão e retrabalho. |
| Pagamento | Recebimento efetivo, forma, data e identificador de confirmação. |
| EstornoPagamento | Devolução vinculada ao pagamento original, com valor, motivo e data. |
| Despesa | Despesa operacional, competência, vencimento e pagamento. |
| Remap | Item ECU/TCU, tipo configurável e identificação técnica básica. |
| Diagnostico | Sintomas, testes, DTCs, causa e vínculo com reparo aprovado. |
| Funcionario | Matrícula, nome, hash BCrypt do PIN, ativação e versão para concorrência. |
| MarcacaoPonto | Presença confirmada no servidor, com fuso e identificação do pedido. |
| JornadaTrabalho | Horários por dia da semana, intervalo, tolerância e vigência. |
| ExecucaoServico | Períodos de trabalho por funcionário e item da OS. |

`Entidade` fornece UUID e versão de concorrência aos novos models. Os enums definem categorias, tipos de cliente, formas de pagamento e estados; stages de remap e categorias de despesas são textos configuráveis, sem preços fixos.

## Critérios previstos para os indicadores

- Faturamento de serviços: somar `receitaFinalizada` pela data `finalizadaEm`, incluindo OS entregues. OS aberta ou cancelada não compõe essa receita.
- Recebimentos: somar pagamentos pela data `recebidoEm`, menos estornos pela data `instante`. Um pagamento antecipado não finaliza nem fatura a OS.
- Lucro bruto: receita dos serviços finalizados menos seus custos diretos registrados. O resultado não inclui despesas operacionais. Gastos diretos não devem ser cadastrados novamente como despesas operacionais.
- Ticket médio por OS: faturamento dividido pelo número de OS finalizadas no período. Quantidade de serviços é outra métrica.
- Variação percentual: comparar a mesma métrica e períodos equivalentes. Base anterior zero deve produzir indicação de base indisponível, sem divisão por zero. Mês em andamento deve ser identificado; comparação com o mesmo intervalo do mês anterior evita apresentar o mês parcial como completo.
- Clientes atendidos: clientes distintos de OS recebidas e não canceladas no período; quantidade de atendimentos é a contagem das OS. Novos/recorrentes depende do primeiro atendimento, não apenas da data do cadastro.
- Serviços mais usados: somar a quantidade dos itens concluídos de OS finalizadas ou entregues. Retrabalho deve aparecer separado para não inflar a procura comercial.
- ECU/TCU: agrupar por categoria histórica do item e tipo configurado no remap. Os valores financeiros vêm da OS, não de números fictícios na tela.
- Operação: consultar o estado atual da OS, seus itens, veículo e previsão de entrega. Tempo de conclusão usa `recebidaEm` e `finalizadaEm`; o histórico detalha as esperas.
- Diagnósticos convertidos: diagnósticos vinculados a reparos aprovados. O serviço de consulta deve considerar cancelamentos posteriores do reparo.
- Horas de presença: parear marcações de ponto válidas, descontando almoço. Jornadas incompletas são pendências, não horas inventadas.
- Horas dedicadas aos serviços: duração dos períodos de execução encerrados. Períodos abertos são exibidos em andamento. Períodos que atravessam os limites do filtro devem ser repartidos pelo intervalo consultado.
- Atrasos/horas excedentes: comparar presença com a jornada vigente; sem configuração, mostrar indicador indisponível. Isso não representa conformidade trabalhista automática.
- Revisões: comparar data prevista com a data atual e km previsto com o último km registrado. Não há estimativa automática de quilometragem atual.

Filtros usam `America/Sao_Paulo`, semana de segunda a domingo e intervalos com início inclusivo e fim exclusivo. O banco registra instantes; os totais monetários usam `BigDecimal` com duas casas decimais, sem arredondamento silencioso.

## Limites desta entrega

Foram implementados models e testes unitários. Posteriormente foram acrescentados repositories, algumas consultas agregadas e testes de integração, documentados em [REPOSITORIES.md](REPOSITORIES.md). Ainda não há controllers, serviço completo dos indicadores, tela de dashboard nem migrations para esses novos models. As anotações JPA descrevem constraints e relações, mas não comprovam que elas existem no MySQL.

Os serviços futuros precisam autenticar e autorizar chamadas, reservar números de OS, validar documentos completos e duplicatas, validar a sequência do ponto, aplicar idempotência e bloquear transações concorrentes. Também precisam impedir períodos produtivos sobrepostos, conclusão de serviços com execução aberta, soma de estornos acima do pagamento, sobreposição de vigências de jornada e recebimentos inconsistentes com o saldo da OS. A versão JPA auxilia a concorrência, mas não substitui essas transações.

Os models técnicos são a base para estes indicadores. Arquivos originais/backup/modificados de remap, hashes, versões, anexos, protocolos/ferramentas, checklist, medições com origem, peças/estoque, garantias, descontos, correções auditadas e políticas configuráveis de fluxo da OS continuam como extensões pendentes. Não foi criada funcionalidade de integração bancária.

## Verificação

Com JDK 21 e dependências disponíveis, executar a suíte unitária dos models no PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Users\Pichau\.jdks\ms-21.0.12.1'
.\mvnw.cmd '-Dtest=br.com.example.autogestao_backend.model.*Test' test
```

Para o ambiente atual sem todas as dependências dos plugins Maven, há um fallback que compila somente os models e executa seus testes com JUnit 5.12.2 do cache local:

```powershell
.\scripts\test-models-offline.ps1 -JdkHome 'C:\Users\Pichau\.jdks\ms-21.0.12.1'
```

Esse fallback utiliza as APIs JPA/Hibernate e Spring Security disponíveis do projeto, mas o launcher JUnit é anterior ao JUnit 6 do Maven. Ele não inicia Spring Boot, não testa o MySQL e não equivale ao build completo.

Resultado em 10/10/2026: compilação dos models com Java 21 e 56 testes unitários aprovados no fallback. O build Maven offline foi tentado e falhou por dependências de plugins ausentes no cache local.
