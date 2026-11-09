# Relatório de Projeto

## SEOS (Software Endurance Orchestration System): redução de boilerplate, auditoria e segurança cibernética na camada de persistência

**Autora:** Maria Antônia Santos de Lima  
**Instituição:** Fundação Edson Queiroz — Universidade de Fortaleza (UNIFOR)  
**Curso:** Pós-Graduação em Engenharia de Software com foco em DevOps  
**Local:** Fortaleza-CE  
**Ano:** 2026

## 1. Introdução

Atualmente, na empresa onde trabalho, os métodos de consulta utilizados em nossos projetos são muito verbosos. Para realizar uma consulta, é necessário escrevê-la manualmente, armazená-la em uma variável, mapear os parâmetros do `PreparedStatement` e, logo em seguida, mapear o `ResultSet`, campo a campo.

Em alguns casos, as consultas não possuem parâmetros. Em outros, os valores são concatenados diretamente na instrução _Structured Query Language_ (SQL), o que cria uma brecha significativa para _SQL Injection_. Além disso, existem diversas versões de operações como `INSERT`, `UPDATE` e consultas para uma mesma tabela. Isso ocorre porque determinadas implementações precisam de estruturas específicas que não podem ser modificadas, resultando na duplicação de operações semelhantes.

Nesse contexto, foi desenvolvido o **SEOS** (_Software Endurance Orchestration System_), um _microframework_ de Mapeamento Objeto-Relacional (ORM, do inglês _Object-Relational Mapping_) criado para atender às necessidades específicas da empresa. O _framework_ mapeia tabelas, campos e _sequences_ por meio de _annotations_ e constrói estruturas _Data Manipulation Language_ (DML) e _Data Query Language_ (DQL) com base nesses mapeamentos. Dessa forma, busca tornar o acesso ao banco de dados mais padronizado e seguro, além de retornar as classes com os valores já mapeados e prontos para serem utilizados pelas implementações.

## 1.1 Objetivo

### Objetivo geral

Evoluir o _microframework_ SEOS com base em práticas consolidadas de mercado e estruturar seu processo de desenvolvimento sob a cultura _DevSecOps_, a fim de demonstrar os ganhos de otimização, padronização, auditoria e segurança na camada de persistência.

### Objetivos específicos

- **Automatização de persistência:** implementar um mecanismo automatizado de geração de instruções DML/DQL (`SQLBuilder`) e de mapeamento objeto-relacional de `ResultSet` baseado em _annotations_ e _reflection_;
- **Segurança contra injeções:** mitigar riscos de _SQL Injection_ na camada de persistência por meio da parametrização obrigatória de consultas no `PreparedStatement` e validação estrutural das operações;
- **Auditoria e observabilidade:** incorporar recursos para rastreamento de erros, parâmetros e tempo de execução das consultas ao banco de dados;
- **Esteira _DevSecOps_ (CI/CD):** estruturar uma _pipeline_ de Integração Contínua com execução automatizada de testes, análise de cobertura e qualidade (JaCoCo e SonarCloud), análise estática de segurança — _Static Application Security Testing_ (SAST) com Semgrep —, notificações via _ChatOps_ e Entrega Contínua configurando o GitHub Packages com versionamento semântico da biblioteca;
- **Avaliação arquitetural:** avaliar a aderência arquitetural do SEOS frente ao cenário corporativo estudado e em comparação com soluções consolidadas de mercado, como Hibernate/_Java Persistence API_ (JPA);
- **Mensuração de resultados:** mensurar os ganhos quantitativos e qualitativos da adoção do SEOS em relação à abordagem tradicional — _Java Database Connectivity_ (JDBC) manual —, considerando redução de linhas de código, taxa de duplicação e exposição a vulnerabilidades baseadas no _Open Worldwide Application Security Project_ (OWASP) _Top_ 10.

## 1.2 Justificativa

