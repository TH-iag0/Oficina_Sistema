# Conexão PostgreSQL 17

O usuário confirmou o banco `Oficina_Felipe` no PostgreSQL 17. O pgAdmin 4 administra o banco; o Spring Boot se conecta diretamente ao PostgreSQL por JDBC.

| Configuração | Padrão |
|---|---|
| Host | 127.0.0.1 |
| Porta | 5433, conforme `postgresql.conf` da instalação 17 |
| Banco | Oficina_Felipe, preservando maiúsculas e minúsculas do nome informado |
| Usuário | postgres, configurável por variável de ambiente |
| Senha | AUTOGESTAO_DB_PASSWORD, obrigatória e sem valor padrão |
| Driver | org.postgresql.Driver |
| URL | jdbc:postgresql://127.0.0.1:5433/Oficina_Felipe |

O `pom.xml` usa o driver PostgreSQL e o módulo Flyway PostgreSQL. As dependências MySQL foram retiradas. Os models UUID continuam mapeados como `CHAR(36)` nesta etapa, compatível com PostgreSQL; não houve mudança de tipo em tabelas existentes.

## Verificar autenticação

No terminal, dentro da pasta do projeto:

```powershell
.\scripts\start-backend.ps1 -CheckConnectionOnly
```

O script solicita a senha de forma oculta e executa somente `SELECT current_database(), current_user, version()`. Não grava credenciais e não cria tabelas. Se o usuário do banco for outro, definir antes:

```powershell
$env:AUTOGESTAO_DB_USER = 'seu_usuario'
```

## Iniciar o backend

```powershell
.\scripts\start-backend.ps1 -JdkHome 'C:\Users\Pichau\.jdks\ms-21.0.12.1'
```

O script solicita a senha, inicia o Maven Wrapper e mantém a credencial disponível apenas durante a execução do backend. Valores anteriores do ambiente são restaurados ao encerrar. Para iniciar pelo IntelliJ, configurar `AUTOGESTAO_DB_PASSWORD` nas variáveis de ambiente da execução; não inserir a senha em arquivos versionados.

Host, porta, banco e usuário podem ser alterados por `AUTOGESTAO_DB_HOST`, `AUTOGESTAO_DB_PORT`, `AUTOGESTAO_DB_NAME` e `AUTOGESTAO_DB_USER`.

## Estado da configuração

`ddl-auto: none` impede alterações automáticas de tabelas pelo Hibernate. Flyway está desativado por padrão enquanto não há migrations; `AUTOGESTAO_FLYWAY_ENABLED` permite ativá-lo depois de preparar o esquema versionado. A configuração desta etapa não cria o banco nem as tabelas dos models. Se o banco estiver vazio, as consultas dos repositories dependem da próxima etapa de migrations.

O pool permite até cinco conexões e usa UTC na persistência. A API está vinculada a `127.0.0.1`. PostgreSQL é o único banco de produção; H2 continua somente nos testes, agora em modo de compatibilidade PostgreSQL.

Em 11/10/2026, o serviço `postgresql-x64-17` foi observado ativo, e `pg_isready` confirmou que a porta 5433 aceita conexões. A consulta autenticada não pôde ser executada sem senha (`no password supplied`); portanto, a conexão autenticada e a inicialização completa do backend ainda não estão comprovadas.

Os 75 testes locais passaram: 56 dos models, cinco da configuração e 14 de integração dos repositories com H2. A execução usa o fallback com JUnit 5.12.2; não equivale a teste em PostgreSQL real nem ao build completo Maven.

```powershell
.\scripts\test-models-offline.ps1 -JdkHome 'C:\Users\Pichau\.jdks\ms-21.0.12.1' -IncludeRepositories
```
