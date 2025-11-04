import { Trade } from "./tradeTypes";
import {
  formatDateForBackend,
  getOneYearFromToday,
  getToday,
} from "./dateUtils";

/**
 * Creates a default trade object with basic values
 * @returns {Trade} Default trade with empty values and two default legs
 */
export const getDefaultTrade = (): Trade =>
  JSON.parse(
    JSON.stringify({
      tradeId: "",
      version: "",
      bookName: "",
      counterpartyName: "",
      traderUserName: "",
      inputterUserName: "",
      tradeType: "Swap",
      tradeSubType: "",
      tradeStatus: "",
      tradeDate: getToday(),
      startDate: getToday(),
      maturityDate: getOneYearFromToday(),
      executionDate: getToday(),
      utiCode: "",
      lastTouchTimestamp: getToday(),
      validityStartDate: getToday(),
      tradeLegs: [
        {
          legId: "",
          legType: "Fixed",
          notional: 1000000,
          currency: "USD",
          rate: "1.0",
          index: "",
          calculationPeriodSchedule: "Quarterly",
          paymentBusinessDayConvention: "Modified Following",
          payReceiveFlag: "Pay",
        },
        {
          legId: "",
          legType: "Floating",
          notional: 1000000,
          currency: "USD",
          rate: "",
          index: "LIBOR",
          calculationPeriodSchedule: "Quarterly",
          paymentBusinessDayConvention: "Modified Following",
          payReceiveFlag: "Receive",
        },
      ],
    })
  );

/**
 * Format trade for backend API
 * @param trade - Trade to format
 * @returns {Object} Formatted trade with proper date formats
 */
export const formatTradeForBackend = (
  trade: Trade
): Record<string, unknown> => {
  return {
    ...trade,
    tradeDate: formatDateForBackend(trade.tradeDate),
    startDate: formatDateForBackend(trade.startDate),
    maturityDate: formatDateForBackend(trade.maturityDate),
    executionDate: formatDateForBackend(trade.executionDate),
    lastTouchTimestamp: formatDateForBackend(trade.lastTouchTimestamp),
    validityStartDate: formatDateForBackend(trade.validityStartDate),
    validityEndDate: formatDateForBackend(
      (
        trade as unknown as {
          validityEndDate?: string | Date;
        }
      ).validityEndDate
    ),
    tradeLegs: trade.tradeLegs.map((leg) => ({
      ...leg,
    })),
  };
};

// DEMO / TEST FALLBACK RATE
// A realistic demo rate used only for local testing and demos when a
// floating leg is left without an explicit rate. This should be adjusted
// or removed when a MarketData/RateProvider is integrated.
export const DEFAULT_FALLBACK_FLOATING_RATE = 0.03; // 3% p.a. (demo-only)
// DEMO / TEST FALLBACK RATE FOR FIXED LEGS
// When a Fixed leg rate is left empty in the UI during demos/tests, use
// this default so cashflow generation and save flows produce numeric values.
// This is demo-only and should be removed or replaced by a proper
// MarketData/RateProvider in production.
export const DEFAULT_FALLBACK_FIXED_RATE = 0.03; // 3% p.a. (demo-only)

/**
 * Validates a trade for completeness and required fields
 * @param trade - Trade to validate
 * @returns {string|null} Error message or null if valid
 */
export const validateTrade = (trade: Trade): string | null => {
  // Propagate top-level maturity into legs
  // Rationale: backend validators require per-leg maturities. For convenience
  // when the user sets the top-level maturity, copy it into any leg that
  // lacks a maturity. If legs differ among themselves and the top-level
  // maturity is present but doesn't match them, return a clear error so the
  // caller can show a 400-like validation message.
  const propagationError = propagateTopLevelMaturityToLegs(trade);
  if (propagationError) return propagationError;

  // Backend-required fields (tradeStatus is NOT required)
  if (!trade.tradeDate) return "Trade date is required.";
  if (!trade.bookName) return "Book is required.";
  if (!trade.counterpartyName) return "Counterparty is required.";
  if (
    !trade.tradeLegs ||
    !Array.isArray(trade.tradeLegs) ||
    trade.tradeLegs.length === 0
  )
    return "At least one trade leg is required.";

  // Validate all trade legs (basic presence)
  for (let i = 0; i < trade.tradeLegs.length; i++) {
    const leg = trade.tradeLegs[i];
    if (!leg.legType) return `Leg ${i + 1}: Leg Type is required.`;
    if (!leg.notional) return `Leg ${i + 1}: Notional is required.`;
    if (!leg.currency) return `Leg ${i + 1}: Currency is required.`;
    if (!leg.calculationPeriodSchedule)
      return `Leg ${i + 1}: Payment Frequency is required.`;
    if (!leg.paymentBusinessDayConvention)
      return `Leg ${i + 1}: Payment BDC is required.`;
    if (!leg.payReceiveFlag) return `Leg ${i + 1}: Pay/Rec is required.`;
    if (
      leg.legType === "Fixed" &&
      (leg.rate === undefined || leg.rate === null || leg.rate === "")
    )
      return `Leg ${i + 1}: Fixed Rate is required for Fixed leg.`;
    if (leg.legType === "Floating" && (!leg.index || leg.index === ""))
      return `Leg ${i + 1}: Floating Rate Index is required for Floating leg.`;
  }
  return null;
};