Ao criar uma implementação para atender a uma demanda e perceber que não poderia reutilizar o mesmo método de persistência no banco de dados, a primeira coisa que pensei foi: _“Por que criar mais uma versão de um `UPDATE` com as mesmas condições?”_. Foi nesse momento que percebi que a **falta de padronização**, a **baixa qualidade do código** e os **problemas de segurança nas consultas** eram fatores que precisavam ser tratados.

Por meio dessa necessidade nasceu o **SEOS**, criado para ser uma solução capaz de reduzir o trabalho manual e a duplicação de métodos, facilitar a manutenção dos projetos empresariais e diminuir os riscos de _SQL Injection_, <u>sem retirar da equipe o controle</u> sobre as consultas, conexões e transações de persistência.

## 1.3 Problema

- **21.381** linhas de código;
- **409** métodos, dos quais **402** acessam o banco de dados;
- **19,6%** de linhas idênticas;
- **54,4%** de linhas repetidas;
- Entre **416** chamadas a `PreparedStatement`, apenas **156** utilizam métodos `setXxx()`;
- Aproximadamente **43%** dos métodos são potencialmente vulneráveis a _SQL Injection_.

Esses são os números mapeados juntamente com a equipe de Arquitetura da empresa a partir de <u>um único arquivo _Repository_ de um único projeto</u> utilizado no contexto empresarial. Os alertas sobre o desenvolvimento inadequado de métodos de persistência nos projetos, que possibilitam a ocorrência de _SQL Injection_, já haviam sido repassados; contudo, devido à ausência histórica de uma abstração padronizada, à pressão por entregas rápidas e ao fato de os projetos existentes serem catalogados como legados, tornou-se difícil corrigir e padronizar o desenvolvimento manualmente, e é exatamente nesse cenário que o SEOS se encaixa.

Embora ele atue como uma biblioteca Java distribuída sob versionamento semântico — não sendo uma aplicação final em si —, ele opera no núcleo da camada de persistência dos módulos corporativos que sustentam a operação da empresa. No exemplo citado anteriormente, a presença de aproximadamente **43% de métodos suscetíveis a _SQL Injection_** representa um risco operacional crítico ao banco de dados. A exploração de uma vulnerabilidade dessa natureza, ou a indisponibilidade do sistema devido à corrupção de tabelas essenciais ao fluxo operacional, exigiria um tempo estimado de recuperação de `[INFORMAR_DEPOIS]` para conter e restaurar o ambiente. Nesse intervalo, estima-se um **prejuízo direto** de aproximadamente `[R$ X]` devido à paralisação do faturamento e das vendas, além de **prejuízos indiretos** severos, como perda de clientes e desgaste reputacional. Diante disso, a adoção do SEOS, aliada à esteira _DevSecOps_ e ao controle estrito de versões, atua diretamente na mitigação preventiva desses impactos financeiros e operacionais.

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

Esses dois sistemas sustentam rotinas operacionais críticas de diferentes fluxos de negócio da empresa — um responsável por **automações produtivas, logísticas e contábeis**, e o outro responsável pelo **_e-commerce_** —, concentrando um elevado volume de consultas e persistências desenvolvidas ao longo dos anos. Pela forma como os sistemas foram estruturados junto ao ERP, as consultas, as conexões e as transações precisam continuar <u>sob o controle direto da aplicação</u>, o que impedia o uso de um ORM tradicional que gerenciasse tudo de forma automática.

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

Abaixo apresenta-se um exemplo claro do que alimentou os dados desses levantamentos:

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

Analisando os dados acima e levando em conta o exemplo que os alimenta, reflete-se o quão verboso e vulnerável é o padrão de desenvolvimento aplicado ao longo dos anos de construção desses sistemas, pois, além do volume de código, a concatenação direta de valores na consulta dificulta a padronização e pode permitir _SQL Injection_. A existência de várias implementações semelhantes e a falta de _logs_ de auditoria também aumentam o custo de manutenção.

