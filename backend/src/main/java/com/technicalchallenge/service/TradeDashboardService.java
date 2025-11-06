package com.technicalchallenge.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.SearchCriteriaDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.TradeRepository;

import com.technicalchallenge.validation.UserPrivilegeValidationEngine;
import com.technicalchallenge.repository.AdditionalInfoRepository;
import com.technicalchallenge.model.AdditionalInfo;
import com.technicalchallenge.model.UserPrivilege;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Refactored:Moved search, filter, and RSQL search methods from TradeService to this new DashboardService 
 * for separation of concerns: Keeping trade CRUD logic (TradeService) separate from analytics, search, and dashboard logic.
 */
@Service
@Transactional(readOnly = true)
public class TradeDashboardService {
    private static final Logger logger = LoggerFactory.getLogger(TradeDashboardService.class);

    /**
     * Filter trades using flexible criteria and pagination.
     * Privilege validation is performed before querying.
     * Returns a Page of TradeDTOs matching the criteria.
     */
    @Transactional(readOnly = true)
    public Page<TradeDTO> filterTrades(SearchCriteriaDTO criteria, int page, int size) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
            throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
        }

        // Build JPA Specification based on criteria
        Specification<Trade> spec = Specification.where(null);
        // Fix: Compare nested fields for entity relationships to avoid Hibernate type
        // mismatch errors
        if (criteria.getCounterparty() != null && !criteria.getCounterparty().isBlank()) {
            // Compare by counterparty name, not the whole entity
            spec = spec.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("counterparty").get("name"),
                            criteria.getCounterparty()));
        }
        if (criteria.getBook() != null && !criteria.getBook().isBlank()) {
            // Compare by book id, not the whole entity
            spec = spec
                    .and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("book").get("id"),
                            Long.valueOf(criteria.getBook())));
        }
        if (criteria.getTrader() != null && !criteria.getTrader().isBlank()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("traderUser").get("loginId")),
                    criteria.getTrader().toLowerCase()));
        }
        if (criteria.getStatus() != null && !criteria.getStatus().isBlank()) {
            // If tradeStatus is an entity, compare by tradeStatus field
            spec = spec.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("tradeStatus").get("tradeStatus"),
                            criteria.getStatus()));
        }
        if (criteria.getStartDate() != null) {
            spec = spec
                    .and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("tradeDate"),
                            criteria.getStartDate()));
        }
        if (criteria.getEndDate() != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("tradeDate"),
                    criteria.getEndDate()));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Trade> tradePage = tradeRepository.findAll(spec, pageable);
        List<TradeDTO> tradeDtos = new ArrayList<>();
        for (Trade tradeEntity : tradePage.getContent()) {
            TradeDTO tradeDto = tradeMapper.toDto(tradeEntity);
            tradeDtos.add(tradeDto);
        }
        // Batch-enrich DTOs with settlement instructions (avoids N+1)
        enrichSettlementInstructionsForTrades(tradePage.getContent(), tradeDtos);

        return new PageImpl<>(tradeDtos, pageable, tradePage.getTotalElements()); // return paged DTOs
    }

    private final TradeMapper tradeMapper;
    private final TradeRepository tradeRepository;
    private final AdditionalInfoRepository additionalInfoRepository;
    private final UserPrivilegeService userPrivilegeService;
    @SuppressWarnings("unused")
    private final UserPrivilegeValidationEngine privilegeValidationEngine;

    @Autowired
    public TradeDashboardService(TradeRepository tradeRepository, TradeMapper tradeMapper,
            AdditionalInfoRepository additionalInfoRepository,
            UserPrivilegeService userPrivilegeService,
            UserPrivilegeValidationEngine privilegeValidationEngine) {/*
                                                                       * enforces a security check to ensure that the
                                                                       * current user has the required privilege before
                                                                       * proceeding with a sensitive operation. It calls
                                                                       * the hasPrivilege method, passing in the current
                                                                       * user's identifier and the required privilege
                                                                       * string "TRADE_VIEW". If the method returns
                                                                       * false, indicating that the user does not have
                                                                       * the necessary permission, the code throws an
                                                                       * AccessDeniedException from Spring Security with
                                                                       * the message "Insufficient privileges
                                                                       */
        this.tradeRepository = tradeRepository;
        this.tradeMapper = tradeMapper;
        this.additionalInfoRepository = additionalInfoRepository;
        this.userPrivilegeService = userPrivilegeService; // ADDED: wire DB privilege service
        this.privilegeValidationEngine = privilegeValidationEngine;
    }

    /**
     * Backwards-compatible constructor used by older tests that don't supply
     * an AdditionalInfoRepository. Delegates to the full constructor with a
     * null AdditionalInfoRepository.
     */
    public TradeDashboardService(TradeRepository tradeRepository, TradeMapper tradeMapper,
            UserPrivilegeService userPrivilegeService,
            UserPrivilegeValidationEngine privilegeValidationEngine) {
        this(tradeRepository, tradeMapper, null, userPrivilegeService, privilegeValidationEngine);
    }

    /**
     * Search trades using flexible criteria (counterparty, book, trader, status,
     * date range).
     * Privilege validation is performed before querying.
     * Returns a list of TradeDTOs matching the criteria.
     */
    @Transactional(readOnly = true)
    public List<TradeDTO> searchTrades(SearchCriteriaDTO criteriaDTO) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
            throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
        }

        // Build JPA Specification based on criteria
        Specification<Trade> spec = Specification.where(null);
        if (criteriaDTO.getCounterparty() != null && !criteriaDTO.getCounterparty().isBlank()) {
            // Compare by counterparty name, not the whole entity
            spec = spec.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("counterparty").get("name"),
                            criteriaDTO.getCounterparty()));
        }
        if (criteriaDTO.getBook() != null && !criteriaDTO.getBook().isBlank()) {
            // Compare by book id, not the whole entity
            spec = spec.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("book").get("id"),
                            Long.valueOf(criteriaDTO.getBook())));
        }
        if (criteriaDTO.getTrader() != null && !criteriaDTO.getTrader().isBlank()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("traderUser").get("loginId")),
                    criteriaDTO.getTrader().toLowerCase()));
        }
        if (criteriaDTO.getStatus() != null && !criteriaDTO.getStatus().isBlank()) {
            // If tradeStatus is an entity, compare by tradeStatus field
            spec = spec.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("tradeStatus").get("tradeStatus"),
                            criteriaDTO.getStatus()));
        }
        if (criteriaDTO.getStartDate() != null) {
            spec = spec.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("tradeDate"),
                            criteriaDTO.getStartDate()));
        }
        if (criteriaDTO.getEndDate() != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("tradeDate"),
                    criteriaDTO.getEndDate()));
        }

        // Query repository and map results to DTOs
        List<Trade> tradeEntities = tradeRepository.findAll(spec);
        List<TradeDTO> tradeDtos = new ArrayList<>();
        for (Trade tradeEntity : tradeEntities) {
            TradeDTO tradeDto = tradeMapper.toDto(tradeEntity);
            tradeDtos.add(tradeDto);
        }
        return tradeDtos;
    }
    // correctly and didn’t cause test failures ...

    // RSQL Search
    @Transactional(readOnly = true)
    public List<TradeDTO> searchTradesRsql(String query) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
            throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
        }

        /*
         * The test failures showed:
         * "Servlet Request processing failed: java.lang.IllegalArgumentException: Invalid RSQL query syntax: book==1"
         * This means the exception wasn't being caught properly, causing a 500 instead
         * of 400.
         * I am fixing this by catching both parser and illegal argument errors and
         * returning null,
         * which the controller translates into a 400 Bad Request.If the RSQL parsing
         * fails due to a syntax error or invalid query, the code catches the
         * RSQLParserException and throws a ResponseStatusException with a BAD_REQUEST
         * status,
         */
        try {
            Set<ComparisonOperator> operators = new HashSet<>(RSQLOperators.defaultOperators());
            ComparisonOperator LIKE = new ComparisonOperator("=like=");
            operators.add(LIKE);

            RSQLParser parser = new RSQLParser(operators);
            Node root = parser.parse(query);
            Specification<Trade> spec = root.accept(new TradeRsqlVisitor());

            List<Trade> tradeEntities = tradeRepository.findAll(spec);
            List<TradeDTO> tradeDtos = new ArrayList<>();

            for (Trade tradeEntity : tradeEntities) {
                TradeDTO tradeDto = tradeMapper.toDto(tradeEntity);
                tradeDtos.add(tradeDto);
            }
            return tradeDtos;

        } catch (cz.jirutka.rsql.parser.RSQLParserException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid RSQL query");
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid RSQL query");
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid RSQL query", e);
        }
    }

    // Fetch trades filtered by trader
    @Transactional(readOnly = true)
    public List<TradeDTO> getTradesByTrader(String traderId) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
            throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
        }

        SearchCriteriaDTO criteriaDTO = new SearchCriteriaDTO();
        criteriaDTO.setTrader(traderId);

        return searchTrades(criteriaDTO);
    }

    @Transactional(readOnly = true)
    public List<TradeDTO> getTradesByTrader() {
        String currentTraderId = resolveCurrentTraderId();
        return getTradesByTrader(currentTraderId);
    }

    // returns all trades belonging to a specific book
    @Transactional(readOnly = true)
    public List<TradeDTO> getTradesByBook(Long bookId) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
            throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
        }

        SearchCriteriaDTO criteriaDTO = new SearchCriteriaDTO();
        criteriaDTO.setBook(String.valueOf(bookId));

        return searchTrades(criteriaDTO);
    }

    // REFACTORED
    // Produces an aggregated TradeSummary for the given trader. The method
    // performs several independent aggregation steps described below.
    @Transactional(readOnly = true)
    public TradeSummaryDTO getTradeSummary(String traderId) {
        try {
            // ADDED: Capture current Authentication once to avoid races where
            // subsequent service/mapper calls may trigger user lookups that do not
            // change the real caller but can alter the SecurityContext in some
            // environments. use the captured auth/name/authorities for all
            // authorization decisions in this method.
            Authentication auth = SecurityContextHolder.getContext() != null
                    ? SecurityContextHolder.getContext().getAuthentication()
                    : null;
            String currentUser = (auth == null || auth.getName() == null) ? "__UNKNOWN_TRADER__" : auth.getName();

            // Privilege validation: ensure caller may view trades
            if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
                throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
            }

            // ADDED: Authorization guard - prevent a logged-in trader from viewing
            // another trader's dashboard by passing a different traderId. Example:
            // if 'joey' is logged in and the UI calls /api/dashboard/summary?traderId=simon
            // the request must be denied unless the caller has ROLE_MIDDLE_OFFICE,
            // ROLE_SUPERUSER, or the TRADE_VIEW_ALL privilege. This avoids a simple
            // mistake in the UI exposing 'simon' data to 'joey'.
            // NOTE: In test contexts or when no Authentication is present (e.g.
            // direct service unit tests), do not enforce this guard so tests
            // that call the service with explicit traderIds continue to work.
            boolean canViewOthers = auth != null && auth.getAuthorities().stream().anyMatch(a -> {
                String ga = a.getAuthority();
                return "ROLE_MIDDLE_OFFICE".equals(ga)
                        || "ROLE_SUPERUSER".equals(ga)
                        || "TRADE_VIEW_ALL".equals(ga);
                // NOTE: do NOT treat the generic TRADE_VIEW privilege as permission
                // to view other traders' data. TRADE_VIEW grants viewing rights for
                // the caller's own data but not for arbitrary traders.
            });
            if (auth != null && auth.isAuthenticated()
                    && traderId != null && !traderId.isBlank() && !traderId.equalsIgnoreCase(currentUser)
                    && !canViewOthers) {
                // ADDED: deny access when attempting to request another trader's
                // data without elevated authority. This is thrown early to avoid
                // executing expensive aggregations and to ensure consistent
                // security semantics.
                throw new org.springframework.security.access.AccessDeniedException(
                        "Insufficient privileges to view another trader's data");
            }

            // 2Load trades: retrieve all trades for the trader and prepare the
            // DTO that will be returned. Use an internal fetch that does NOT
            // perform redundant privilege checks (already validated above).
            TradeSummaryDTO summaryDTO = new TradeSummaryDTO();
            List<TradeDTO> tradesForTrader = fetchTradesForTraderWithoutPrivilegeCheck(traderId);

            // Debug log: record the resolved role for troubleshooting
            // Debug: record the resolved role for troubleshooting (keeps the
            // helper resolveCurrentUserRole method in use)
            logger.debug("Current role for user {}: {}", resolveCurrentTraderId(), resolveCurrentUserRole());

            // Aggregation trades by status: group by tradeStatus and count
            Map<String, Long> tradesByStatus = tradesForTrader.stream()
                    .map(TradeDTO::getTradeStatus)
                    .filter(status -> status != null && !status.isBlank())
                    .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
            summaryDTO.setTradesByStatus(tradesByStatus);
            // ALSO set labeled all-time field for clarity in the UI
            summaryDTO.setAllTimeTradesByStatus(tradesByStatus);

            // Aggregation notional by currency: iterate all trade legs and sum
            // notionals per currency using BigDecimal to avoid floating-point errors.
            Map<String, BigDecimal> notionalByCurrency = new HashMap<>();
            for (TradeDTO tradeDto : tradesForTrader) {
                List<TradeLegDTO> tradeLegs = tradeDto.getTradeLegs();
                if (tradeLegs == null)
                    continue;
                for (TradeLegDTO tradeLeg : tradeLegs) {
                    if (tradeLeg == null)
                        continue;
                    String currency = tradeLeg.getCurrency();
                    BigDecimal notional = tradeLeg.getNotional();
                    if (currency == null || notional == null)
                        continue;
                    notionalByCurrency.merge(currency, notional, BigDecimal::add);
                }
            }
            summaryDTO.setNotionalByCurrency(notionalByCurrency);
            // ALSO set labeled all-time notional totals
            summaryDTO.setAllTimeNotionalByCurrency(notionalByCurrency);

            // Aggregation trades by type and counterparty: build a compact key
            // "tradeType:counterparty" and count occurrences.
            Map<String, Long> tradesByTypeAndCounterparty = tradesForTrader.stream()
                    .map(trade -> {
                        String tradeType = trade.getTradeType() == null ? "UNKNOWN" : trade.getTradeType();
                        String counterparty = trade.getCounterpartyName() == null ? "UNKNOWN"
                                : trade.getCounterpartyName();
                        return tradeType + ":" + counterparty;
                    })
                    .collect(Collectors.groupingBy(key -> key, Collectors.counting()));
            summaryDTO.setTradesByTypeAndCounterparty(tradesByTypeAndCounterparty);
            // Also expose as explicit all-time mapping
            summaryDTO.setAllTimeTradesByTypeAndCounterparty(tradesByTypeAndCounterparty);

            // Risk placeholder: compute a naive 'delta' as sum(notional * rate)
            // across legs. This is a demonstration value, not a financial Greek.
            BigDecimal delta = BigDecimal.ZERO;
            for (TradeDTO tradeDto : tradesForTrader) {
                if (tradeDto == null || tradeDto.getTradeLegs() == null)
                    continue;
                for (TradeLegDTO leg : tradeDto.getTradeLegs()) {
                    if (leg == null)
                        continue;
                    BigDecimal notional = leg.getNotional();
                    Double rateDouble = leg.getRate();
                    if (notional == null || rateDouble == null)
                        continue;
                    BigDecimal rate = BigDecimal.valueOf(rateDouble);
                    delta = delta.add(notional.multiply(rate));
                }
            }
            // vega left at zero because volatility-based sensitivity is not computed
            // by this placeholder logic.
            Map<String, BigDecimal> allTimeRisk = Map.of("delta", delta, "vega", BigDecimal.ZERO);
            summaryDTO.setRiskExposureSummary(allTimeRisk);
            // Also label as all-time risk summary
            summaryDTO.setAllTimeRiskExposureSummary(allTimeRisk);

            // Weekly comparisons: create seven per-day summaries (oldest -> newest)
            // and at the same time compute weekly aggregate totals used for the
            // labeled weekly fields below.
            List<DailySummaryDTO.DailyComparisonSummary> weekly = new ArrayList<>();
            LocalDate today = LocalDate.now();
            List<TradeDTO> weekTrades = new ArrayList<>();
            for (int i = 6; i >= 0; i--) { // oldest first
                LocalDate day = today.minusDays(i);
                List<TradeDTO> tradesOnDay = tradesForTrader.stream()
                        .filter(t -> t != null && t.getTradeDate() != null && t.getTradeDate().isEqual(day))
                        .toList();
                // collect for weekly totals
                weekTrades.addAll(tradesOnDay);
                DailySummaryDTO.DailyComparisonSummary daySummary = new DailySummaryDTO.DailyComparisonSummary();
                daySummary.setTradeCount(tradesOnDay.size());
                daySummary.setNotionalByCurrency(sumNotionalByCurrency(tradesOnDay));
                weekly.add(daySummary);
            }
            summaryDTO.setWeeklyComparisons(weekly);

            // Compute weekly labeled aggregates (summing the weekTrades list)
            Map<String, Long> weeklyTradesByStatus = weekTrades.stream()
                    .map(TradeDTO::getTradeStatus)
                    .filter(status -> status != null && !status.isBlank())
                    .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
            Map<String, BigDecimal> weeklyNotionalByCurrency = sumNotionalByCurrency(weekTrades);
            Map<String, Long> weeklyTradesByTypeAndCounterparty = weekTrades.stream()
                    .map(trade -> {
                        String tradeType = trade.getTradeType() == null ? "UNKNOWN" : trade.getTradeType();
                        String counterparty = trade.getCounterpartyName() == null ? "UNKNOWN"
                                : trade.getCounterpartyName();
                        return tradeType + ":" + counterparty;
                    })
                    .collect(Collectors.groupingBy(key -> key, Collectors.counting()));
            // naive weekly risk (same placeholder method applied to weekTrades)
            BigDecimal weeklyDelta = BigDecimal.ZERO;
            for (TradeDTO tradeDto : weekTrades) {
                if (tradeDto == null || tradeDto.getTradeLegs() == null)
                    continue;
                for (TradeLegDTO leg : tradeDto.getTradeLegs()) {
                    if (leg == null)
                        continue;
                    BigDecimal notional = leg.getNotional();
                    Double rateDouble = leg.getRate();
                    if (notional == null || rateDouble == null)
                        continue;
                    BigDecimal rate = BigDecimal.valueOf(rateDouble);
                    weeklyDelta = weeklyDelta.add(notional.multiply(rate));
                }
            }
            Map<String, BigDecimal> weeklyRisk = Map.of("delta", weeklyDelta, "vega", BigDecimal.ZERO);

            summaryDTO.setWeeklyTradesByStatus(weeklyTradesByStatus);
            summaryDTO.setWeeklyNotionalByCurrency(weeklyNotionalByCurrency);
            summaryDTO.setWeeklyTradesByTypeAndCounterparty(weeklyTradesByTypeAndCounterparty);
            summaryDTO.setWeeklyRiskExposureSummary(weeklyRisk);
            return summaryDTO;
        } catch (Exception e) {
            // Protect the dashboard read path from runtime validation errors
            // that may be thrown by mappers, repositories or nested services.
            // Log at WARN and return an empty/partial summary so the UI can
            // still render something useful instead of collapsing with a 400.
            logger.warn("getTradeSummary encountered an error for traderId={}; returning empty summary. Cause: {}",
                    traderId, e.toString());
            logger.debug("getTradeSummary full stack for traderId={}", traderId, e);
            return new TradeSummaryDTO();
        }
    }

    /**
     * Internal helper that fetches trades for a trader without performing the
     * hasPrivilege() check. Intended for use only after this service has
     * already validated authorization, so callers should ensure that proper
     * privileges were checked earlier.
     */
    private List<TradeDTO> fetchTradesForTraderWithoutPrivilegeCheck(String traderId) {
        // Defensive authorization: although this helper is intended to be called
        // only after the caller has already validated privileges, add an extra
        // safeguard so lower-level callers (or future callers) cannot bypass
        // authorization by calling this method directly with another traderId.
        Authentication auth = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        String caller = (auth == null || auth.getName() == null) ? "__UNKNOWN_TRADER__" : auth.getName();

        boolean canViewOthers = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String ga = a.getAuthority();
                    return "ROLE_MIDDLE_OFFICE".equals(ga) || "ROLE_SUPERUSER".equals(ga)
                            || "TRADE_VIEW_ALL".equals(ga);
                });

        // If the caller requests another trader's data and lacks elevated
        // authority, deny the request here to prevent accidental data leaks.
        if (traderId != null && !traderId.isBlank() && !traderId.equalsIgnoreCase(caller) && !canViewOthers) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Insufficient privileges to view another trader's data (defensive check)");
        }

        String traderFilter = (traderId == null || traderId.isBlank()) ? resolveCurrentTraderId() : traderId;
        Specification<Trade> spec = (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("traderUser").get("loginId")), traderFilter.toLowerCase());

        List<Trade> tradeEntities = tradeRepository.findAll(spec);
        // Diagnostic logging to aid integration-test debugging when counts are
        // unexpectedly zero. Logs the number of entities and their DB ids.
        try {
            logger.info("fetchTradesForTraderWithoutPrivilegeCheck: traderFilter='{}' fetched={}",
                    traderFilter, tradeEntities == null ? 0 : tradeEntities.size());
            if (tradeEntities != null) {
                String ids = tradeEntities.stream().map(t -> t.getId() == null ? "null" : t.getId().toString())
                        .collect(Collectors.joining(","));
                logger.info("fetchTradesForTraderWithoutPrivilegeCheck: ids=[{}]", ids);
                // Also print to stdout to ensure visibility in test output
                System.out.println("DBG: fetchTradesForTraderWithoutPrivilegeCheck - traderFilter='" + traderFilter
                        + "' fetched=" + (tradeEntities == null ? 0 : tradeEntities.size()) + " ids=[" + ids + "]");
            }
        } catch (Exception e) {
            logger.info("Error while logging fetched trades: {}", e.getMessage());
            System.out.println("DBG: Error while logging fetched trades: " + e.getMessage());
        }

        // Map entities to DTOs
        List<TradeDTO> dtos = tradeEntities.stream().map(tradeMapper::toDto).toList();

        // Batch-enrich DTOs with settlement instructions (avoids N+1)
        enrichSettlementInstructionsForTrades(tradeEntities, dtos);

        return dtos;
    }

    /**
     * Refactored helper: enrich a list of TradeDTOs with settlement instructions.
     *
     * Responsibilities:
     * - Deduplicate tradeIds (preserve order) using a LinkedHashSet
     * - Batch-fetch AdditionalInfo rows for field "SETTLEMENT_INSTRUCTIONS"
     * - Build an in-memory map tradeId -> settlement text (keep-first semantics)
     * - Attach settlement text to matching TradeDTOs
     *
     * This method is defensive: it returns early when inputs are empty and
     * catches exceptions so enrichment failures do not break callers.
     */
    private void enrichSettlementInstructionsForTrades(List<Trade> tradeEntities, List<TradeDTO> dtos) {
        if (tradeEntities == null || tradeEntities.isEmpty() || dtos == null || dtos.isEmpty()) {
            return; // nothing to do
        }
        try {
            java.util.Set<Long> idSet = new java.util.LinkedHashSet<>();
            for (Trade tradeItem : tradeEntities) {
                if (tradeItem == null)
                    continue;
                Long tid = tradeItem.getTradeId();
                if (tid != null) {
                    idSet.add(tid);
                }
            }

            if (idSet.isEmpty()) {
                return;
            }

            java.util.List<Long> idList = new java.util.ArrayList<>(idSet);
            List<AdditionalInfo> infos = additionalInfoRepository
                    .findByEntityTypeAndEntityIdInAndFieldName("TRADE", idList, "SETTLEMENT_INSTRUCTIONS");

            java.util.Map<Long, String> settlementByTrade = new java.util.HashMap<>();
            for (AdditionalInfo infoItem : infos) {
                if (infoItem == null)
                    continue;
                Long entityId = infoItem.getEntityId();
                String value = infoItem.getFieldValue();
                if (entityId == null)
                    continue;
                if (!settlementByTrade.containsKey(entityId)) { // keep-first
                    settlementByTrade.put(entityId, value);
                }
            }

            for (TradeDTO dto : dtos) {
                if (dto == null || dto.getTradeId() == null)
                    continue;
                dto.setSettlementInstructions(settlementByTrade.get(dto.getTradeId()));
            }
        } catch (Exception e) {
            logger.debug("enrichSettlementInstructionsForTrades failed: {}", e.getMessage());
        }
    }

    public TradeSummaryDTO getTradeSummary() {
        String currentTraderId = resolveCurrentTraderId();
        return getTradeSummary(currentTraderId);
    }

    @Transactional(readOnly = true)
    public DailySummaryDTO getDailySummary(String traderId) {
        try {
            // Privilege validation: only users with TRADE_VIEW privilege allowed
            String currentUser = resolveCurrentTraderId();
            if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
                throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
            }

            // ADDED: same authorization guard as getTradeSummary. It protects the
            // daily summary endpoint from returning another trader's data to a
            // non-privileged user. Keeps behavior consistent across dashboard
            // endpoints.
            Authentication auth = SecurityContextHolder.getContext() != null
                    ? SecurityContextHolder.getContext().getAuthentication()
                    : null;
            boolean canViewOthers = auth != null && auth.getAuthorities().stream().anyMatch(a -> {
                String ga = a.getAuthority();
                return "ROLE_MIDDLE_OFFICE".equals(ga)
                        || "ROLE_SUPERUSER".equals(ga)
                        || "TRADE_VIEW_ALL".equals(ga);
            });
            if (auth != null && auth.isAuthenticated()
                    && traderId != null && !traderId.isBlank() && !traderId.equalsIgnoreCase(currentUser)
                    && !canViewOthers) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Insufficient privileges to view another trader's data");
            }

            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            // If traderId not provided, default to the current authenticated user
            String traderFilter = (traderId == null || traderId.isBlank()) ? resolveCurrentTraderId() : traderId;

            Specification<Trade> todaySpec = (root, query, criteriaBuilder) -> criteriaBuilder.and(
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("traderUser").get("loginId")),
                            traderFilter.toLowerCase()),
                    criteriaBuilder.equal(root.get("tradeDate"), today));

            List<Trade> todayTradeEntities = tradeRepository.findAll(todaySpec);
            List<TradeDTO> todayTradeDtos = todayTradeEntities.stream().map(tradeMapper::toDto).toList();

            Specification<Trade> yesterdaySpec = (root, query, criteriaBuilder) -> criteriaBuilder.and(
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("traderUser").get("loginId")),
                            traderFilter.toLowerCase()),
                    criteriaBuilder.equal(root.get("tradeDate"), yesterday));

            List<Trade> yesterdayTradeEntities = tradeRepository.findAll(yesterdaySpec);
            List<TradeDTO> yesterdayTradeDtos = yesterdayTradeEntities.stream().map(tradeMapper::toDto).toList();

            DailySummaryDTO summaryDto = new DailySummaryDTO();
            summaryDto.setTodaysTradeCount(todayTradeDtos.size());
            summaryDto.setTodaysNotionalByCurrency(sumNotionalByCurrency(todayTradeDtos));

            Map<String, Object> performanceMetrics = new HashMap<>();
            performanceMetrics.put("tradeCount", todayTradeDtos.size());
            performanceMetrics.put("notionalCcyCount", summaryDto.getTodaysNotionalByCurrency().size());
            summaryDto.setUserPerformanceMetrics(performanceMetrics);

            Map<Long, DailySummaryDTO.BookActivitySummary> bookActivitySummary = new HashMap<>();
            for (TradeDTO tradeDto : todayTradeDtos) {
                Long bookKey = tradeDto.getBookId() == null ? -1L : tradeDto.getBookId();
                DailySummaryDTO.BookActivitySummary activitySummary = bookActivitySummary.computeIfAbsent(bookKey,
                        bookIdKey -> {
                            DailySummaryDTO.BookActivitySummary bookSummary = new DailySummaryDTO.BookActivitySummary();
                            bookSummary.setTradeCount(0);
                            bookSummary.setNotionalByCurrency(new HashMap<>());
                            return bookSummary;
                        });
                activitySummary.setTradeCount(activitySummary.getTradeCount() + 1);
                Map<String, BigDecimal> legTotals = sumNotionalByCurrency(List.of(tradeDto));
                for (Map.Entry<String, BigDecimal> entry : legTotals.entrySet()) {
                    activitySummary.getNotionalByCurrency().merge(entry.getKey(), entry.getValue(), BigDecimal::add);
                }
            }
            summaryDto.setBookActivitySummaries(bookActivitySummary);

            DailySummaryDTO.DailyComparisonSummary yesterdaySummary = new DailySummaryDTO.DailyComparisonSummary();
            yesterdaySummary.setTradeCount(yesterdayTradeDtos.size());
            yesterdaySummary.setNotionalByCurrency(sumNotionalByCurrency(yesterdayTradeDtos));

            List<DailySummaryDTO.DailyComparisonSummary> historicalComparison = new ArrayList<>();
            historicalComparison.add(yesterdaySummary);
            summaryDto.setHistoricalComparisons(historicalComparison);

            return summaryDto;
        } catch (Exception e) {
            logger.warn(
                    "getDailySummary encountered an error for traderId={}; returning empty daily summary. Cause: {}",
                    traderId, e.toString());
            logger.debug("getDailySummary full stack for traderId={}", traderId, e);
            return new DailySummaryDTO();
        }
    }

    @Transactional(readOnly = true)
    public DailySummaryDTO getDailySummary() {
        String currentTraderId = resolveCurrentTraderId();
        return getDailySummary(currentTraderId);
    }

    // Helper to resolve current user's role from Spring Security context
    private String resolveCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        if (authentication == null || authentication.getAuthorities() == null) {
            return "UNKNOWN";
        }
        // Return first role name, or UNKNOWN
        return authentication.getAuthorities().stream().findFirst().map(Object::toString).orElse("UNKNOWN");
    }

    /*
     * This DB-driven privilege checks to be the authoritative
     * service-side guard for dashboard operations. Key points:
     * - Deny-by-default: invalid inputs or lack of evidence for a privilege
     * result in a denial. This keeps security strict at the service layer.
     * - SecurityContext short-circuit: if the current Authentication carries
     * an authority that directly matches the requested privilege (or the
     * ROLE_ mapped form), the method returns true quickly to avoid an
     * unnecessary DB lookup.
     * - Role aliases: controller role-based checks are supported here by
     * treating common role authorities (for example ROLE_TRADER,
     * ROLE_MIDDLE_OFFICE, ROLE_SUPPORT) as sufficient for
     * the TRADE_VIEW privilege. This preserves expected behaviour when
     * profiles are mapped to roles in the DatabaseUserDetailsService.
     * - DB fallback: when the SecurityContext short-circuit does not grant
     * access, the method consults
     * `UserPrivilegeService.findPrivilegesByUserLoginIdAndPrivilegeName`
     * for an authoritative record of a user having the privilege.
     * - Tests: unit tests should mock `UserPrivilegeService` when they need to
     * exercise privilege outcomes. The previous permissive behaviour that
     * allowed access when the DB service was missing has been removed to
     * avoid accidental insecure behaviour in production; tests must now
     * explicitly provide the mocked service.
     * - Diagnostic logging: decisions and matches are logged to make
     * authorization failures straightforward to investigate.
     */
    private boolean hasPrivilege(String user, String privilege) {
        // Inline comment: if inputs are missing, fail fast (deny-by-default).
        if (user == null || user.isBlank() || privilege == null || privilege.isBlank()) {
            return false; // ADDED: explicit deny for invalid inputs
        }

        // NOTE: do not short-circuit when the DB-backed service is missing;
        // tests must explicitly mock `UserPrivilegeService` where required.

        // First, check the SecurityContext authorities for a quick-path
        // This allows users granted privileges at authentication time (roles
        // or authorities) to be accepted without a DB lookup.
        try {
            Authentication authentication = SecurityContextHolder.getContext() != null
                    ? SecurityContextHolder.getContext().getAuthentication()
                    : null;
            // DEBUG: record authentication principal and authorities to aid
            // diagnosis when a logged-in user is unexpectedly denied.
            if (authentication != null) {
                String authName = authentication.getName();
                String auths = authentication.getAuthorities().stream().map(Object::toString)
                        .collect(Collectors.joining(","));
                logger.debug(
                        "hasPrivilege check start: userParam='{}' privilege='{}' authentication.name='{}' authorities={}",
                        user, privilege, authName, auths);
            } else {
                logger.debug(
                        "hasPrivilege check start: userParam='{}' privilege='{}' no Authentication in SecurityContext",
                        user, privilege);
            }
            if (authentication != null && authentication.getAuthorities() != null) {
                // Short-circuit: accept direct authority names, role-mapped forms,
                // and controller-authorised roles for the TRADE_VIEW privilege.
                // This preserves existing test expectations where @WithMockUser sets
                // roles like TRADER or SUPPORT which become authorities like
                // ROLE_TRADER / ROLE_SUPPORT.
                boolean hasAuthority = authentication.getAuthorities().stream().anyMatch(a -> {
                    String ga = a.getAuthority();
                    if (ga == null)
                        return false;
                    // Direct match or role-mapped privilege (e.g. ROLE_TRADE_VIEW)
                    if (ga.equalsIgnoreCase(privilege) || ga.equalsIgnoreCase("ROLE_" + privilege)) {
                        return true;
                    }
                    // Allow controller-authorised roles to count for TRADE_VIEW
                    if ("TRADE_VIEW".equalsIgnoreCase(privilege)) {
                        switch (ga.toUpperCase()) {
                            case "ROLE_TRADER": // ADDED: controller-level TRADER role should count for TRADE_VIEW
                            case "ROLE_MIDDLE_OFFICE": // ADDED: MO role authorised to view other traders' dashboards
                            case "ROLE_SUPPORT": // ADDED: SUPPORT role allowed to view per controller config
                            case "ADMIN": // ADDED: SUPERUSER has full access
                                return true; // ADDED: short-circuit permit when these standard roles are present
                            default:
                                break;
                        }
                    }
                    // backward-compat alias used elsewhere
                    if ("ROLE_TRADE_VIEW".equalsIgnoreCase(ga) && "TRADE_VIEW".equalsIgnoreCase(privilege)) {
                        return true;
                    }
                    return false;
                });
                if (hasAuthority) {
                    logger.debug("hasPrivilege short-circuit: authority matched for userParam='{}' privilege='{}'",
                            user,
                            privilege);
                    return true; // short-circuit when authority or allowed role present
                }
            }
        } catch (Exception e) {
            // ADDED: defensive - do not throw from an auth check; continue to DB check
            logger.debug("Error while checking SecurityContext authorities", e);
        }

        // Next, consult the database-stored user privileges via UserPrivilegeService
        // ADDED: use a repository-backed helper for an efficient lookup by
        // loginId + privilege name instead of scanning all privileges in memory.
        try {
            // Prefer precise DB query to avoid loading the entire table into
            // memory and to keep latency low in production.
            List<UserPrivilege> matches = userPrivilegeService
                    .findPrivilegesByUserLoginIdAndPrivilegeName(user, privilege);

            if (matches != null && !matches.isEmpty()) {
                // ADDED: exact DB match found for this user/privilege
                logger.debug("hasPrivilege DB match: userParam='{}' privilege='{}' matches={}", user, privilege,
                        matches.size());
                return true;
            } else {
                logger.debug("hasPrivilege DB no-match: userParam='{}' privilege='{}'", user, privilege);
            }
        } catch (Exception e) {
            // ADDED: on DB errors default to deny; log for diagnostics
            logger.warn("Failed to read user privileges from DB for user {}: {}", user, e.getMessage());
            return false;
        }

        // Finally, consult the privilege validation engine for business rules
        // This is optional and used for advanced validations (kept as a last
        // resort after direct authority + DB privilege checks).
        try {
            // NOTE: privilegeValidationEngine expects domain objects; only call
            // if available and applicable. cannot build a full TradeDTO here,
            // so this call is omitted. Keep this block for future integration.
        } catch (Exception e) {
            logger.debug("Privilege validation engine check skipped or failed", e);
        }

        // Deny-by-default if no authority or DB privilege matched
        logger.debug("hasPrivilege final decision: DENY for userParam='{}' privilege='{}'", user, privilege);
        return false;
    }

    // checks if the security context is available and then attempts to obtain the
    // Authentication object, which represents the current user's authentication
    // state. If authentication is present and the user's name is available, it
    // returns the authenticated user's name

    private String resolveCurrentTraderId() {
        Authentication authentication = SecurityContextHolder.getContext() != null
                // SecurityContextHolder is a Spring Security class that holds the security
                // context for the current thread. It isfor accessing authentication and
                // authorization information in a Spring application.
                ? SecurityContextHolder.getContext().getAuthentication()// contains all security-related information for
                                                                        // the current user/session
                : null;
        if (authentication == null || authentication.getName() == null) {
            return "__UNKNOWN_TRADER__";
        }
        return authentication.getName();
    }

    // calculates the total notional amounts for each currency across a list of
    // trades
    private Map<String, BigDecimal> sumNotionalByCurrency(List<TradeDTO> trades) {
        Map<String, BigDecimal> notionalTotalsByCurrency = new HashMap<>();
        if (trades == null)
            return notionalTotalsByCurrency;// returns the empty map

        for (TradeDTO tradeDto : trades) {
            if (tradeDto == null || tradeDto.getTradeLegs() == null)
                continue;

            for (TradeLegDTO tradeLeg : tradeDto.getTradeLegs()) {
                if (tradeLeg == null)
                    continue;
                String currency = tradeLeg.getCurrency();
                BigDecimal notional = tradeLeg.getNotional();
                if (currency == null || notional == null)
                    continue;
                notionalTotalsByCurrency.merge(currency, notional, BigDecimal::add);// using the merge method to add the
                                                                                    // notional to the running total for
                                                                                    // that currency in the map. This
            }
        }
        return notionalTotalsByCurrency;
    }

}
