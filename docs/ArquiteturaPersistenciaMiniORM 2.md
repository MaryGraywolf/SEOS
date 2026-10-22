# Arquitetura de Persistência Customizada: MiniORM híbrido JDBC

**Escopo:** documentação técnica, guia de uso e defesa arquitetural da camada de persistência baseada em JDBC, reflexão e anotações próprias.

**Contexto:** customizações Sankhya sobre Oracle 19c, nas quais previsibilidade transacional, baixo overhead e controle do SQL são requisitos importantes.

**Implementação de referência:** `SQLBuilder`, `SQLResult`, `@TableDB`, `@ColumnDB`, `SolicitacaoRevAdmDAO`, `FinanceiroTBMDAO`, `ParceiroDAO` e as entidades anotadas do projeto.

---

## 1. Descrição e motivo da existência

O MiniORM é a **camada padronizada de persistência JDBC para as extensões Sankhya deste projeto**. Foi desenhado para o contexto operacional já adotado pela aplicação: Oracle 19c, conexões fornecidas pela plataforma, transações controladas pelo serviço e necessidade de SQL previsível. Seu objetivo é automatizar as partes mecânicas e mais sujeitas a erro desse acesso:

- geração de `INSERT`, `UPDATE` parcial e `SELECT` a partir de classes anotadas;
- associação ordinal entre placeholders e parâmetros;
- execução padronizada de comandos e consultas;
- mapeamento simples de `ResultSet` para DTO, mapa, escalar ou dicionário;
- conversão de tipos frequentes do Oracle;
- inserção em lote;
- auditoria básica de erro, origem e duração de consultas.

O motivo do MiniORM é **reduzir boilerplate e defeitos do JDBC sem retirar da equipe o controle sobre SQL, conexão e transação**. Ele ocupa deliberadamente o espaço entre o JDBC repetitivo e um ORM de propósito geral: entrega a automação necessária às rotinas do ERP sem introduzir um segundo modelo de gerenciamento de entidades sobre o ambiente Sankhya.

O DTO anotado é a fonte de verdade do **mapeamento gerado de uma tabela**: nome físico, colunas, tipos Java e sequences. Ele não é a única fonte de verdade de toda a persistência, porque cláusulas `WHERE`, ordenações e consultas SQL livres continuam explícitas no DAO. Essa delimitação é intencional.

### Fronteiras deliberadas da solução

O MiniORM não replica funcionalidades que não são necessárias ao seu papel ou que já pertencem a outra camada do projeto:

| Tema | Decisão adotada | Razão no contexto atual |
|---|---|---|
| Relacionamentos e joins | Permanecem explícitos no SQL e no DAO. | Preserva as consultas Oracle já conhecidas e evita carregamentos implícitos. |
| Estado de entidades | Operações são independentes, sem dirty checking ou `flush`. | Torna cada escrita visível no código e previsível dentro de ações e jobs. |
| Linguagem de consulta | Usa SQL parametrizado em vez de JPQL/HQL. | O banco-alvo é Oracle e as rotinas utilizam recursos específicos desse ambiente. |
| Cache de entidades | Não mantém cache próprio. | Evita estado duplicado ou desatualizado em relação ao ERP. |
| Concorrência | Regras de versão e lock são expressas na SQL quando necessárias. | Mantém a condição concorrente junto da regra de negócio que a exige. |
| Schema | Validação e migração ficam no processo de banco. | O MiniORM atua em runtime e não substitui governança de DDL. |
| Conexão e transação | Permanecem com o chamador. | Permite participar da conexão e do limite transacional definidos pela plataforma. |
| Portabilidade | Prioriza Oracle/Sankhya. | Portabilidade entre SGBDs não é requisito desta implementação. |

Essas fronteiras estabelecem responsabilidades claras, evitam sobreposição com a plataforma e mantêm a camada de persistência objetiva.

---

## 2. Componentes e contrato de mapeamento

### `@TableDB`