### 3.3 Solução Proposta

Diante dos números levantados na seção anterior — que evidenciaram o excesso de código manual, o risco de _SQL Injection_ e a ausência de _logs_ nos dois sistemas da empresa —, foi idealizado o **SEOS**: um _microframework_ de ORM dentro do ecossistema Java capaz de resolver essas falhas respeitando a arquitetura existente, sem adicionar complexidade e garantindo maior celeridade no desenvolvimento.

A proposta central do SEOS é automatizar justamente a parte mecânica e propensa a erros do uso do JDBC. Em vez de o desenvolvedor repetir manualmente todo o ciclo de preparação e leitura de dados em cada método, a biblioteca assume a construção segura das instruções, a validação dos parâmetros e a auditoria das execuções, devolvendo os objetos já preenchidos <u>sem retirar da aplicação o controle explícito sobre a conexão e a transação</u>.

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

Nas operações de escrita (`INSERT` e `UPDATE`), a execução ocorre principalmente por meio do método `executeUpdate`, que aplica os parâmetros no banco e retorna a quantidade de registros afetados. Contudo, em inserções nas quais a aplicação precisa utilizar imediatamente o identificador recém-gerado pela _sequence_ do Oracle, o método `executeAndReturnId` aproveita a marcação `isPrimaryKey = true` da classe para recuperar a chave primária gerada no banco e preenchê-la diretamente no próprio objeto enviado. Já para cenários de alto volume de dados, o método `executeBatch` processa coleções inteiras de objetos em uma única comunicação em lote com o banco, permitindo que o chamador controle explicitamente o escopo transacional (`commit` e `rollback`) sobre os objetos `newUser` e `updateUser` exemplificados anteriormente:

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

Como já mencionado, a conexão JDBC necessária para a execução dessas funcionalidades parte sempre do chamador; não é responsabilidade do SEOS fechar, confirmar ou reverter a `Connection`, sendo encerrados internamente apenas o `PreparedStatement` e o `ResultSet`.

Nas operações de leitura (`SELECT`), um dos principais gargalos observados nos sistemas legados da empresa era a repetição de laços `while (rs.next())` para extrair manualmente cada coluna do `ResultSet`, independentemente de a consulta retornar uma lista completa de entidades, um único registro, um agrupamento para relatórios ou apenas um contador numérico. Para eliminar essa repetição sem obrigar a equipe a criar classes DTO para todo tipo de consulta, o `SQLResult` foi estruturado em diferentes modalidades de leitura.

Quando a consulta envolve entidades mapeadas da aplicação, a leitura pode ser feita diretamente para objetos tipados por meio dos métodos `executeQuery` e `executeQuerySingle`. Enquanto o primeiro percorre todo o `ResultSet` e instancia automaticamente uma lista da classe informada com base nas anotações `@ColumnDB`, o segundo é voltado a buscas pontuais (como consultas por chave primária), retornando apenas o primeiro registro encontrado ou `null` quando não há correspondência no banco. Além disso, para facilitar a adoção gradual da biblioteca em métodos legados que ainda manipulam o `PreparedStatement` manualmente, o método `mapearResultSet` foi exposto publicamente, permitindo aproveitar o mapeamento automático a partir de um `ResultSet` já aberto:

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

Por outro lado, em rotinas de relatórios, totalizadores ou consultas parciais nas quais a criação de um DTO específico seria desnecessária, o `SQLResult` permite extrair os dados em estruturas dinâmicas do próprio Java. Nesses cenários, os métodos `executeQueryAsMap` e `executeQueryAsSingleMap` convertem as linhas retornadas em mapas (`Map<String, Object>`) indexados pelo nome da coluna em letras maiúsculas. De modo complementar, o método `executeAsDictionary` transforma consultas de duas colunas diretamente em um dicionário de chave e valor (`Map<K, V>`), facilitando o carregamento de pares como identificador e nome em memória, enquanto o `executeScalar` extrai e converte automaticamente o valor da primeira coluna da primeira linha (tratando, inclusive, o retorno `BigDecimal` padrão do Oracle para tipos como `Integer` ou `Long`):

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

