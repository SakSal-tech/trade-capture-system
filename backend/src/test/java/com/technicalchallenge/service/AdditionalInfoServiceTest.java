package com.technicalchallenge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.technicalchallenge.dto.AdditionalInfoDTO;
import com.technicalchallenge.dto.AdditionalInfoRequestDTO;
import com.technicalchallenge.mapper.AdditionalInfoMapper;
import com.technicalchallenge.model.AdditionalInfo;
import com.technicalchallenge.repository.AdditionalInfoAuditRepository;
import com.technicalchallenge.repository.AdditionalInfoRepository;
import com.technicalchallenge.validation.TradeValidationEngine;
import com.technicalchallenge.validation.TradeValidationResult;
import com.technicalchallenge.validation.SettlementInstructionValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AdditionalInfoServiceTest {

    @Mock
    AdditionalInfoRepository additionalInfoRepository;

    @Mock
    AdditionalInfoMapper additionalInfoMapper;

    @Mock
    AdditionalInfoAuditRepository additionalInfoAuditRepository;

    @Mock
    SettlementInstructionValidator settlementInstructionValidator; // constructor requires this param

    @Mock
    TradeValidationEngine tradeValidationEngine;

    @InjectMocks
    AdditionalInfoService additionalInfoService;

    private AdditionalInfoRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new AdditionalInfoRequestDTO();
        request.setEntityType("TRADE");
        request.setEntityId(1L);
    }

    @Test
    @DisplayName("Creating settlement with invalid text should throw IllegalArgumentException")
    void createSettlement_invalid_throws() {
        // Arrange: mark this request as a settlement instruction with an invalid value
        request.setFieldName("SETTLEMENT_INSTRUCTIONS");
        request.setFieldValue("bad; value");

        // The engine should report an error for the provided invalid text
        TradeValidationResult bad = new TradeValidationResult();
        bad.setError("Semicolons are not allowed in settlement instructions.");

        when(tradeValidationEngine.validateSettlementInstructions(anyString())).thenReturn(bad);

        // Act & Assert: expect the service to throw due to validation failure
        // Use an anonymous Executable instead of a lambda to avoid using
        // lambda expressions in tests (keeps the code style explicit).
        Executable exec = new Executable() {
            @Override
            public void execute() throws Throwable {
                additionalInfoService.createAdditionalInfo(request);
            }
        };

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, exec);

        // Verify: error message contains the validation detail and service did not
        // persist anything
        assertTrue(ex.getMessage().contains("Semicolons"));
        verify(tradeValidationEngine).validateSettlementInstructions(anyString());
        verifyNoInteractions(additionalInfoRepository);
    }

    @Test
    @DisplayName("Creating non-settlement additional info should save and return DTO")
    void createNonSettlement_valid_savesAndReturnsDto() {
        // Arrange: a non-settlement field with a valid, long-enough value
        request.setFieldName("NOTE");
        request.setFieldValue("This is a valid non-settlement note that is long enough.");

        // Map the request into an entity and simulate a save
        AdditionalInfo entity = new AdditionalInfo();
        when(additionalInfoMapper.toEntity(request)).thenReturn(entity);
        when(additionalInfoRepository.save(entity)).thenReturn(entity);

        // Simulate mapper returning a DTO for the saved entity
        AdditionalInfoDTO dto = new AdditionalInfoDTO();
        when(additionalInfoMapper.toDto(entity)).thenReturn(dto);

        // Act
        AdditionalInfoDTO result = additionalInfoService.createAdditionalInfo(request);

        // Assert: DTO returned and repository/mapper were invoked as expected
        assertNotNull(result);
        verify(additionalInfoRepository).save(entity);
        verify(additionalInfoMapper).toDto(entity);
    }
}