Declara no DTO o nome físico da tabela usada pelo `SQLBuilder`.

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TableDB {
    String name();
}
```

Uma classe usada na geração de SQL deve possuir essa anotação. A ausência provoca `IllegalArgumentException` antes da execução no banco.

### `@ColumnDB`

Associa um campo Java à coluna física e, opcionalmente, a uma expressão de sequence Oracle.

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnDB {
    String localName();
    String sequence() default "";
}
```

Exemplo:

```java
@TableDB(name = "AD_SOLICITREVADM")
public class SolicitacaoRevAdmDTO {

    @ColumnDB(localName = "ID", sequence = "SEQ_AD_SOLICITREVADM.NEXTVAL")
    private Integer id;

    @ColumnDB(localName = "NUNOTA")
    private Integer nuNota;

    @ColumnDB(localName = "DH_RESOLUCAO")
    private Timestamp dhResolucao;
}
```

A sequence é inserida como expressão SQL, sem placeholder. Ela deve ser uma constante versionada no código e conter a expressão completa, como `SEQ_AD_SOLICITREVADM.NEXTVAL`; nunca deve receber conteúdo do usuário.

### `SQLBuilder`

Lê as anotações e produz um `SQLResult`, que reúne a instrução e seus parâmetros na ordem do JDBC. Há uma API direta para operações comuns e um builder fluente para seleção de campos.

### `SQLResult`

Executa SQL gerado ou SQL escrito manualmente. Além de `executeUpdate`, oferece diferentes formatos de leitura: lista tipada, objeto único, mapas, escalar e dicionário.

A conexão sempre pertence ao chamador. Os métodos fecham seus próprios `PreparedStatement` e `ResultSet`, mas **não fecham, confirmam nem revertem a `Connection`**.

Antes da execução, `getSql()` e `getParams()` permitem inspecionar o comando e os binds. Em lotes, `getSql()` e `getBatchParams()` oferecem a mesma visão por linha:

```java
SQLResult result = SQLBuilder.buildSelect(
    SolicitacaoRevAdmDTO.class,
    "ID = ?",
    id
);

String sqlGerado = result.getSql();
List<Object> parametros = result.getParams();

SQLResult.Batch batch = SQLBuilder.buildInsertBatch(dtos);
String sqlDoLote = batch.getSql();
List<List<Object>> parametrosDoLote = batch.getBatchParams();
```

Esses acessores são úteis em testes e diagnóstico. Ao registrar informações, não exponha parâmetros que contenham dados pessoais, credenciais ou conteúdo sensível.

---

## 3. Funções de construção SQL

### 3.1 `buildInsert(dto)` — inserção parcial

Inclui campos anotados com valor não nulo. Um campo nulo sem sequence é omitido; um campo nulo com sequence usa a expressão declarada na anotação.

```java
SolicitacaoRevAdmDTO solicitacao = SolicitacaoRevAdmDTO.builder()
    .nuNota(123456)
    .sequencia(1)
    .dhCriacao(new Timestamp(System.currentTimeMillis()))
    .build();

int inseridas = SQLBuilder
    .buildInsert(solicitacao)
    .executeUpdate(conn);
```

SQL conceitual:

```sql
INSERT INTO AD_SOLICITREVADM (ID, NUNOTA, SEQUENCIA, DH_CRIACAO)
VALUES (SEQ_AD_SOLICITREVADM.NEXTVAL, ?, ?, ?)
```

Se nenhum campo puder ser persistido, o builder lança `IllegalStateException`.

### 3.2 `buildUpdate(dto, whereSql, whereParams)` — atualização parcial

Gera o `SET` somente com campos anotados e não nulos. O `WHERE` é obrigatório para impedir atualização irrestrita.

```java
SolicitacaoRevAdmDTO patch = SolicitacaoRevAdmDTO.builder()
    .dhResolucao(new Timestamp(System.currentTimeMillis()))
    .build();

int atualizadas = SQLBuilder
    .buildUpdate(patch, "ID = ?", idSolicitacao)
    .executeUpdate(conn);
```

SQL conceitual:

```sql
UPDATE AD_SOLICITREVADM SET DH_RESOLUCAO = ? WHERE ID = ?
```

