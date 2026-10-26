# Relatório de Projeto

## SEOS (Software Endurance Orchestration System): redução de boilerplate, auditoria e segurança cibernética na camada de persistência

**Autora:** Maria Antônia Santos de Lima  
**Instituição:** Fundação Edson Queiroz — Universidade de Fortaleza (UNIFOR)  
**Curso:** Pós-Graduação em Engenharia de Software com foco em DevOps  
**Local:** Fortaleza-CE  
**Ano:** 2025

## 1. Introdução

Atualmente, na empresa onde trabalho, os métodos de consulta utilizados em nossos projetos são muito verbosos. Para realizar uma consulta, é necessário escrevê-la manualmente, armazená-la em uma variável, mapear os parâmetros do `PreparedStatement` e, logo em seguida, mapear o `ResultSet`, campo a campo.

Em alguns casos, as consultas não possuem parâmetros. Em outros, os valores são concatenados diretamente na instrução SQL, o que cria uma brecha significativa para SQL Injection. Além disso, existem diversas versões de operações como `INSERT`, `UPDATE` e consultas para uma mesma tabela. Isso ocorre porque determinadas implementações precisam de estruturas específicas que não podem ser modificadas, resultando na duplicação de operações semelhantes.

Nesse contexto, foi desenvolvido o SEOS (Software Endurance Orchestration System), um microframework ORM criado para atender às necessidades específicas da empresa. O framework mapeia tabelas, campos e _sequences_ por meio de _annotations_ e constrói estruturas DML e DQL com base nesses mapeamentos. Dessa forma, busca tornar o acesso ao banco de dados mais padronizado e seguro, além de retornar as classes com os valores já mapeados e prontos para serem utilizados pelas implementações.

## 1.1 Objetivo

### Objetivo geral

Evoluir o microframework SEOS com base em práticas consolidadas de mercado e estruturar seu processo de desenvolvimento sob a cultura DevSecOps, a fim de demonstrar os ganhos de otimização, padronização, auditoria e segurança na camada de persistência.

### Objetivos específicos

- Implementar um mecanismo automatizado de geração de instruções DML/DQL (`SQLBuilder`) e de mapeamento objeto-relacional de `ResultSet` baseado em _annotations_ e _reflection_;
- Mitigar riscos de _SQL Injection_ na camada de persistência por meio da parametrização obrigatória de consultas no `PreparedStatement` e validação estrutural das operações;
- Incorporar recursos de auditoria e observabilidade para rastreamento de erros, parâmetros e tempo de execução das consultas ao banco de dados;
- Estruturar uma pipeline de Integração Contínua e DevSecOps com execução automatizada de testes, análise de cobertura e qualidade (JaCoCo e SonarCloud), análise estática de segurança (SAST com Semgrep), notificações via ChatOps e Entrega Contínua configurando o GitHub Package com versionamento semântico da biblioteca;
- Avaliar a aderência arquitetural do SEOS frente ao cenário corporativo estudado e em comparação com soluções consolidadas de mercado, como Hibernate/JPA;
- Mensurar os ganhos quantitativos e qualitativos da adoção do SEOS em relação à abordagem tradicional (JDBC manual), considerando redução de linhas de código, taxa de duplicação e exposição a vulnerabilidades baseadas no OWASP Top 10.

## 1.2 Justificativa

Ao criar uma implementação para atender a uma demanda e perceber que não poderia reutilizar o mesmo método de persistência no banco de dados, a primeira coisa que pensei foi: “Por que criar mais uma versão de um `UPDATE` com as mesmas condições?”. Foi nesse momento que percebi que a falta de padronização, a baixa qualidade do código e os problemas de segurança nas consultas eram fatores que precisavam ser tratados.

Por meio dessa necessidade nasceu o SEOS, criado para ser uma solução capaz de reduzir o trabalho manual e a duplicação de métodos, facilitar a manutenção dos projetos empresariais e diminuir os riscos de SQL Injection, sem retirar da equipe o controle sobre as consultas, conexões e transações de persistência.

## 1.3 Problema