Além das instruções DQL e DML tradicionais, o ambiente Oracle 19c integrado ao ERP concentra diversas regras de negócio dentro do próprio banco por meio de _procedures_ e _packages_ escritas em _Procedural Language/Structured Query Language_ (PL/SQL). Na abordagem manual com `CallableStatement`, a invocação dessas rotinas exigia a montagem da sintaxe `{call ...}`, o controle manual da posição numérica de cada parâmetro e a leitura dos valores de saída também por índice, aumentando a incidência de erros de manutenção. Para padronizar esse fluxo, foi desenvolvida a classe `SQLCall`, que oferece uma interface fluente para declarar o nome da _procedure_ (`.name()`) e registrar parâmetros de entrada (`.in()`), de saída (`.out()`) ou mistos (`.inOut()`), associando os retornos a apelidos legíveis (_aliases_) que podem ser recuperados de forma tipada após a execução:

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

Por fim, para solucionar a grave ausência de rastreabilidade apontada na Seção 3.2 — na qual **34,9%** dos métodos do sistema de automações e **94,4%** dos métodos do _e-commerce_ não possuíam nenhum registro de _log_ —, o SEOS incorporou uma camada transversal de auditoria e observabilidade baseada na interface _Service Provider Interface_ (SPI) `OrmLogger`, gerenciada globalmente pela classe `OrmConfig`. Por meio dessa estrutura, todas as execuções realizadas pelo `SQLResult` e pelo `SQLCall` têm seu tempo de resposta cronometrado automaticamente e inspecionam a pilha de execução da _thread_ (`StackTraceElement`) para identificar exatamente a classe e o método da aplicação que originaram a chamada ao banco. Quando ocorre qualquer exceção SQL ou quando uma operação ultrapassa o limite de tempo configurado — **10 segundos** para consultas no `SQLResult` e **3 segundos** para chamadas PL/SQL no `SQLCall` —, a biblioteca registra automaticamente o alerta com a origem da chamada, o tempo decorrido e a quantidade de parâmetros enviados, permitindo que os sistemas legados conectem seu próprio mecanismo de _logs_ na inicialização da aplicação:

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

Com base nessas escolhas, a automação no GitHub Actions foi dividida em dois fluxos de trabalho (_workflows_) independentes: um dedicado à Integração, Qualidade e Segurança Contínua (`ci_integracao_e_qualidade.yml`) e outro exclusivo para a Entrega Contínua (`cd_entrega_continua.yml`). Como camada adicional de proteção contra ataques à cadeia de suprimentos (_Supply Chain_), todas as _actions_ externas utilizadas nas esteiras foram fixadas pelo _hash_ imutável do _commit_ (SHA-256) em vez de _tags_ mutáveis, prevenindo a execução de código adulterado em dependências de terceiros.

O fluxo de Integração Contínua — _Continuous Integration_ (CI) — é acionado a cada _push_ ou abertura de _Pull Request_ na _branch_ `main`, provisionando um ambiente Ubuntu com JDK 11 (Temurin) para compilação e execução do comando `mvn clean verify`. Nessa etapa, a confiabilidade do _microframework_ é validada pela bateria de testes unitários com JUnit 5 e Mockito, utilizando objetos simulados (_mocks_) para reproduzir conexões JDBC, `PreparedStatement`, `CallableStatement` e `ResultSet` em memória sem depender de uma instância de banco de dados durante a execução da esteira, enquanto o JaCoCo coleta os dados e gera o relatório de cobertura:

![Pipeline de Integração Contínua](/docs/image/imagePipelineCI.png)

![Execução dos testes automatizados](/docs/image/imagePipelineTestes.png)