Valores nulos significam “não alterar”. Portanto, a API atual não representa `SET COLUNA = NULL`; para esse caso, use SQL explícito com `SQLResult`.

### 3.3 `buildSelect(classe, whereSql, whereParams)` — seleção completa do DTO

Projeta todas as colunas anotadas, evitando `SELECT *`. O filtro é opcional.

```java
List<SolicitacaoRevAdmDTO> solicitacoes = SQLBuilder
    .buildSelect(SolicitacaoRevAdmDTO.class, "NUNOTA = ? ORDER BY ID DESC", nuNota)
    .executeQuery(conn, SolicitacaoRevAdmDTO.class);
```

Sem filtro:

```java
List<SolicitacaoRevAdmDTO> todas = SQLBuilder
    .buildSelect(SolicitacaoRevAdmDTO.class, null)
    .executeQuery(conn, SolicitacaoRevAdmDTO.class);
```

### 3.4 `select(classe).campos(...).where(...).build()` — seleção fluente e projeção parcial

Permite escolher campos específicos. `campos(...)` recebe **nomes dos atributos Java**, não nomes de colunas Oracle.

```java
SQLResult consulta = SQLBuilder
    .select(SolicitacaoRevAdmDTO.class)
    .campos("id", "nuNota", "dhResolucao")
    .where("NUNOTA = ? ORDER BY ID DESC", nuNota)
    .build();

List<SolicitacaoRevAdmDTO> resumo = consulta
    .executeQuery(conn, SolicitacaoRevAdmDTO.class);
```

Campos anotados que não estiverem na projeção permanecem com o valor padrão no DTO. Se `campos(...)` não for chamado, todas as colunas anotadas são projetadas. Na implementação atual, chame `where(...)` apenas uma vez por builder: uma segunda chamada substitui o texto do filtro, mas acumula parâmetros.

### 3.5 `buildInsertBatch(dtos)` — inserção em lote

Prepara uma única instrução e executa cada linha por `PreparedStatement.addBatch()`.

```java
List<AuditoriaFolhaPagamento> auditorias = carregarAuditorias();

boolean autoCommitOriginal = conn.getAutoCommit();
try {
    conn.setAutoCommit(false);

    int[] resultados = SQLBuilder
        .buildInsertBatch(auditorias)
        .executeBatch(conn);

    conn.commit();
} catch (Exception e) {
    conn.rollback();
    throw e;
} finally {
    conn.setAutoCommit(autoCommitOriginal);
}
```

A forma das colunas e placeholders é definida pelo primeiro DTO. Todos os itens devem ser não nulos, da mesma classe e homogêneos quanto ao uso de sequences. Diferentemente de `buildInsert`, o batch inclui todas as colunas anotadas; valores nulos podem ser enviados como parâmetros. Para registros esparsos com formatos diferentes, normalize os objetos ou faça inserts individuais.

### 3.6 SQL explícito — `DELETE`, `MERGE`, agregações e consultas especializadas

Não há builders próprios para `DELETE` ou `MERGE`. A extensão prevista é criar um `SQLResult` diretamente, preservando parametrização e execução padronizada.

```java
int removidas = new SQLResult(
    "DELETE FROM AD_SOLICITREVADM WHERE ID = ?",
    java.util.Arrays.asList(idSolicitacao)
).executeUpdate(conn);
```

```java
int afetadas = new SQLResult(
    "MERGE INTO AD_EXEMPLO D "
        + "USING (SELECT ? ID, ? VALOR FROM DUAL) O "
        + "ON (D.ID = O.ID) "
        + "WHEN MATCHED THEN UPDATE SET D.VALOR = O.VALOR "
        + "WHEN NOT MATCHED THEN INSERT (ID, VALOR) VALUES (O.ID, O.VALOR)",
    java.util.Arrays.asList(id, valor)
).executeUpdate(conn);
```

---

## 4. Formatos de execução e leitura

### 4.1 `executeUpdate(conn)`

Executa qualquer comando DML e retorna a quantidade de linhas afetadas.

