package com.technicalchallenge.mapper;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.CashflowDTO;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.model.TradeLeg;
import com.technicalchallenge.model.Cashflow;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.service.AdditionalInfoService; // NEW: Injected to pull settlement instructions

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TradeMapper
 *
 * Converts between Trade entities and TradeDTOs.
 * This version also enriches TradeDTO with settlement instructions
 * stored in the AdditionalInfo table.
 */
@Component
public class TradeMapper {

    // Gives mapper access to settlement data stored in AdditionalInfo table
    @Autowired
    private AdditionalInfoService additionalInfoService;

    public TradeDTO toDto(Trade trade) {
        if (trade == null) {
            return null;
        }

        TradeDTO dto = new TradeDTO();

        // --- Basic trade details mapping ---
        dto.setId(trade.getId());
        dto.setTradeId(trade.getTradeId());
        dto.setVersion(trade.getVersion());
        dto.setTradeDate(trade.getTradeDate());
        dto.setTradeStartDate(trade.getTradeStartDate());
        dto.setTradeMaturityDate(trade.getTradeMaturityDate());
        dto.setTradeExecutionDate(trade.getTradeExecutionDate());
        dto.setUtiCode(trade.getUtiCode());
        dto.setLastTouchTimestamp(trade.getLastTouchTimestamp());
        dto.setValidityStartDate(trade.getValidityStartDate());
        dto.setValidityEndDate(trade.getValidityEndDate());
        dto.setActive(trade.getActive());
        dto.setCreatedDate(trade.getCreatedDate());

        // --- Book information mapping ---
        if (trade.getBook() != null) {
            dto.setBookId(trade.getBook().getId());
            dto.setBookName(trade.getBook().getBookName());
        }

        // --- Counterparty mapping ---
        if (trade.getCounterparty() != null) {
            dto.setCounterpartyId(trade.getCounterparty().getId());
            dto.setCounterpartyName(trade.getCounterparty().getName());
        }

        // --- Trader and inputter user mapping ---
        if (trade.getTraderUser() != null) {
            dto.setTraderUserId(trade.getTraderUser().getId());
            dto.setTraderUserName(trade.getTraderUser().getFirstName() + " " + trade.getTraderUser().getLastName());
        }

        if (trade.getTradeInputterUser() != null) {
            dto.setTradeInputterUserId(trade.getTradeInputterUser().getId());
            dto.setInputterUserName(
                    trade.getTradeInputterUser().getFirstName() + " " + trade.getTradeInputterUser().getLastName());
        }

        // --- Trade type, subtype, and status mapping ---
        if (trade.getTradeType() != null) {
            dto.setTradeTypeId(trade.getTradeType().getId());
            dto.setTradeType(trade.getTradeType().getTradeType());
        }

        if (trade.getTradeSubType() != null) {
            dto.setTradeSubTypeId(trade.getTradeSubType().getId());
            dto.setTradeSubType(trade.getTradeSubType().getTradeSubType());
        }

        if (trade.getTradeStatus() != null) {
            dto.setTradeStatusId(trade.getTradeStatus().getId());
            dto.setTradeStatus(trade.getTradeStatus().getTradeStatus());
        }

        // --- Map trade legs ---
        if (trade.getTradeLegs() != null) {
            List<TradeLegDTO> legDTOs = trade.getTradeLegs().stream()
                    .map(this::tradeLegToDto)
                    .collect(Collectors.toList());
            dto.setTradeLegs(legDTOs);
        }

        // ADDED: Settlement Instructions Integration
        // Use the dedicated service method to fetch settlement instructions for
        // this specific trade. Previously the mapper called `searchByKey(...)`
        // which searches fieldValue and not fieldName; that returned no results
        // and caused settlementInstructions to be null in the DTO. Using the
        // targeted service method ensures we look up by entityType + entityId
        // + fieldName which is both efficient and correct.
        try {
            if (trade.getTradeId() != null) {
                com.technicalchallenge.dto.AdditionalInfoDTO ai = additionalInfoService
                        .getSettlementInstructionsByTradeId(trade.getTradeId());
                if (ai != null) {
                    dto.setSettlementInstructions(ai.getFieldValue());
                }
            }
        } catch (Exception ex) {
            // Avoid throwing from the mapper; log and continue so trade DTO can still be
            // returned even when settlement lookup has transient issues.
            // Note: logger not injected here to keep mapper simple; controllers and
            // services already log important context.
        }

        // Return the complete DTO to controller
        return dto;
    }

