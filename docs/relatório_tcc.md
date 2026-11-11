# Relatório de Projeto

## SEOS (_Software Endurance Orchestration System_): redução de _boilerplate_, auditoria e segurança cibernética na camada de persistência

**Autora:** Maria Antônia Santos de Lima  
**Instituição:** Fundação Edson Queiroz — Universidade de Fortaleza (UNIFOR)  
**Curso:** Pós-Graduação em Engenharia de Software com foco em DevOps  
**Local:** Fortaleza-CE  
**Ano:** 2026

## 1. Introdução

Atualmente, no ambiente corporativo analisado neste estudo, os métodos de consulta utilizados nos projetos legados são muito verbosos. Para realizar uma consulta, é necessário escrevê-la manualmente, armazená-la em uma variável, mapear os parâmetros do `PreparedStatement` e, logo em seguida, mapear o `ResultSet`, campo a campo.

Em alguns casos, as consultas não possuem parâmetros. Em outros, os valores são concatenados diretamente na instrução _Structured Query Language_ (SQL), o que cria uma brecha significativa para _SQL Injection_. Além disso, existem diversas versões de operações como `INSERT`, `UPDATE` e consultas para uma mesma tabela. Isso ocorre porque determinadas implementações precisam de estruturas específicas que não podem ser modificadas, resultando na duplicação de operações semelhantes.

Nesse contexto, foi desenvolvido o **SEOS** (_Software Endurance Orchestration System_), um _microframework_ de Mapeamento Objeto-Relacional (ORM, do inglês _Object-Relational Mapping_) criado para atender às necessidades específicas da empresa. O _framework_ mapeia tabelas, campos e _sequences_ por meio de _annotations_ e constrói estruturas _Data Manipulation Language_ (DML) e _Data Query Language_ (DQL) com base nesses mapeamentos. Dessa forma, busca tornar o acesso ao banco de dados mais padronizado e seguro, além de retornar as classes com os valores já mapeados e prontos para serem utilizados pelas implementações.

### 1.1 Objetivo

#### Objetivo geral

Evoluir o _microframework_ SEOS com base em práticas consolidadas de mercado e estruturar seu processo de desenvolvimento sob a cultura _DevSecOps_, a fim de demonstrar os ganhos de otimização, padronização, auditoria e segurança na camada de persistência.

#### Objetivos específicos

- Automatizar a geração de instruções DML e DQL e o mapeamento objeto-relacional de consultas por meio de _annotations_ e _reflection_;
- Mitigar os riscos de _SQL Injection_ na camada de persistência mediante a parametrização obrigatória das consultas e a validação estrutural das operações;
- Incorporar mecanismos de auditoria e observabilidade para o rastreamento de erros, parâmetros e tempo de execução das operações no banco de dados;
- Estruturar uma esteira _DevSecOps_ de Integração e Entrega Contínua — _Continuous Integration/Continuous Delivery_ (CI/CD) — contemplando testes automatizados, inspeção de qualidade e cobertura, análise estática de segurança — _Static Application Security Testing_ (SAST) —, alertas via _ChatOps_ e publicação com versionamento semântico;
- Avaliar a aderência arquitetural do SEOS frente às restrições do cenário corporativo estudado em comparação com soluções tradicionais de mercado, como o Hibernate/_Java Persistence API_ (JPA);
- Mensurar os ganhos quantitativos e qualitativos da adoção da biblioteca em relação à escrita manual via _Java Database Connectivity_ (JDBC), considerando a redução de código, a eliminação de duplicações e a mitigação de vulnerabilidades baseadas no _Open Worldwide Application Security Project_ (OWASP) _Top_ 10.

### 1.2 Justificativa

Ao desenvolver uma implementação para atender a uma demanda operacional e constatar a impossibilidade de reutilizar um método de persistência já existente no banco de dados, surgiu o seguinte questionamento arquitetural: _“Por que criar mais uma versão de um `UPDATE` com as mesmas condições?”_. A partir dessa análise, verificou-se que a **falta de padronização**, a **baixa qualidade do código** e os **problemas de segurança nas consultas** eram fatores estruturais que precisavam ser tratados.

Por meio dessa necessidade nasceu o **SEOS**, concebido como uma solução autoral capaz de reduzir o trabalho manual e a duplicação de métodos, facilitar a manutenção dos projetos empresariais e diminuir os riscos de _SQL Injection_, **sem retirar da equipe o controle** sobre as consultas, conexões e transações de persistência.

### 1.3 Problema

Para dimensionar a criticidade do cenário estudado, um diagnóstico inicial realizado em conjunto com a equipe de Arquitetura sobre uma única classe de acesso a dados — Objeto de Acesso a Dados, do inglês _Data Access Object_ (DAO) / _Repository_ — de um dos projetos legados da empresa revelou um passivo técnico expressivo. Ao todo, o arquivo concentrava **21.381 linhas de código** distribuídas em **409 métodos** (dos quais **402** acessam diretamente o banco de dados), apresentando uma taxa de **19,6% de linhas idênticas** e **54,4% de linhas repetidas**. Além do alto volume de duplicação, constatou-se que, entre **416 chamadas** a `PreparedStatement`, apenas **156** parametrizavam os valores adequadamente, resultando em aproximadamente **43% dos métodos** potencialmente vulneráveis a _SQL Injection_. Os alertas sobre essas falhas na camada de persistência já eram conhecidos internamente; contudo, devido à ausência histórica de uma abstração padronizada, à pressão por entregas rápidas e à complexidade dos sistemas legados, tornou-se inviável corrigir e uniformizar esse desenvolvimento de forma manual — sendo exatamente nesse cenário que o SEOS se insere.

Embora ele atue como uma biblioteca Java distribuída sob versionamento semântico — não sendo uma aplicação final em si —, ele opera no núcleo da camada de persistência dos módulos corporativos que sustentam a operação da empresa. No exemplo citado anteriormente, a presença de aproximadamente **43% de métodos suscetíveis a _SQL Injection_** representa um risco operacional crítico ao banco de dados. A exploração de uma vulnerabilidade dessa natureza, ou a indisponibilidade do sistema devido à corrupção de tabelas essenciais ao fluxo operacional, exigiria um tempo estimado de recuperação de múltiplas horas de operação para conter o incidente, auditar os registros afetados e restaurar o ambiente. Nesse intervalo, a paralisação do faturamento e das vendas acarretaria **prejuízos financeiros diretos** expressivos à organização, além de **prejuízos indiretos** severos, como perda de clientes e desgaste reputacional. Diante disso, a adoção do SEOS, aliada à esteira _DevSecOps_ e ao controle estrito de versões, atua diretamente na mitigação preventiva desses impactos financeiros e operacionais.

## 2. Referencial Teórico

