package com.technicalchallenge.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap; // ADDED: for building mutable maps during aggregation
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException; // 403 on privilege failures. I also updated the POM dependency
import org.springframework.security.core.Authentication; // read current logged-in principal
import org.springframework.security.core.GrantedAuthority; // map authorities to role
import org.springframework.security.core.context.SecurityContextHolder; // pull Authentication
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.SearchCriteriaDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO; // ADDED: used to aggregate notional by currency
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.model.UserProfile; // ADDED: for passing user role to validator
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.validation.TradeValidationResult; // ADDED: validation result container
import com.technicalchallenge.validation.UserPrivilegeValidationEngine; // ADDED: privilege validator entrypoint

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;

/*Refactored:Moved search, filter, and RSQL search logic from TradeService to this new DashboardService for separation of concerns: Keeping trade CRUD logic (TradeService) separate from analytics, search, and dashboard logic: */
@Service
@Transactional(readOnly = true)
public class TradeDashboardService {

    private final TradeMapper tradeMapper;
    private final TradeRepository tradeRepository;

    // ADDED: Injected privilege validation engine from the validation package
    private final UserPrivilegeValidationEngine privilegeValidationEngine;

    public TradeDashboardService(TradeRepository tradeRepository, TradeMapper tradeMapper,
            UserPrivilegeValidationEngine privilegeValidationEngine) {
        this.tradeRepository = tradeRepository;
        this.tradeMapper = tradeMapper;
        this.privilegeValidationEngine = privilegeValidationEngine; // ADDED
    }