- 21.381 linhas de código;
- 409 métodos, dos quais 402 acessam o banco de dados;
- 19,6% de linhas idênticas;
- 54,4% de linhas repetidas;
- Entre 416 chamadas a `PreparedStatement`, apenas 156 utilizam métodos `setXxx()`;
- Aproximadamente 43% dos métodos são potencialmente vulneráveis a SQL Injection.

Esses são os números mapeados juntamente com a equipe de Arquitetura da empresa a partir de um único arquivo _Repository_ de um único projeto utilizado no contexto empresarial. Os alertas sobre o desenvolvimento inadequado de métodos de persistência nos projetos, que possibilitam a ocorrência de SQL Injection, já haviam sido repassados; contudo, devido ao tamanho da equipe — atualmente composta por 30 pessoas — e ao fato de os projetos existentes serem catalogados como legados, tornou-se difícil corrigir e padronizar o desenvolvimento manualmente, e é exatamente nesse cenário que o SEOS se encaixa.

Embora ele atue como uma biblioteca Java distribuída sob versionamento semântico — não sendo uma aplicação final em si —, ele opera no núcleo da camada de persistência dos módulos corporativos que sustentam a operação da empresa. No exemplo citado anteriormente, a presença de aproximadamente 43% de métodos suscetíveis a SQL Injection representa um risco operacional crítico ao banco de dados. A exploração de uma vulnerabilidade dessa natureza, ou a indisponibilidade do sistema devido à corrupção de tabelas essenciais ao fluxo operacional, exigiria um tempo estimado de recuperação de `[INFORMAR_DEPOIS]` para conter e restaurar o ambiente. Nesse intervalo, estima-se um prejuízo direto de aproximadamente `[R$ X]` devido à paralisação do faturamento e das vendas, além de prejuízos indiretos severos, como perda de clientes e desgaste reputacional. Diante disso, a adoção do SEOS, aliada à esteira DevSecOps e ao controle estrito de versões, atua diretamente na mitigação preventiva desses impactos financeiros e operacionais.

## 2. Referencial Teórico

O projeto utiliza conceitos relacionados a:

- JDBC e acesso a bancos de dados relacionais;
- Mapeamento objeto-relacional (ORM);
- SQL Injection e consultas parametrizadas;
- OWASP Top 10;
- DevSecOps;
- Integração e entrega contínuas;
- SAST (Static Application Security Testing);
- auditoria e observabilidade de aplicações.

Esta seção deverá ser complementada com a fundamentação dos conceitos e com pelo menos cinco artigos atuais sobre ataques cibernéticos, segurança de aplicações e práticas DevSecOps.

## 3. Caso de Uso

### 3.1 Contexto do Trabalho

O SEOS foi desenvolvido a partir de uma necessidade identificada em projetos reais da empresa onde trabalho. O cenário utiliza aplicações Java e operações de persistência baseadas em JDBC. A solução foi criada para atender ao contexto existente, no qual as consultas, as conexões e as transações precisam permanecer sob controle da aplicação.

O projeto deverá complementar este contexto com as seguintes informações:

- segmento e porte da empresa;
- quantidade aproximada de funcionários;
- banco de dados e plataforma utilizados;
- quantidade de projetos, módulos e consultas existentes;
- setores analisados, como o setor financeiro;
- quantidade de pessoas envolvidas;
- volume e período de desenvolvimento.

### 3.2 Situação Atual

Na situação anterior ao SEOS, as consultas eram construídas manualmente. O desenvolvedor precisava criar a instrução SQL, concatenar ou mapear os parâmetros, executar o `PreparedStatement`, percorrer o `ResultSet` e atribuir cada valor ao objeto correspondente.

De forma mais ilustrativa:

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

Além do volume de código, a concatenação direta de valores na consulta dificulta a padronização e pode permitir SQL Injection. A existência de várias implementações semelhantes também aumenta o custo de manutenção.

### 3.3 Solução Proposta

A partir do conhecimento sobre ORM e Spring Boot, foi desenvolvido o SEOS para atender ao contexto da empresa. O framework utiliza _annotations_ para mapear tabelas, campos e _sequences_, constrói estruturas DML e DQL e retorna as classes com os valores já mapeados.

Com o SEOS, a consulta pode ser representada de forma mais objetiva:

```java
SQLResult sql = SQLBuilder.select(UserTestDTO.class)
        .where("STATUS = ? AND USER_AGE >= ?", status, idadeMinima)
        .build();
List<UserTestDTO> usuarios = sql.executeQuery(conn, UserTestDTO.class);
```

### 3.4 Exemplo de uso end-to-end

```java
public class UserRepository implements AutoCloseable {

    private final Connection connection;

    public UserRepository() throws Exception {
        this.connection = ConnectionPool.getInstance()
            .getConnection(UserRepository.class);
    }

    public void insert(UserDTO user) throws Exception {
        SQLBuilder.buildInsert(user).executeUpdate(connection);
    }

    public UserDTO findById(Integer id) throws Exception {
        return SQLBuilder
            .buildSelect(UserDTO.class, "ID = ?", id)
            .executeQuerySingle(connection, UserDTO.class);
    }

    public boolean updateStatus(Integer id, String status) throws Exception {
        UserDTO patch = UserDTO.builder()
            .status(status)
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

try (UserRepository userRepository = new UserRepository()) {
    UserDTO newUser = UserDTO.builder()
        .name("Maria")
        .age(30)
        .status("ACTIVE")
        .build();

    userRepository.insert(newUser);
    UserDTO existingUser = userRepository.findById(1);

    if (existingUser != null) {
        userRepository.updateStatus(existingUser.getId(), "INACTIVE");
    }
}
```

### 3.5 Vantagens e desvantagens

Entre os benefícios esperados estão a redução de boilerplate, a padronização das operações de persistência, a reutilização de código, o mapeamento automático dos resultados e a redução da exposição a SQL Injection.

Como limitações, o SEOS mantém parte do controle explícito no código da aplicação. Relacionamentos, _joins_, conexões e transações não são gerenciados automaticamente pelo framework. Essas características devem ser avaliadas em conjunto com as necessidades do ambiente empresarial.

### 3.6 Comparação com Hibernate/JPA

Antes de começar a implementar o SEOS no ambiente da empresa, foi realizada uma comparação com o Hibernate, uma das soluções ORM mais utilizadas no ecossistema Java.

| Requisito do projeto         | Hibernate / JPA                                                                         | SEOS híbrido JDBC                                                                  | Adequação ao cenário atual                                       |
| ---------------------------- | --------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| Integração com conexões ERP  | Requer configurar e integrar o contexto de persistência ao ciclo da plataforma.         | Recebe diretamente a `Connection` já fornecida pela aplicação.                     | O SEOS preserva o modelo operacional existente.                  |
| Previsibilidade das escritas | O resultado depende do estado da entidade, do contexto e das configurações de `flush`.  | Cada chamada representa um comando visível, sem sincronização implícita posterior. | O SEOS facilita a rastreabilidade nos projetos.                  |
| SQL Oracle específico        | Pode ser usado por SQL nativo, paralelamente ao modelo de entidades.                    | É parte natural da API e permanece concentrado na camada de `Repository`.          | O SEOS evita manter duas formas de consulta para o mesmo módulo. |
| CRUD de tabelas customizadas | Exige mapear entidades e configurar o ambiente JPA.                                     | Exige DTO anotado e _builders_ diretos.                                            | O SEOS atende com menor superfície de configuração.              |
| Atualizações parciais        | Possui alternativas como entidade gerenciada, consultas específicas e `@DynamicUpdate`. | A estratégia Anti-Null transforma o DTO parcial diretamente no `SET`.              | O SEOS expressa de forma simples o padrão recorrente do projeto. |
| Controle transacional        | Integra-se a JTA, Spring e outros gerenciadores quando disponíveis.                     | Usa `commit`, `rollback` e conexão definidos pelo chamador.                        | O SEOS participa do limite transacional adotado pelo projeto.    |
| Consultas e projeções        | Oferece JPQL, Criteria, SQL nativo e projeções.                                         | Oferece DTO, resultado único, escalar, mapa, dicionário e SQL explícito.           | A API atual cobre os formatos utilizados pelos DAOs.             |
| Relacionamentos de domínio   | Automatiza associações, cascatas e estratégias de carregamento.                         | Mantém _joins_ e composição de resultados explícitos.                              | O escopo atual privilegia operações objetivas.                   |
| Dependências e inicialização | Requer o _runtime_ e a configuração do provedor ORM.                                    | Utiliza JDBC e poucas classes utilitárias já incorporadas ao projeto.              | O SEOS acrescenta baixo _overhead_ à implantação.                |
| Diagnóstico                  | Possui ecossistema de logs e métricas, sujeito à configuração adotada.                  | Concentra SQL e parâmetros e audita erros, origem e consultas lentas.              | A observabilidade atende ao diagnóstico necessário.              |

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