- **2.1 Persistência de Dados e Mapeamento Objeto-Relacional (ORM):** fundamentos de bancos de dados relacionais, comunicação via interfaces de baixo nível (JDBC) e o problema da impedância objeto-relacional;
- **2.2 Metaprogramação, _Reflection_ e Arquitetura de _Microframeworks_:** conceitos de introspecção em tempo de execução, programação orientada a metadados (_annotations_) e diferenças conceituais entre _frameworks_ tradicionais e bibliotecas leves;
- **2.3 Segurança Cibernética em Aplicações e _SQL Injection_:** panorama de ataques cibernéticos à camada de persistência, vulnerabilidades de injeção de comandos, princípio de consultas parametrizadas e diretrizes do OWASP _Top_ 10;
- **2.4 Cultura _DevSecOps_ e Engenharia de Software Contínua (CI/CD):** transição do modelo DevOps tradicional para o _DevSecOps_, conceito de segurança antecipada (_Shift-Left Security_) e automação de integração e entrega contínuas;
- **2.5 Qualidade de Código, Análise Estática de Segurança (SAST) e _Supply Chain Security_:** conceitos de código repetitivo (_boilerplate_), dívida técnica, verificação estática de vulnerabilidades e integridade da cadeia de suprimentos de _software_;
- **2.6 Observabilidade, Auditoria de Sistemas e _ChatOps_:** fundamentos de rastreabilidade de erros e desempenho em sistemas críticos e práticas de operação colaborativa orientada a notificações.

Esta seção deverá ser complementada com a fundamentação teórica dos conceitos e com pelo menos cinco artigos atuais sobre ataques cibernéticos, segurança de aplicações e práticas _DevSecOps_.

## 3. Caso de Uso

### 3.1 Contexto do Trabalho

O cenário utilizado como inspiração e base para o desenvolvimento e a aplicabilidade do SEOS é o de uma empresa têxtil que possui **dois projetos legados principais**. Esses dois sistemas foram desenvolvidos utilizando o ecossistema Java (nas versões 8 e 11), realizando o acesso direto ao banco de dados Oracle 19c via JDBC e comunicando-se via _Application Programming Interface_ (API) com o sistema de _Enterprise Resource Planning_ (ERP) responsável pelo gerenciamento dos processos empresariais. Por razões de confidencialidade e políticas internas de segurança da informação, a identidade da organização e os indicadores corporativos globais são preservados neste relatório, concentrando-se a caracterização no ambiente técnico de engenharia de software.

Esses dois sistemas sustentam rotinas operacionais críticas de diferentes fluxos de negócio da empresa — um responsável por **automações produtivas, logísticas e contábeis**, e o outro responsável pelo **_e-commerce_** —, concentrando um elevado volume de consultas e persistências desenvolvidas ao longo dos anos. Pela forma como os sistemas foram estruturados junto ao ERP, as consultas, as conexões e as transações precisam continuar **sob o controle direto da aplicação**, o que impedia o uso de um ORM tradicional que gerenciasse tudo de forma automática.

Além disso, como esses dois sistemas evoluíram ao longo de vários anos passando por diferentes desenvolvedores e demandas urgentes de negócio, manter a escrita SQL manual segura e uniforme sem uma ferramenta centralizadora tornou-se um desafio diário. Por conta disso, o SEOS precisou ser pensado como uma biblioteca leve, compatível tanto com o Java 8 quanto com o Java 11, para se encaixar diretamente nos projetos já existentes sem exigir mudanças na arquitetura da empresa.

### 3.2 Situação Atual

Na situação anterior ao SEOS, não havia uma padronização para a camada de persistência nos dois sistemas. O desenvolvedor precisava construir cada instrução SQL manualmente, mapear os parâmetros seja via `PreparedStatement` ou concatenando, percorrer o `ResultSet` campo a campo e tratar possíveis erros por conta própria. Às vezes, eram reaproveitados métodos parecidos para aceleração da demanda, mas, em grande parte dos casos, esses métodos estavam desenvolvidos erroneamente. Para dimensionar o impacto real dessas práticas na totalidade dos dois projetos legados — ampliando a análise inicial daquele arquivo isolado apresentado na Seção 1.3 —, foi realizado um levantamento junto à equipe de Arquitetura analisando todos os métodos que interagem com o banco de dados (totalizando **2.890 métodos** somados entre as duas aplicações).

No primeiro sistema, responsável pelas automações corporativas (produtivas, logísticas e contábeis), foram analisados **1.641 métodos** de acesso ao banco, cujos resultados foram organizados conforme apresentado na Tabela 1:

#### Tabela 1 — Levantamento de métodos de persistência no sistema de automações corporativas

| #   | Indicador                                                                            | Quantidade | % dos 1.641 |
| --- | ------------------------------------------------------------------------------------ | ---------- | ----------- |
| 1   | Métodos que realizam funções no banco                                                | 1.641      | 100%        |
| 2   | Usam conexão JDBC (`PreparedStatement`, `CallableStatement`, `getPreparedStatement`) | 1.321      | 80,5%       |
| 3   | Não usam JDBC direto                                                                 | 320        | 19,5%       |
| 4   | Com alguma má prática de consulta                                                    | **395**    | **24,1%**   |
| 5   | Vulneráveis (concatenam `String` ou tipo não resolvido)                              | **283**    | **17,2%**   |
| 6   | Sem nenhuma chamada de _log_                                                         | **573**    | **34,9%**   |

**Fonte:** Elaborada pela autora (2026).

Dos **573 métodos sem _logs_**, **254** possuem `try/catch` mas não registram _logs_ de erro ou de sucesso, **58** só aplicam `System.out` ou `printStackTrace` e **28** possuem vulnerabilidades. Já no segundo sistema, responsável pelo _e-commerce_, o cenário mostrou-se ainda mais crítico quanto à exposição a _SQL Injection_ e à falta de rastreabilidade, conforme demonstrado na Tabela 2:

#### Tabela 2 — Levantamento de métodos de persistência no sistema de _e-commerce_

| #   | Indicador                                                  | Quantidade | % dos 1.249 |
| --- | ---------------------------------------------------------- | ---------- | ----------- |
| 1   | Métodos que fazem operações no banco                       | 1.249      | 100%        |
| 2   | Usam JDBC (`prepareStatement`, `prepareCall`, `ResultSet`) | 1.229      | 98,4%       |
| 3   | Não usam JDBC (JPA/Hibernate via `EntityManager`)          | 20         | 1,6%        |
| 4   | Fora das boas práticas (concatenam variáveis na SQL)       | **468**    | **37,5%**   |
| 5   | Vulneráveis a _SQL Injection_                              | **299**    | **23,9%**   |
| 6   | Sem auditoria de _logs_                                    | **1.179**  | **94,4%**   |

**Fonte:** Elaborada pela autora (2026).

A seguir, apresenta-se um exemplo claro do padrão de código que alimentou os dados desses levantamentos:

```java
public List<Usuario> consultarUsuarios(String status, Integer idadeMinima) {
    List<Usuario> listaResultado = new ArrayList<>();

    String dql = "SELECT USER_ID, USER_NAME, USER_AGE, STATUS "
            + "FROM TB_USER WHERE 1=1 ";

    if (status != null && !status.isEmpty()) {
        dql += " AND STATUS = '" + status + "'";
    }
    if (idadeMinima != null) {
        dql += " AND USER_AGE >= " + idadeMinima;
    }

    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
         PreparedStatement ps = conn.prepareStatement(dql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            Long id = rs.getLong("USER_ID");
            String nome = rs.getString("USER_NAME");
            Integer idade = rs.getInt("USER_AGE");
            String statusRetornado = rs.getString("STATUS");

            Usuario usuario = new Usuario();
            usuario.setId(id);
            usuario.setNome(nome);
            usuario.setIdade(idade);
            usuario.setStatus(statusRetornado);
            listaResultado.add(usuario);
        }
    } catch (SQLException e) {
        System.err.println("Erro ao executar consulta: " + e.getMessage());
        e.printStackTrace();
    }
    return listaResultado;
}
```

