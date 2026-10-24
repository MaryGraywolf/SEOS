package com.github.marygraywolf.seos.core;

import com.github.marygraywolf.seos.annotations.ColumnDB;
import com.github.marygraywolf.seos.annotations.TableDB;
import com.github.marygraywolf.seos.config.OrmConfig;
import com.github.marygraywolf.seos.spi.OrmLogger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SQLResultTest {

    @BeforeAll
    static void setUpLogger() {
        // Mock simples para não poluir os logs
        OrmConfig.setLogger(mock(OrmLogger.class));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @TableDB(name = "TB_TESTE")
    static class DtoTeste {
        @ColumnDB(localName = "ID")
        private Long id;
        @ColumnDB(localName = "NOME")
        private String nome;
    }

    @Test
    @DisplayName("Deve validar a correspondência entre os parâmetros informados e os placeholders ?")
    void deveValidarQuantidadeDeParametros() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            new SQLResult("SELECT * FROM TABELA WHERE ID = ? AND NOME = ?", Arrays.asList(1L)); // Faltou 1 parametro
        });
        assertTrue(ex.getMessage().contains("O comando espera 2 parâmetros (?), mas foram fornecidos 1"));
    }

    @Test
    @DisplayName("Deve executar o executeUpdate e retornar a quantidade de linhas afetadas")
    void deveExecutarUpdate() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        when(conn.prepareStatement("UPDATE TABELA SET NOME = ?")).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(3);

        SQLResult sqlResult = new SQLResult("UPDATE TABELA SET NOME = ?", Arrays.asList("Joao"));
        int linhas = sqlResult.executeUpdate(conn);

        assertEquals(3, linhas);
        verify(ps).setObject(1, "Joao");
        verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("Deve executar executeQuery e retornar uma lista de DTOs")
    void deveExecutarQueryRetornandoDto() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject("ID")).thenReturn(10L);
        when(rs.getObject("NOME")).thenReturn("Maria");

        SQLResult sqlResult = new SQLResult("SELECT ID, NOME FROM TB_TESTE WHERE ID = ?", Arrays.asList(10L));
        List<DtoTeste> lista = sqlResult.executeQuery(conn, DtoTeste.class);

        assertEquals(1, lista.size());
        assertEquals(10L, lista.get(0).getId());
        assertEquals("Maria", lista.get(0).getNome());
        verify(ps).setObject(1, 10L);
    }

    @Test
    @DisplayName("Deve executar executeQuerySingle e retornar apenas 1 objeto ou null")
    void deveExecutarQuerySingle() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject("ID")).thenReturn(20L);

        SQLResult sqlResult = new SQLResult("SELECT ID FROM TB_TESTE WHERE ID = ?", Arrays.asList(20L));
        DtoTeste dto = sqlResult.executeQuerySingle(conn, DtoTeste.class);

        assertNotNull(dto);
        assertEquals(20L, dto.getId());

        when(rs.next()).thenReturn(false);
        DtoTeste dtoNulo = sqlResult.executeQuerySingle(conn, DtoTeste.class);
        assertNull(dtoNulo);
    }

    @Test
    @DisplayName("Deve executar executeQueryAsMap e converter nomes de coluna para maiúsculo")
    void deveExecutarQueryAsMap() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(1)).thenReturn("id_cliente");
        when(metaData.getColumnName(2)).thenReturn("NOME_CLIENTE");

        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn(55L);
        when(rs.getObject(2)).thenReturn("Pedro");

        SQLResult sqlResult = new SQLResult("SELECT id_cliente, NOME_CLIENTE FROM X", Arrays.asList());
        List<Map<String, Object>> mapList = sqlResult.executeQueryAsMap(conn);

        assertEquals(1, mapList.size());
        Map<String, Object> map = mapList.get(0);

        assertTrue(map.containsKey("ID_CLIENTE"));
        assertTrue(map.containsKey("NOME_CLIENTE"));
        assertEquals(55L, map.get("ID_CLIENTE"));
        assertEquals("Pedro", map.get("NOME_CLIENTE"));
    }

    @Test
    @DisplayName("Deve executar executeScalar e converter numero corretamente")
    void deveExecutarScalar() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);

        when(rs.getObject(1)).thenReturn(new BigDecimal("99"));

        SQLResult sqlResult = new SQLResult("SELECT COUNT(1) FROM X", Arrays.asList());

        Long valorConvertido = sqlResult.executeScalar(conn, Long.class);
        assertEquals(99L, valorConvertido);

        Integer valorConvertidoInt = sqlResult.executeScalar(conn, Integer.class);
        assertEquals(99, valorConvertidoInt);
    }

    @Test
    @DisplayName("Deve executar executeAsDictionary e mapear 2 colunas")
    void deveExecutarAsDictionary() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);

        when(rs.getObject(1)).thenReturn(1L, 2L);
        when(rs.getObject(2)).thenReturn("Ativo", "Inativo");

        SQLResult sqlResult = new SQLResult("SELECT ID, STATUS FROM X", Arrays.asList());
        Map<Long, String> dict = sqlResult.executeAsDictionary(conn);

        assertEquals(2, dict.size());
        assertEquals("Ativo", dict.get(1L));
        assertEquals("Inativo", dict.get(2L));
    }

    @Test
    @DisplayName("Deve executar um Batch Insert na API")
    void deveExecutarBatchInsert() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch()).thenReturn(new int[]{1, 1});

        List<List<Object>> params = Arrays.asList(
                Arrays.asList(1L, "Joao"),
                Arrays.asList(2L, "Maria")
        );

        SQLResult.Batch batch = new SQLResult.Batch("INSERT INTO X (ID, NOME) VALUES (?, ?)", params);
        int[] resultados = batch.executeBatch(conn);

        assertEquals(2, resultados.length);
        verify(ps).setObject(1, 1L);
        verify(ps).setObject(2, "Joao");
        verify(ps).setObject(1, 2L);
        verify(ps).setObject(2, "Maria");
        verify(ps, times(2)).addBatch();
        verify(ps).executeBatch();
    }

    @Test
    @DisplayName("Deve executar executeQueryAsSingleMap e retornar apenas 1 mapa")
    void deveExecutarQueryAsSingleMap() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnName(1)).thenReturn("NOME");

        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn("Joao");

        SQLResult sqlResult = new SQLResult("SELECT NOME FROM X", Arrays.asList());
        Map<String, Object> map = sqlResult.executeQueryAsSingleMap(conn);

        assertNotNull(map);
        assertEquals("Joao", map.get("NOME"));

        when(rs.next()).thenReturn(false);
        Map<String, Object> mapNulo = sqlResult.executeQueryAsSingleMap(conn);
        assertNull(mapNulo);
    }

    @Test
    @DisplayName("Deve propagar exceções e auditar erro no executeUpdate")
    void devePropagarExcecaoExecuteUpdate() throws Exception {
        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenThrow(new java.sql.SQLException("DB Off"));

        SQLResult sqlResult = new SQLResult("UPDATE X SET Y = ?", Arrays.asList("A"));
        Exception ex = assertThrows(Exception.class, () -> sqlResult.executeUpdate(conn));
        assertEquals("DB Off", ex.getMessage());
    }

    @Test
    @DisplayName("Deve aceitar null na lista de params no construtor")
    void deveAceitarParamsNull() {
        SQLResult result = new SQLResult("SELECT * FROM X", null);
        assertNull(result.getParams());
    }

    enum StatusEnum {ATIVO, INATIVO}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @TableDB(name = "TB_TIPOS")
    static class DtoTipos {
        @ColumnDB(localName = "CAMPO_ENUM")
        private StatusEnum status;
        @ColumnDB(localName = "CAMPO_DOUBLE")
        private Double campoDouble;
        @ColumnDB(localName = "CAMPO_FLOAT")
        private Float campoFloat;
        @ColumnDB(localName = "CAMPO_CLOB")
        private String campoClob;
        @ColumnDB(localName = "CAMPO_TS_TO_STRING")
        private String campoTsToString;
        @ColumnDB(localName = "CAMPO_DATE")
        private java.util.Date campoDate;
        @ColumnDB(localName = "CAMPO_DATE_SQL")
        private java.util.Date campoDateSql;
    }

    @Test
    @DisplayName("Deve converter tipos avançados (Enum, Double, Float, Clob, Timestamp, Date) no mapearResultSet")
    void deveConverterTiposAvancadosNoResultSet() throws Exception {
        ResultSet rsMock = mock(ResultSet.class);
        when(rsMock.next()).thenReturn(true, false);

        // Mockando os valores vindos do banco
        when(rsMock.getObject("CAMPO_ENUM")).thenReturn("ATIVO");
        when(rsMock.getObject("CAMPO_DOUBLE")).thenReturn(new BigDecimal("99.99"));
        when(rsMock.getObject("CAMPO_FLOAT")).thenReturn(new BigDecimal("10.5"));

        java.sql.Clob clobMock = mock(java.sql.Clob.class);
        when(clobMock.length()).thenReturn(5L);
        when(clobMock.getSubString(1L, 5)).thenReturn("TEXTO");
        when(rsMock.getObject("CAMPO_CLOB")).thenReturn(clobMock);

        java.sql.Timestamp ts = java.sql.Timestamp.valueOf("2026-09-24 10:00:00");
        when(rsMock.getObject("CAMPO_TS_TO_STRING")).thenReturn(ts);

        java.util.Date data = new java.util.Date();
        when(rsMock.getObject("CAMPO_DATE")).thenReturn(data);

        java.sql.Date dataSql = new java.sql.Date(data.getTime());
        when(rsMock.getObject("CAMPO_DATE_SQL")).thenReturn(dataSql);

        SQLResult sqlResult = new SQLResult("SELECT *", Arrays.asList());
        List<DtoTipos> lista = sqlResult.mapearResultSet(rsMock, DtoTipos.class);

        assertEquals(1, lista.size());
        DtoTipos dto = lista.get(0);

        assertEquals(StatusEnum.ATIVO, dto.getStatus());
        assertEquals(99.99, dto.getCampoDouble());
        assertEquals(10.5f, dto.getCampoFloat());
        assertEquals("TEXTO", dto.getCampoClob());
        assertTrue(dto.getCampoTsToString().contains("24/09/2026"));
        assertNotNull(dto.getCampoDate());
        assertNotNull(dto.getCampoDateSql());
    }

    @Test
    @DisplayName("Deve capturar SQLException no mapearResultSet e converter para RuntimeException")
    void deveLancarRuntimeExceptionNoMapear() throws Exception {
        ResultSet rsMock = mock(ResultSet.class);
        when(rsMock.next()).thenReturn(true);
        when(rsMock.getObject(anyString())).thenThrow(new java.sql.SQLException("Coluna não existe"));

        SQLResult sqlResult = new SQLResult("SELECT *", Arrays.asList());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            sqlResult.mapearResultSet(rsMock, DtoTipos.class);
        });

        assertTrue(ex.getMessage().contains("Erro ao mapear os setters"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @TableDB(name = "TB_PK")
    static class DtoComPk {
        @ColumnDB(localName = "ID_PK", isPrimaryKey = true)
        private Long id;
        @ColumnDB(localName = "NOME")
        private String nome;
        private String campoSemAnotacao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @TableDB(name = "TB_SEM_PK")
    static class DtoSemPk {
        @ColumnDB(localName = "NOME")
        private String nome;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @TableDB(name = "TB_TIPOS_EXTRAS")
    static class DtoTiposExtras {
        @ColumnDB(localName = "CAMPO_ENUM")
        private StatusEnum status;
        @ColumnDB(localName = "CAMPO_INT")
        private Integer campoInt;
        @ColumnDB(localName = "CAMPO_TS_DIRETO")
        private java.sql.Timestamp campoTsDireto;
        @ColumnDB(localName = "CAMPO_DATE_TO_STRING")
        private String campoDateToString;
        @ColumnDB(localName = "CAMPO_NULO")
        private String campoNulo;
        private String ignoradoSemColumnDB;
    }

    @Test
    @DisplayName("Deve executar executeAndReturnId com sucesso e preencher o ID no DTO")
    void deveExecutarAndReturnIdComSucesso() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        when(conn.prepareStatement(eq("INSERT INTO TB_PK (NOME) VALUES (?)"), eq(new String[]{"ID_PK"}))).thenReturn(ps);
        when(ps.getGeneratedKeys()).thenReturn(rsKeys);
        when(rsKeys.next()).thenReturn(true);
        when(rsKeys.getObject(1)).thenReturn(new BigDecimal("42"));

        DtoComPk dto = new DtoComPk(null, "Teste", "Ignorado");
        SQLResult sqlResult = new SQLResult("INSERT INTO TB_PK (NOME) VALUES (?)", Arrays.asList("Teste"));

        Long idRetornado = sqlResult.executeAndReturnId(conn, dto);

        assertEquals(42L, idRetornado);
        assertEquals(42L, dto.getId());
        verify(ps).setObject(1, "Teste");
        verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException no executeAndReturnId quando DTO for nulo")
    void deveFalharExecuteAndReturnIdComDtoNulo() {
        Connection conn = mock(Connection.class);
        SQLResult sqlResult = new SQLResult("INSERT INTO TB_PK (NOME) VALUES (?)", Arrays.asList("Teste"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            sqlResult.executeAndReturnId(conn, null);
        });

        assertTrue(ex.getMessage().contains("O DTO de destino do ID gerado nao pode ser nulo"));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException no executeAndReturnId quando DTO não tiver chave primária")
    void deveFalharExecuteAndReturnIdSemChavePrimaria() {
        Connection conn = mock(Connection.class);
        DtoSemPk dto = new DtoSemPk("Teste");
        SQLResult sqlResult = new SQLResult("INSERT INTO TB_SEM_PK (NOME) VALUES (?)", Arrays.asList("Teste"));

        assertThrows(IllegalStateException.class, () -> {
            sqlResult.executeAndReturnId(conn, dto);
        });
    }

    @Test
    @DisplayName("Deve lançar SQLException no executeAndReturnId quando getGeneratedKeys não retornar linhas")
    void deveFalharExecuteAndReturnIdSemGeneratedKeys() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        when(conn.prepareStatement(anyString(), any(String[].class))).thenReturn(ps);
        when(ps.getGeneratedKeys()).thenReturn(rsKeys);
        when(rsKeys.next()).thenReturn(false);

        DtoComPk dto = new DtoComPk(null, "Teste", null);
        SQLResult sqlResult = new SQLResult("INSERT INTO TB_PK (NOME) VALUES (?)", Arrays.asList("Teste"));

        java.sql.SQLException ex = assertThrows(java.sql.SQLException.class, () -> {
            sqlResult.executeAndReturnId(conn, dto);
        });

        assertTrue(ex.getMessage().contains("nao retornou a coluna ID_PK"));
    }

    @Test
    @DisplayName("Deve lançar SQLException no executeAndReturnId quando a chave retornada for nula")
    void deveFalharExecuteAndReturnIdComChaveNula() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        when(conn.prepareStatement(anyString(), any(String[].class))).thenReturn(ps);
        when(ps.getGeneratedKeys()).thenReturn(rsKeys);
        when(rsKeys.next()).thenReturn(true);
        when(rsKeys.getObject(1)).thenReturn(null);

        DtoComPk dto = new DtoComPk(null, "Teste", null);
        SQLResult sqlResult = new SQLResult("INSERT INTO TB_PK (NOME) VALUES (?)", Arrays.asList("Teste"));

        java.sql.SQLException ex = assertThrows(java.sql.SQLException.class, () -> {
            sqlResult.executeAndReturnId(conn, dto);
        });

        assertTrue(ex.getMessage().contains("a coluna ID_PK retornou nula"));
    }

    @Test
    @DisplayName("Deve testar todas as conversões do executeScalar (BigDecimal, Number, String, null e sem linhas)")
    void deveTestarVariacoesExecuteScalar() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        SQLResult sqlResult = new SQLResult("SELECT VAL FROM X", null);

        // 1. Quando rs.next() é false -> retorna null
        when(rs.next()).thenReturn(false);
        assertNull(sqlResult.executeScalar(conn, Long.class));

        // 2. Quando rs.getObject(1) é null -> retorna null
        when(rs.next()).thenReturn(true);
        when(rs.getObject(1)).thenReturn(null);
        assertNull(sqlResult.executeScalar(conn, Long.class));

        // 3. Quando tipoEsperado é Object.class ou já é instância do tipo esperado
        when(rs.getObject(1)).thenReturn("Texto Direto");
        assertEquals("Texto Direto", sqlResult.executeScalar(conn, Object.class));
        assertEquals("Texto Direto", sqlResult.executeScalar(conn, String.class));
        assertEquals("Texto Direto", sqlResult.executeScalar(conn, null));

        // 4. Conversões a partir de BigDecimal (Long, Integer, Double, primitivos e String)
        when(rs.getObject(1)).thenReturn(new BigDecimal("123.45"));
        assertEquals(123L, sqlResult.executeScalar(conn, long.class));
        assertEquals(123, sqlResult.executeScalar(conn, int.class));
        assertEquals(123.45, sqlResult.executeScalar(conn, Double.class));
        assertEquals(123.45, sqlResult.executeScalar(conn, double.class));
        assertEquals("123.45", sqlResult.executeScalar(conn, String.class));

        // 5. Conversões a partir de Number genérico (Integer -> Long, Short, Double, Float, primitivos, BigDecimal, String)
        when(rs.getObject(1)).thenReturn(Integer.valueOf(77));
        assertEquals(77L, sqlResult.executeScalar(conn, Long.class));
        assertEquals(77L, sqlResult.executeScalar(conn, long.class));
        assertEquals(Short.valueOf((short) 77), sqlResult.executeScalar(conn, Short.class));
        assertEquals(Short.valueOf((short) 77), sqlResult.executeScalar(conn, short.class));
        assertEquals(77.0, sqlResult.executeScalar(conn, Double.class));
        assertEquals(77.0, sqlResult.executeScalar(conn, double.class));
        assertEquals(77.0f, sqlResult.executeScalar(conn, Float.class));
        assertEquals(77.0f, sqlResult.executeScalar(conn, float.class));
        assertEquals(new BigDecimal("77"), sqlResult.executeScalar(conn, BigDecimal.class));
        assertEquals("77", sqlResult.executeScalar(conn, String.class));

        // 6. Conversões a partir de Number genérico (Long -> Integer e int)
        when(rs.getObject(1)).thenReturn(Long.valueOf(88L));
        assertEquals(88, sqlResult.executeScalar(conn, Integer.class));
        assertEquals(88, sqlResult.executeScalar(conn, int.class));

        // 7. Conversões a partir de String (" 250 " -> Long, Integer, primitivos, BigDecimal e fallback)
        when(rs.getObject(1)).thenReturn(" 250 ");
        assertEquals(250L, sqlResult.executeScalar(conn, Long.class));
        assertEquals(250L, sqlResult.executeScalar(conn, long.class));
        assertEquals(250, sqlResult.executeScalar(conn, Integer.class));
        assertEquals(250, sqlResult.executeScalar(conn, int.class));
        assertEquals(new BigDecimal("250"), sqlResult.executeScalar(conn, BigDecimal.class));
        Object valorSemConversao = sqlResult.executeScalar(conn, Boolean.class);
        assertEquals(" 250 ", valorSemConversao);
    }

    @Test
    @DisplayName("Deve ignorar chaves nulas no executeAsDictionary")
    void deveIgnorarChaveNulaNoDictionary() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getObject(1)).thenReturn(null, 10L);
        when(rs.getObject(2)).thenReturn("Ignorado", "Valido");

        SQLResult sqlResult = new SQLResult("SELECT K, V FROM X", null);
        Map<Long, String> dict = sqlResult.executeAsDictionary(conn);

        assertEquals(1, dict.size());
        assertEquals("Valido", dict.get(10L));
    }

    @Test
    @DisplayName("Deve mapear Enum inválido como null, Integer, Timestamp direto, Date para String e campos nulos")
    void deveMapearCasosEspeciaisNoResultSet() throws Exception {
        ResultSet rsMock = mock(ResultSet.class);
        when(rsMock.next()).thenReturn(true, false);

        when(rsMock.getObject("CAMPO_ENUM")).thenReturn("VALOR_INEXISTENTE");
        when(rsMock.getObject("CAMPO_INT")).thenReturn(new BigDecimal("42"));

        java.sql.Timestamp ts = java.sql.Timestamp.valueOf("2026-10-21 15:30:00");
        when(rsMock.getObject("CAMPO_TS_DIRETO")).thenReturn(ts);

        java.sql.Date sqlDate = java.sql.Date.valueOf("2026-10-21");
        when(rsMock.getObject("CAMPO_DATE_TO_STRING")).thenReturn(sqlDate);

        when(rsMock.getObject("CAMPO_NULO")).thenReturn(null);

        SQLResult sqlResult = new SQLResult("SELECT *", null);
        List<DtoTiposExtras> lista = sqlResult.mapearResultSet(rsMock, DtoTiposExtras.class);

        assertEquals(1, lista.size());
        DtoTiposExtras dto = lista.get(0);

        assertNull(dto.getStatus());
        assertEquals(42, dto.getCampoInt());
        assertEquals(ts, dto.getCampoTsDireto());
        assertEquals("21/10/2026", dto.getCampoDateToString());
        assertNull(dto.getCampoNulo());
        assertNull(dto.getIgnoradoSemColumnDB());
    }

    @Test
    @DisplayName("Deve propagar exceções em executeQuery, executeQueryAsMap, executeScalar e executeAsDictionary")
    void devePropagarExcecoesNosMetodosDeConsulta() throws Exception {
        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenThrow(new java.sql.SQLException("Falha na conexao"));

        SQLResult sqlResult = new SQLResult("SELECT * FROM X", Arrays.asList());

        assertThrows(java.sql.SQLException.class, () -> sqlResult.executeQuery(conn, DtoTeste.class));
        assertThrows(java.sql.SQLException.class, () -> sqlResult.executeQueryAsMap(conn));
        assertThrows(java.sql.SQLException.class, () -> sqlResult.executeScalar(conn, Long.class));
        assertThrows(java.sql.SQLException.class, () -> sqlResult.executeAsDictionary(conn));
    }

    @Test
    @DisplayName("Deve validar getters de SQLResult e SQLResult.Batch")
    void deveValidarGetters() {
        List<Object> params = Arrays.asList(1L);
        SQLResult res = new SQLResult("SELECT * FROM X WHERE ID = ?", params);
        assertEquals("SELECT * FROM X WHERE ID = ?", res.getSql());
        assertEquals(params, res.getParams());

        List<List<Object>> batchParams = Arrays.asList(Arrays.asList(1L));
        SQLResult.Batch batch = new SQLResult.Batch("INSERT INTO X VALUES (?)", batchParams);
        assertEquals("INSERT INTO X VALUES (?)", batch.getSql());
        assertEquals(batchParams, batch.getBatchParams());
    }
}
