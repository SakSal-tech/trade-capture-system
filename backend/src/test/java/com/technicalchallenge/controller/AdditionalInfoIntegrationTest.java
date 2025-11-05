package com.technicalchallenge.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.technicalchallenge.repository.AdditionalInfoAuditRepository;
import com.technicalchallenge.model.AdditionalInfoAudit;

// Added: imports for repositories and entities to support validator dependencies.
// This ensures the test can create reference data only when needed, avoiding reliance on external SQL files.
import com.technicalchallenge.repository.BookRepository;
import com.technicalchallenge.repository.CounterpartyRepository;
import com.technicalchallenge.repository.ApplicationUserRepository;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.ApplicationUser;

import java.util.Map;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

// @SpringBootTest boots the full Spring context (controllers, services, repositories and DB)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AdditionalInfoIntegrationTest {

    // MockMvc is used to perform HTTP requests against controller endpoints
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper serialises request payloads into JSON
    @Autowired
    private ObjectMapper objectMapper;

    // Repository is used to verify persisted state after controller calls.
    // Note: only need the audit repository in this test because the
    // controller endpoint writes an audit record; do not read
    // AdditionalInfoRepository here so it is intentionally omitted.
    @Autowired
    private AdditionalInfoAuditRepository auditRepository;

    // Added: repositories needed to verify or insert minimal reference data
    // without duplicating what data.sql already loads.
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CounterpartyRepository counterpartyRepository;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @BeforeEach
    void setUp() {
        // Changed: previous version inserted records unconditionally, causing
        // DataIntegrityViolation when data.sql already contained the same entries.
        // The new logic checks for existence before inserting, keeping the setup
        // idempotent.

        // Check if a book named "TestBook" exists before inserting to prevent
        // duplicates
        if (bookRepository.findByBookName("TestBook").isEmpty()) {
            Book book = new Book();
            book.setBookName("TestBook");
            book.setActive(true);
            bookRepository.saveAndFlush(book);
        }

        // Check if counterparty "BigBank" exists before inserting to avoid duplicate
        // rows
        if (counterpartyRepository.findByName("BigBank").isEmpty()) {
            Counterparty counterparty = new Counterparty();
            counterparty.setName("BigBank");
            counterparty.setActive(true);
            counterpartyRepository.saveAndFlush(counterparty);
        }

        // Check if user "simon" exists before inserting to prevent unique constraint
        // violations
        if (applicationUserRepository.findByLoginId("simon").isEmpty()) {
            ApplicationUser trader = new ApplicationUser();
            trader.setLoginId("simon");
            trader.setFirstName("Simon");
            trader.setLastName("Trader");
            trader.setActive(true);
            applicationUserRepository.saveAndFlush(trader);
        }
    }

    @Test
    @DisplayName("Controller -> Service -> Repository: settlement instruction audit records must store authenticated username")
    // The controller PUT endpoint requires TRADER or SALES to edit settlement
    // instructions. Use role TRADER in the test so security checks succeed.
    // The trade with tradeId=200001 in the seed data belongs to 'simon'.
    // Use Simon as the authenticated principal so the ownership check in the
    // service permits the operation and the test remains focused on audit
    // behaviour rather than authorization policy.
    @WithMockUser(username = "simon", roles = { "TRADER" })
    /*
     * This integration test boots the application context, calls the
     * AdditionalInfo controller to persist settlement instructions, and
     * then verifies that an audit record exists with the authenticated
     * principal name in the changedBy field.
     */
    void whenPostSettlement_thenAuditSavedWithUsername() throws Exception {

        // Build a minimal payload compatible with the controller's DTO.
        // Using Map.of keeps the test concise and flexible.
        // Use a real trade identifier from test fixtures (see
        // src/main/resources/data.sql).
        // Note: the repository uses the column `trade_id` (not the PK id) when
        // looking up trades, so must use the trade_id value found in the
        // seed data. The row with PK 2000 has trade_id = 200001, therefore we
        // use 200001L here so the controller finds an active trade.
        long tradeId = 200001L;

        // Build payload matching AdditionalInfoRequestDTO expected by the
        // controller. Field name must be exactly SETTLEMENT_INSTRUCTIONS.
        var payload = Map.of(
                "fieldName", "SETTLEMENT_INSTRUCTIONS",
                "fieldValue", "Pay to ABC");

        // Perform PUT to the real controller endpoint. The controller reads
        // the authenticated principal via SecurityContext and passes the
        // username into the service audit record; this is what the test asserts.
        mockMvc.perform(
                put("/api/trades/{id}/settlement-instructions", tradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        // Retrieve audit records and locate the entry created by the
        // controller call. Defensive null-checks guard against any
        // unexpected null elements returned by the repository.

        List<AdditionalInfoAudit> audits = auditRepository.findAll();
        AdditionalInfoAudit audit = null;
        for (AdditionalInfoAudit a : audits) {
            if (a == null) {
                continue; // skip null entries if present
            }
            if ("simon".equals(a.getChangedBy()) && "SETTLEMENT_INSTRUCTIONS".equals(a.getFieldName())) {
                audit = a;
                break; // stop on first match
            }
        }

        // Assert that a matching audit record was found and contains the
        // authenticated principal noted in the audit.
        assertThat(audit).isNotNull();
        Objects.requireNonNull(audit);
        assertThat(audit.getChangedBy()).isEqualTo("simon");
        assertThat(audit.getFieldName()).isEqualTo("SETTLEMENT_INSTRUCTIONS");
    }
}
