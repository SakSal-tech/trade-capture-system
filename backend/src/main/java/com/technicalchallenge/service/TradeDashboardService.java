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
import org.springframework.transaction.annotation.Transactional;

import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.SearchCriteriaDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.model.UserProfile;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.validation.TradeValidationResult;
import com.technicalchallenge.validation.UserPrivilegeValidationEngine;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/*
 * Refactored:Moved search, filter, and RSQL search methods from TradeService to this new DashboardService 
 * for separation of concerns: Keeping trade CRUD logic (TradeService) separate from analytics, search, and dashboard logic.
 */
@Service
@Transactional(readOnly = true)
public class TradeDashboardService {
    /**
     * Filter trades using flexible criteria and pagination.
     * Privilege validation is performed before querying.
     * Returns a Page of TradeDTOs matching the criteria.
     */
    public Page<TradeDTO> filterTrades(SearchCriteriaDTO criteria, int page, int size) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        String currentRole = resolveCurrentUserRole();
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
        return new PageImpl<>(tradeDtos, pageable, tradePage.getTotalElements());
    }

    private final TradeMapper tradeMapper;
    private final TradeRepository tradeRepository;
    private final UserPrivilegeValidationEngine privilegeValidationEngine;

    public TradeDashboardService(TradeRepository tradeRepository, TradeMapper tradeMapper,
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
        this.privilegeValidationEngine = privilegeValidationEngine;
    }

    /**
     * Search trades using flexible criteria (counterparty, book, trader, status,
     * date range).
     * Privilege validation is performed before querying.
     * Returns a list of TradeDTOs matching the criteria.
     */
    public List<TradeDTO> searchTrades(SearchCriteriaDTO criteriaDTO) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        String currentRole = resolveCurrentUserRole();
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
    public List<TradeDTO> searchTradesRsql(String query) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        String currentRole = resolveCurrentUserRole();
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
    public List<TradeDTO> getTradesByTrader(String traderId) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        String currentRole = resolveCurrentUserRole();
        if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
            throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
        }

        SearchCriteriaDTO criteriaDTO = new SearchCriteriaDTO();
        criteriaDTO.setTrader(traderId);

        return searchTrades(criteriaDTO);
    }

    public List<TradeDTO> getTradesByTrader() {
        String currentTraderId = resolveCurrentTraderId();
        return getTradesByTrader(currentTraderId);
    }

    // returns all trades belonging to a specific book
    public List<TradeDTO> getTradesByBook(Long bookId) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        String currentRole = resolveCurrentUserRole();
        if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
            throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
        }

        SearchCriteriaDTO criteriaDTO = new SearchCriteriaDTO();
        criteriaDTO.setBook(String.valueOf(bookId));

        return searchTrades(criteriaDTO);
    }

    // Calculate totals, notionals, and risk exposure for this trader
    public TradeSummaryDTO getTradeSummary(String traderId) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        String currentRole = resolveCurrentUserRole();
        if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
            throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
        }

        TradeSummaryDTO summaryDTO = new TradeSummaryDTO();
        List<TradeDTO> tradesForTrader = getTradesByTrader(traderId);

        Map<String, Long> tradesByStatus = tradesForTrader.stream()
                .map(TradeDTO::getTradeStatus)
                .filter(status -> status != null && !status.isBlank())
                .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
        summaryDTO.setTradesByStatus(tradesByStatus);

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

        Map<String, Long> tradesByTypeAndCounterparty = tradesForTrader.stream()
                .map(trade -> {
                    String tradeType = trade.getTradeType() == null ? "UNKNOWN" : trade.getTradeType();
                    String counterparty = trade.getCounterpartyName() == null ? "UNKNOWN" : trade.getCounterpartyName();
                    return tradeType + ":" + counterparty;
                })
                .collect(Collectors.groupingBy(key -> key, Collectors.counting()));
        summaryDTO.setTradesByTypeAndCounterparty(tradesByTypeAndCounterparty);

        summaryDTO.setRiskExposureSummary(Map.of("delta", BigDecimal.ZERO, "vega", BigDecimal.ZERO));
        return summaryDTO;
    }

    public TradeSummaryDTO getTradeSummary() {
        String currentTraderId = resolveCurrentTraderId();
        return getTradeSummary(currentTraderId);
    }

    public DailySummaryDTO getDailySummary(String traderId) {
        // Privilege validation: only users with TRADE_VIEW privilege allowed
        String currentUser = resolveCurrentTraderId();
        String currentRole = resolveCurrentUserRole();
        if (!hasPrivilege(currentUser, "TRADE_VIEW")) {
            throw new org.springframework.security.access.AccessDeniedException("Insufficient privileges");
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        String traderFilter = traderId;

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
    }

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

    // Stub for privilege validation (replace with actual logic or delegate to
    // engine)
    private boolean hasPrivilege(String user, String privilege) {
        // TODO: Replace with actual privilege validation logic
        // For now, always allow for demonstration
        return true;
    }

    // private boolean hasPrivilege(String username, String privilege) {

    // // Temporary safeguard to avoid null-pointer failures in tests or
    // misconfigured
    // // contexts.
    // // If either dependency is not injected, I allow access so we do not return
    // // false 403s.
    // if (userPrivilegeService == null || privilegeValidationEngine == null) {
    // return true;
    // }

    // // Step 1: read privileges from the database and check by user loginId and
    // // privilege name.
    // List<UserPrivilege> allPrivileges =
    // userPrivilegeService.getAllUserPrivileges();

    // boolean dbMatch = allPrivileges.stream().anyMatch(p -> p != null
    // && p.getUser() != null
    // && p.getUser().getLoginId() != null
    // && p.getPrivilege() != null
    // && p.getPrivilege().getName() != null
    // && p.getUser().getLoginId().equalsIgnoreCase(username)
    // && p.getPrivilege().getName().equalsIgnoreCase(privilege));

    // // If the user does not have this privilege in the database, fail fast.
    // if (!dbMatch) {
    // return false;
    // }

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
    // Adding this line to force commit

}
