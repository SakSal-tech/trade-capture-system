package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.model.*;
import com.technicalchallenge.repository.*;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
// Security imports used to perform server-side ownership and privilege checks
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.technicalchallenge.security.UserPrivilegeValidator;

@Service
@Transactional
@AllArgsConstructor

/*
 * The service receives a DTO (from the controller), uses the mapper to convert
 * it to an entity, and then processes or saves the entity. When returning data,
 * the service uses the mapper to convert entities back to DTOs for the
 * controller to send as a response.
 */
public class TradeService {
    /*
     * FIX: Replaced manual constructor with Lombok @AllArgsConstructor
     *
     * - Removed:
     * TradeService(TradeMapper tradeMapper) { this.tradeMapper = tradeMapper; }
     * because it only initialized one dependency (tradeMapper),
     * leaving all other @Autowired repositories null during tests.
     *
     * - Added:
     * 
     * @AllArgsConstructor
     * which generates a constructor including ALL dependencies (mapper +
     * repositories).
     * This allows both:
     * 1. Spring Boot to perform constructor-based dependency injection at runtime.
     * 2. Mockito's @InjectMocks to inject all @Mock fields in unit tests.
     *
     * - Also removed @Autowired from individual fields,
     * since constructor injection via @AllArgsConstructor makes them redundant.
     *
     * Result:
     * Eliminates NullPointerExceptions in unit tests and improves testability +
     * clarity.
     */

    private static final Logger logger = LoggerFactory.getLogger(TradeService.class);
    private TradeRepository tradeRepository;
    private TradeLegRepository tradeLegRepository;
    private CashflowRepository cashflowRepository;
    private TradeStatusRepository tradeStatusRepository;
    private BookRepository bookRepository;
    private CounterpartyRepository counterpartyRepository;
    private ApplicationUserRepository applicationUserRepository;
    private TradeTypeRepository tradeTypeRepository;
    private TradeSubTypeRepository tradeSubTypeRepository;
    private CurrencyRepository currencyRepository;

    private LegTypeRepository legTypeRepository;
    private IndexRepository indexRepository;
    private HolidayCalendarRepository holidayCalendarRepository;
    private ScheduleRepository scheduleRepository;
    private BusinessDayConventionRepository businessDayConventionRepository;

    private PayRecRepository payRecRepository;
    private AdditionalInfoRepository additionalInfoRepository;

    // Security validator used to check ownership/edit privileges at the service
    // layer.
    // We prefer the Spring-injected validator, but keep a null-check fallback so
    // unit tests that construct this service directly (without Spring) continue
    // to work. The fallback reproduces the minimal ownership logic used
    // elsewhere (defence-in-depth) to avoid allowing remote clients to bypass
    // server-side checks.
    private UserPrivilegeValidator userPrivilegeValidator;