```java
int linhas = SQLBuilder.buildInsert(dto).executeUpdate(conn);
```

### 4.2 `executeQuery(conn, classe)`

Retorna uma lista de DTOs. A classe precisa de construtor sem argumentos e campos anotados com `@ColumnDB`.

```java
List<Funcionarios> funcionarios = SQLBuilder
    .buildSelect(Funcionarios.class, "CODBANCO = ?", codBanco)
    .executeQuery(conn, Funcionarios.class);
```

### 4.3 `executeQuerySingle(conn, classe)`

Retorna o primeiro DTO ou `null` quando não houver linhas.

```java
Funcionarios funcionario = SQLBuilder
    .buildSelect(Funcionarios.class, "CGC_CPF = ? AND CODBANCO = ?", cpf, codBanco)
    .executeQuerySingle(conn, Funcionarios.class);
```

O método não valida unicidade e não acrescenta limite à SQL; quando isso for relevante, expresse a regra na consulta.

### 4.4 `executeQueryAsMap(conn)`

Retorna uma lista de linhas dinâmicas. As chaves usam `ResultSetMetaData.getColumnName()` em maiúsculas e os valores permanecem nos tipos fornecidos pelo driver.

```java
List<Map<String, Object>> totais = new SQLResult(
    "SELECT CODEMP, SUM(VLRDOC) TOTAL FROM AD_FOLHAAUD GROUP BY CODEMP",
    java.util.Collections.emptyList()
).executeQueryAsMap(conn);

Object codEmp = totais.get(0).get("CODEMP");
```

Como a implementação usa `getColumnName`, aliases podem não ser as chaves retornadas por todos os drivers. Quando a chave do alias for requisito, valide o comportamento do driver Oracle usado pelo ambiente.

### 4.5 `executeQueryAsSingleMap(conn)`

Retorna o primeiro mapa ou `null`.

```java
Map<String, Object> linha = new SQLResult(
    "SELECT NUNOTA, STATUS FROM AD_PEDIDO WHERE NUNOTA = ?",
    java.util.Arrays.asList(nuNota)
).executeQueryAsSingleMap(conn);
```

Assim como a versão tipada, não impõe unicidade ou limite no banco.

### 4.6 `executeScalar(conn, tipo)`

Retorna a primeira coluna da primeira linha ou `null`.

```java
Integer total = new SQLResult(
    "SELECT COUNT(1) FROM AD_FOLHAAUD WHERE STATUS = ?",
    java.util.Arrays.asList(status.getCodigo())
).executeScalar(conn, Integer.class);
```

A conversão explícita atual cobre `BigDecimal` para `Long`, `Integer`, `Double` e `String`. Nos demais casos, o valor do driver é retornado por cast; escolha um tipo compatível.

### 4.7 `executeAsDictionary(conn)`

Transforma as duas primeiras colunas de cada linha em chave e valor.

```java
Map<Integer, String> descricoes = new SQLResult(
    "SELECT CODTIPOPER, DESCROPER FROM TGFTOP WHERE ATIVO = ?",
    java.util.Arrays.asList("S")
).executeAsDictionary(conn);
```

Chaves nulas são ignoradas e chaves repetidas mantêm o último valor lido. Não há conversão de tipo além do cast genérico.

### 4.8 `mapearResultSet(rs, classe)`

É a função de mapeamento usada internamente por `executeQuery`, mas também pode receber um `ResultSet` já aberto.

```java
try (PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    SQLResult resultado = new SQLResult(sql, java.util.Collections.emptyList());
    return resultado.mapearResultSet(rs, SolicitacaoRevAdmDTO.class);
}
```

Para SQL manual com parâmetros, prefira `executeQuery`, que evita aplicar os valores duas vezes em código diferente.

---

## 5. Mudanças incorporadas pela versão atual

Além do CRUD inicialmente documentado, a implementação corrente contém:

| Mudança | Efeito prático |
|---|---|
| `SelectBuilder` fluente | Permite projeção parcial por nome de campo Java. |
| `executeQuerySingle` | Simplifica leituras tipadas de zero ou um resultado esperado. |
| `executeQueryAsMap` e `executeQueryAsSingleMap` | Atendem consultas dinâmicas sem criar DTO específico. |
| `executeScalar` | Atende `COUNT`, `SUM`, identificadores e outras consultas de uma célula. |
| `executeAsDictionary` | Converte consultas de duas colunas em `Map<K, V>`. |
| Conversão de enums na escrita | Persiste por `getCodigo()`, depois `getId()`, depois `name()`. |
| Conversão de enums na leitura | Resolve por `name()`, `getCodigo()` ou `getId()`. |
| Validação de placeholders | O construtor de `SQLResult` rejeita divergência entre quantidade de `?` e parâmetros. |
| Auditoria de desempenho | Registra falhas e alerta consultas acima de 10 segundos, incluindo a origem identificada na stack. |
| Execução DML auditada | O tempo é medido após `executeUpdate`, antes do retorno da quantidade de linhas. |
| Batch transacional em uso real | `FinanceiroTBMDAO` controla `autoCommit`, `commit`, `rollback` e restauração do estado da conexão. |

Esses recursos ampliam o MiniORM de um gerador CRUD para uma pequena fachada de execução JDBC, mantendo a possibilidade de SQL explícito.

---

## 6. Conversões e regras de dados

### Tipos Oracle/JDBC para campos de DTO

| Valor retornado | Campo Java | Tratamento |
|---|---|---|
| `Number` / `BigDecimal` | `Integer` | `intValue()` |
| `Number` / `BigDecimal` | `Long` | `longValue()` |
| `Number` / `BigDecimal` | `Double` | `doubleValue()` |
| `Number` / `BigDecimal` | `Float` | `floatValue()` |
| `Clob` | normalmente `String` | Leitura integral com `getSubString`. |
| `Timestamp` | `String` | Formato `dd/MM/yyyy HH:mm:ss`. |
| outro `Date` JDBC | `String` | Formato `dd/MM/yyyy`. |
| data compatível | `Date` / `Timestamp` | Atribuição direta ou cópia para `java.util.Date`. |
| código ou nome | `enum` | Busca por `name()`, `getCodigo()` e `getId()`. |

Na leitura de enum, códigos numéricos são comparados por `BigDecimal`, evitando diferença entre representações como `1` e `1.0`. Se não houver constante compatível, o campo recebe `null`.

### Enum na escrita

```java
public enum StatusAuditoriaFolha {
    PENDENTE("P"), PROCESSADO("F");

    private final String codigo;

    public String getCodigo() {
        return codigo;
    }
}
```

Ao inserir ou atualizar esse enum, o valor físico será `P` ou `F`. Se `getCodigo()` não existir ou retornar `null`, o MiniORM tenta `getId()` e então usa o nome da constante.

### Estratégia Anti-Null

Em `buildInsert` e `buildUpdate`, campos nulos são interpretados como ausentes. Isso favorece DTOs parciais e evita sobrescritas, mas distingue o MiniORM de um ORM com estado completo de entidade. Quando `null` for um valor de negócio a persistir, escreva a operação explicitamente.

---

## 7. Validação, auditoria e regras de uso

### Correspondência entre placeholders e parâmetros

O construtor de `SQLResult` conta os caracteres `?` e compara com a lista de parâmetros. A falha ocorre antes de abrir o `PreparedStatement`.

```java
// Lança IllegalArgumentException: há dois placeholders e um parâmetro.
new SQLResult(
    "SELECT 1 FROM DUAL WHERE ? = ?",
    java.util.Arrays.asList(1)
);
```

A contagem é textual; um `?` dentro de literal ou comentário SQL também será contado. Evite esse caractere em literais ou evolua o validador antes de adotar consultas desse tipo.

### Auditoria de execução

`executeUpdate`, `executeQuery`, `executeQueryAsMap`, `executeScalar` e `executeAsDictionary`:

- registram erro via `AppLog` e relançam a exceção;
- identificam a origem provável pela stack de chamada;
- emitem aviso quando a duração medida ultrapassa 10 segundos.

