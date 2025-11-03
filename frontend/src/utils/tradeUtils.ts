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

/**
 * Validates a trade for completeness and required fields
 * @param trade - Trade to validate
 * @returns {string|null} Error message or null if valid
 */
export const validateTrade = (trade: Trade): string | null => {
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
        // the frontend leaves them as empty strings. If you see unexpected
        // 400 validation errors, check this list and narrow it so required
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