Concluída a fase de testes, a esteira envia os resultados e o relatório de cobertura para o SonarCloud, dando início à etapa de Qualidade Contínua (_Continuous Quality_). A plataforma analisa a cobertura de testes, o percentual de duplicação, _bugs_, vulnerabilidades e _code smells_, cabendo ao _Quality Gate_ verificar os critérios estabelecidos e reprovar automaticamente a integração da _Pull Request_ caso qualquer indicador fique abaixo do padrão exigido:

![Imagem da plataforma do SonarQube Cloud](/docs/image/imageSonarQube.png)

Na sequência, aplica-se a prática de Segurança Antecipada (_Shift-Left Security_) por meio do Semgrep, que realiza a varredura SAST no código Java do SEOS com base nas categorias do OWASP _Top_ 10. Para proteger o ambiente de _build_, a instalação da ferramenta restringe pacotes binários desconhecidos por meio do parâmetro `--only-binary :all:`, e o resultado da análise é exportado no padrão _Static Analysis Results Interchange Format_ (SARIF) diretamente para a aba _GitHub Security_ do repositório:

![Análise SAST com Semgrep](/docs/image/imagePipelineSAST.png)

Para garantir resposta rápida caso qualquer validação anterior apresente falha, os dois _workflows_ contam com um mecanismo assíncrono de observabilidade e _ChatOps_ (`if: failure()`) que aciona um alerta via _Webhook_ integrado ao Microsoft Teams. A notificação é renderizada em formato _Adaptive Card_ e informa em tempo real a esteira afetada, o repositório, a _branch_ ou versão, o motivo da falha e o atalho direto para os _logs_ da execução, reduzindo o Tempo Médio de Recuperação — _Mean Time to Recovery_ (MTTR) — durante o ciclo de desenvolvimento:

![Alerta automatizado no Microsoft Teams](/docs/image/imageMensagemTeams.png)

Por sua vez, a disponibilização oficial da biblioteca ocorre na esteira de Entrega Contínua — _Continuous Delivery_ (CD) — definida em `cd_entrega_continua.yml`, disparada exclusivamente quando uma _tag_ de versionamento semântico (`v*.*.*`) é enviada ao repositório. Nessa etapa, a _pipeline_ extrai o número da _tag_, atualiza dinamicamente a versão no `pom.xml` (`mvn versions:set`) e executa a publicação autenticada (`mvn deploy`) no _GitHub Packages_, disponibilizando o artefato `.jar` pronto para ser importado como dependência Maven por projetos externos:

![Pipeline de Entrega Contínua](/docs/image/imagePipelineCD.png)

![Pacote publicado no GitHub Packages](/docs/image/imagePackageMaven.png)

Por fim, a Tabela 5 apresenta o plano e cronograma estruturado para o desenvolvimento da biblioteca, a implantação de sua esteira _DevSecOps_ e a sua aplicação prática na refatoração do cenário estudado:

#### Tabela 5 — Plano e cronograma de aplicação _DevSecOps_ e adoção do SEOS

| Fase | Período | Etapa do Plano | Entregável e Objetivo Técnico |
| :--- | :--- | :--- | :--- |
| **Fase 1** | Mês 1 | Diagnóstico e Concepção do SEOS | Levantamento do cenário legado e implementação autoral do núcleo em Java puro/JDBC (`@TableDB`, `@ColumnDB`, `SQLBuilder`, `SQLResult`, `SQLCall` e `OrmLogger`). |
| **Fase 2** | Mês 2 | Integração, Qualidade e SAST | Implementação do _workflow_ `ci_integracao_e_qualidade.yml` com testes unitários (JUnit 5, Mockito, JaCoCo), _Quality Gate_ no SonarCloud, análise SAST com Semgrep (SARIF) e fixação de _actions_ por _hash_ SHA-256. |
| **Fase 3** | Mês 3 | _ChatOps_ e Entrega Contínua | Implementação do _workflow_ `cd_entrega_continua.yml`, alertas automatizados de falha no Microsoft Teams via _Webhook_ e publicação versionada (`v*.*.*`) no GitHub Packages. |
| **Fase 4** | Mês 4 | Aplicação e Avaliação Comparativa | Refatoração de estudo de caso sobre os padrões vulneráveis mapeados no arquivo _Repository_ crítico (402 métodos e 21.381 linhas) para aferição das métricas de redução de código e segurança. |
| **Fase 5** | Mês 5 | Plano de Adoção Gradual | Recomendação de diretrizes para importação da biblioteca nos novos desenvolvimentos e substituição progressiva dos métodos legados vulneráveis. |