`executeQuerySingle` e `executeQueryAsSingleMap` herdam a auditoria dos métodos que chamam. O batch ainda não possui essa auditoria. Em consultas tipadas, a medição de sucesso ocorre depois da execução da query e antes do mapeamento completo do `ResultSet`; portanto, não representa necessariamente o tempo total de materialização.

### Regras e respectivas salvaguardas

| Comportamento atual | Uso recomendado ou mitigação |
|---|---|
| A reflexão considera os campos declarados diretamente na classe. | Use DTOs planos para persistência; não dependa de campos anotados herdados. |
| Colunas ausentes são ignoradas durante o mapeamento. | Esse comportamento viabiliza projeções parciais; cubra o mapeamento completo com testes de integração para detectar divergências de schema ou alias. |
| `executeQuerySingle` retorna a primeira linha encontrada. | Use filtros por chave única ou uma limitação Oracle quando a unicidade não vier do modelo. |
| O builder não interpreta a sintaxe nem os identificadores SQL. | Mantenha tabela, coluna, sequence e `whereSql` como constantes revisadas no código; dados externos entram somente como parâmetros. |
| A contagem de placeholders é textual. | Não use `?` em literais ou comentários SQL; a validação já protege o caso recorrente de divergência entre binds. |
| O batch adota o formato do primeiro item. | Valide e normalize lotes no serviço para que todos os DTOs sejam homogêneos. |
| Campos nulos significam “ausente” em updates gerados. | Use SQL explícito quando a regra de negócio for gravar `NULL`; a semântica padrão protege patches contra sobrescrita acidental. |
| `SQLResult` mantém as listas recebidas. | Trate parâmetros como imutáveis depois da construção. |

Essas regras são compatíveis com o escopo atual e tornam explícitos os pontos que exigem convenção de desenvolvimento ou teste.

---

## 8. Exemplo end-to-end em um DAO

```java
public class SolicitacaoRevAdmDAO implements AutoCloseable {

    private final Connection connection;

    public SolicitacaoRevAdmDAO() throws Exception {
        this.connection = ConnectionPool.getInstance()
            .getConnection(SolicitacaoRevAdmDAO.class);
    }

    public void inserir(SolicitacaoRevAdmDTO dto) throws Exception {
        SQLBuilder.buildInsert(dto).executeUpdate(connection);
    }

    public SolicitacaoRevAdmDTO buscar(Integer id) throws Exception {
        return SQLBuilder
            .buildSelect(SolicitacaoRevAdmDTO.class, "ID = ?", id)
            .executeQuerySingle(connection, SolicitacaoRevAdmDTO.class);
    }

    public boolean resolver(Integer id, Timestamp data) throws Exception {
        SolicitacaoRevAdmDTO patch = SolicitacaoRevAdmDTO.builder()
            .dhResolucao(data)
            .build();

        return SQLBuilder
            .buildUpdate(patch, "ID = ?", id)
            .executeUpdate(connection) == 1;
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
```

Uso:

```java
try (SolicitacaoRevAdmDAO dao = new SolicitacaoRevAdmDAO()) {
    SolicitacaoRevAdmDTO nova = SolicitacaoRevAdmDTO.builder()
        .nuNota(123456)
        .sequencia(1)
        .dhCriacao(new Timestamp(System.currentTimeMillis()))
        .build();

    dao.inserir(nova);
    SolicitacaoRevAdmDTO existente = dao.buscar(34268);

    if (existente != null) {
        dao.resolver(existente.getId(), new Timestamp(System.currentTimeMillis()));
    }
}
```

Se o serviço precisar agrupar várias escritas atomicamente, a conexão e a transação devem ser compartilhadas e controladas no limite transacional apropriado; abrir uma conexão por método impediria esse agrupamento.

---

## 9. Adequação das alternativas ao contexto Sankhya

Hibernate/JPA e o MiniORM resolvem problemas de persistência em níveis diferentes. Hibernate oferece um modelo completo de entidades e unidade de trabalho. O MiniORM oferece padronização JDBC especializada para o ambiente já existente. A avaliação abaixo considera os requisitos concretos deste projeto, não uma comparação genérica de quantidade de funcionalidades.

