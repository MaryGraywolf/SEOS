Relatório de Projeto
1 -Introdução
1.1 Objetivo
1.2 Justificativa
1.3 Problema: 
Ampliar as funcionalidades do ORM no que tange a parte de segurança;
Validar se ele está seguro contra ataque cibernético;
Se houvesse um ataque em quanto tempo a empresa consegue voltar a operar?
Prejuízo Direto -> 2 dias sem vender (R$ 6.666 reais de prejuízo)
Prejuízo Indireto -> Perda de cliente
Aplicar com base no OWASP TOP 10 um questionário (Bater uma foto de como está hoje)
Reduzir a probabilidade em x porcento quantidade de Injections
Estudar ou Pesquisar, 15% das empresas que sofrem ataque é com SQL INJECTION, estamos fazendo isso para reduzir 15% a chance;
Ampliar a segurança contra sql injections;

2 - Referencial Teórico
Cyber Ataques
Sugestão você ler 5 artigos atuais sobre o tema
3 - Caso de Uso
3.1 Contexto do Trabalho
Materializar o cenários;
Empresa do Segmento Têxtil;
X funcionários;
 Porte Grande;
Quantidade de consultas existentes;
Categorizar (Quantidade de linhas)
Por setor (Financeiro)
3.2 - Situação Atual
3.3 - Vantagens e desvantagens

3.4  - Benefícios Esperados
x
x
t
x
3.5  em aberto
3.5 - Quadro de Respostas (Antes e Depois)
3.6 - Resultado
Concluirmos x, y z;
3.7 - Recomendações em formato de Plano ou Cronograma
Propor, implementar e avaliar um conjunto de práticas de SecOps/DevSecOps aplicadas ao processo de desenvolvimento e operação baseado em ORM de uma empresa do setor têxtil.
3.8 Dados dos Projeto
Quantidade de pessoas
Volume de trabalho
4 - Conclusão
5 - Referências
6 - Anexos (Questionário)

pontos de melhoria:

Incluir um diagrama de classes ou componentes da biblioteca e um diagrama de sequência de uma execução, do DTO até o banco e o registro do log (aqui a ideia é as pessoas terem noção do tempo de um trabalho destes)	3 e 4	até +0,45
Escrever uma seção sobre as limitações da avaliação (amostra, testes só com simulações, um único depoimento, desempenho não medido) e discutir a brecha da cláusula where, com a forma de mitigar	4 e 6	até +0,40
Criar uma tabela de requisitos funcionais e não funcionais, com uma meta de desempenho, ligando cada requisito à sua evidência	3	até +0,30
Colocar datas em cada fase do cronograma e dizer se a Fase 5 foi executada	2	até +0,25
Recalcular a Tabela 6 com a média real dos 69 arquivos, explicar como os 15 métodos foram escolhidos e trocar "≈232" pelo número exato	5	até +0,20
Discutir a LGPD nos logs (o que pode ser registrado e por quanto tempo) e estimar o custo de um incidente de SQL Injection	8	até +0,20
Deixar o texto em voz impessoal, incluir um resumo, trocar os artigos da Alura por fontes primárias e usar o OWASP Top 10:2025 até +0,15

Divergências a corrigir após a inclusão das limitações da avaliação:
1. Confirmar e documentar a participação da equipe de Arquitetura na refatoração dos 15 métodos (linhas 42, 843 e 856); se não houve, retirar a frase da seção de limitações.
2. Ajustar "o SQLBuilder gera apenas marcadores ?" (controle do A03) para valores; a cláusula WHERE é fornecida pela aplicação.
3. Qualificar a matriz do A03 ("Mitigado") com nota sobre a cláusula de filtro e trocar "parametrização obrigatória das consultas" por "dos valores" no objetivo específico.