Analisando os levantamentos apresentados nas Tabelas 1 e 2 em conjunto com a estrutura exemplificada no código anterior, evidencia-se o quão verboso e vulnerável tornou-se o padrão de desenvolvimento acumulado ao longo dos anos nesses sistemas. Além do elevado volume de código repetitivo, a concatenação direta de variáveis na instrução SQL dificulta a padronização e expõe a aplicação a ataques de _SQL Injection_, enquanto a coexistência de múltiplas implementações semelhantes e a ausência de _logs_ de auditoria elevam significativamente o custo de manutenção.

### 3.3 Solução Proposta

Diante dos números levantados na seção anterior — que evidenciaram o excesso de código manual, o risco de _SQL Injection_ e a ausência de _logs_ nos dois sistemas da empresa —, foi idealizado o **SEOS**: um _microframework_ de ORM dentro do ecossistema Java capaz de resolver essas falhas respeitando a arquitetura existente, sem adicionar complexidade e garantindo maior celeridade no desenvolvimento.

A proposta central do SEOS é automatizar justamente a parte mecânica e propensa a erros do uso do JDBC. Em vez de o desenvolvedor repetir manualmente todo o ciclo de preparação e leitura de dados em cada método, a biblioteca assume a construção segura das instruções, a validação dos parâmetros e a auditoria das execuções, devolvendo os objetos já preenchidos **sem retirar da aplicação o controle explícito sobre a conexão e a transação**.

Para que essa automação funcione na prática, o ponto de partida do SEOS é o mapeamento das tabelas e dos campos diretamente nas classes de Objeto de Transferência de Dados — _Data Transfer Object_ (DTO) — por meio de _annotations_, inspirado no modelo de mapeamento do JPA/Spring Boot, porém aplicado diretamente aos DTOs por meio das interfaces próprias `@TableDB` e `@ColumnDB`.

Pela `@TableDB`, é sinalizado e identificado o nome da tabela ou da _view_ criada no banco de dados; já pela `@ColumnDB`, é possível informar o nome do campo correspondente naquele objeto de banco e declarar se ele é uma _primary key_ e se possui _sequences_. Um detalhe importante a ser ressaltado é que as _sequences_ informadas entram automaticamente nos construtores de `INSERT`, enquanto atributos da classe que não possuem `@ColumnDB` são ignorados pelas demais funcionalidades do SEOS, conforme demonstrado a seguir:

```java
@TableDB(name = "TB_USER")
public class UserDTO {

    @ColumnDB(
        localName = "USER_ID",
        sequence = "SEQ_TB_USER.NEXTVAL",
        isPrimaryKey = true
    )
    private Long id;

    @ColumnDB(localName = "USER_NAME")
    private String name;

    @ColumnDB(localName = "STATUS")
    private String status;

    @ColumnDB(localName = "USER_AGE")
    private Integer age;
}
```

Com as classes configuradas, é possível utilizar a segunda função da ferramenta, o `SQLBuilder`, que lê as _annotations_ via _reflection_ e monta automaticamente os comandos SQL de DQL e DML. A geração do `SELECT` pode montar uma consulta com todos os campos mapeados da classe de forma direta (`buildSelect`) ou retornar campos específicos de forma fluente (`select`) por meio do método `.campos()`, no qual se informam os nomes dos atributos Java da classe, funcionando da mesma maneira para a cláusula de filtro com `.where()`:

```java
// 1. Geração direta com todos os campos mapeados da classe:
SQLBuilder.buildSelect(UserDTO.class, "STATUS = ?", status);

// 2. Geração fluente selecionando campos específicos da classe:
SQLBuilder.select(UserDTO.class)
          .campos("id", "name")
          .where("STATUS = ?", status)
          .build();
```

Para as operações DML (`INSERT` e `UPDATE`), o `SQLBuilder` aplica o conceito de _Anti-Null_: apenas os atributos que estiverem preenchidos serão persistidos no banco. Dessa forma, não é mais preciso desenvolver métodos diferentes para a mesma tabela, sendo necessário apenas informar a classe com as informações preenchidas.

Quando é utilizado o _builder_ de `INSERT` (`buildInsert`) sem preencher os dados da _primary key_, automaticamente é utilizada a _sequence_ configurada anteriormente no `@ColumnDB`, sendo também possível realizar inserções em lote por meio do método `buildInsertBatch()`:

```java
UserDTO newUser = UserDTO.builder()
                         .name("Maria")
                         .status("ATIVO")
                         .build();

SQLBuilder.buildInsert(newUser);

// Instrução SQL conceitual gerada internamente:
// INSERT INTO TB_USER (USER_ID, USER_NAME, STATUS) VALUES (SEQ_TB_USER.NEXTVAL, ?, ?)
```

Já no _builder_ de `UPDATE` (`buildUpdate`), a ferramenta só dá continuidade à função se, e somente se, forem informadas as condições de filtro (`WHERE`). Sem essa parametrização, é lançada uma exceção ao desenvolvedor, impedindo modificações acidentais sem filtro no banco de dados:

```java
UserDTO updateUser = UserDTO.builder()
                         .name("Maria Antônia")
                         .age(26)
                         .build();

SQLBuilder.buildUpdate(updateUser, "USER_ID = ?", idUser);

// Instrução SQL conceitual gerada internamente:
// UPDATE TB_USER SET USER_NAME = ?, USER_AGE = ? WHERE USER_ID = ?
```

Os comandos DQL e DML construídos, juntamente com a lista ordenada de parâmetros, são encapsulados na classe `SQLResult`. Nela são centralizados o preenchimento seguro dos valores no `PreparedStatement` e o mapeamento dos dados retornados pelo `ResultSet` para as classes configuradas com `@ColumnDB`, além de ser validado previamente se a quantidade de parâmetros informados corresponde à quantidade de _placeholders_ (`?`) existentes nos comandos SQL.

Nas operações de escrita (`INSERT` e `UPDATE`), o `SQLResult` oferece três formas principais de execução, adaptadas à necessidade de cada rotina: a atualização direta retornando o total de registros afetados, a inserção com recuperação imediata do identificador gerado pela _sequence_ do Oracle (preenchendo automaticamente a chave primária no próprio objeto enviado) e o processamento em lote para grandes volumes de dados. Em todas essas modalidades, a conexão JDBC parte sempre do chamador, garantindo que o controle transacional (`commit` e `rollback`) permaneça sob domínio exclusivo da aplicação, enquanto a biblioteca se encarrega de fechar internamente os recursos de preparação e leitura:

```java
// 1. Atualização retornando a quantidade de linhas afetadas:
int linhasAtualizadas = SQLBuilder
    .buildUpdate(updateUser, "USER_ID = ?", idUser)
    .executeUpdate(conn);

// 2. Inserção retornando o ID gerado pela sequence (e preenchendo newUser.id):
Long idGerado = SQLBuilder
    .buildInsert(newUser)
    .executeAndReturnId(conn, newUser);

// 3. Inserção em lote com controle transacional explícito pelo chamador:
boolean autoCommitOriginal = conn.getAutoCommit();
try {
    conn.setAutoCommit(false);
    int[] resultados = SQLBuilder.buildInsertBatch(newUsers).executeBatch(conn);
    conn.commit();
} catch (Exception e) {
    conn.rollback();
    throw e;
} finally {
    conn.setAutoCommit(autoCommitOriginal);
}
```