| Requisito do projeto | Hibernate / JPA | MiniORM híbrido JDBC | Adequação ao cenário atual |
|---|---|---|---|
| Integração com conexões Sankhya | Requer configurar e integrar o contexto de persistência ao ciclo da plataforma. | Recebe diretamente a `Connection` já fornecida pela aplicação. | MiniORM preserva o modelo operacional existente. |
| Previsibilidade das escritas | O resultado depende do estado da entidade, do contexto e das configurações de flush. Também permite comandos explícitos. | Cada chamada representa um comando visível, sem sincronização implícita posterior. | MiniORM facilita rastreabilidade em ações e jobs. |
| SQL Oracle específico | Pode ser usado por SQL nativo, paralelamente ao modelo de entidades. | É parte natural da API e permanece concentrado no DAO. | MiniORM evita manter duas formas de consulta para o mesmo módulo. |
| CRUD de tabelas customizadas | Exige mapear entidades e configurar o ambiente JPA. | Exige DTO anotado e builders diretos. | MiniORM atende com menor superfície de configuração. |
| Atualizações parciais | Possui alternativas como entidade gerenciada, consultas específicas e `@DynamicUpdate`. | A estratégia Anti-Null transforma o DTO parcial diretamente no `SET`. | MiniORM expressa de forma simples o padrão recorrente do projeto. |
| Controle transacional | Integra-se a JTA, Spring e outros gerenciadores quando disponíveis. | Usa commit, rollback e conexão definidos pelo chamador. | MiniORM participa do limite transacional adotado pela extensão. |
| Consultas e projeções | Oferece JPQL, Criteria, SQL nativo e projeções. | Oferece DTO, resultado único, escalar, mapa, dicionário e SQL explícito. | A API atual cobre os formatos efetivamente usados pelos DAOs. |
| Relacionamentos de domínio | Automatiza associações, cascatas e estratégias de carregamento. | Mantém joins e composição de resultados explícitos. | O escopo atual privilegia operações objetivas; não depende de navegação automática de grafos. |
| Concorrência | Dispõe de mecanismos padronizados como `@Version` e locks. | Permite expressar versão, status e locks diretamente nas condições SQL. | Ambas são viáveis; no MiniORM a regra permanece explícita e deve ser implementada quando necessária. |
| Inserção em lote | Oferece batching integrado ao ciclo das entidades e ampla configuração. | Oferece batch homogêneo com transação controlada pelo serviço. | O MiniORM cobre as cargas controladas existentes. |
| Estado e cache | Mantém cache de primeiro nível e pode usar segundo nível. | É stateless e não mantém cópia de entidades. | A ausência de cache local reduz risco de leitura desatualizada em relação ao ERP. |
| Portabilidade de banco | Dialetos abstraem parte das diferenças entre SGBDs. | Usa conscientemente recursos Oracle. | Oracle/Sankhya é premissa, portanto a especialização é aceitável e útil. |
| Dependências e inicialização | Requer o runtime e a configuração do provedor ORM. | Utiliza JDBC e poucas classes utilitárias já incorporadas ao projeto. | MiniORM acrescenta baixo overhead ao deployment das extensões. |
| Diagnóstico | Possui amplo ecossistema de logs e métricas, sujeito à configuração adotada. | Concentra SQL e parâmetros e audita erro, origem e consultas lentas. | A observabilidade implementada atende ao diagnóstico direto hoje necessário. |
| Evolução e testes | Framework maduro, com comportamento e ecossistema amplamente documentados. | Código menor, específico e sob controle da equipe, que requer cobertura automatizada própria. | O custo de governança é conhecido e proporcional à superfície reduzida da solução. |

### Decisão arquitetural

O MiniORM é a alternativa selecionada para esta camada porque atende diretamente aos requisitos predominantes: integração JDBC com o Sankhya, Oracle como banco definido, transações explícitas, DTOs parciais, SQL rastreável e baixo overhead operacional. A escolha reduz código repetitivo sem introduzir um contexto de persistência adicional onde ele não é necessário.

