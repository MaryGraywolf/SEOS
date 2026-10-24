# 🚀 SEOS — Mini-ORM Framework

Um micro-framework ORM (_Object-Relational Mapping_) leve, rápido e 100% focado em produtividade para Java/JDBC (com suporte especial a particularidades do Oracle e Sankhya). Desenvolvido para eliminar o boilerplate de código JDBC manual, mapear `ResultSet` automaticamente para DTOs ou `Map` e padronizar chamadas a banco de dados e Procedures (`PL/SQL`).

---

## 📦 Instalação

Adicione a dependência no seu `pom.xml`:

```xml
<dependency>
    <groupId>com.github.marygraywolf</groupId>
    <artifactId>seos</artifactId>
    <version>1.0.3</version>
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

## 🛠️ Guia Rápido de Uso

### 1. Mapeando suas Entidades (DTOs)

Utilize as anotações `@TableDB` e `@ColumnDB` para mapear a classe e seus atributos para as colunas físicas do banco de dados e sequences:

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

Permite projetar apenas alguns atributos do DTO (informando os **nomes dos atributos Java** da classe) ou buscar todas as colunas mapeadas:

```java
import com.github.marygraywolf.seos.core.SQLBuilder;
import com.github.marygraywolf.seos.core.SQLResult;

SQLResult result = SQLBuilder.select(Cliente.class)
        .campos("idCliente", "nome") // Nomes dos campos no DTO (opcional: se omitido, projeta todas as @ColumnDB)
        .where("STATUS = ?", "ATIVO")
        .build();

// Retorna lista de DTOs
List<Cliente> clientes = result.executeQuery(conn, Cliente.class);

// Ou retorna apenas o primeiro registro encontrado (ou null)
Cliente unico = result.executeQuerySingle(conn, Cliente.class);
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

Perfeito para inserir grandes volumes de registros de uma única vez utilizando `addBatch()` e `executeBatch()` do JDBC:

```java
List<Cliente> lote = new ArrayList<>();
// ... popule os itens na lista ...

SQLResult.Batch batch = SQLBuilder.buildInsertBatch(lote);
int[] resultados = batch.executeBatch(conn);
```

---

### 5. Stored Procedures (`PL/SQL`)

Execute chamadas de procedures e packages com fluidez, suportando parâmetros de entrada (`IN`), saída (`OUT`) e entrada/saída (`IN OUT`):

```java
import com.github.marygraywolf.seos.core.PLSQLCall;
import java.math.BigDecimal;
import java.sql.Types;

PLSQLCall.Result retorno = PLSQLCall.name("STP_PROCESSAR_FECHAMENTO")
        .in(12345L)                                     // Parâmetro 1 (IN)
        .in("FATURAR")                                  // Parâmetro 2 (IN)
        .out("PO_MENSAGEM", Types.VARCHAR)              // Parâmetro 3 (OUT)
        .inOut("PIO_TAXA", new BigDecimal("1.5"), Types.NUMERIC) // Parâmetro 4 (IN OUT)
        .execute(conn);

String mensagem = retorno.getString("PO_MENSAGEM");
BigDecimal taxaFinal = retorno.getBigDecimal("PIO_TAXA");
Long taxaLong = retorno.getLong("PIO_TAXA");
Object valorBruto = retorno.getObject("PO_MENSAGEM");
```

---

## 🔄 Conversões Automáticas de Tipos

Ao mapear um `ResultSet` para DTOs (`executeQuery`, `executeQuerySingle`, `mapearResultSet`) ou ler escalares (`executeScalar`), o framework trata automaticamente as diferenças comuns entre tipos do driver JDBC/Oracle e tipos Java:

| Tipo no Banco / JDBC                   | Tipo no DTO Java                              | Comportamento                                                      |
| :------------------------------------- | :-------------------------------------------- | :----------------------------------------------------------------- |
| `VARCHAR` / `CHAR`                     | `Enum`                                        | Converte via `Enum.valueOf` (valores inválidos resultam em `null`) |
| `NUMBER` (`BigDecimal`, etc.)          | `Long`, `Integer`, `Double`, `Float`, `Short` | Conversão numérica automática sem `ClassCastException`             |
| `CLOB` (`java.sql.Clob`)               | `String`                                      | Extrai o conteúdo textual completo automaticamente                 |
| `TIMESTAMP` (`java.sql.Timestamp`)     | `String`, `java.util.Date`, `Timestamp`       | Se `String`, formata para `"dd/MM/yyyy HH:mm:ss"`                  |
| `DATE` (`java.sql.Date` / `util.Date`) | `String`, `java.util.Date`, `sql.Date`        | Se `String`, formata para `"dd/MM/yyyy"`                           |

---

## 🔒 Auditoria, Performance e Segurança (Padrão LGPD)

- **Prevenção contra SQL Injection e Vazamento de Dados:** Todos os valores são trafegados exclusivamente via `PreparedStatement` / `CallableStatement` e validados quanto à contagem de placeholders `?`. Os logs de erro e lentidão registram apenas a quantidade de parâmetros (`Params: N`), garantindo que dados sensíveis nunca vazem nos logs do servidor.
- **Rastreabilidade de Pilha (_Stack Trace Inspection_):** Se uma query ou procedure falhar ou apresentar lentidão, o `OrmLogger` registra exatamente a `Classe.metodo` da sua aplicação que originou a chamada:
  - `SQL-ERR-001` / `PLSQL-ERR-001`: Falha na execução SQL ou PL/SQL.
  - `SQL-WARN-001`: _Slow Query_ detectada (tempo de execução superior a **10s**).
  - `PLSQL-WARN-001`: _Procedure Lenta_ detectada (tempo de execução superior a **3s**).
