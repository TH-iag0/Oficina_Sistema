# Contexto do projeto — AutoGestão Local

Resumo recuperado dos documentos fornecidos pelo proprietário. O pedido atual é transferir contexto; não iniciar implementação automaticamente.

## Objetivo
Aplicativo Windows offline para oficina especializada em Remap ECU/TCU, manutenção/revisão e diagnóstico. V1 em um PC; futura LAN com API central e PostgreSQL. Priorizar cadastro rápido, busca, autocompletar, modelos reutilizáveis e rascunhos persistentes.

## Stack e arquitetura
React + TypeScript + Vite; Tailwind CSS, padrão shadcn/ui, Lucide; React Hook Form + Zod; TanStack Query; Apache ECharts. Desktop Tauri 2 como host leve. Backend Java 21 + Spring Boot + Maven Wrapper, REST, Spring Security, Bean Validation e JUnit. SQLite via JDBC/Spring JDBC com migrations SQL versionadas. React nunca acessa o banco diretamente; validação e regras de negócio no backend. Arquitetura modular por domínio, preparada para PostgreSQL sem abstrações prematuras.

## Design
Referência Atlas Financeiro: menu horizontal flutuante em cápsula, aba ativa azul clara, tema claro branco/off-white, fundo azul suave, cartões arredondados, sombras discretas, tipografia azul-marinho e títulos com palavra azul em itálico. Não usar sidebar escura nem copiar a marca Atlas. Desktop 1366x768 e 1920x1080. Gráficos com acabamento visual 3D sem WebGL desnecessário. Referências originais constavam no ZIP, mas não estão disponíveis no caminho informado no momento da transferência.

## Entregas incrementais
A: inspecionar projeto existente; fundação executável, design system e navegação; backend, configurações e migrations SQLite.
B: CRUD real de clientes e veículos; catálogo de serviços e OS; remap ECU com campos técnicos, anexos, busca e edição; testes dos fluxos críticos.
C: TCU, manutenção, diagnóstico, estoque, funcionários/ponto, financeiro e relatórios.
D: integrar backend embarcado ao Tauri, instalador Windows, backup e restauração; preparar expansão LAN.
Não construir todos os módulos de uma vez nem criar dezenas de arquivos vazios.

## Domínios e regras
Clientes PF/PJ, contatos, documentos opcionais conforme operação, endereço e observações. Veículos vinculados ao cliente: placa, marca/modelo/versão, ano, combustível, motor/câmbio, km, VIN e histórico. Busca por nome, telefone, documento e placa; máscaras, validação e prevenção de duplicatas.
OS: RASCUNHO -> RECEBIDO -> DIAGNOSTICO -> AGUARDANDO_APROVACAO -> APROVADO -> EM_SERVICO -> FINALIZADO -> ENTREGUE. CANCELADO e AGUARDANDO_PECA explícitos. Backend valida transições e gera número da OS. Incluir serviços, peças, custos e preços separados, pagamentos, checklist, aprovação com data/hora e consentimento, anexos e garantia.
Remap ECU prioritário: cliente/veículo/OS; identificação fabricante/modelo/hardware/software/calibração; protocolo, ferramenta, técnico e data; tipo configurável, objetivo e alterações efetivamente realizadas; arquivos original/backup/modificado com versões, SHA-256, tamanho e técnico. Proteger originais de sobrescrita. Potência/torque apenas com origem, distinguindo medido, estimado e indisponível; checklists, DTCs, testes e autorização. TCU segue estrutura semelhante.
Manutenção: checklist, peças, óleos, custo/preço, mão de obra, responsável e próxima revisão; baixa de estoque somente no consumo confirmado.
Diagnóstico: sintomas, equipamento, DTCs, testes e medições, hipóteses e causa, laudo/anexos; não simular scanner.
Estoque: produtos, unidade, custo/preço, estoque mínimo, fornecedores e movimentos com justificativa.
Ponto: matrícula/PIN individual, entrada/saída almoço/retorno/saída, validação de estado, horário do servidor e auditoria. Não afirmar conformidade trabalhista automática.
Financeiro/dashboard: indicadores calculados a partir dos dados reais, filtros e exportações quando funcionais; nunca valores fixos apresentados como produção.

## Dados, segurança e distribuição
UUIDs/FKs/constraints/índices; exclusão lógica quando relevante; datas com fuso definido; dinheiro sem float impreciso. Arquivos grandes sob controle do backend, fora da instalação; banco guarda metadados/hash/versão. Login individual, Argon2id/BCrypt, RBAC admin/gerente/recepção/mecânico, auditoria e mínimo necessário de dados pessoais; não registrar segredos.
API V1 apenas 127.0.0.1, CORS restrito e proteção contra chamadas locais não autorizadas. Tauri gerencia inicialização, saúde, conflitos de porta/instâncias, reinício e encerramento seguro do Spring. Dados em diretório do usuário, por exemplo LOCALAPPDATA/AutoGestaoLocal/data. Backup consistente de SQLite em uso e restauração testada. Nunca compartilhar SQLite na rede para escrita simultânea.
Empacotar runtime Java; cliente final não instala Java/Maven/Node/Python/Rust/banco separadamente. Verificar WebView2. Essencial offline; CEP, mensageria e atualizações opcionais. LAN futura com autenticação, TLS e migração validada.
Não implementar recursos destinados a desativar emissões/segurança ou burlar inspeções.

## Colaboração e aceite
Preservar mudanças existentes. Documentar comandos exatos de execução, migrations, testes e build. Executar checks pertinentes e relatar somente testes efetivamente executados; não afirmar que exe foi testado sem teste Windows real. Não fazer push/deploy sem autorização. Dados demonstrativos isolados e identificados; não usar localStorage como persistência de produção. MVP deve manter cliente/veículo/OS/remap após reinício. Documentar placeholders e pendências.
Campos técnicos, ferramentas, stages/preços, garantia, etapas OS, papéis, regras de ponto, indicadores e backup devem ser configuráveis e validados com proprietário ao longo do projeto.