**Fonte:** Elaborada pela autora (2026).

### 3.7 Quadro de respostas — antes e depois

Este quadro deverá ser preenchido após a execução das avaliações:

| Critério                                      | Antes do SEOS                            | Depois do SEOS                 |
| --------------------------------------------- | ---------------------------------------- | ------------------------------ |
| Linhas de código                              | 21.381 linhas no `ProjectDAO` analisado. | A preencher.                   |
| Linhas repetidas                              | 54,4%.                                   | A preencher.                   |
| Linhas idênticas                              | 19,6%.                                   | A preencher.                   |
| Chamadas a `PreparedStatement` com `setXxx()` | 156 de 416 chamadas.                     | A preencher.                   |
| Métodos potencialmente vulneráveis            | Aproximadamente 43%.                     | A preencher.                   |
| Mapeamento do `ResultSet`                     | Manual, campo a campo.                   | Realizado pelo SEOS.           |
| Construção das consultas                      | Manual e distribuída nos métodos.        | Padronizada pelo `SQLBuilder`. |

#### Resultados da metrificação

A comparação foi realizada em duas frentes complementares no repositório do portal corporativo (`portalTbm`): (i) o código **novo**, escrito já com o SEOS (9 DAOs), contrastado com o padrão histórico de JDBC manual do `SankhyaDAO`; e (ii) a **refatoração de uma amostra de métodos do próprio `SankhyaDAO`**, que permite a medição direta do antes e do depois sobre o mesmo arquivo analisado na Seção 1.3. O `SankhyaDAO` é o arquivo-base do levantamento (416 chamadas a `PreparedStatement`, coincidindo com o valor apresentado na Seção 1.3); na data desta medição ele possui 21.412 linhas, ante as 21.381 registradas no levantamento original.

##### Quadro A — Padrão de código: JDBC manual × SEOS (medição em `src/main/java`)

| Critério                                              | JDBC manual (restante do projeto)        | SEOS (9 DAOs do módulo de recebimento fiscal) |
| ----------------------------------------------------- | ---------------------------------------- | --------------------------------------------- |
| Arquivos                                              | 69 com `prepareStatement`                | 9 DAOs, 11 DTOs com `@TableDB`               |
| Chamadas a `prepareStatement` escritas manualmente    | 1.314                                    | 0                                             |
| Mapeamento manual `rs.getXxx()` nos DAOs              | Campo a campo                            | 0 (feito por `SQLResult`)                    |
| Pontos de montagem de SQL pelo SEOS                   | —                                        | 82 (`SQLBuilder.build*`/`select`/`SQLResult`) |
| Execuções via SEOS                                    | —                                        | 62                                            |
| Linhas por método (indicativo)                        | ≈ 68 (21.412 linhas / 316 métodos)¹      | ≈ 27 (1.652 linhas / 62 métodos)              |
| Arquivos sem nenhuma chamada de _log_                 | 41 de 69 (59%)                           | 0 de 9 (100% com `Logger`)                    |

¹ Contagem de métodos feita por _script_ sobre as assinaturas do arquivo; o levantamento da equipe de Arquitetura (Seção 1.3) considera 409 métodos, o que resultaria em ≈ 52 linhas por método. As duas funcionalidades comparadas não são idênticas, de modo que os valores devem ser lidos como ordem de grandeza.

