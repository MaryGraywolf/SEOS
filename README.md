# 🚀 SEOS — Mini-ORM Framework

[![CI Status](https://github.com/MaryGraywolf/SEOS/actions/workflows/ci_integracao_e_qualidade.yml/badge.svg)](https://github.com/MaryGraywolf/SEOS/actions/workflows/ci_integracao_e_qualidade.yml)
[![CD Status](https://github.com/MaryGraywolf/SEOS/actions/workflows/cd_entrega_continua.yml/badge.svg)](https://github.com/MaryGraywolf/SEOS/actions/workflows/cd_entrega_continua.yml)
[![GitHub Release](https://img.shields.io/github/v/release/MaryGraywolf/SEOS?include_prereleases&label=version&color=blue)](https://github.com/MaryGraywolf/SEOS/releases)
![Java](https://img.shields.io/badge/Java-8%20%7C%2011-ED8B00?logo=openjdk&logoColor=white)
[![License: MIT](https://img.shields.io/github/license/MaryGraywolf/SEOS?color=green)](LICENSE)

O **SEOS** é um micro-framework **Mini-ORM híbrido para Java/JDBC**, leve, sem dependências externas em tempo de execução e desenhado com suporte especial às particularidades do **Oracle Database**.

---

## 💡 O que é o SEOS e qual problema ele resolve?

Em sistemas corporativos que exigem controle fino de SQL e transações sobre JDBC/Oracle, o desenvolvedor geralmente enfrenta dois extremos:
1. **ORMs completos (Hibernate/JPA):** Introduzem alto consumo de memória, *dirty checking* implícito, cache de sessão que pode conflitar com triggers/procedures do banco e menor previsibilidade sobre o SQL executado.
2. **JDBC Puro:** Dá controle total sobre o SQL e a conexão, mas exige dezenas de linhas repetitivas e propensas a erros humanos (contagem manual de `?`, índices trocados no `PreparedStatement`, leitura manual de `ResultSet`, `ClassCastException` com `BigDecimal` do Oracle, leitura de `CLOB` e retorno de `ROWID` em sequences).

O **SEOS** ocupa exatamente o meio-termo (**ORM Híbrido**):
- **Automatiza o trabalho mecânico:** Gera `INSERT`, `UPDATE` parcial (*Anti-Null*) e `SELECT` via anotações, faz o bind automático dos parâmetros `?` na ordem exata e converte o `ResultSet` para DTOs, `Map`, valores escalares ou dicionários.
- **Mantém o controle explícito com você:** As cláusulas `WHERE`, ordenações, joins complexos, chamadas `PL/SQL` e o controle transacional da `Connection` continuam 100% explícitos e previsíveis no seu DAO.

### Comparativo Rápido: JDBC Puro vs. SEOS

```java
// ❌ ANTES (JDBC Puro): verboso, repetitivo e frágil a mudanças de colunas
String sql = "INSERT INTO CLIENTES (ID_CLIENTE, NOME, STATUS) VALUES (SEQ_CLIENTE.NEXTVAL, ?, ?)";
try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID_CLIENTE"})) {
    ps.setString(1, cliente.getNome());
    ps.setString(2, cliente.getEnumStatus().name());
    ps.executeUpdate();
    try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
            cliente.setIdCliente(rs.getBigDecimal(1).longValue());
        }
    }
}

// ✅ DEPOIS (Com SEOS): limpo, seguro e preenche o ID gerado no próprio DTO
Long id = SQLBuilder.buildInsert(cliente).executeAndReturnId(conn, cliente);
```

---

## 🧩 Arquitetura: Como a biblioteca funciona?

O SEOS é dividido em **4 componentes principais**:

| Componente | Papel na Arquitetura |
| :--- | :--- |
| **`@TableDB` / `@ColumnDB`** | **Contrato de Mapeamento:** Anotações colocadas no DTO para indicar o nome físico da tabela, as colunas, a chave primária (`isPrimaryKey = true`) e a sequence Oracle (`sequence = "SEQ_X.NEXTVAL"`). |
| **`SQLBuilder`** | **Gerador de SQL:** Lê as anotações do DTO via reflexão leve e monta comandos `SELECT`, `INSERT`, `UPDATE` parcial e `Batch`, retornando um objeto `SQLResult`. |
| **`SQLResult`** | **Executor e Mapeador SQL:** Guarda o SQL gerado (ou nativo) e a lista ordenada de parâmetros. Valida a contagem de placeholders `?`, executa o `PreparedStatement`, audita o tempo de execução e converte o `ResultSet` para DTO, `Map`, escalar ou dicionário. |
| **`SQLCall`** | **Executor PL/SQL:** Builder fluente para chamadas de Stored Procedures e Packages (`{call PROC(?, ?)}`) com suporte a parâmetros `IN`, `OUT` e `IN OUT` lidos por alias. |

> 🔗 **Regra de Ouro sobre Conexões (`java.sql.Connection`):**
> O SEOS **nunca fecha, dá commit ou rollback na sua `Connection`**. Ele fecha automaticamente apenas os recursos internos (`PreparedStatement`, `CallableStatement` e `ResultSet` via *try-with-resources*). Isso garante que você possa passar a conexão gerenciada pelo seu serviço, pool de conexões (`DataSource`) ou transação manual sem interferir na transação em andamento.

---

## 📦 Instalação

Adicione a dependência no seu `pom.xml`:

```xml
<dependency>
    <groupId>com.github.marygraywolf</groupId>
    <artifactId>seos</artifactId>
    <version>1.1.0</version>
</dependency>
```

> **Compatibilidade Java:** O projeto compila por padrão em **Java 11** (profile `java11`), mas possui suporte total ao **Java 8** via profile Maven (`mvn clean package -Pjava8`).

---

## ⚙️ Inicialização (Configurando o Log)

A biblioteca é livre de dependências externas em tempo de execução e já conta com um logger padrão no console (`DefaultConsoleLogger`). Para integrar os logs de erro e auditoria de lentidão ao mecanismo de observabilidade da sua aplicação (ex: SLF4J, Log4j ou logger próprio), injete uma implementação de `OrmLogger` na inicialização:

```java
import com.github.marygraywolf.seos.config.OrmConfig;
import com.github.marygraywolf.seos.spi.OrmLogger;

public class AppInitializer {
    public static void init() {
        OrmConfig.setLogger(new OrmLogger() {
            @Override
            public void info(String origin, String code, String msg, Object... params) {
                logger.info("{} [{}] {}", origin, code, msg);
            }

            @Override
            public void warn(String origin, String code, String msg, Object... params) {
                logger.warn("{} [{}] {}", origin, code, msg);
            }

            @Override
            public void error(String origin, String code, String msg, Exception e, Object... params) {
                logger.error("{} [{}] {}", origin, code, msg, e);
            }
        });
    }
}
```

---

## 🛠️ Guia Passo a Passo de Uso

### 1. Mapeando suas Entidades (DTOs)

Utilize as anotações `@TableDB` na classe e `@ColumnDB` nos atributos que correspondem às colunas físicas do banco de dados. Atributos sem `@ColumnDB` são ignorados pelo SEOS (podem ser usados livremente como campos auxiliares no DTO):

```java
import com.github.marygraywolf.seos.annotations.ColumnDB;
import com.github.marygraywolf.seos.annotations.TableDB;
import lombok.Data;

@Data
@TableDB(name = "CLIENTES")
public class Cliente {

    @ColumnDB(localName = "ID_CLIENTE", sequence = "SEQ_CLIENTE.NEXTVAL", isPrimaryKey = true)
    private Long idCliente;

    @ColumnDB(localName = "NOME")
    private String nome;

    @ColumnDB(localName = "STATUS")
    private StatusCliente enumStatus; // Suporte nativo a Enums!
}
```

> **Dica Oracle:** Mantenha o valor de `localName` em **letras maiúsculas** (ex: `"ID_CLIENTE"`), especialmente na chave primária (`isPrimaryKey = true`), pois o dicionário de dados do Oracle exige o nome da coluna em maiúsculas ao recuperar chaves geradas por sequence.

---

### 2. DQL: Consultas (`SELECT`)

Você pode usar a **Fluent API** (`SQLBuilder.select`), o método direto (`SQLBuilder.buildSelect`) ou escrever seu **SQL nativo** diretamente em `SQLResult`.

#### Modo Fluente (`SelectBuilder`)

Permite buscar todas as colunas mapeadas no DTO ou projetar apenas atributos específicos (informando os **nomes dos atributos Java** da classe):

```java
import com.github.marygraywolf.seos.core.SQLBuilder;
import com.github.marygraywolf.seos.core.SQLResult;

// 1. Projetando todas as colunas @ColumnDB para mapear o DTO completo
SQLResult result = SQLBuilder.select(Cliente.class)
        .where("STATUS = ?", "ATIVO")
        .build();

// Retorna lista de DTOs
List<Cliente> clientes = result.executeQuery(conn, Cliente.class);

// Ou retorna apenas o primeiro registro encontrado (ou null)
Cliente unico = result.executeQuerySingle(conn, Cliente.class);

// 2. Projetando apenas um subconjunto de atributos com .campos(...)
// Nota: como executeQuery(conn, Classe) exige todas as colunas @ColumnDB no ResultSet,
// projeções parciais devem ser lidas como Map, Dicionário ou Escalar:
SQLResult parcial = SQLBuilder.select(Cliente.class)
        .campos("idCliente", "nome")
        .where("STATUS = ?", "ATIVO")
        .build();

List<Map<String, Object>> resumo = parcial.executeQueryAsMap(conn);
```

#### Modo Direto (`buildSelect`)

Projeta todas as colunas anotadas com `@ColumnDB` no DTO evitando `SELECT *`:

```java
SQLResult result = SQLBuilder.buildSelect(Cliente.class, "STATUS = ? ORDER BY ID_CLIENTE DESC", "ATIVO");
List<Cliente> clientes = result.executeQuery(conn, Cliente.class);
```

#### Modo SQL Nativo (Mapas, Escalares e Dicionários)

Ideal para relatórios dinâmicos, contagens ou consultas avulsas onde criar um DTO seria desnecessário:

```java
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// 1. Valor Escalar (primeira coluna da primeira linha, com conversão automática de tipo)
Integer total = new SQLResult("SELECT COUNT(1) FROM CLIENTES WHERE STATUS = ?", Arrays.asList("ATIVO"))
        .executeScalar(conn, Integer.class);

// 2. Dicionário Chave-Valor (1ª coluna = chave, 2ª coluna = valor; chaves nulas são ignoradas)
Map<Long, String> mapaClientes = new SQLResult("SELECT ID_CLIENTE, NOME FROM CLIENTES", null)
        .executeAsDictionary(conn);

// 3. Lista de Mapas (chaves das colunas padronizadas em UPPERCASE)
List<Map<String, Object>> linhas = new SQLResult("SELECT ID_CLIENTE, NOME FROM CLIENTES WHERE STATUS = ?", Arrays.asList("ATIVO"))
        .executeQueryAsMap(conn);

// 4. Mapa Único (primeira linha como Map<String, Object> ou null)
Map<String, Object> clienteMap = new SQLResult("SELECT ID_CLIENTE, NOME FROM CLIENTES WHERE ID_CLIENTE = ?", Arrays.asList(10L))
        .executeQueryAsSingleMap(conn);
```

---

### 3. DML: Inserções e Atualizações (`INSERT` / `UPDATE`)

O framework utiliza uma estratégia **Anti-Null**: apenas os atributos populados (não nulos) no DTO entram na instrução SQL gerada. No `INSERT`, atributos nulos que possuam `sequence` configurada em `@ColumnDB` têm a expressão da sequence injetada automaticamente no `VALUES`.

```java
// INSERT simples
Cliente novo = new Cliente();
novo.setNome("Empresa XYZ");
int linhasInseridas = SQLBuilder.buildInsert(novo).executeUpdate(conn);

// INSERT retornando o ID gerado pela sequence (compatível com Oracle sem retornar ROWID)
// Exige @ColumnDB(isPrimaryKey = true) no atributo de chave primária.
// Além de retornar o ID como Long, preenche automaticamente o atributo no próprio DTO!
Cliente comId = new Cliente();
comId.setNome("Empresa XYZ");
Long idGerado = SQLBuilder.buildInsert(comId).executeAndReturnId(conn, comId);
// comId.getIdCliente() agora contém o mesmo valor de idGerado

// UPDATE parcial (cria o SET apenas com os campos não-nulos do DTO; cláusula WHERE é obrigatória)
Cliente patch = new Cliente();
patch.setEnumStatus(StatusCliente.INATIVO);
int linhasAfetadas = SQLBuilder.buildUpdate(patch, "ID_CLIENTE = ?", 12345L).executeUpdate(conn);
```

---

### 4. DML em Lote (`Execute Batch`)

Perfeito para inserir grandes volumes de registros de uma única vez utilizando `addBatch()` e `executeBatch()` do JDBC.

> **Nota sobre o Batch:** Diferentemente do `buildInsert` individual, o `buildInsertBatch` inclui todas as colunas `@ColumnDB` no comando SQL único, definindo a estrutura a partir do primeiro item da lista (`lote.get(0)`). Se um campo possuir `sequence` e estiver `null` no primeiro item, a sequence será usada para todas as linhas do lote.

```java
List<Cliente> lote = new ArrayList<>();
// ... popule os itens na lista ...

SQLResult.Batch batch = SQLBuilder.buildInsertBatch(lote);
int[] resultados = batch.executeBatch(conn);
```

---

### 5. Stored Procedures (`PL/SQL`)

Execute chamadas de procedures e packages com fluidez através de `SQLCall`, suportando parâmetros de entrada (`IN`), saída (`OUT`) e entrada/saída (`IN OUT`):

```java
import com.github.marygraywolf.seos.core.SQLCall;
import java.math.BigDecimal;
import java.sql.Types;

SQLCall.Result retorno = SQLCall.name("STP_PROCESSAR_FECHAMENTO")
        .in(12345L)                                              // Parâmetro 1 (IN)
        .in("FATURAR")                                           // Parâmetro 2 (IN)
        .out("PO_MENSAGEM", Types.VARCHAR)                       // Parâmetro 3 (OUT)
        .inOut("PIO_TAXA", new BigDecimal("1.5"), Types.NUMERIC) // Parâmetro 4 (IN OUT)
        .execute(conn);

String mensagem = retorno.getString("PO_MENSAGEM");
BigDecimal taxaFinal = retorno.getBigDecimal("PIO_TAXA");
Long taxaLong = retorno.getLong("PIO_TAXA");
Object valorBruto = retorno.getObject("PO_MENSAGEM");
```

---

### 6. Exemplo Prático Completo: Estruturando um DAO

A recomendação arquitetural do SEOS é concentrar as operações de banco em classes **DAO** (*Data Access Object*), recebendo a `Connection` da camada de serviço/regra de negócio:

```java
import com.github.marygraywolf.seos.core.SQLBuilder;
import com.github.marygraywolf.seos.core.SQLResult;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

public class ClienteDAO {

    public Long criar(Connection conn, Cliente novoCliente) throws Exception {
        return SQLBuilder.buildInsert(novoCliente).executeAndReturnId(conn, novoCliente);
    }

    public Cliente buscarPorId(Connection conn, Long idCliente) throws Exception {
        return SQLBuilder.buildSelect(Cliente.class, "ID_CLIENTE = ?", idCliente)
                .executeQuerySingle(conn, Cliente.class);
    }

    public List<Cliente> listarAtivos(Connection conn) throws Exception {
        return SQLBuilder.select(Cliente.class)
                .where("STATUS = ? ORDER BY NOME ASC", StatusCliente.ATIVO.name())
                .build()
                .executeQuery(conn, Cliente.class);
    }

    public void inativar(Connection conn, Long idCliente) throws Exception {
        Cliente patch = new Cliente();
        patch.setEnumStatus(StatusCliente.INATIVO);
        SQLBuilder.buildUpdate(patch, "ID_CLIENTE = ?", idCliente).executeUpdate(conn);
    }

    public void excluir(Connection conn, Long idCliente) throws Exception {
        new SQLResult("DELETE FROM CLIENTES WHERE ID_CLIENTE = ?", Arrays.asList(idCliente))
                .executeUpdate(conn);
    }
}
```

> 🔍 **Dica de Debug e Testes Unitários:** Antes de executar qualquer comando no banco, você pode inspecionar o SQL gerado e os parâmetros através de `result.getSql()` e `result.getParams()` (ou `batch.getSql()` e `batch.getBatchParams()` em lotes).

---

## 🔄 Conversões Automáticas de Tipos

Ao mapear um `ResultSet` para DTOs (`executeQuery`, `executeQuerySingle`, `mapearResultSet`) ou ler escalares e chaves geradas (`executeScalar`, `executeAndReturnId`), o framework trata automaticamente as diferenças comuns entre tipos do driver JDBC/Oracle e tipos Java:

| Tipo no Banco / JDBC                   | Tipo de Destino Java                                        | Comportamento                                                                             |
| :------------------------------------- | :---------------------------------------------------------- | :---------------------------------------------------------------------------------------- |
| `VARCHAR` / `CHAR`                     | `Enum`                                                      | Converte via `Enum.valueOf` no DTO (valores inválidos resultam em `null`)                 |
| `NUMBER` (`BigDecimal`, etc.)          | `Long`, `Integer`, `Double`, `Float`                        | Conversão numérica automática em DTOs e escalares sem `ClassCastException`                |
| `NUMBER` / `VARCHAR`                   | `Short`, `BigDecimal`, `String` (e primitivos equivalentes) | Suportados em leituras escalares (`executeScalar`) e retorno de ID (`executeAndReturnId`) |
| `CLOB` (`java.sql.Clob`)               | `String`                                                    | Extrai o conteúdo textual completo automaticamente no mapeamento de DTOs                  |
| `TIMESTAMP` (`java.sql.Timestamp`)     | `String`, `java.util.Date`, `Timestamp`                     | No DTO: se `String`, formata para `"dd/MM/yyyy HH:mm:ss"`                                 |
| `DATE` (`java.sql.Date` / `util.Date`) | `String`, `java.util.Date`, `sql.Date`                      | No DTO: se `String`, formata para `"dd/MM/yyyy"`                                          |

---

## 🔒 Auditoria, Performance e Segurança (Padrão LGPD)

- **Prevenção contra SQL Injection e Vazamento de Dados:** Todos os valores são trafegados exclusivamente via `PreparedStatement` / `CallableStatement` e validados quanto à contagem de placeholders `?`. Os logs de erro e lentidão registram apenas a quantidade de parâmetros (`Params: N`), garantindo que dados sensíveis nunca vazem nos logs do servidor.
- **Rastreabilidade de Pilha (_Stack Trace Inspection_):** Se uma query ou procedure falhar ou apresentar lentidão, o `OrmLogger` registra exatamente a `Classe.metodo` da sua aplicação que originou a chamada:
  - `SQL-ERR-001` / `PLSQL-ERR-001`: Falha na execução SQL (`SQLResult`) ou PL/SQL (`SQLCall`).
  - `SQL-WARN-001`: _Slow Query_ detectada (tempo de execução superior a **10s**).
  - `PLSQL-WARN-001`: _Procedure Lenta_ detectada (tempo de execução superior a **3s**).