Hibernate/JPA continua sendo uma tecnologia válida, porém orientada a outro perfil de necessidade. Sua adoção deve ser considerada em módulos que realmente demandem unidade de trabalho abrangente, navegação intensiva de relacionamentos ou um modelo de entidades gerenciadas. Isso não reduz a adequação do MiniORM ao escopo para o qual foi projetado.

---

## 10. Defesa arquitetural atualizada

> **Registro de decisão arquitetural**
>
> O MiniORM híbrido JDBC é o padrão de persistência adotado para simplificar e proteger o acesso a dados das extensões Sankhya. Seu desenho parte das características reais do ambiente: Oracle 19c, conexões controladas pela plataforma, rotinas transacionais objetivas e necessidade de compreender rapidamente qual SQL será executada.
>
> A solução elimina fragilidades recorrentes do JDBC manual sem ocultar decisões importantes. O `SQLBuilder` mantém tabela e colunas alinhadas ao DTO, a validação de placeholders detecta inconsistências antes da execução, o `SQLResult` preserva a ordem dos parâmetros, o `UPDATE` exige `WHERE` e a estratégia Anti-Null evita sobrescritas involuntárias. O mapeamento de tipos Oracle, enums, resultados escalares, mapas e batches concentra comportamentos que antes tenderiam a ser repetidos pelos DAOs.
>
> A manutenção explícita de SQL, conexão e transação é uma característica deliberada. Ela permite usar regras Oracle, participar do limite transacional do Sankhya e diagnosticar comandos sem depender do estado interno de um contexto de entidades. Ao mesmo tempo, os recursos repetitivos ficam encapsulados, produzindo uma camada mais uniforme que o JDBC escrito isoladamente em cada rotina.
>
> A arquitetura possui fronteiras claras: não tenta administrar grafo de entidades, cache, DDL ou migrações, pois essas responsabilidades não fazem parte da necessidade desta camada. Quando uma operação exige SQL analítico, `MERGE`, gravação explícita de `NULL` ou regra concorrente específica, o próprio `SQLResult` permite preservar parametrização e observabilidade sem forçar a ampliação artificial dos builders.
>
> Portanto, o MiniORM constitui uma escolha arquitetural proporcional e sustentável para as customizações atuais. Ele combina o controle operacional requerido pelo ERP com ganhos concretos de padronização, segurança de parâmetros, produtividade e manutenção.

### Governança e evolução

- Cobrir builders, conversões, enums, projeções e validações com testes automatizados.
- Manter `whereSql`, nomes estruturais e sequences como constantes controladas pelo código.
- Adicionar novas abstrações apenas para padrões recorrentes, preservando a previsibilidade da API.
- Evoluir observabilidade e mensagens de erro sem registrar dados sensíveis dos parâmetros.
- Avaliar uma solução de entidades gerenciadas somente em novos módulos cujos requisitos justifiquem esse modelo, sem desestabilizar a camada JDBC existente.

---

## 11. Recomendações de uso

- Use builders para persistência simples ou média de uma tabela.
- Envie dados por placeholders; nunca concatene entrada externa em SQL.
- Use `whereSql` como texto controlado pelo código e informe `WHERE` em todo update.
- Use `buildInsertBatch` somente com listas homogêneas e transação explícita.
- Garanta construtor sem argumentos nos DTOs mapeados, por exemplo com `@NoArgsConstructor`.
- Exponha `getCodigo()` ou `getId()` em enums quando o valor físico diferir de `name()`.
- Use SQL explícito para gravar `NULL`, fazer `DELETE`, `MERGE`, SQL analítico ou operações específicas.
- Prefira `executeScalar` para uma célula, `executeAsDictionary` para duas colunas chave/valor e mapas para projeções realmente dinâmicas.
- Use `try-with-resources` em DAOs que implementam `AutoCloseable`.
- Verifique a quantidade de linhas afetadas em operações que deveriam alterar exatamente um registro.
- Não exponha `SQLResult` ou builders como substitutos do limite transacional da camada de serviço.