    public List<TradeDTO> searchTrades(SearchCriteriaDTO criteriaDTO) {

        // Specification is an object that represents a single search/filter for the
        // database query. A blank slate for building up the search query to add
        // conditions later
        Specification<Trade> spec = Specification.where(null);

        // Filter by Counterparty
        if (criteriaDTO.getCounterparty() != null && !criteriaDTO.getCounterparty().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("counterparty").get("name")),
                    criteriaDTO.getCounterparty().toLowerCase()));
        }

        // Filter by Book
        if (criteriaDTO.getBook() != null && !criteriaDTO.getBook().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("book").get("bookName")),
                    criteriaDTO.getBook().toLowerCase()));
        }

        // Filter by Trader
        if (criteriaDTO.getTrader() != null && !criteriaDTO.getTrader().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("traderUser").get("firstName")),
                    criteriaDTO.getTrader().toLowerCase()));
        }

        // Filter by Trade Status
        if (criteriaDTO.getStatus() != null && !criteriaDTO.getStatus().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("tradeStatus").get("tradeStatus")),
                    criteriaDTO.getStatus().toLowerCase()));
        }

        // Filter by Date Range
        if (criteriaDTO.getStartDate() != null && criteriaDTO.getEndDate() != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("tradeDate"),
                    criteriaDTO.getStartDate(), criteriaDTO.getEndDate()));
        } else if (criteriaDTO.getStartDate() != null) {
            spec = spec
                    .and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("tradeDate"),
                            criteriaDTO.getStartDate()));
        } else if (criteriaDTO.getEndDate() != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("tradeDate"),
                    criteriaDTO.getEndDate()));
        }

        // Execute the query
        List<Trade> tradeEntities = tradeRepository.findAll(spec);

        // Convert Trade entities into DTOs
        List<TradeDTO> tradeDtos = new ArrayList<>();
        for (Trade tradeEntity : tradeEntities) {
            tradeDtos.add(tradeMapper.toDto(tradeEntity));
        }

        return tradeDtos;
    }

    // Identical method to the above one for pagination.
    public Page<TradeDTO> filterTrades(SearchCriteriaDTO criteriaDTO, int page, int size) {
        Specification<Trade> spec = Specification.where(null);

        if (criteriaDTO.getCounterparty() != null && !criteriaDTO.getCounterparty().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("counterparty").get("name")),
                    criteriaDTO.getCounterparty().toLowerCase()));
        }

        if (criteriaDTO.getBook() != null && !criteriaDTO.getBook().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("book").get("bookName")),
                    criteriaDTO.getBook().toLowerCase()));
        }

        if (criteriaDTO.getTrader() != null && !criteriaDTO.getTrader().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("traderUser").get("firstName")),
                    criteriaDTO.getTrader().toLowerCase()));
        }

        if (criteriaDTO.getStatus() != null && !criteriaDTO.getStatus().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("tradeStatus").get("tradeStatus")),
                    criteriaDTO.getStatus().toLowerCase()));
        }

        if (criteriaDTO.getStartDate() != null && criteriaDTO.getEndDate() != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("tradeDate"),
                    criteriaDTO.getStartDate(), criteriaDTO.getEndDate()));
        } else if (criteriaDTO.getStartDate() != null) {
            spec = spec
                    .and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("tradeDate"),
                            criteriaDTO.getStartDate()));
        } else if (criteriaDTO.getEndDate() != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("tradeDate"),
                    criteriaDTO.getEndDate()));
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<Trade> tradePage = tradeRepository.findAll(spec, pageable);

        List<TradeDTO> tradeDtoList = tradePage.getContent()
                .stream()
                .map(tradeMapper::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(tradeDtoList, pageable, tradePage.getTotalElements());
    }

    // RSQL Search
    public List<TradeDTO> searchTradesRsql(String query) {
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
    }

    // Fetch trades filtered by trader
    public List<TradeDTO> getTradesByTrader(String traderId) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserType(resolveCurrentUserRole());

        TradeDTO privilegeCheckTradeDto = new TradeDTO();
        privilegeCheckTradeDto.setAction("VIEW");

        TradeValidationResult privilegeResult = privilegeValidationEngine
                .validateUserPrivilegeBusinessRules(privilegeCheckTradeDto, userProfile);

        if (!privilegeResult.isValid()) {
            throw new AccessDeniedException(String.join(", ", privilegeResult.getErrors()));
        }

        SearchCriteriaDTO criteriaDTO = new SearchCriteriaDTO();
        criteriaDTO.setTrader(traderId);

        List<TradeDTO> traderTrades = searchTrades(criteriaDTO);
        return traderTrades;
    }

    public List<TradeDTO> getTradesByTrader() {
        String currentTraderId = resolveCurrentTraderId();
        return getTradesByTrader(currentTraderId);
    }

    // returns all trades belonging to a specific book
    public List<TradeDTO> getTradesByBook(Long bookId) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserType(resolveCurrentUserRole());

        TradeDTO privilegeCheckTradeDto = new TradeDTO();
        privilegeCheckTradeDto.setAction("VIEW");

        TradeValidationResult privilegeResult = privilegeValidationEngine
                .validateUserPrivilegeBusinessRules(privilegeCheckTradeDto, userProfile);

        if (!privilegeResult.isValid()) {
            throw new AccessDeniedException(String.join(", ", privilegeResult.getErrors()));
        }

        SearchCriteriaDTO criteriaDTO = new SearchCriteriaDTO();
        criteriaDTO.setBook(String.valueOf(bookId));

        List<TradeDTO> tradesByBook = searchTrades(criteriaDTO);
        return tradesByBook;
    }

    // Calculate totals, notionals, and risk exposure for this trader
    public TradeSummaryDTO getTradeSummary(String traderId) {
        // Create user profile and set role for privilege validation
        UserProfile userProfile = new UserProfile();
        userProfile.setUserType(resolveCurrentUserRole());

        // Prepare DTO for privilege check
        TradeDTO privilegeCheckTradeDto = new TradeDTO();
        privilegeCheckTradeDto.setAction("VIEW");

        // Validate user privileges for viewing trade summary
        TradeValidationResult privilegeResult = privilegeValidationEngine
                .validateUserPrivilegeBusinessRules(privilegeCheckTradeDto, userProfile); // Checks if the user has the
                                                                                          // necessary privileges to
                                                                                          // perform the specified
                                                                                          // action on the trade

        if (!privilegeResult.isValid()) {
            throw new AccessDeniedException(String.join(", ", privilegeResult.getErrors()));
        }

        // Initialize summary DTO to hold aggregated results
        TradeSummaryDTO summaryDTO = new TradeSummaryDTO();

        // Retrieve trades for the specified trader
        List<TradeDTO> tradesForTrader = getTradesByTrader(traderId);

        // Aggregate trade counts by status (e.g., NEW, CANCELLED)
        Map<String, Long> tradesByStatus = tradesForTrader.stream()
                .map(TradeDTO::getTradeStatus)
                .filter(status -> status != null && !status.isBlank())
                .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
        summaryDTO.setTradesByStatus(tradesByStatus);

        // Sum notional amounts per currency across all trade legs
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

        // Group trades by type and counterparty for summary statistics
        Map<String, Long> tradesByTypeAndCounterparty = tradesForTrader.stream()
                .map(trade -> {
                    String tradeType = trade.getTradeType() == null ? "UNKNOWN" : trade.getTradeType();
                    String counterparty = trade.getCounterpartyName() == null ? "UNKNOWN" : trade.getCounterpartyName();
                    return tradeType + ":" + counterparty;
                })
                .collect(Collectors.groupingBy(key -> key, Collectors.counting()));
        summaryDTO.setTradesByTypeAndCounterparty(tradesByTypeAndCounterparty);

        // Placeholder for future risk metrics integration
        summaryDTO.setRiskExposureSummary(Map.of("delta", BigDecimal.ZERO, "vega", BigDecimal.ZERO));

        return summaryDTO;
    }

    public TradeSummaryDTO getTradeSummary() {
        String currentTraderId = resolveCurrentTraderId();
        return getTradeSummary(currentTraderId);
    }

    public DailySummaryDTO getDailySummary(String traderId) {
        // Create user profile and set role for privilege validation
        UserProfile userProfile = new UserProfile();
        userProfile.setUserType(resolveCurrentUserRole());

        // Prepare DTO for privilege check
        TradeDTO privilegeCheckTradeDto = new TradeDTO();
        privilegeCheckTradeDto.setAction("VIEW");

        // Validate user privileges for viewing daily summary
        TradeValidationResult privilegeResult = privilegeValidationEngine
                .validateUserPrivilegeBusinessRules(privilegeCheckTradeDto, userProfile);

        if (!privilegeResult.isValid()) {
            throw new AccessDeniedException(String.join(", ", privilegeResult.getErrors()));
        }

        // Set up date filters for today and yesterday
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        String traderFilter = traderId;

        // Build specification for today's trades
        Specification<Trade> todaySpec = (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(criteriaBuilder.lower(root.get("traderUser").get("firstName")),
                        traderFilter.toLowerCase()),
                criteriaBuilder.equal(root.get("tradeDate"), today));

        // Retrieve and map today's trades
        List<Trade> todayTradeEntities = tradeRepository.findAll(todaySpec);
        List<TradeDTO> todayTradeDtos = todayTradeEntities.stream().map(tradeMapper::toDto).toList();

        // Build specification for yesterday's trades
        Specification<Trade> yesterdaySpec = (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(criteriaBuilder.lower(root.get("traderUser").get("firstName")),
                        traderFilter.toLowerCase()),
                criteriaBuilder.equal(root.get("tradeDate"), yesterday));

        // Retrieve and map yesterday's trades
        List<Trade> yesterdayTradeEntities = tradeRepository.findAll(yesterdaySpec);
        List<TradeDTO> yesterdayTradeDtos = yesterdayTradeEntities.stream().map(tradeMapper::toDto).toList();

        // Initialize summary DTO for daily statistics
        DailySummaryDTO summaryDto = new DailySummaryDTO();

        // Set today's trade count and notional by currency
        summaryDto.setTodaysTradeCount(todayTradeDtos.size());
        summaryDto.setTodaysNotionalByCurrency(sumNotionalByCurrency(todayTradeDtos));

        // Calculate and set user performance metrics
        Map<String, Object> performanceMetrics = new HashMap<>();
        performanceMetrics.put("tradeCount", todayTradeDtos.size());
        performanceMetrics.put("notionalCcyCount", summaryDto.getTodaysNotionalByCurrency().size());
        summaryDto.setUserPerformanceMetrics(performanceMetrics);

        // Aggregate book activity summaries for today's trades
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

        // Build and set yesterday's comparison summary
        DailySummaryDTO.DailyComparisonSummary yesterdaySummary = new DailySummaryDTO.DailyComparisonSummary();
        yesterdaySummary.setTradeCount(yesterdayTradeDtos.size());
        yesterdaySummary.setNotionalByCurrency(sumNotionalByCurrency(yesterdayTradeDtos));

        // Store historical comparison for reporting
        Map<String, DailySummaryDTO.DailyComparisonSummary> historicalComparison = new HashMap<>();
        historicalComparison.put(yesterday.toString(), yesterdaySummary);
        summaryDto.setHistoricalComparisons(historicalComparison);

        return summaryDto;
    }

    public DailySummaryDTO getDailySummary() {
        String currentTraderId = resolveCurrentTraderId();
        return getDailySummary(currentTraderId);
    }

    // ADDED: helper methods.
    // Retrieve the ID (typically the username) of the
    // currently authenticated trader from the Spring Security context.If
    // authentication is present and the user's name is available, it returns that
    // name as the trader, otherwise unknown

    private String resolveCurrentTraderId() {
        Authentication authentication = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()// determine which user is making the request
                                                                        // and what permissions they have
                : null;
        if (authentication == null || authentication.getName() == null) {
            return "__UNKNOWN_TRADER__";
        }
        return authentication.getName();
    }

    private String resolveCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        if (authentication != null && authentication.getAuthorities() != null
                && !authentication.getAuthorities().isEmpty()) {
            String authority = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("TRADER");
            if (authority.endsWith("TRADER"))
                return "TRADER";
            if (authority.endsWith("SALES"))
                return "SALES";
            if (authority.endsWith("MIDDLE_OFFICE"))
                return "MIDDLE_OFFICE";
            if (authority.endsWith("SUPPORT"))
                return "SUPPORT";
            return authority;
        }
        return "TRADER";
    }

    // Calculate the total notional amounts for each currency across a list of
    // trades
    private Map<String, BigDecimal> sumNotionalByCurrency(List<TradeDTO> trades) {
        Map<String, BigDecimal> notionalTotalsByCurrency = new HashMap<>();
        if (trades == null)
            return notionalTotalsByCurrency;

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
                // Add the notional amount for each currency to the running total in the map. If
                // the currency already exists in the map, it adds the new notional to the
                // existing value; if not, it creates a new entry.
                notionalTotalsByCurrency.merge(currency, notional, BigDecimal::add);
            }
        }
        return notionalTotalsByCurrency;
    }
}