Nas operações de leitura (`SELECT`), o principal gargalo dos sistemas legados era a repetição de laços manuais para extrair coluna por coluna do `ResultSet`, independentemente do formato do retorno. Para eliminar esse trabalho repetitivo quando a consulta envolve tabelas mapeadas, o `SQLResult` converte os registros retornados pelo banco diretamente em uma lista de objetos tipados ou em uma instância única (voltada a buscas pontuais, como consultas por chave primária). Além disso, para facilitar a migração gradual de classes legadas que ainda montam a consulta manualmente, a biblioteca permite reaproveitar apenas o seu motor de mapeamento sobre um resultado já aberto:

```java
// 1. Retorno de lista de objetos mapeados:
List<UserDTO> users = SQLBuilder
    .buildSelect(UserDTO.class, "STATUS = ?", status)
    .executeQuery(conn, UserDTO.class);

// 2. Retorno de um único objeto (ou null se não encontrado):
UserDTO user = SQLBuilder
    .buildSelect(UserDTO.class, "USER_ID = ? AND STATUS = ?", idUser, status)
    .executeQuerySingle(conn, UserDTO.class);

// 3. Aproveitamento do mapeamento em consultas legadas com ResultSet aberto:
try (PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    return new SQLResult(sql, Collections.emptyList()).mapearResultSet(rs, UserDTO.class);
}
```

Por outro lado, em rotinas de relatórios, totalizadores ou consultas parciais nas quais a criação de uma classe DTO específica seria desnecessária, o `SQLResult` permite extrair os dados diretamente em estruturas dinâmicas do próprio Java. Nesses cenários, o resultado pode ser retornado como listas ou linhas únicas em formato de mapa (indexadas pelo nome da coluna), como um dicionário de chave e valor a partir de consultas de duas colunas, ou ainda como um valor escalar único já convertido para o tipo numérico ou textual esperado pela aplicação:

```java
// 1. Consulta agregada retornando lista de mapas (sem necessidade de DTO):
List<Map<String, Object>> totaisPorStatus = new SQLResult(
    "SELECT STATUS, COUNT(1) TOTAL FROM TB_USER GROUP BY STATUS",
    Collections.emptyList()
).executeQueryAsMap(conn);

// 2. Projeção parcial de uma única linha em mapa:
Map<String, Object> dadosParciais = new SQLResult(
    "SELECT USER_NAME, STATUS FROM TB_USER WHERE USER_ID = ?",
    Arrays.asList(idUser)
).executeQueryAsSingleMap(conn);

// 3. Mapeamento direto de duas colunas em dicionário (chave -> valor):
Map<Long, String> nomesPorId = new SQLResult(
    "SELECT USER_ID, USER_NAME FROM TB_USER WHERE STATUS = ?",
    Arrays.asList("ATIVO")
).executeAsDictionary(conn);

// 4. Retorno de valor escalar único com conversão automática de tipo:
Integer totalAtivos = new SQLResult(
    "SELECT COUNT(1) FROM TB_USER WHERE STATUS = ?",
    Arrays.asList("ATIVO")
).executeScalar(conn, Integer.class);
```

Além das instruções SQL tradicionais, o banco Oracle 19c integrado ao ERP concentra diversas regras de negócio em _procedures_ e _packages_ escritas em _Procedural Language/Structured Query Language_ (PL/SQL). Na abordagem tradicional do JDBC, a chamada dessas rotinas exigia o controle manual da posição numérica de cada parâmetro de entrada e saída, aumentando a incidência de erros de manutenção. Para simplificar esse fluxo, a classe `SQLCall` introduz uma escrita fluente na qual os parâmetros são declarados de forma encadeada e associados a apelidos legíveis (_aliases_), permitindo recuperar os valores retornados pela _procedure_ pelo nome, e não mais por índices numéricos:

```java
SQLCall.Result resultado = SQLCall
    .name("PKG_USER.PROCESSAR_CADASTRO")
    .in(newUser.getName())
    .inOut("STATUS_ATUAL", newUser.getStatus(), Types.VARCHAR)
    .out("ID_GERADO", Types.NUMERIC)
    .execute(conn);

Long idGerado = resultado.getLong("ID_GERADO");
String statusFinal = resultado.getString("STATUS_ATUAL");
```

Por fim, para solucionar a grave ausência de rastreabilidade apontada na Seção 3.2 — na qual **34,9%** dos métodos do sistema de automações e **94,4%** dos métodos do _e-commerce_ não possuíam nenhum registro de _log_ —, o SEOS incorporou uma camada transversal de auditoria baseada na interface _Service Provider Interface_ (SPI) `OrmLogger`, gerenciada pela classe `OrmConfig`. Por meio dessa estrutura, todas as operações executadas pela biblioteca têm seu tempo de resposta cronometrado e identificam automaticamente a classe e o método da aplicação que originaram a chamada ao banco. Sempre que ocorre uma falha SQL ou quando uma operação ultrapassa o limite de tempo configurado — **10 segundos** para consultas e **3 segundos** para chamadas PL/SQL —, um alerta padronizado é emitido com a origem, a duração e a contagem de parâmetros, permitindo que a aplicação conecte seu próprio sistema de _logs_ na inicialização:

```java
// Integração opcional do logger da aplicação com a auditoria automática do SEOS:
OrmConfig.setLogger(new OrmLogger() {
    @Override
    public void info(String origin, String code, String msg, Object... params) {
        appLogger.info("{} [{}] {}", origin, code, msg);
    }

    @Override
    public void warn(String origin, String code, String msg, Object... params) {
        appLogger.warn("{} [{}] {}", origin, code, msg);
    }

    @Override
    public void error(String origin, String code, String msg, Exception e, Object... params) {
        appLogger.error("{} [{}] {}", origin, code, msg, e);
    }
});
```

### 3.4 Comparação com Hibernate/JPA

Para fundamentar a construção de uma biblioteca híbrida própria em vez da adoção direta de uma ferramenta tradicional de mercado no cenário estudado, foi realizada uma comparação arquitetural com o Hibernate, uma das soluções ORM mais utilizadas no ecossistema Java, conforme sintetizado na Tabela 3:

#### Tabela 3 — Comparação arquitetural entre o Hibernate/JPA e o SEOS

