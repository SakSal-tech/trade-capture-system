package com.technicalchallenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeController.class)
public class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeService tradeService;

    @MockBean
    private TradeMapper tradeMapper;

    private ObjectMapper objectMapper;
    private TradeDTO tradeDTO;
    private Trade trade;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create a sample TradeDTO for testing
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(1001L);
        tradeDTO.setVersion(1);
        tradeDTO.setTradeDate(LocalDate.now());
        tradeDTO.setTradeStartDate(LocalDate.now().plusDays(2));
        tradeDTO.setTradeMaturityDate(LocalDate.now().plusYears(5));
        tradeDTO.setTradeStatus("LIVE");
        tradeDTO.setBookName("TestBook");
        tradeDTO.setCounterpartyName("TestCounterparty");
        tradeDTO.setTraderUserName("TestTrader");
        tradeDTO.setInputterUserName("TestInputter");
        tradeDTO.setUtiCode("UTI123456789");

        // Create a sample Trade entity for testing
        trade = new Trade();
        trade.setId(1L);
        //// Setting the tradeId ensures the entity matches the expected value in the
        //// response JSON, so test assertions for tradeId will pass.
        trade.setTradeId(1001L);
        trade.setVersion(1);
        trade.setTradeDate(LocalDate.now());
        trade.setTradeStartDate(LocalDate.now().plusDays(2));
        trade.setTradeMaturityDate(LocalDate.now().plusYears(5));

        // Set up default mappings
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);
        when(tradeMapper.toEntity(any(TradeDTO.class))).thenReturn(trade);
    }

    @Test
    void testGetAllTrades() throws Exception {
        // Given
        List<Trade> trades = List.of(trade);

        when(tradeService.getAllTrades()).thenReturn(trades);

        // When/Then
        mockMvc.perform(get("/api/trades")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tradeId", is(1001)))
                .andExpect(jsonPath("$[0].bookName", is("TestBook")))
                .andExpect(jsonPath("$[0].counterpartyName", is("TestCounterparty")));

        verify(tradeService).getAllTrades();
    }

    @Test
    void testGetTradeById() throws Exception {
        // Given
        when(tradeService.getTradeById(1001L)).thenReturn(Optional.of(trade));

        // When/Then
        mockMvc.perform(get("/api/trades/1001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId", is(1001)))
                .andExpect(jsonPath("$.bookName", is("TestBook")))
                .andExpect(jsonPath("$.counterpartyName", is("TestCounterparty")));

        verify(tradeService).getTradeById(1001L);
    }

    @Test
    void testGetTradeByIdNotFound() throws Exception {
        // Given
        when(tradeService.getTradeById(9999L)).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/trades/9999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(tradeService).getTradeById(9999L);
    }

    @Test
    void testCreateTrade() throws Exception {
        // Given
        when(tradeService.saveTrade(any(Trade.class), any(TradeDTO.class))).thenReturn(trade);
        doNothing().when(tradeService).populateReferenceDataByName(any(Trade.class), any(TradeDTO.class));

        // When/Then
        mockMvc.perform(post("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeDTO)))
                // FIX: expect 201 CREATED (not 200)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tradeId", is(1001)));

        verify(tradeService).saveTrade(any(Trade.class), any(TradeDTO.class));
        verify(tradeService).populateReferenceDataByName(any(Trade.class), any(TradeDTO.class));
    }

    @Test
    void testUpdateTrade() throws Exception {
        // Given
        Long tradeId = 1001L;
        tradeDTO.setTradeId(tradeId);
        trade.setTradeId(tradeId); // FIX: ensure entity tradeId is set too
        when(tradeService.amendTrade(any(Long.class), any(TradeDTO.class))).thenReturn(trade);

        // When/Then
        mockMvc.perform(put("/api/trades/{id}", tradeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId", is(1001)));

        verify(tradeService).amendTrade(eq(tradeId), any(TradeDTO.class));
    }

    @Test
    void testUpdateTradeIdMismatch() throws Exception {
        // Given
        Long pathId = 1001L;
        tradeDTO.setTradeId(2002L); // Different from path ID

        // When/Then
        mockMvc.perform(put("/api/trades/{id}", pathId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeDTO)))
                .andExpect(status().isBadRequest())
                // FIX: matches controller's error message
                .andExpect(content().string("Trade ID in path must match Trade ID in request body"));

        verify(tradeService, never()).saveTrade(any(Trade.class), any(TradeDTO.class));
    }

    @Test
    void testDeleteTrade() throws Exception {
        // Given
        doNothing().when(tradeService).deleteTrade(1001L);

        // When/Then
        mockMvc.perform(delete("/api/trades/1001")
                .contentType(MediaType.APPLICATION_JSON))
                // FIX: expect 204 instead of 200
                .andExpect(status().isNoContent());

        verify(tradeService).deleteTrade(1001L);
    }

    @SpringBootTest
    @AutoConfigureMockMvc
    class TradeControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void searchTrades_shouldReturnMatchingTrades() throws Exception {
            // Arrange: Insert test trades into DB

            // Act & Assert. simulates HTTP requests
            mockMvc.perform(get("/api/trades/search")
                    /*
                     * // adds a query parameter to the request, so the final URL is
                     * // /api/trades/search?counterparty=BigBank
                     * // index 0 is used: it always refers
                     * // to the first item in a
                     * // JSON"$[0].counterpartyName" points
                     * // to
                     * // the counterpartyName field of the
                     * // first object in the returned JSON
                     * // array.
                     */
                    .param("counterparty", "BigBank"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].counterpartyName").value("BigBank"));

        }

        @Test
        void filterTrades_shouldReturnPaginatedResults() throws Exception {
            // Arrange: Insert test trades into DB

            // Act & Assert
            mockMvc.perform(get("/api/trades/filter")
                    .param("counterparty", "BigBank")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].counterpartyName").value("BigBank"));
        }

        @Test
        void searchTradesRsql_shouldReturnMatchingTrades() throws Exception {
            // Arrange: Insert test trades into DB

            // Act & Assert
            mockMvc.perform(get("/api/trades/rsql")
                    .param("query", "counterparty.name==BigBank"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].counterpartyName").value("BigBank"));
        }
    }
}