/**
 * If trade.maturityDate exists, copy it into any leg that lacks a
 * `tradeMaturityDate`. If legs contain differing maturity dates and the
 * top-level maturity is present but does not match them, return an error
 * message describing the conflict. Otherwise return null. This keeps the
 * front-end convenience behaviour local and predictable for validators.
 */
function propagateTopLevelMaturityToLegs(trade: Trade): string | null {
  // `topLevelMaturity` is the trade-level maturity date. When present we
  // use it as the source of truth to populate any per-leg `tradeMaturityDate`
  // values that are missing. We keep per-leg values if they already exist.
  const topLevelMaturity = trade.maturityDate;
  if (!topLevelMaturity) {
    // nothing to propagate
    return null;
  }
  if (!trade.tradeLegs || trade.tradeLegs.length === 0) return null;

  // collect non-null leg maturities
  const nonNullLegDates: string[] = [];
  for (const leg of trade.tradeLegs) {
    const typedLeg = leg as import("./tradeTypes").TradeLeg & {
      tradeMaturityDate?: string;
    };
    const legDate = typedLeg.tradeMaturityDate as string | undefined;
    if (legDate) nonNullLegDates.push(legDate);
  }

  // If legs have differing maturities among themselves and top-level present
  // but does not match them, treat as conflict and fail early.
  const uniqueLegDates = Array.from(new Set(nonNullLegDates));
  if (uniqueLegDates.length > 1) {
    // legs differ — check if top matches all existing leg dates
    const allMatchTop = uniqueLegDates.every((d) => d === topLevelMaturity);
    if (!allMatchTop) {
      return (
        "Conflict: legs have different maturity dates and the top-level maturity " +
        "does not match them. Please make leg maturities consistent or remove the top-level maturity."
      );
    }
  }

  // Propagate top-level maturity into any leg missing it (keep existing ones)
  for (const leg of trade.tradeLegs) {
    const typedLeg = leg as import("./tradeTypes").TradeLeg & {
      tradeMaturityDate?: string;
    };
    if (!typedLeg.tradeMaturityDate) {
      // Add per-leg maturity for backend DTO compatibility
      typedLeg.tradeMaturityDate = topLevelMaturity;
    }
  }
  return null;
}

/**
 * Recursively convert empty strings to null for numeric/date/enum fields
 * @param obj - Object to convert
 * @returns {Object} Converted object
 */
export function convertEmptyStringsToNull(
  obj: Record<string, unknown> | unknown[] | unknown
): Record<string, unknown> | unknown[] | unknown {
  if (Array.isArray(obj)) {
    return obj.map(convertEmptyStringsToNull);
  } else if (obj && typeof obj === "object") {
    const newObj: Record<string, unknown> = {};
    for (const key in obj as Record<string, unknown>) {
      if (Object.prototype.hasOwnProperty.call(obj, key)) {
        const value = (obj as Record<string, unknown>)[key];
        // DEV: Convert empty strings for `utiCode`, settlement and user id/name
        // fields to null here so the backend can auto-generate UTI and
        // receive nulls instead of empty strings. See docs/DeveloperLog-30-10-2025-detailed.md
        // CHANGED: This list was expanded to include a number of date/user
        // fields during the settlement integration work. Beware: this
        // conversion is broad and can convert required fields to `null` if
        // fields (e.g. tradeDate, tradeLegs[].notional) are not nulled.
        // List of fields that should be null if empty string (including enums)
        // NOTE: include UTI, settlement and common user/id fields so the
        // backend receives `null` (allowing auto-generation) rather than
        // an empty string which some endpoints treat as a value.
        const keysToNullIfEmpty = [
          "tradeId",
          "version",
          "legId",
          "rate",
          "notional",
          "id",
          "tradeDate",
          "startDate",
          "maturityDate",
          "executionDate",
          "lastTouchTimestamp",
          "validityStartDate",
          "validityEndDate",
          "paymentValue",
          "valueDate",
          "tradeStatus",
          "index",
          "tradeType",
          "tradeSubType",
          // Added fields to ensure empty strings are converted to null
          "utiCode", // CHANGED: ensure empty UTI becomes null for backend auto-gen
          "settlementInstructions", // CHANGED: added settlement field to null-conversion
          "traderUserId", // CHANGED: include trader id so empty string -> null
          "tradeInputterUserId", // CHANGED: include inputter id so empty string -> null
          "traderUserName",
          "inputterUserName",
        ];
        if (keysToNullIfEmpty.includes(key) && value === "") {
          newObj[key] = null;
        } else {
          newObj[key] = convertEmptyStringsToNull(value);
        }
      }
    }
    return newObj;
  }
  return obj;
}