**Fonte:** Elaborada pela autora (2026), a partir do repositório `portalTbm`.

##### Exemplo comparativo

O método `SankhyaDAO.buscarParceiroPorCpfCnpj` ocupa **32 linhas**: abre o `PreparedStatement`, aplica `setString`, percorre o `ResultSet` e lê cinco colunas com `rs.getString`, tratando o erro com `printStackTrace` e sem registro estruturado. Já no `VisitanteDAO`, a leitura de uma entidade completa por filtro parametrizado, o cadastro e a atualização condicional correspondem, respectivamente, a uma expressão encadeada de 3 linhas efetivas (`buildSelect(...).executeQuerySingle(...)`) e a duas expressões de 1 linha (`buildInsert(...).executeUpdate(...)` e `buildUpdate(..., "ID_VISITANTE = ?", id).executeUpdate(...)`), sendo que o `UPDATE` só é executado com a cláusula `WHERE` informada.

##### Quadro B — Refatoração de amostra do `SankhyaDAO` (antes × depois)

A amostra é composta por 15 métodos do `SankhyaDAO` (3 `UPDATE`, 1 `INSERT`, 6 consultas escalares ou de contagem, 2 listas, 2 consultas de linha única e 1 dicionário), selecionados por apresentarem JDBC manual, chamadores identificados e comportamento preservável. Os valores de "Antes" foram medidos por _script_ sobre o código atual; os de "Depois" são a **projeção** obtida a partir do código-alvo definido no plano de refatoração.

| Indicador (soma dos 15 métodos)              | Antes (medido) | Depois (projetado)  | Variação |
| -------------------------------------------- | -------------- | ------------------- | -------- |
| Linhas de código                             | 363            | ≈ 232               | ≈ −36%   |
| `prepareStatement` escritos manualmente      | 15             | 0                   | −100%    |
| `setXxx(n, ...)`                             | 11             | 0                   | −100%    |
| `rs.getXxx(...)`                             | 18             | 0                   | −100%    |
| Concatenações de valor na SQL                | 8              | 0                   | −100%    |
| `printStackTrace`                            | 10             | 0                   | −100%    |
| Chamadas `.close()` manuais                  | 15             | 0                   | −100%    |
| Chamadas explícitas de `Logger`              | 0              | 10                  | +10      |
| Métodos com auditoria de tempo, origem e erro | 0 de 15       | 15 de 15            | +100 p.p. |

No arquivo como um todo, a projeção é de redução de 416 para 401 chamadas manuais a `prepareStatement` e de 26 para 16 ocorrências de `printStackTrace`.

**Fonte:** Elaborada pela autora (2026).

### 3.8 Resultados e Dados Consolidados do Projeto

Os dados já levantados são:

- 21.381 linhas de código no `ProjectDAO` analisado;
- 409 métodos, dos quais 402 acessam o banco de dados;
- 19,6% de linhas idênticas;
- 54,4% de linhas repetidas;
- 416 chamadas a `PreparedStatement`;
- 156 chamadas que utilizam métodos `setXxx()`;
- aproximadamente 43% de métodos potencialmente vulneráveis a SQL Injection.

Até o momento, foram identificadas a necessidade de padronização da camada de persistência, a redução de código repetitivo e a necessidade de ampliar os mecanismos de segurança. Os resultados quantitativos da aplicação do SEOS ainda deverão ser registrados nesta seção.

#### Resultados da metrificação

**Aplicabilidade comprovada em código real.** O SEOS é utilizado em produção no módulo de recebimento fiscal e portaria do portal corporativo, com 9 DAOs, 11 DTOs mapeados por `@TableDB`/`@ColumnDB`, 82 pontos de montagem de SQL e 62 execuções, todos operando sobre a `Connection` fornecida pelo próprio chamador (`SankhyaConnector.conectarSankhya()`), o que confirma a premissa de preservação do controle de conexão e de transação. Nesses DAOs não há nenhuma chamada manual a `prepareStatement` nem a `rs.getXxx()`, e todos registram falhas por meio de `Logger`, ao passo que, no restante do projeto, 41 dos 69 arquivos com JDBC manual (59%) não possuem nenhuma chamada de _log_.