| Requisito do projeto         | Hibernate / JPA                                                                         | SEOS híbrido JDBC                                                                  | Adequação ao cenário atual                                       |
| ---------------------------- | --------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| Integração com conexões ERP  | Requer configurar e integrar o contexto de persistência ao ciclo da plataforma.         | Recebe diretamente a `Connection` já fornecida pela aplicação.                     | O SEOS preserva o modelo operacional existente.                  |
| Previsibilidade das escritas | O resultado depende do estado da entidade, do contexto e das configurações de `flush`.  | Cada chamada representa um comando visível, sem sincronização implícita posterior. | O SEOS facilita a rastreabilidade nos projetos.                  |
| SQL Oracle específico        | Pode ser usado por SQL nativo, paralelamente ao modelo de entidades.                    | É parte natural da API e permanece concentrado na camada de `Repository`.          | O SEOS evita manter duas formas de consulta para o mesmo módulo. |
| CRUD de tabelas customizadas | Exige mapear entidades e configurar o ambiente JPA.                                     | Exige DTO anotado e _builders_ diretos.                                            | O SEOS atende com menor superfície de configuração.              |
| Atualizações parciais        | Possui alternativas como entidade gerenciada, consultas específicas e `@DynamicUpdate`. | A estratégia _Anti-Null_ transforma o DTO parcial diretamente no `SET`.            | O SEOS expressa de forma simples o padrão recorrente do projeto. |
| Controle transacional        | Integra-se a JTA, Spring e outros gerenciadores quando disponíveis.                     | Usa `commit`, `rollback` e conexão definidos pelo chamador.                        | O SEOS participa do limite transacional adotado pelo projeto.    |
| Consultas e projeções        | Oferece JPQL, Criteria, SQL nativo e projeções.                                         | Oferece DTO, resultado único, escalar, mapa, dicionário e SQL explícito.           | A API atual cobre os formatos utilizados pelos DAOs.             |
| Relacionamentos de domínio   | Automatiza associações, cascatas e estratégias de carregamento.                         | Mantém _joins_ e composição de resultados explícitos.                              | O escopo atual privilegia operações objetivas.                   |
| Dependências e inicialização | Requer o _runtime_ e a configuração do provedor ORM.                                    | Utiliza JDBC e poucas classes utilitárias já incorporadas ao projeto.              | O SEOS acrescenta baixo _overhead_ à implantação.                |
| Diagnóstico                  | Possui ecossistema de _logs_ e métricas, sujeito à configuração adotada.                | Concentra SQL e parâmetros e audita erros, origem e consultas lentas.              | A observabilidade atende ao diagnóstico necessário.              |

**Fonte:** Elaborada pela autora (2026).

### 3.5 Vantagens e desvantagens

A principal vantagem de adotar o SEOS é a garantia de um processo mais simplificado, auditável e seguro para realizar operações em banco de dados, sem afetar o controle interno de conexões JDBC nem a _performance_ da aplicação. Graças aos seus métodos que automatizam a parametrização das consultas e mapeiam os resultados via _reflection_, os riscos de expor o produto a ataques de _SQL Injection_ são mitigados. Em suma, a arquitetura do _framework_ consolida-se em quatro pilares fundamentais: **Padronização, Simplificação, Proteção e Reutilização**.

Como contrapartida natural de sua proposta enxuta frente a um ORM completo como o Hibernate/JPA, o SEOS concentra sua automação em operações diretas por tabela e não abrange recursos avançados de modelo de domínio, como o mapeamento automático de relacionamentos entre entidades (`@OneToMany`, `@ManyToOne`), o carregamento sob demanda (_lazy loading_) ou mecanismos próprios de _cache_. Dessa forma, consultas que envolvem múltiplos _joins_ ou regras de atualização fora do padrão _Anti-Null_ continuam exigindo a escrita da instrução SQL pela equipe, ainda que se beneficiem do encapsulamento seguro, do mapeamento de retorno e da auditoria fornecidos pelo `SQLResult`.

### 3.6 Plano de aplicação _DevSecOps_

Embora tenha sido inspirado nas dores reais observadas no ambiente corporativo estudado, o SEOS foi desenvolvido como um projeto autoral independente (`com.github.marygraywolf.seos`), hospedado em repositório próprio no GitHub e desvinculado de qualquer nomenclatura, regra de negócio ou código proprietário da empresa. Por se tratar de uma biblioteca projetada para atuar no núcleo da camada de persistência de aplicações Java/JDBC, sua estabilidade e sua segurança são requisitos essenciais: qualquer falha estrutural ou vulnerabilidade na biblioteca impactaria diretamente os sistemas que a importassem como dependência. Diante disso, o ciclo de vida do SEOS foi estruturado sob a cultura _DevSecOps_, priorizando ferramentas gerenciadas no modelo _Software as a Service_ (SaaS), segurança por padrão (_security by design_) e ausência de custos com servidores dedicados. A Tabela 4 sintetiza a matriz de decisão arquitetural utilizada para a escolha de cada tecnologia da esteira frente às alternativas de mercado:

#### Tabela 4 — Matriz de decisão das ferramentas da esteira _DevSecOps_

| Categoria | Ferramenta Adotada | Principais Concorrentes | Justificativa Técnica da Decisão |
| :--- | :--- | :--- | :--- |
| **Orquestração (CI/CD)** | **GitHub Actions** | Jenkins, GitLab CI | O Jenkins exige provisionamento, hospedagem e manutenção contínua de servidores e _plugins_. O GitLab CI exigiria manter a biblioteca fora do ecossistema onde o repositório já está hospedado. O GitHub Actions oferece execução nativa e efêmera diretamente no repositório do projeto, sem custo de infraestrutura. |
| **_Build_ e Empacotamento** | **Apache Maven** | Gradle | Embora o Gradle utilize _scripts_ dinâmicos (Groovy/Kotlin), essa flexibilidade permite lógica imperativa no arquivo de _build_, dificultando a auditoria. O Maven possui ciclo de vida estrito e declarativo em _Extensible Markup Language_ (XML), facilitando a injeção padronizada de _plugins_ de segurança e o controle determinístico de versões. |
| **Inspeção de Qualidade** | **SonarCloud** | Codacy, Code Climate | Possui regras maduras para o ecossistema Java/JDBC e aplica a metodologia _Clean as You Code_ com _Quality Gate_ nativo, capaz de bloquear _Pull Requests_ automaticamente caso novas métricas de cobertura, duplicação ou _code smells_ sejam violadas. |
| **Segurança (SAST)** | **Semgrep** | Checkmarx, Fortify, Snyk | Soluções comerciais como Checkmarx e Fortify demandam alto custo de licenciamento e elevam o tempo de _build_, prejudicando o _feedback_ rápido da esteira. O Semgrep executa localmente no agente de integração (_runner_) em poucos segundos e exporta nativamente o laudo em formato SARIF para a aba _Security_ do próprio GitHub. |
| **Repositório de Pacotes (CD)** | **GitHub Packages** | Sonatype Nexus, JFrog Artifactory | Nexus e Artifactory demandam máquinas virtuais próprias, gestão de armazenamento e controle separado de credenciais. O GitHub Packages elimina a complexidade de Gestão de Identidade e Acesso — _Identity and Access Management_ (IAM) — ao publicar o pacote diretamente no repositório do projeto via `GITHUB_TOKEN`. |
| **Observabilidade (_ChatOps_)** | **Microsoft Teams** | Slack, E-mail | Notificações por e-mail não atendem à resposta em tempo real e geram fadiga de alertas, enquanto o Slack exigiria licenciamento adicional para simular um cenário corporativo. O Microsoft Teams, integrado via _Workflows_ e _Adaptive Cards_, reflete fielmente o padrão de comunicação utilizado em ambientes empresariais. |

**Fonte:** Elaborada pela autora (2026).