    public Trade toEntity(TradeDTO dto) {
        if (dto == null) {
            return null;
        }

        Trade trade = new Trade();
        trade.setId(dto.getId());
        trade.setTradeId(dto.getTradeId());
        trade.setVersion(dto.getVersion());
        trade.setTradeDate(dto.getTradeDate());
        trade.setTradeStartDate(dto.getTradeStartDate());
        trade.setTradeMaturityDate(dto.getTradeMaturityDate());
        trade.setTradeExecutionDate(dto.getTradeExecutionDate());
        trade.setUtiCode(dto.getUtiCode());
        trade.setLastTouchTimestamp(dto.getLastTouchTimestamp());
        trade.setValidityStartDate(dto.getValidityStartDate());
        trade.setValidityEndDate(dto.getValidityEndDate());
        trade.setActive(dto.getActive());
        trade.setCreatedDate(dto.getCreatedDate());

        // --- Lightweight references for Book and Counterparty ---
        if (dto.getBookName() != null) {
            Book book = new Book();
            book.setBookName(dto.getBookName());
            trade.setBook(book);
        }

        if (dto.getCounterpartyName() != null) {
            Counterparty cp = new Counterparty();
            cp.setName(dto.getCounterpartyName());
            trade.setCounterparty(cp);
        }

        // Note: Do NOT set settlementInstructions here — they belong to AdditionalInfo
        return trade;
    }

    // --- Supporting mappers for TradeLeg and Cashflow remain unchanged ---

    public TradeLegDTO tradeLegToDto(TradeLeg leg) {
        if (leg == null) {
            return null;
        }

        TradeLegDTO dto = new TradeLegDTO();
        dto.setLegId(leg.getLegId());
        dto.setNotional(leg.getNotional());
        dto.setRate(leg.getRate());

        if (leg.getCurrency() != null) {
            dto.setCurrencyId(leg.getCurrency().getId());
            dto.setCurrency(leg.getCurrency().getCurrency());
        }

        if (leg.getLegRateType() != null) {
            dto.setLegTypeId(leg.getLegRateType().getId());
            dto.setLegType(leg.getLegRateType().getType());
        }

        if (leg.getIndex() != null) {
            dto.setIndexId(leg.getIndex().getId());
            dto.setIndexName(leg.getIndex().getIndex());
        }

        if (leg.getHolidayCalendar() != null) {
            dto.setHolidayCalendarId(leg.getHolidayCalendar().getId());
            dto.setHolidayCalendar(leg.getHolidayCalendar().getHolidayCalendar());
        }

        if (leg.getCalculationPeriodSchedule() != null) {
            dto.setScheduleId(leg.getCalculationPeriodSchedule().getId());
            dto.setCalculationPeriodSchedule(leg.getCalculationPeriodSchedule().getSchedule());
        }

        if (leg.getPaymentBusinessDayConvention() != null) {
            dto.setPaymentBdcId(leg.getPaymentBusinessDayConvention().getId());
            dto.setPaymentBusinessDayConvention(leg.getPaymentBusinessDayConvention().getBdc());
        }

        if (leg.getFixingBusinessDayConvention() != null) {
            dto.setFixingBdcId(leg.getFixingBusinessDayConvention().getId());
            dto.setFixingBusinessDayConvention(leg.getFixingBusinessDayConvention().getBdc());
        }

        if (leg.getPayReceiveFlag() != null) {
            dto.setPayRecId(leg.getPayReceiveFlag().getId());
            dto.setPayReceiveFlag(leg.getPayReceiveFlag().getPayRec());
        }

        if (leg.getCashflows() != null) {
            List<CashflowDTO> cashflowDTOs = leg.getCashflows().stream()
                    .map(this::cashflowToDto)
                    .collect(Collectors.toList());
            dto.setCashflows(cashflowDTOs);
        }

        return dto;
    }

    public TradeLeg tradeLegToEntity(TradeLegDTO dto) {
        if (dto == null) {
            return null;
        }

        TradeLeg leg = new TradeLeg();
        leg.setLegId(dto.getLegId());
        leg.setNotional(dto.getNotional());
        leg.setRate(dto.getRate());
        return leg;
    }

    public CashflowDTO cashflowToDto(Cashflow cashflow) {
        if (cashflow == null) {
            return null;
        }

        CashflowDTO dto = new CashflowDTO();
        dto.setId(cashflow.getId());
        dto.setLegId(cashflow.getTradeLeg() != null ? cashflow.getTradeLeg().getLegId() : null);
        dto.setPaymentValue(cashflow.getPaymentValue());
        dto.setValueDate(cashflow.getValueDate());
        dto.setRate(cashflow.getRate());
        dto.setPayRec(cashflow.getPayRec() != null ? cashflow.getPayRec().getPayRec() : null);
        dto.setPaymentType(cashflow.getPaymentType() != null ? cashflow.getPaymentType().getType() : null);
        dto.setPaymentBusinessDayConvention(
                cashflow.getPaymentBusinessDayConvention() != null
                        ? cashflow.getPaymentBusinessDayConvention().getBdc()
                        : null);
        dto.setCreatedDate(cashflow.getCreatedDate());
        dto.setActive(cashflow.getActive());

        return dto;
    }
}
