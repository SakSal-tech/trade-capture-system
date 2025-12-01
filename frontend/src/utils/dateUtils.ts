/**
 * Date utilities for trade operations
 */

/**
 * Get today's date in yyyy-mm-dd format
 * @returns {string} Today's date as yyyy-mm-dd
 */
export const getToday = (): string => {
  const d = new Date();
  return d.toISOString().slice(0, 10);
};

/**
 * Get a date one year from today in yyyy-mm-dd format
 * @returns {string} Date one year from today as yyyy-mm-dd
 */
export const getOneYearFromToday = (): string => {
  const d = new Date();
  d.setFullYear(d.getFullYear() + 1);
  return d.toISOString().slice(0, 10);
};

/**
 * Format a date for backend API (LocalDate fields - date only)
 * @param d - Date to format
 * @returns {string|null} Formatted date string or null
 */
export const formatDateForBackend = (
  d: string | Date | undefined | null
): string | null => {
  if (!d) return null;

  if (typeof d === "string") {
    if (d.includes("T")) {
      // Extract just the date part for LocalDate fields
      return d.split("T")[0];
    }
    return d; // Already in YYYY-MM-DD format
  }

  if (d instanceof Date) {
    // Format as date-only for LocalDate fields
    const pad = (n: number) => String(n).padStart(2, "0");
    const year = d.getFullYear();
    const month = pad(d.getMonth() + 1);
    const day = pad(d.getDate());
    return `${year}-${month}-${day}`;
  }

  return null;
};

/**
 * Format a datetime for backend API (LocalDateTime fields - full datetime)
 * @param d - Date to format
 * @returns {string|null} Formatted datetime string or null
 */
export const formatDateTimeForBackend = (
  d: string | Date | undefined | null
): string | null => {
  if (!d) return null;

  if (typeof d === "string") {
    if (d.includes("T")) {
      // Ensure seconds are included for LocalDateTime fields
      const [datePart, timePart] = d.split("T");
      const time = timePart
        ? timePart.split(":").slice(0, 2).join(":") + ":00"
        : "00:00:00";
      return `${datePart}T${time}`;
    }
    // Convert date-only string to datetime with midnight time
    return `${d}T00:00:00`;
  }

  if (d instanceof Date) {
    // Include seconds for complete LocalDateTime compatibility
    const pad = (n: number) => String(n).padStart(2, "0");
    const year = d.getFullYear();
    const month = pad(d.getMonth() + 1);
    const day = pad(d.getDate());
    const hours = pad(d.getHours());
    const minutes = pad(d.getMinutes());
    const seconds = pad(d.getSeconds());
    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
  }

  return null;
};

interface TradeData {
  tradeDate?: string;
  startDate?: string;
  maturityDate?: string;
  executionDate?: string;
  lastTouchTimestamp?: string;
  validityStartDate?: string;
  [key: string]: unknown; // Allow for additional properties
}

/**
 * Format dates from backend API response for display
 * @param trade - Trade data from backend
 * @returns Trade with properly formatted dates
 */
export const formatDatesFromBackend = (trade: TradeData): TradeData => {
  // Convert any full ISO date strings (with time) to just the date portion (YYYY-MM-DD)
  if (trade.tradeDate && (trade.tradeDate as string).includes("T")) {
    trade.tradeDate = (trade.tradeDate as string).split("T")[0];
  }
  if (trade.startDate && (trade.startDate as string).includes("T")) {
    trade.startDate = (trade.startDate as string).split("T")[0];
  }
  if (trade.maturityDate && (trade.maturityDate as string).includes("T")) {
    trade.maturityDate = (trade.maturityDate as string).split("T")[0];
  }
  if (trade.executionDate && (trade.executionDate as string).includes("T")) {
    trade.executionDate = (trade.executionDate as string).split("T")[0];
  }
  if (
    trade.lastTouchTimestamp &&
    (trade.lastTouchTimestamp as string).includes("T")
  ) {
    trade.lastTouchTimestamp = (trade.lastTouchTimestamp as string).split(
      "T"
    )[0];
  }
  if (
    trade.validityStartDate &&
    (trade.validityStartDate as string).includes("T")
  ) {
    trade.validityStartDate = (trade.validityStartDate as string).split("T")[0];
  }
  const endDate = (trade as TradeData & { validityEndDate?: string })
    .validityEndDate;
  if (endDate && endDate.includes("T")) {
    (trade as TradeData & { validityEndDate?: string }).validityEndDate =
      endDate.split("T")[0];
  }
  return trade;
};