Com base nessas escolhas, a automação no GitHub Actions foi dividida em dois fluxos de trabalho (_workflows_) independentes: um dedicado à Integração, Qualidade e Segurança Contínua (`ci_integracao_e_qualidade.yml`) e outro exclusivo para a Entrega Contínua (`cd_entrega_continua.yml`). Como camada adicional de proteção contra ataques à cadeia de suprimentos (_Supply Chain_), todas as _actions_ externas utilizadas nas esteiras foram fixadas pelo _hash_ imutável do _commit_ via _Secure Hash Algorithm_ de 256 _bits_ (SHA-256) em vez de _tags_ mutáveis, prevenindo a execução de código adulterado em dependências de terceiros.

O fluxo de Integração Contínua — _Continuous Integration_ (CI) — é acionado a cada _push_ ou abertura de _Pull Request_ na _branch_ `main`, provisionando um ambiente Ubuntu com _Java Development Kit_ (JDK) 11 (Temurin) para compilação e execução do comando `mvn clean verify`. Nessa etapa, a confiabilidade do _microframework_ é validada pela bateria de testes unitários com JUnit 5 e Mockito, utilizando objetos simulados (_mocks_) para reproduzir conexões JDBC, `PreparedStatement`, `CallableStatement` e `ResultSet` em memória sem depender de uma instância de banco de dados durante a execução da esteira, enquanto o JaCoCo coleta os dados e gera o relatório de cobertura:

![Pipeline de Integração Contínua](/docs/image/imagePipelineCI.png)

![Execução dos testes automatizados](/docs/image/imagePipelineTestes.png)

Concluída a fase de testes, a esteira envia os resultados e o relatório de cobertura para o SonarCloud, dando início à etapa de Qualidade Contínua (_Continuous Quality_). A plataforma analisa a cobertura de testes, o percentual de duplicação, _bugs_, vulnerabilidades e _code smells_, cabendo ao _Quality Gate_ verificar os critérios estabelecidos e reprovar automaticamente a integração da _Pull Request_ caso qualquer indicador fique abaixo do padrão exigido:

![Imagem da plataforma do SonarQube Cloud](/docs/image/imageSonarQube.png)

Na sequência, aplica-se a prática de Segurança Antecipada (_Shift-Left Security_) por meio do Semgrep, que realiza a varredura SAST no código Java do SEOS com base nas categorias do OWASP _Top_ 10. Para proteger o próprio ambiente de _build_, a instalação da ferramenta bloqueia a execução de pacotes binários desconhecidos, e o laudo final da análise é exportado no padrão _Static Analysis Results Interchange Format_ (SARIF) diretamente para a aba de segurança do repositório no GitHub:

![Análise SAST com Semgrep](/docs/image/imagePipelineSAST.png)

Para garantir resposta rápida caso qualquer validação anterior apresente erro, os dois fluxos contam com um mecanismo automatizado de observabilidade e _ChatOps_ que dispara um alerta via _Webhook_ para o Microsoft Teams sempre que uma etapa falha. A notificação é exibida em formato _Adaptive Card_ e informa em tempo real a esteira afetada, o repositório, a _branch_ ou versão, o motivo da falha e o atalho direto para os _logs_ da execução, reduzindo o Tempo Médio de Recuperação — _Mean Time to Recovery_ (MTTR) — durante o desenvolvimento:

![Alerta automatizado no Microsoft Teams](/docs/image/imageMensagemTeams.png)

Por sua vez, a disponibilização oficial da biblioteca ocorre na esteira de Entrega Contínua — _Continuous Delivery_ (CD) —, acionada exclusivamente quando uma nova _tag_ de versionamento semântico é publicada no repositório. Nessa etapa, a _pipeline_ extrai automaticamente o identificador da versão, atualiza o descritor do projeto Maven e realiza a publicação autenticada no _GitHub Packages_, disponibilizando o pacote `.jar` pronto para ser importado como dependência por projetos externos:

![Pipeline de Entrega Contínua](/docs/image/imagePipelineCD.png)

![Pacote publicado no GitHub Packages](/docs/image/imagePackageMaven.png)

Por fim, a Tabela 5 apresenta o plano e cronograma estruturado para o desenvolvimento da biblioteca, a implantação de sua esteira _DevSecOps_ e a sua aplicação prática na refatoração do cenário estudado:

#### Tabela 5 — Plano e cronograma de aplicação _DevSecOps_ e adoção do SEOS

| Fase | Período | Etapa do Plano | Entregável e Objetivo Técnico |
| :--- | :--- | :--- | :--- |
| **Fase 1** | Mês 1 | Diagnóstico e Concepção do SEOS | Levantamento do cenário legado e implementação autoral do núcleo em Java puro/JDBC (`@TableDB`, `@ColumnDB`, `SQLBuilder`, `SQLResult`, `SQLCall` e `OrmLogger`). |
| **Fase 2** | Mês 2 | Integração, Qualidade e SAST | Implementação do _workflow_ `ci_integracao_e_qualidade.yml` com testes unitários (JUnit 5, Mockito, JaCoCo), _Quality Gate_ no SonarCloud, análise SAST com Semgrep (SARIF) e fixação de _actions_ por _hash_ SHA-256. |
| **Fase 3** | Mês 3 | _ChatOps_ e Entrega Contínua | Implementação do _workflow_ `cd_entrega_continua.yml`, alertas automatizados de falha no Microsoft Teams via _Webhook_ e publicação versionada (`v*.*.*`) no GitHub Packages. |
| **Fase 4** | Mês 4 | Aplicação e Avaliação Comparativa | Aplicação prática em 9 classes DAO de novos módulos do _e-commerce_ e refatoração de amostra da classe legada `ErpIntegracaoDAO` (21.381 linhas) para aferição das métricas de redução de código e segurança. |
| **Fase 5** | Mês 5 | Plano de Adoção Gradual | Recomendação de diretrizes para importação da biblioteca nos novos desenvolvimentos e substituição progressiva dos métodos legados vulneráveis. |

**Fonte:** Elaborada pela autora (2026).

### 3.7 Quadro de respostas — antes e depois

Para mensurar de forma concreta os ganhos da adoção do SEOS frente ao cenário diagnosticado nas Seções 1.3 e 3.2, a avaliação comparativa foi conduzida em duas frentes complementares dentro do **sistema de _e-commerce_** da empresa. A primeira consistiu na análise de **9 classes DAO recém-desenvolvidas já utilizando o SEOS** em novos módulos operacionais integrados à plataforma, contrastadas com os demais 69 arquivos de persistência em JDBC manual do mesmo repositório; a segunda abrangeu a **refatoração de uma amostra representativa de 15 métodos críticos da classe legada `ErpIntegracaoDAO`** (o arquivo-base de 21.381 linhas e 416 chamadas a `PreparedStatement` apresentado na Seção 1.3), permitindo aferir diretamente o comportamento antes e depois da biblioteca sobre o mesmo código.

Na primeira frente, apresentada na Tabela 6, observa-se o impacto da adoção nativa do SEOS nos novos módulos do _e-commerce_ em comparação com o padrão histórico de escrita manual do repositório:

#### Tabela 6 — Comparação entre o padrão legado (JDBC manual) e os módulos desenvolvidos com o SEOS no _e-commerce_

