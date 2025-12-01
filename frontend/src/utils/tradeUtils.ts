import { Trade, TradeLeg } from "./tradeTypes";
import {
  formatDateForBackend,
  formatDateTimeForBackend,
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
    // LocalDate fields - use date-only format (YYYY-MM-DD)
    tradeDate: formatDateForBackend(trade.tradeDate),
    startDate: formatDateForBackend(trade.startDate),
    maturityDate: formatDateForBackend(trade.maturityDate),
    executionDate: formatDateForBackend(trade.executionDate),
    validityStartDate: formatDateForBackend(trade.validityStartDate),
    validityEndDate: formatDateForBackend(
      (
        trade as unknown as {
          validityEndDate?: string | Date;
        }
      ).validityEndDate
    ),
    // LocalDateTime fields - use full datetime format (YYYY-MM-DDTHH:MM:SS)
    lastTouchTimestamp: formatDateTimeForBackend(trade.lastTouchTimestamp),
    // FIXED: Format individual leg dates properly for backend validation
    // Each leg needs its tradeMaturityDate formatted as LocalDate (YYYY-MM-DD)
    tradeLegs: trade.tradeLegs.map((leg) => {
      const typedLeg = leg as TradeLeg & { tradeMaturityDate?: string };
      return {
        ...leg,
        // Format tradeMaturityDate in leg if it exists (LocalDate field)
        // This ensures backend validation "Both legs must have a maturity date defined" passes
        tradeMaturityDate: typedLeg.tradeMaturityDate
          ? formatDateForBackend(typedLeg.tradeMaturityDate)
          : undefined,
      };
    }),
  };
};

// DEMO / TEST FALLBACK RATE
export const DEFAULT_FALLBACK_FLOATING_RATE = 0.03; // 3% p.a. (demo-only)
export const DEFAULT_FALLBACK_FIXED_RATE = 0.03; // 3% p.a. (demo-only)

/**
 * Validates a trade for completeness and required fields
 * @param trade - Trade to validate
 * @returns {string|null} Error message or null if valid
 */
export const validateTrade = (trade: Trade): string | null => {
  const propagationError = propagateTopLevelMaturityToLegs(trade);
  if (propagationError) return propagationError;

  if (!trade.tradeDate) return "Trade date is required.";
  if (!trade.bookName) return "Book is required.";
  if (!trade.counterpartyName) return "Counterparty is required.";
  if (
    !trade.tradeLegs ||
    !Array.isArray(trade.tradeLegs) ||
    trade.tradeLegs.length === 0
  )
    return "At least one trade leg is required.";

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
 * top-level maturity is present but does not match them, return an error.
 */
function propagateTopLevelMaturityToLegs(trade: Trade): string | null {
  const topLevelMaturity = trade.maturityDate;
  if (!topLevelMaturity) return null;
  if (!trade.tradeLegs || trade.tradeLegs.length === 0) return null;

  const nonNullLegDates: string[] = [];
  for (const leg of trade.tradeLegs) {
    const typedLeg = leg as import("./tradeTypes").TradeLeg & {
      tradeMaturityDate?: string;
    };
    const legDate = typedLeg.tradeMaturityDate as string | undefined;
    if (legDate) nonNullLegDates.push(legDate);
  }

  const uniqueLegDates = Array.from(new Set(nonNullLegDates));
  if (uniqueLegDates.length > 1) {
    const allMatchTop = uniqueLegDates.every((d) => d === topLevelMaturity);
    if (!allMatchTop) {
      return (
        "Conflict: legs have different maturity dates and the top-level maturity " +
        "does not match them. Please make leg maturities consistent or remove the top-level maturity."
      );
    }
  }

  for (const leg of trade.tradeLegs) {
    const typedLeg = leg as import("./tradeTypes").TradeLeg & {
      tradeMaturityDate?: string;
    };
    if (!typedLeg.tradeMaturityDate) {
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

          // CHANGED: keep settlement + UTI + user fields null when empty
          "utiCode",
          "settlementInstructions",
          "traderUserId",
          "tradeInputterUserId",

          // CHANGED: removed unsafe nulling of traderUserName/inputterUserName
          // because empty strings should remain empty strings (backend expects names as text)
        ];

        if (keysToNullIfEmpty.includes(key) && value === "") {
          newObj[key] = null; // CHANGED: ensure correct type for backend
        } else {
          newObj[key] = convertEmptyStringsToNull(value);
        }
      }
    }
    return newObj;
  }
  return obj;
}