**Redução de volume.** Os DAOs escritos com o SEOS apresentam média aproximada de 27 linhas por método, contra aproximadamente 68 linhas por método no `SankhyaDAO` (21.412 linhas), incluindo nos primeiros o texto SQL e as regras de negócio. Operações que no padrão manual exigem cerca de 30 linhas (como a busca de parceiro por CPF/CNPJ) correspondem, com o `SQLBuilder`, a uma a três linhas, e o mesmo DTO parcial atende às variações de `INSERT` e `UPDATE` pela estratégia _Anti-Null_, eliminando a duplicação de métodos por combinação de campos.

**Segurança.** Todos os valores informados pelos chamadores trafegam como parâmetros de `PreparedStatement` aplicados centralmente por `SQLResult.setParameters`, e a classe valida previamente a correspondência entre placeholders (`?`) e parâmetros. O `buildUpdate` rejeita a operação sem cláusula `WHERE`. A mitigação nos métodos legados do `SankhyaDAO` será demonstrada pela refatoração de amostra (Quadro B da Seção 3.7), na qual as concatenações de valor na SQL são substituídas por parâmetros.

**Projeção da refatoração de amostra.** Aplicando o SEOS a 15 métodos do `SankhyaDAO`, projeta-se a redução de 363 para aproximadamente 232 linhas (cerca de 36%) e a eliminação total, nessa amostra, dos `prepareStatement` manuais (15), dos `setXxx` (11), dos `rs.getXxx` (18), dos `printStackTrace` (10) e das 8 concatenações de valor na instrução SQL, distribuídas em 7 métodos, que passam a ser parâmetros vinculados. Em contrapartida, todos os 15 métodos passam a contar com a auditoria automática de tempo, origem e erro do `SQLResult`, e 10 deles registram a falha por meio de `Logger`, contra nenhum registro de _log_ na versão original.

**Limitações da medição.** (i) Os DAOs que utilizam o SEOS foram criados já com a biblioteca, sem versão anterior em JDBC manual no histórico do repositório, razão pela qual a comparação direta antes/depois foi feita sobre a amostra refatorada do `SankhyaDAO`; (ii) os valores de linhas por método são indicativos, pois os métodos comparados não têm funcionalidade idêntica; (iii) a cópia do SEOS incorporada ao portal está em versão anterior à descrita na Seção 3.3, sem as classes `SQLCall`, `OrmLogger` e `OrmConfig`, de modo que a auditoria nesse repositório utiliza `java.util.logging` diretamente; (iv) a contagem de exposição a _SQL Injection_ por critérios do OWASP _Top_ 10 (Anexo A) e a varredura SAST dependem de execução própria e não foram consolidadas nesta seção.

## 4. Conclusão

Esta seção será preenchida após a conclusão das avaliações. Deverá retomar o problema identificado, apresentar os resultados do SEOS, discutir suas limitações e indicar possíveis trabalhos futuros (como suporte a herança na leitura de _annotations_ dos DTOs, marcação explícita para permitir gravação de `NULL` em atualizações parciais fora do padrão _Anti-Null_, suporte a lotes heterogêneos no `buildInsertBatch` e construtores auxiliares para consultas com _joins_).

## 5. Referências

As referências bibliográficas deverão ser incluídas após a seleção das fontes utilizadas no referencial teórico e na análise de segurança.

## 6. Anexos

### Anexo A — Questionário baseado no OWASP Top 10

O questionário deverá ser incluído nesta seção e suas respostas deverão ser relacionadas ao quadro de respostas apresentado na seção 3.7.