| Critério Avaliado | Padrão Legado (JDBC Manual) | Módulos com SEOS (9 DAOs) |
| :--- | :--- | :--- |
| **Arquivos analisados** | 69 classes com `prepareStatement` | 9 DAOs e 11 DTOs anotados com `@TableDB` |
| **Chamadas manuais a `prepareStatement`** | 1.314 ocorrências | **0** (encapsuladas pelo SEOS) |
| **Mapeamento manual (`rs.getXxx()`)** | Manual, campo a campo | **0** (automatizado pelo `SQLResult`) |
| **Pontos de montagem e execução via SEOS** | — | 82 montagens (`SQLBuilder`) e 62 execuções |
| **Média de linhas por método** | $\approx 52$ linhas (21.381 linhas / 409 métodos) | **$\approx 27$ linhas** (1.652 linhas / 62 métodos) |
| **Arquivos sem registro de _log_** | 41 de 69 arquivos (**59,4%**) | **0 de 9 arquivos** (100% auditados) |

**Fonte:** Elaborada pela autora (2026).

A redução de **48% na média de linhas por método** (de 52 para 27 linhas, já contabilizando regras de negócio) reflete diretamente a eliminação do código repetitivo de infraestrutura. Enquanto um método de consulta simples no padrão legado do _e-commerce_ — como a busca de usuário por documento na classe `ErpIntegracaoDAO` — consome **32 linhas** para preparar a instrução, preencher parâmetros, percorrer o resultado, extrair cinco colunas manualmente e capturar exceções de forma inadequada, operações equivalentes nas classes baseadas no SEOS (como `UsuarioAcessoDAO`) são resolvidas em uma a três linhas encadeadas por meio dos construtores e executores da biblioteca.

Já na segunda frente de avaliação, selecionou-se uma amostra de **15 métodos do `ErpIntegracaoDAO`** — contemplando três operações de `UPDATE`, um `INSERT`, seis consultas escalares ou de contagem, duas consultas de lista, duas buscas de registro único e uma consulta em formato de dicionário — para medir o efeito direto da refatoração com o SEOS. Conforme demonstrado na Tabela 7, a substituição do JDBC manual na amostra proporciona uma redução de **36,1% no volume total de linhas de código**, além de extinguir integralmente as concatenações vulneráveis e a manipulação manual de recursos:

#### Tabela 7 — Comparativo antes e depois na refatoração da amostra de 15 métodos do `ErpIntegracaoDAO`

| Indicador (Soma dos 15 métodos analisados) | Antes do SEOS | Depois do SEOS | Variação Obtida |
| :--- | :--- | :--- | :--- |
| **Total de linhas de código** | 363 linhas | $\approx 232$ linhas | **$-36,1\%$** |
| **Chamadas manuais a `prepareStatement`** | 15 | 0 | **$-100\%$** |
| **Preenchimento manual de parâmetros (`setXxx`)** | 11 | 0 | **$-100\%$** |
| **Extração manual de colunas (`rs.getXxx`)** | 18 | 0 | **$-100\%$** |
| **Concatenações diretas de variáveis na SQL** | 8 (em 7 métodos) | 0 (100% parametrizado) | **$-100\%$** |
| **Tratamento inadequado com `printStackTrace`** | 10 | 0 | **$-100\%$** |
| **Fechamentos manuais de recursos (`.close()`)** | 15 | 0 (`try-with-resources` interno) | **$-100\%$** |
| **Métodos com auditoria de tempo, origem e erro** | 0 de 15 (0%) | 15 de 15 (100%) | **$+100$ p.p.** |

**Fonte:** Elaborada pela autora (2026).

### 3.8 Resultados e Dados Consolidados do Projeto

A consolidação dos levantamentos realizados nos dois sistemas legados da empresa (**2.890 métodos de persistência** analisados na Seção 3.2) em conjunto com as medições práticas obtidas no sistema de _e-commerce_ (Seção 3.7) comprova a aderência e a eficácia arquitetural do SEOS. No que tange à produtividade e à redução de _boilerplate_, a aplicação real da biblioteca em 9 classes DAO novas — totalizando 82 pontos de montagem de instruções SQL e 62 execuções operando sobre conexões fornecidas pela própria aplicação — reduziu a média de código de aproximadamente **52 para 27 linhas por método** ($-48\%$), confirmando a preservação do controle transacional sem sobrecarga de escrita manual. Esse ganho é corroborado pela refatoração direta da amostra de 15 métodos da classe `ErpIntegracaoDAO`, que registrou redução imediata de **36,1% no total de linhas** (de 363 para cerca de 232 linhas) e extinguiu todas as manipulações manuais de preparação, leitura e fechamento de recursos.

Sob a ótica da segurança cibernética na camada de persistência, tanto nos novos módulos desenvolvidos com o SEOS quanto nos 15 métodos refatorados da amostra legada, a exposição à concatenação direta de variáveis na instrução SQL foi reduzida a **zero ($-100\%$)**. Todos os filtros e valores persistidos passaram a trafegar obrigatoriamente de forma parametrizada e validada quanto à correspondência de parâmetros antes da execução, somados ao bloqueio preventivo de atualizações desprovidas de cláusula de filtro.

Por fim, no eixo de observabilidade e governança operacional, frente a um passivo histórico no qual **94,4%** dos métodos do sistema de _e-commerce_ (Tabela 2) e **59,4%** dos arquivos em JDBC manual não possuíam nenhum registro de _log_, a adoção do SEOS elevou a cobertura de auditoria e o tratamento estruturado de falhas para **100%** nos módulos avaliados, garantindo o rastreamento automatizado do tempo de execução, da contagem de parâmetros e da classe e método de origem de cada operação.

## 4. Conclusão

O desenvolvimento deste trabalho partiu de um desafio real na manutenção de sistemas corporativos legados: lidar com uma camada de persistência extensa, repetitiva e historicamente exposta a falhas de segurança, mas que, pelas regras de integração com o ERP, exigia que o controle sobre conexões e transações permanecesse sob domínio direto da aplicação. A evolução do **SEOS** (_Software Endurance Orchestration System_) aliada à cultura _DevSecOps_ comprovou que é possível equilibrar essas duas necessidades, oferecendo uma abordagem híbrida que automatiza o trabalho mecânico do JDBC sem impor a complexidade e a sobrecarga de um ORM tradicional de mercado.

Os resultados alcançados no estudo de caso confirmam, na prática, os pilares de padronização, simplificação, proteção e reutilização que nortearam o projeto. A adoção da biblioteca reduziu quase pela metade a média de linhas por método nos novos módulos do _e-commerce_ ($-48\%$) e enxugou em $36,1\%$ o volume de código na amostra legada refatorada, além de eliminar integralmente ($-100\%$) as concatenações diretas suscetíveis a _SQL Injection_ e elevar para $100\%$ a cobertura de auditoria e rastreamento de falhas nas rotinas avaliadas. Em conjunto com esses ganhos na aplicação, a construção da esteira automatizada de integração, análise de qualidade, verificação estática de segurança, alertas em tempo real e entrega contínua assegurou que a própria biblioteca seja evoluída e distribuída com alto rigor de confiabilidade.