### 3.8 Resultado

Até o momento, foram identificadas a necessidade de padronização da camada de persistência, a redução de código repetitivo e a necessidade de ampliar os mecanismos de segurança. Os resultados quantitativos da aplicação do SEOS ainda deverão ser registrados nesta seção.

### 3.9 Recomendações — plano de aplicação DevSecOps

O projeto será estruturado com base nas seguintes práticas:

1. **Integração Contínua (Continuous Integration):** o workflow [`devsecops.yml`](.github/workflows/devsecops.yml) é executado em _push_ na branch `main` ou quando uma _pull request_ é aberta para essa branch. O GitHub Actions configura o JDK 11, executa `mvn clean verify`, compila a aplicação e executa os testes automatizados com JUnit 5 e Mockito. O JaCoCo coleta os dados dos testes e gera o relatório de cobertura.

![alt text](/docs/image/imagePipelineCI.png)

![alt text](/docs/image/imagePipelineTestes.png)

2. **Qualidade Contínua (Continuous Quality):** a pipeline envia os resultados dos testes e da cobertura para o SonarCloud. A ferramenta analisa cobertura, duplicação, _bugs_, vulnerabilidades e _code smells_. O _Quality Gate_ verifica os critérios definidos e pode reprovar a alteração antes da integração da _pull request_.

![Imagem da plataforma do SonarQube Cloud](/docs/image/imageSonarQube.png)

3. **Segurança Antecipada (Shift-Left Security):** após a compilação, os testes e a análise de qualidade, o Semgrep realiza uma análise estática de segurança (SAST) no código Java do SEOS. O resultado é gerado em formato SARIF e enviado ao GitHub Security. As versões das dependências utilizadas na etapa são fixadas por _hash_ e a instalação de pacotes binários desconhecidos é restringida por `--only-binary :all:`.

![alt text](/docs/image/imagePipelineSAST.png)

4. **Observabilidade e ChatOps:** a pipeline possui um mecanismo de notificação assíncrona que aciona um alerta via _Webhook_ integrado ao Microsoft Teams quando uma etapa anterior falha. A mensagem informa a etapa, o projeto, o motivo e um link para os logs da execução.

![alt text](/docs/image/imageMensagemTeams.png)

5. **Entrega Contínua (Continuous Delivery):** esta etapa deverá ser detalhada com o fluxo de publicação, implantação e validação adotado pelo projeto.

![alt text](/docs/image/imagePipelineCD.png)

![alt text](/docs/image/imagePackageMaven.png)

### 3.10 Dados do Projeto

Os dados já levantados são:

- 21.381 linhas de código no `ProjectDAO` analisado;
- 409 métodos, dos quais 402 acessam o banco de dados;
- 19,6% de linhas idênticas;
- 54,4% de linhas repetidas;
- 416 chamadas a `PreparedStatement`;
- 156 chamadas que utilizam métodos `setXxx()`;
- aproximadamente 43% de métodos potencialmente vulneráveis a SQL Injection.

Ainda devem ser informados a quantidade de pessoas envolvidas, o volume total de trabalho, o período de desenvolvimento e a quantidade de módulos ou consultas analisados.

## 4. Conclusão

Esta seção será preenchida após a conclusão das avaliações. Deverá retomar o problema identificado, apresentar os resultados do SEOS, discutir suas limitações e indicar possíveis trabalhos futuros.

## 5. Referências

As referências bibliográficas deverão ser incluídas após a seleção das fontes utilizadas no referencial teórico e na análise de segurança.

## 6. Anexos

### Anexo A — Questionário baseado no OWASP Top 10

O questionário deverá ser incluído nesta seção e suas respostas deverão ser relacionadas ao quadro de respostas apresentado na seção 3.7.