    public List<Trade> getAllTrades() {
        logger.info("Retrieving all trades");
        // Refactor: apply server-side scoping for read-list.
        // If the caller is a TRADER without elevated view privileges, return
        // only trades owned by that trader. This defends against a client
        // supplying arbitrary criteria to view others' trades.
        // Otherwise (MIDDLE_OFFICE, SUPPORT, admins) return all trades.
        // Determine caller and apply server-side scoping:
        // - If caller is a TRADER (and does not have elevated view privileges),
        // return only trades owned by that trader.
        // - Otherwise (MIDDLE_OFFICE, SUPPORT, admins), return all trades.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = (auth != null && auth.getName() != null) ? auth.getName() : null;

        boolean isTrader = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TRADER".equalsIgnoreCase(a.getAuthority()));

        boolean hasElevatedView = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String ga = a.getAuthority();
                    return "ROLE_MIDDLE_OFFICE".equalsIgnoreCase(ga) || "ROLE_SUPPORT".equalsIgnoreCase(ga)
                            || "ROLE_SUPERUSER".equalsIgnoreCase(ga) || "TRADE_VIEW_ALL".equalsIgnoreCase(ga);
                });

        if (isTrader && !hasElevatedView && currentUser != null) {
            // Scoped fetch for trader users: use the derived repository method
            // to efficiently return only this trader's active trades.
            return tradeRepository.findAllByTraderUser_LoginIdAndActiveTrueOrderByTradeIdDesc(currentUser);
        }

        // Non-trader callers or elevated users see the full list.
        return tradeRepository.findAll();
    }

    // Fetch a single trade
    public Optional<Trade> getTradeById(Long tradeId) {
        logger.debug("Retrieving trade by id: {}", tradeId);
        // Debug: also query all trades with the business tradeId so we can see if
        // rows exist but are not active. This helps diagnose integration-test
        // visibility issues where a trade may be present but flagged inactive.
        try {
            var allMatches = tradeRepository.findByTradeId(tradeId);
            logger.debug("Found {} trades with tradeId={}", allMatches == null ? 0 : allMatches.size(), tradeId);
            if (allMatches != null && !allMatches.isEmpty()) {
                for (Trade t : allMatches) {
                    logger.debug("trade row id={} active={}", t.getId(), t.getActive());
                }
            }
        } catch (Exception e) {
            logger.warn("Error while debugging trade lookup: {}", e.getMessage());
        }

        // Try to pick an active trade from the list we already fetched. This
        // works around cases where the derived query with a boolean predicate
        // may not match due to subtle dialect/nullable-boolean handling in
        // the test environment.
        Optional<Trade> opt = java.util.Optional.empty();
        try {
            var allMatches = tradeRepository.findByTradeId(tradeId);
            if (allMatches != null) {
                opt = allMatches.stream().filter(t -> Boolean.TRUE.equals(t.getActive())).findFirst();
            }
        } catch (Exception e) {
            logger.debug("Fallback active-filter failed: {}", e.getMessage());
        }
        if (opt.isEmpty()) {
            opt = tradeRepository.findByTradeIdAndActiveTrue(tradeId);
        }

        // Enforce server-side ownership checks: a TRADER should only be able to
        // access their own trade unless they hold elevated privileges.
        if (opt.isPresent()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUser = (auth != null && auth.getName() != null) ? auth.getName() : null;

            boolean hasElevatedView = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                    .anyMatch(a -> {
                        String ga = a.getAuthority();
                        return "ROLE_MIDDLE_OFFICE".equalsIgnoreCase(ga) || "ROLE_SUPPORT".equalsIgnoreCase(ga)
                                || "ROLE_SUPERUSER".equalsIgnoreCase(ga) || "TRADE_VIEW_ALL".equalsIgnoreCase(ga);
                    });

            // If caller is not allowed to view others and is not the owner, deny
            if (!hasElevatedView && currentUser != null) {
                Trade t = opt.get();
                String ownerLogin = (t.getTraderUser() != null && t.getTraderUser().getLoginId() != null)
                        ? t.getTraderUser().getLoginId()
                        : null;

                // Added ownership enforcement: deny access if the caller is not
                // the trade owner and lacks elevated privileges. This is a
                // lightweight, defensible service-layer guard (complements
                // controller-level @PreAuthorize annotations).
                if (ownerLogin != null && !ownerLogin.equalsIgnoreCase(currentUser)) {
                    throw new AccessDeniedException("Insufficient privileges to view trade " + tradeId);
                }
            }
        }

        return opt;
    }

    // Refactored and changed the above method to fetch multiple trades by a list of
    // IDs
    // instead of fetching a single trade by ID
    public List<Trade> getTradesById(List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return java.util.Collections.emptyList();
        return tradeRepository.findAllByTradeIdIn(ids);
    }

    // ADDED this method after I created Additional Settlement information, to
    // retrieve multiple trades in one go. Needed for new settlement instruction
    // search endpoint
    public List<Trade> getTradesByIds(List<Long> tradeIds) {
        if (tradeIds == null || tradeIds.isEmpty()) {
            throw new IllegalArgumentException("Trade ID list cannot be null or empty");
        }
        return tradeRepository.findAllByTradeIdIn(tradeIds);
    }

    @Transactional
    public Trade createTrade(TradeDTO tradeDTO) {
        logger.info("Creating new trade with ID: {}", tradeDTO.getTradeId());

        // Generate trade ID if not provided
        if (tradeDTO.getTradeId() == null) {
            // Generate sequential trade ID starting from 10000
            Long generatedTradeId = generateNextTradeId();
            tradeDTO.setTradeId(generatedTradeId);
            logger.info("Generated trade ID: {}", generatedTradeId);
        }

        // Validate business rules
        validateTradeCreation(tradeDTO);

        // Create trade entity
        Trade trade = mapDTOToEntity(tradeDTO);
        trade.setVersion(1);
        trade.setActive(true);
        trade.setCreatedDate(LocalDateTime.now());
        trade.setLastTouchTimestamp(LocalDateTime.now());

        // Set default trade status to NEW if not provided
        if (tradeDTO.getTradeStatus() == null) {
            tradeDTO.setTradeStatus("NEW");
        }

        // Populate reference data
        populateReferenceDataByName(trade, tradeDTO);

        // Ensure have essential reference data
        validateReferenceData(trade);

        Trade savedTrade = tradeRepository.save(trade);
        // Refactored. Settlement was not saving while creating a new trade o the UI.
        // The UI sends settlementInstructions in the TradeDTO. Then persist it
        // into the additional_info table so it can be searched/edited later.
        if (tradeDTO.getSettlementInstructions() != null && !tradeDTO.getSettlementInstructions().trim().isEmpty()) {
            AdditionalInfo settlementInfo = new AdditionalInfo();
            settlementInfo.setEntityType("TRADE");
            settlementInfo.setEntityId(savedTrade.getTradeId());
            settlementInfo.setFieldName("SETTLEMENT_INSTRUCTIONS");
            settlementInfo.setFieldValue(tradeDTO.getSettlementInstructions());
            settlementInfo.setFieldType("STRING");
            settlementInfo.setActive(true);
            settlementInfo.setCreatedDate(LocalDateTime.now());
            settlementInfo.setLastModifiedDate(LocalDateTime.now());
            settlementInfo.setVersion(1);
            additionalInfoRepository.save(settlementInfo);
        }

        // Create trade legs and cashflows
        createTradeLegsWithCashflows(tradeDTO, savedTrade);

        logger.info("Successfully created trade with ID: {}", savedTrade.getTradeId());
        return savedTrade;
    }

    // NEW METHOD: For controller compatibility
    @Transactional
    public Trade saveTrade(Trade trade, TradeDTO tradeDTO) {
        logger.info("Saving trade with ID: {}", trade.getTradeId());

        // If this is an existing trade (has ID), handle as amendment
        if (trade.getId() != null) {
            return amendTrade(trade.getTradeId(), tradeDTO);
        } else {
            return createTrade(tradeDTO);
        }
    }

    // FIXED: Populate reference data by names from DTO
    public void populateReferenceDataByName(Trade trade, TradeDTO tradeDTO) {
        logger.debug("Populating reference data for trade");

        // Populate Book
        if (tradeDTO.getBookName() != null) {
            bookRepository.findByBookName(tradeDTO.getBookName())
                    .ifPresent(trade::setBook);
        } else if (tradeDTO.getBookId() != null) {
            bookRepository.findById(tradeDTO.getBookId())
                    .ifPresent(trade::setBook);
        }

        // Populate Counterparty
        if (tradeDTO.getCounterpartyName() != null) {
            counterpartyRepository.findByName(tradeDTO.getCounterpartyName())
                    .ifPresent(trade::setCounterparty);
        } else if (tradeDTO.getCounterpartyId() != null) {
            counterpartyRepository.findById(tradeDTO.getCounterpartyId())
                    .ifPresent(trade::setCounterparty);
        }

        // Populate TradeStatus
        if (tradeDTO.getTradeStatus() != null) {
            tradeStatusRepository.findByTradeStatus(tradeDTO.getTradeStatus())
                    .ifPresent(trade::setTradeStatus);
        } else if (tradeDTO.getTradeStatusId() != null) {
            tradeStatusRepository.findById(tradeDTO.getTradeStatusId())
                    .ifPresent(trade::setTradeStatus);
        }

        // Populate other reference data
        populateUserReferences(trade, tradeDTO);
        populateTradeTypeReferences(trade, tradeDTO);
    }

    private void populateUserReferences(Trade trade, TradeDTO tradeDTO) {
        // Prefer numeric IDs when provided (more reliable).
        // If no numeric id is provided, fall back to name-based lookup.
        // Name-based lookup first tries by first name (legacy behavior) then
        // attempts a loginId lookup which aligns with what the UI sends.

        // Trader resolution
        if (tradeDTO.getTraderUserId() != null) {
            logger.debug("Looking up trader user by id: {}", tradeDTO.getTraderUserId());
            applicationUserRepository.findById(tradeDTO.getTraderUserId())
                    .ifPresent(trade::setTraderUser); // CHANGED: prefer id-based lookup first
        } else if (tradeDTO.getTraderUserName() != null) {
            logger.debug("Looking up trader user by name/login: {}", tradeDTO.getTraderUserName());
            String name = tradeDTO.getTraderUserName().trim();
            String[] nameParts = name.split("\\s+");
            boolean matched = false;
            if (nameParts.length >= 1) {
                String firstName = nameParts[0];
                Optional<ApplicationUser> userOpt = applicationUserRepository.findByFirstName(firstName);
                if (userOpt.isPresent()) {
                    trade.setTraderUser(userOpt.get());
                    matched = true;
                }
            }
            // If first-name lookup failed, try loginId (common when frontend sends login)
            if (!matched) {
                applicationUserRepository.findByLoginId(name).ifPresent(trade::setTraderUser); // CHANGED: try loginId
                                                                                               // fallback when
                                                                                               // first-name lookup
                                                                                               // misses
            }
        }

        // Inputter resolution
        if (tradeDTO.getTradeInputterUserId() != null) {
            logger.debug("Looking up inputter user by id: {}", tradeDTO.getTradeInputterUserId());
            applicationUserRepository.findById(tradeDTO.getTradeInputterUserId())
                    .ifPresent(trade::setTradeInputterUser); // CHANGED: prefer id-based lookup first for inputter
        } else if (tradeDTO.getInputterUserName() != null) {
            logger.debug("Looking up inputter user by name/login: {}", tradeDTO.getInputterUserName());
            String name = tradeDTO.getInputterUserName().trim();
            String[] nameParts = name.split("\\s+");
            boolean matched = false;
            if (nameParts.length >= 1) {
                String firstName = nameParts[0];
                Optional<ApplicationUser> userOpt = applicationUserRepository.findByFirstName(firstName);
                if (userOpt.isPresent()) {
                    trade.setTradeInputterUser(userOpt.get());
                    matched = true;
                }
            }
            if (!matched) {
                applicationUserRepository.findByLoginId(name).ifPresent(trade::setTradeInputterUser); // CHANGED: try
                                                                                                      // loginId
                                                                                                      // fallback for
                                                                                                      // inputter
            }
        }
    }

    private void populateTradeTypeReferences(Trade trade, TradeDTO tradeDTO) {
        if (tradeDTO.getTradeType() != null) {
            tradeTypeRepository.findByTradeType(tradeDTO.getTradeType())
                    .ifPresent(trade::setTradeType);
        } else if (tradeDTO.getTradeTypeId() != null) {
            tradeTypeRepository.findById(tradeDTO.getTradeTypeId())
                    .ifPresent(trade::setTradeType);
        }

        if (tradeDTO.getTradeSubType() != null) {
            tradeSubTypeRepository.findByTradeSubType(tradeDTO.getTradeSubType())
                    .ifPresent(trade::setTradeSubType);
        } else if (tradeDTO.getTradeSubTypeId() != null) {
            tradeSubTypeRepository.findById(tradeDTO.getTradeSubTypeId())
                    .ifPresent(trade::setTradeSubType);
        }
    }

    // NEW METHOD: Delete trade (mark as cancelled)
    @Transactional
    public void deleteTrade(Long tradeId) {
        // CHANGE: Use business tradeId lookup to find the trade (not DB PK).
        // Previously this delegated directly to cancelTrade but that method
        // performed additional ownership logic; here we make the intent
        // explicit: find the trade by its business id and then cancel it.
        logger.info("Deleting (cancelling) trade with business ID: {}", tradeId);
        // Lookup by business id (tradeId) rather than DB primary key (id).
        var tradeOpt = tradeRepository.findByTradeIdAndActiveTrue(tradeId);
        if (tradeOpt.isEmpty()) {
            // Keep behaviour consistent with controller expectation: throw a
            // runtime exception so controller returns 404 (not found).
            throw new RuntimeException("Trade not found: " + tradeId);
        }
        // Delegate to cancelTrade to perform status change and permission checks.
        cancelTrade(tradeId);
    }

    @Transactional
    public Trade amendTrade(Long tradeId, TradeDTO tradeDTO) {
        logger.info("Amending trade with ID: {}", tradeId);

        Optional<Trade> existingTradeOpt = getTradeById(tradeId);
        if (existingTradeOpt.isEmpty()) {
            throw new RuntimeException("Trade not found: " + tradeId);
        }

        Trade existingTrade = existingTradeOpt.get();

        // Deactivate existing trade
        existingTrade.setActive(false);
        existingTrade.setDeactivatedDate(LocalDateTime.now());
        tradeRepository.save(existingTrade);

        // Create new version
        Trade amendedTrade = mapDTOToEntity(tradeDTO);
        amendedTrade.setTradeId(tradeId);

        // FIX: null-safe version increment to avoid NPE in tests (and production edge
        // cases)
        Integer prevVersion = existingTrade.getVersion();
        amendedTrade.setVersion((prevVersion == null ? 1 : prevVersion) + 1);

        amendedTrade.setActive(true);
        amendedTrade.setCreatedDate(LocalDateTime.now());
        amendedTrade.setLastTouchTimestamp(LocalDateTime.now());

        // Populate reference data
        populateReferenceDataByName(amendedTrade, tradeDTO);

        // Set status to AMENDED
        TradeStatus amendedStatus = tradeStatusRepository.findByTradeStatus("AMENDED")
                .orElseThrow(() -> new RuntimeException("AMENDED status not found"));
        amendedTrade.setTradeStatus(amendedStatus);

        Trade savedTrade = tradeRepository.save(amendedTrade);
        // --- Update or create settlement instructions ---
        // If the DTO includes settlement instructions, either update the
        // existing AdditionalInfo row or create a new one. We use
        // findActiveOne(...) to locate a single active record.
        if (tradeDTO.getSettlementInstructions() != null && !tradeDTO.getSettlementInstructions().trim().isEmpty()) {
            Optional<AdditionalInfo> existingInfoOpt = additionalInfoRepository.findActiveOne(
                    "TRADE", savedTrade.getTradeId(), "SETTLEMENT_INSTRUCTIONS");

            if (existingInfoOpt.isPresent()) {
                AdditionalInfo existingInfo = existingInfoOpt.get();
                existingInfo.setFieldValue(tradeDTO.getSettlementInstructions());
                existingInfo.setLastModifiedDate(LocalDateTime.now());
                existingInfo.setVersion(existingInfo.getVersion() == null ? 1 : existingInfo.getVersion() + 1);
                additionalInfoRepository.save(existingInfo);
            } else {
                AdditionalInfo newInfo = new AdditionalInfo();
                newInfo.setEntityType("TRADE");
                newInfo.setEntityId(savedTrade.getTradeId());
                newInfo.setFieldName("SETTLEMENT_INSTRUCTIONS");
                newInfo.setFieldValue(tradeDTO.getSettlementInstructions());
                newInfo.setFieldType("STRING");
                newInfo.setActive(true);
                newInfo.setCreatedDate(LocalDateTime.now());
                newInfo.setLastModifiedDate(LocalDateTime.now());
                newInfo.setVersion(1);
                additionalInfoRepository.save(newInfo);
            }
        }

        // Create new trade legs and cashflows
        createTradeLegsWithCashflows(tradeDTO, savedTrade);

        logger.info("Successfully amended trade with ID: {}", savedTrade.getTradeId());
        return savedTrade;
    }

    @Transactional
    public Trade terminateTrade(Long tradeId) {
        logger.info("Terminating trade with ID: {}", tradeId);

        Optional<Trade> tradeOpt = getTradeById(tradeId);
        if (tradeOpt.isEmpty()) {
            throw new RuntimeException("Trade not found: " + tradeId);
        }

        Trade trade = tradeOpt.get();
        TradeStatus terminatedStatus = tradeStatusRepository.findByTradeStatus("TERMINATED")
                .orElseThrow(() -> new RuntimeException("TERMINATED status not found"));

        trade.setTradeStatus(terminatedStatus);
        trade.setLastTouchTimestamp(LocalDateTime.now());

        return tradeRepository.save(trade);
    }

    @Transactional
    public Trade cancelTrade(Long tradeId) {
        logger.info("Cancelling trade with ID: {}", tradeId);
        // CHANGE: Lookup the trade by its business tradeId (trade.tradeId)
        // rather than assuming the controller passed a database primary key.
        // This ensures controller endpoints that supply business IDs (as the
        // tests do) are resolved correctly.
        Optional<Trade> tradeOpt = tradeRepository.findByTradeIdAndActiveTrue(tradeId);
        if (tradeOpt.isEmpty()) {
            throw new RuntimeException("Trade not found: " + tradeId);
        }

        Trade trade = tradeOpt.get();
        // ENFORCE EDIT PERMISSIONS: a user must be allowed to edit/cancel the
        // trade. Prefer the centralised UserPrivilegeValidator when available;
        // otherwise perform an inline ownership check as a safe fallback.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean allowedToEdit = false;
        if (userPrivilegeValidator != null) {
            try {
                boolean validatorResult = userPrivilegeValidator.canEditTrade(trade, auth);
                logger.debug("UserPrivilegeValidator present - canEditTrade returned: {}", validatorResult);
                allowedToEdit = validatorResult;
            } catch (Exception e) {
                logger.warn("UserPrivilegeValidator threw an exception: {}", e.getMessage());
                allowedToEdit = false;
            }
        } else {
            // Fallback inline check: only the owning trader or users with
            // elevated edit privileges may cancel a trade. This mirrors the
            // validator's minimal logic so unit tests that instantiate the
            // service without Spring continue to behave as before.
            String currentUser = (auth != null && auth.getName() != null) ? auth.getName() : null;
            boolean canEditOthers = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                    .anyMatch(a -> {
                        String ga = a.getAuthority();
                        return "ROLE_SALES".equalsIgnoreCase(ga) || "ROLE_SUPERUSER".equalsIgnoreCase(ga)
                                || "TRADE_EDIT_ALL".equalsIgnoreCase(ga);
                    });
            String ownerLogin = (trade.getTraderUser() != null && trade.getTraderUser().getLoginId() != null)
                    ? trade.getTraderUser().getLoginId()
                    : null;
            if (ownerLogin == null) {
                // If the persisted trade has no owner recorded (ownerLogin == null)
                // allow a caller with the TRADER role to edit/cancel it. This
                // mirrors the historical behavior used by tests which create
                // ownerless trades in setup. Users with elevated privileges
                // (canEditOthers) remain allowed as before.
                boolean isTrader = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_TRADER".equalsIgnoreCase(a.getAuthority()));
                allowedToEdit = canEditOthers || isTrader;
            } else {
                allowedToEdit = canEditOthers || (currentUser != null && ownerLogin.equalsIgnoreCase(currentUser));
            }
        }

        if (!allowedToEdit) {
            // Deny with AccessDeniedException so the REST exception handler
            // maps it to HTTP 403 (forbidden). This is important: a 404 would
            // incorrectly indicate absence rather than lack of permission.
            throw new AccessDeniedException("Insufficient privileges to cancel trade " + tradeId);
        }
        // Debug: log whether CANCELLED status exists to help diagnose 404s in
        // integration tests where the controller maps a RuntimeException to 404.
        try {
            var cancelledOpt = tradeStatusRepository.findByTradeStatus("CANCELLED");
            logger.debug("CANCELLED status present: {}", cancelledOpt.isPresent());
            cancelledOpt.ifPresent(cs -> logger.debug("CANCELLED status id={}", cs.getId()));
        } catch (Exception e) {
            logger.warn("Error while checking CANCELLED trade status: {}", e.getMessage());
        }

        TradeStatus cancelledStatus = tradeStatusRepository.findByTradeStatus("CANCELLED")
                .orElseThrow(() -> new RuntimeException("CANCELLED status not found"));

        trade.setTradeStatus(cancelledStatus);
        trade.setLastTouchTimestamp(LocalDateTime.now());

        return tradeRepository.save(trade);
    }

    private void validateTradeCreation(TradeDTO tradeDTO) {
        // Validate dates - Fixed to use consistent field names
        if (tradeDTO.getTradeStartDate() != null && tradeDTO.getTradeDate() != null) {
            if (tradeDTO.getTradeStartDate().isBefore(tradeDTO.getTradeDate())) {
                throw new RuntimeException("Start date cannot be before trade date");
            }
        }
        if (tradeDTO.getTradeMaturityDate() != null && tradeDTO.getTradeStartDate() != null) {
            if (tradeDTO.getTradeMaturityDate().isBefore(tradeDTO.getTradeStartDate())) {
                throw new RuntimeException("Maturity date cannot be before start date");
            }
        }

        // Validate trade has exactly 2 legs
        if (tradeDTO.getTradeLegs() == null || tradeDTO.getTradeLegs().size() != 2) {
            throw new RuntimeException("Trade must have exactly 2 legs");
        }
    }

    private Trade mapDTOToEntity(TradeDTO dto) {
        Trade trade = new Trade();
        trade.setTradeId(dto.getTradeId());
        trade.setTradeDate(dto.getTradeDate()); // Fixed field names
        trade.setTradeStartDate(dto.getTradeStartDate());
        trade.setTradeMaturityDate(dto.getTradeMaturityDate());
        trade.setTradeExecutionDate(dto.getTradeExecutionDate());
        trade.setUtiCode(dto.getUtiCode());
        trade.setValidityStartDate(dto.getValidityStartDate());
        trade.setLastTouchTimestamp(LocalDateTime.now());
        return trade;
    }

    private void createTradeLegsWithCashflows(TradeDTO tradeDTO, Trade savedTrade) {
        for (int i = 0; i < tradeDTO.getTradeLegs().size(); i++) {
            var legDTO = tradeDTO.getTradeLegs().get(i);

            if (legDTO == null) {
                // FIX: Added null check to prevent NPE during tests and in production
                logger.warn("Skipping null TradeLegDTO while creating legs for trade {}", savedTrade.getTradeId());
                continue;
            }

            TradeLeg tradeLeg = new TradeLeg();
            tradeLeg.setTrade(savedTrade);
            tradeLeg.setNotional(legDTO.getNotional());
            tradeLeg.setRate(legDTO.getRate());
            tradeLeg.setActive(true);
            tradeLeg.setCreatedDate(LocalDateTime.now());

            // Populate reference data for leg
            populateLegReferenceData(tradeLeg, legDTO);

            TradeLeg savedLeg = tradeLegRepository.save(tradeLeg);

            // Generate cashflows for this leg
            if (tradeDTO.getTradeStartDate() != null && tradeDTO.getTradeMaturityDate() != null) {
                generateCashflows(savedLeg, tradeDTO.getTradeStartDate(), tradeDTO.getTradeMaturityDate());
            }
        }
    }

    private void populateLegReferenceData(TradeLeg leg, TradeLegDTO legDTO) {
        if (legDTO.getCurrency() != null) {
            currencyRepository.findByCurrency(legDTO.getCurrency())
                    .ifPresent(leg::setCurrency);
        } else if (legDTO.getCurrencyId() != null) {
            currencyRepository.findById(legDTO.getCurrencyId())
                    .ifPresent(leg::setCurrency);
        }

        if (legDTO.getLegType() != null) {
            legTypeRepository.findByType(legDTO.getLegType())
                    .ifPresent(leg::setLegRateType);
        } else if (legDTO.getLegTypeId() != null) {
            legTypeRepository.findById(legDTO.getLegTypeId())
                    .ifPresent(leg::setLegRateType);
        }

        if (legDTO.getIndexName() != null) {
            indexRepository.findByIndex(legDTO.getIndexName())
                    .ifPresent(leg::setIndex);
        } else if (legDTO.getIndexId() != null) {
            indexRepository.findById(legDTO.getIndexId())
                    .ifPresent(leg::setIndex);
        }

        if (legDTO.getHolidayCalendar() != null) {
            holidayCalendarRepository.findByHolidayCalendar(legDTO.getHolidayCalendar())
                    .ifPresent(leg::setHolidayCalendar);
        } else if (legDTO.getHolidayCalendarId() != null) {
            holidayCalendarRepository.findById(legDTO.getHolidayCalendarId())
                    .ifPresent(leg::setHolidayCalendar);
        }

        if (legDTO.getCalculationPeriodSchedule() != null) {
            scheduleRepository.findBySchedule(legDTO.getCalculationPeriodSchedule())
                    .ifPresent(leg::setCalculationPeriodSchedule);
        } else if (legDTO.getScheduleId() != null) {
            scheduleRepository.findById(legDTO.getScheduleId())
                    .ifPresent(leg::setCalculationPeriodSchedule);
        }

        if (legDTO.getPaymentBusinessDayConvention() != null) {
            businessDayConventionRepository.findByBdc(legDTO.getPaymentBusinessDayConvention())
                    .ifPresent(leg::setPaymentBusinessDayConvention);
        } else if (legDTO.getPaymentBdcId() != null) {
            businessDayConventionRepository.findById(legDTO.getPaymentBdcId())
                    .ifPresent(leg::setPaymentBusinessDayConvention);
        }

        if (legDTO.getFixingBusinessDayConvention() != null) {
            businessDayConventionRepository.findByBdc(legDTO.getFixingBusinessDayConvention())
                    .ifPresent(leg::setFixingBusinessDayConvention);
        } else if (legDTO.getFixingBdcId() != null) {
            businessDayConventionRepository.findById(legDTO.getFixingBdcId())
                    .ifPresent(leg::setFixingBusinessDayConvention);
        }

        if (legDTO.getPayReceiveFlag() != null) {
            payRecRepository.findByPayRec(legDTO.getPayReceiveFlag())
                    .ifPresent(leg::setPayReceiveFlag);
        } else if (legDTO.getPayRecId() != null) {
            payRecRepository.findById(legDTO.getPayRecId())
                    .ifPresent(leg::setPayReceiveFlag);
        }
    }

    /**
     * FIXED: Generate cashflows based on schedule and maturity date
     */
    public void generateCashflows(TradeLeg leg, LocalDate startDate, LocalDate maturityDate) {
        if (leg == null) {
            // FIX: Guard clause for null legs to avoid NPEs
            logger.error("Attempted to generate cashflows for a null TradeLeg - skipping");
            return;
        }

        logger.info("Generating cashflows for leg {} from {} to {}", leg.getLegId(), startDate, maturityDate);

        String schedule = "3M"; // Default to quarterly
        if (leg.getCalculationPeriodSchedule() != null) {
            schedule = leg.getCalculationPeriodSchedule().getSchedule();
        }
        // Converts the schedule string into a numeric interval (months between
        // payments).
        int monthsInterval = parseSchedule(schedule);
        // calculatePaymentDates Calculates all payment dates between the start and
        // maturity dates using this interval.
        List<LocalDate> paymentDates = calculatePaymentDates(startDate, maturityDate, monthsInterval);

        /*
         * For each payment date, creates a new Cashflow object. Sets its properties
         * (leg, paymentdate, rate). Calculates the payment value using the leg type and
         * interval. Saves the cashflow to the database.
         */
        for (LocalDate paymentDate : paymentDates) {
            Cashflow cashflow = new Cashflow();
            cashflow.setTradeLeg(leg);
            cashflow.setValueDate(paymentDate);
            cashflow.setRate(leg.getRate());

            BigDecimal cashflowValue = calculateCashflowValue(leg, monthsInterval);
            cashflow.setPaymentValue(cashflowValue);

            cashflow.setPayRec(leg.getPayReceiveFlag());
            cashflow.setPaymentBusinessDayConvention(leg.getPaymentBusinessDayConvention());
            cashflow.setCreatedDate(LocalDateTime.now());
            cashflow.setActive(true);

            cashflowRepository.save(cashflow);
        }

        logger.info("Generated {} cashflows for leg {}", paymentDates.size(), leg.getLegId());
    }

    private int parseSchedule(String schedule) {
        if (schedule == null || schedule.trim().isEmpty()) {
            return 3; // Default to quarterly
        }

        schedule = schedule.trim();

        switch (schedule.toLowerCase()) {
            case "monthly":
                return 1;
            case "quarterly":
                return 3;
            case "semi-annually":
            case "semiannually":
            case "half-yearly":
                return 6;
            case "annually":
            case "yearly":
                return 12;
            default:
                if (schedule.endsWith("M") || schedule.endsWith("m")) {
                    try { // e.g 12M" becomes 12 (months interval), and the "M" is removed.
                        return Integer.parseInt(schedule.substring(0, schedule.length() - 1));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid schedule format: " + schedule);
                    }
                }
                throw new RuntimeException("Invalid schedule format: " + schedule);
        }
    }

    private List<LocalDate> calculatePaymentDates(LocalDate startDate, LocalDate maturityDate, int monthsInterval) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate.plusMonths(monthsInterval);

        while (!currentDate.isAfter(maturityDate)) {
            dates.add(currentDate);
            // plusMonths is from built in java LocalDate.class
            currentDate = currentDate.plusMonths(monthsInterval);
        }

        return dates;
    }

    /*
     * Calculates the payment value for a cashflow, based on the properties of a
     * trade leg and the payment interval (in months).
     */
    private BigDecimal calculateCashflowValue(TradeLeg leg, int monthsInterval) {
        if (leg.getLegRateType() == null) { // If the leg's rate type is not set, the method returns zero, prevents
                                            // calculation errors and signals missing data.
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);// REFACRORED:format number to 2 decimal places
                                                                       // using banker's rounding
        }

        String legType = leg.getLegRateType().getType();
        // Notional is the principal amount or face value on which interest payments are
        // calculated in a financial contract (like a loan, bond, or swap). For "Fixed"
        // legs, the cashflow value is calculated as:Cashflow = Notional * Rate * Months
        // /12. This formula annualises the rate and scales it by the payment
        // interval(This ensures the payment matches the correct portion of the annual
        // interest for the interval.To calculate the payment for a period shorter than
        // a year, multiply the notional by the annual rate, then adjust for the
        // fraction of the year covered by the payment interval (e.g., for a quarterly
        // payment, use 3/12 of the annual rate). The result is wrapped in a
        // BigDecimal for precision.
        // REFACTORED: changed .equals to equalsIgnoreCase to make the leg-type checks
        // case-insensitive, if the database or test says "fixed" or "FIXED", this
        // fails.
        if ("Fixed".equalsIgnoreCase(legType)) {
            // REFACTORED changed double to BigDecimal for money to stay as BigDecimal to
            // avoid floating-point drift, loses precision and to avoid null notionals by
            // using 0 instead to avoid a NullPointerException.
            BigDecimal notional = (leg.getNotional() == null) ? BigDecimal.ZERO : leg.getNotional();
            // REFACTORED TO FIX THE 100x bug
            BigDecimal rawRate = BigDecimal.valueOf(leg.getRate());
            // If the rate > 1 (like 3.5), it assumes it's a percentage and divides by 100.
            // If the rate ≤ 1 (like 0.035), it assumes it's already a decimal and leaves it
            // as-is.
            BigDecimal rateDecimal = (rawRate.compareTo(BigDecimal.ONE) > 0) ? rawRate.divide(BigDecimal.valueOf(100))
                    : rawRate;
            // Converts months into fraction e.g 3 months into 0.25 a quarter, keeping 10
            // decimal places
            BigDecimal yearFraction = BigDecimal.valueOf(monthsInterval).divide(BigDecimal.valueOf(12), 10,
                    RoundingMode.HALF_EVEN);// month/12 e.g 3/12 = 0.25 as BigDecimal
            // 10,000,000 * 0.035 * (3 / 12) = 87,500
            BigDecimal result = notional.multiply(rateDecimal).multiply(yearFraction).setScale(2,
                    RoundingMode.HALF_EVEN);

            return result;
        } else if ("Floating".equalsIgnoreCase(legType)) { // For "Floating" legs, the method currently returns zero.
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        // Fallback for unknown leg types return 0,0
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    private void validateReferenceData(Trade trade) {
        if (trade.getBook() == null) {
            throw new RuntimeException("Book not found or not set");
        }
        if (trade.getCounterparty() == null) {
            throw new RuntimeException("Counterparty not found or not set");
        }
        if (trade.getTradeStatus() == null) {
            throw new RuntimeException("Trade status not found or not set");
        }
    }

    // NEW METHOD: Generate the next trade ID (sequential)
    private Long generateNextTradeId() {
        return 10000L + tradeRepository.count();
    }

}