Por fim, respeitando a proposta enxuta que caracteriza o _microframework_, identificam-se caminhos naturais para a sua evolução contínua. Como trabalhos futuros, pretende-se expandir gradualmente seus recursos para suportar o reaproveitamento de mapeamentos por herança entre classes, permitir a gravação intencional de valores nulos em atualizações específicas, flexibilizar as inserções em lote para objetos com diferentes combinações de campos e oferecer suporte auxiliar à montagem de consultas com múltiplos relacionamentos, preservando sempre a simplicidade e a previsibilidade que motivaram a sua criação.

## 5. Referências

As referências bibliográficas deverão ser incluídas após a seleção das fontes utilizadas no referencial teórico e na análise de segurança.

## 6. Anexos

### Anexo A — Questionário baseado no OWASP Top 10

## Questionário de Maturidade por Categoria

### A01:2021 – Quebra de Controle de Acesso (*Broken Access Control*)
*   **Cenário ANTES:** Como os privilégios e permissões de usuários eram validados nas rotas e APIs? Havia testes manuais ou revisões pontuais de código para garantir que um usuário comum não acessasse dados confidenciais ou funções de administrador?
*   **Cenário DEPOIS (DevSecOps):** Existem testes automatizados de comportamento/integração (como BDD) integrados ao pipeline que tentam simular a quebra de privilégios? Há ferramentas varrendo as definições de rotas de API em busca de endpoints expostos incorretamente?

### A02:2021 – Falhas Criptográficas (*Cryptographic Failures*)
*   **Cenário ANTES:** Como era verificado se dados sensíveis (senhas, cartões, tokens) estavam criptografados no banco de dados e em trânsito (HTTPS)? A checagem da validade de certificados e algoritmos dependia de auditoria manual?
*   **Cenário DEPOIS (DevSecOps):** A esteira de CI/CD utiliza ferramentas de **SAST** (Static Application Security Testing) para detectar o uso de chaves criptográficas fracas ou segredos expostos no código (*Hardcoded Secrets*) antes do merge?

### A03:2021 – Injeção (*Injection*)
*   **Cenário ANTES:** A validação, filtragem e sanitização de dados inseridos pelos usuários (*inputs*) dependiam exclusivamente da disciplina e da revisão manual feita pelos desenvolvedores?
*   **Cenário DEPOIS (DevSecOps):** Ferramentas de SAST barram o *build* se identificarem strings concatenadas em consultas de banco de dados? Existe alguma etapa de análise dinâmica (**DAST**) ou testes automatizados simulando payloads de injeção em ambiente de homologação?

### A04:2021 – Design Inseguro (*Insecure Design*)
*   **Cenário ANTES:** A segurança foi debatida ou planejada durante o desenho da arquitetura da aplicação, ou as correções de vulnerabilidades eram feitas de forma reativa após o código estar pronto?
*   **Cenário DEPOIS (DevSecOps):** Foram adotados padrões arquiteturais seguros desde o início (ex: autenticação centralizada, APIs com *Rate Limiting*) e modelagem de ameaças integrada ao planejamento das sprints?

### A05:2021 – Configuração Incorreta de Segurança (*Security Misconfiguration*)
*   **Cenário ANTES:** O endurecimento (*hardening*) de servidores, gerenciamento de permissões de nuvem e cabeçalhos HTTP de segurança (CSP, HSTS) eram configurados manualmente direto nos ambientes?
*   **Cenário DEPOIS (DevSecOps):** A infraestrutura é gerenciada como código (**IaC**)? Existem ferramentas no pipeline (como Checkov, TFLint) para escanear os arquivos de configuração de infraestrutura antes do provisionamento?

### A06:2021 – Componentes Vulneráveis e Desatualizados (*Vulnerable and Outdated Components*)
*   **Cenário ANTES:** Como a equipe descobria se uma biblioteca de terceiros (pacotes npm, pip, NuGet, Maven) continha uma vulnerabilidade crítica conhecida? Havia controle por planilha ou varreduras esporádicas?
*   **Cenário DEPOIS (DevSecOps):** Foi implementada uma ferramenta de **SCA** (Software Composition Analysis), como *Snyk* ou *Dependency-Check*, que interrompe o pipeline automaticamente caso uma dependência insegura seja introduzida?

### A07:2021 – Falhas de Identificação e Autenticação (*Identification and Authentication Failures*)
*   **Cenário ANTES:** A aplicação permitia ataques de força bruta nas telas de login? O gerenciamento de sessões (como tokens JWT) possuía validação estritamente manual quanto ao tempo de expiração e rotação?
*   **Cenário DEPOIS (DevSecOps):** Cenários automatizados de testes cobrem e validam a robustez do fluxo de autenticação, políticas de bloqueio de conta e obrigatoriedade de segundo fator (MFA)?

### A08:2021 – Falhas de Integridade de Dados e Software (*Software and Data Integrity Failures*)
*   **Cenário ANTES:** O processo de build e deploy dependia de ações manuais (ex: gerar o zip da aplicação e enviar via FTP/SSH), gerando riscos de adulteração e falta de rastreabilidade do artefato?
*   **Cenário DEPOIS (DevSecOps):** O pipeline CI/CD garante a assinatura dos artefatos (imagens Docker, pacotes)? As imagens de container sofrem varredura automatizada de vulnerabilidades (ex: *Trivy*) antes de irem para o registro oficial?

### A09:2021 – Falhas de Log e Monitoramento (*Security Logging and Monitoring Failures*)
*   **Cenário ANTES:** Onde os erros críticos e possíveis tentativas de invasão eram registrados? O diagnóstico de incidentes dependia de o desenvolvedor acessar arquivos locais de log no servidor após o problema acontecer?
*   **Cenário DEPOIS (DevSecOps):** Os logs de segurança estão centralizados de forma estruturada (ex: ELK Stack, Splunk) e integrados a alertas em tempo real que notificam o time sobre anomalias no ambiente de produção?

### A10:2021 – Falsificação de Requisição do Lado do Servidor (*SSRF*)
*   **Cenário ANTES:** A aplicação consome URLs fornecidas por usuários? Como era garantido manualmente que o servidor não realizaria requisições maliciosas para dentro da própria rede interna?
*   **Cenário DEPOIS (DevSecOps):** Regras de isolamento de rede e políticas de saída (*egress*) são validadas de forma automatizada via IaC, e funções de requisição externa no código-fonte passam por políticas rígidas de validação do SAST?

---

## 3. Matriz Comparativa de Evidências
Preencha a tabela abaixo com o resumo prático do que foi constatado na sua aplicação.

| Categoria OWASP | Cenário ANTES (Processo Manual / Risco) | Prática/Ferramenta DevSecOps Adotada | Cenário DEPOIS (Mitigação / Automação) |
| :--- | :--- | :--- | :--- |
| **A01: Quebra de Controle de Acesso** | *Ex: Teste manual de rotas.* | *Ex: Testes de integração automatizados.* | *Ex: Cobertura de testes valida privilégios a cada deploy.* |
| **A02: Falhas Criptográficas** | | | |
| **A03: Injeção** | | | |
| **A04: Design Inseguro** | | | |
| **A05: Configuração Incorreta** | | | |
| **A06: Componentes Vulneráveis** | | | |
| **A07: Falhas de Autenticação** | | | |
| **A08: Integridade de Dados** | | | |
| **A09: Falhas de Log/Monitoramento** | | | |
| **A10: SSRF** | | | |

---
