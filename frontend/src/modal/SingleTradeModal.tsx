import React from "react";
import Button from "../components/Button";
import { Trade, TradeLeg } from "../utils/tradeTypes";
import CashflowModal from "./CashflowModal";
import api from "../utils/api";
import { CashflowDTO } from "../utils/tradeTypes";
import Snackbar from "../components/Snackbar";
import TradeDetails from "../components/TradeDetails";
import TradeLegDetails from "../components/TradeLegDetails";
import {
  getDefaultTrade,
  validateTrade,
  formatTradeForBackend,
  convertEmptyStringsToNull,
} from "../utils/tradeUtils";
import {
  formatDatesFromBackend,
  formatDateForBackend,
} from "../utils/dateUtils";
import LoadingSpinner from "../components/LoadingSpinner";
import userStore from "../stores/userStore";
import staticStore from "../stores/staticStore";
import {
  DEFAULT_FALLBACK_FLOATING_RATE,
  DEFAULT_FALLBACK_FIXED_RATE,
} from "../utils/tradeUtils";
// REFACTOR SUMMARY:
// This file was updated to integrate settlement instructions into the
// main trade save flow. Key reasons for the edits:
// - Make settlement persistence a parent-controlled responsibility
//   (saveSettlement prop) so the UI component remains UI-only and the
//   parent can orchestrate saving trade + settlement together.
// - Save settlement asynchronously (non-blocking) after the trade is
//   created/updated so a missing or failing settlement save does not
//   prevent the trade from being stored. Failures set `settlementUnsaved`
//   so the user can retry.
// - Add defensive DTO assignments and extra debug logging to help
//   diagnose missing backend fields (e.g., `uti_code`, `trader_user_id`) and
//   to coerce string ids into numbers before sending to the backend.
// See developer log entry: docs/DeveloperLog-30-10-2025-detailed.md

/**
 * Props for SingleTradeModal component
 */
interface SingleTradeModalProps {
  mode: "view" | "edit";
  trade?: Trade;
  isOpen: boolean;
  onClear?: () => void;
  // REFACTOR: These optional props were added to integrate settlement
  // persistence into the main trade save flow.
  // - `settlement`: the current settlement text held by the parent UI.
  // - `saveSettlement`: a parent-provided function that persists settlement
  //    (this centralises network logic in the parent). When the trade is
  //    created or updated the component will call `saveSettlement` so a
  //    single "Save Trade" action results in both the trade entity and
  //    its settlement being persisted.
  settlement?: string;
  saveSettlement?: (tradeId: string, text: string) => Promise<void>;
}

/**
 * Modal component for viewing, editing and managing a single trade
 */
export const SingleTradeModal: React.FC<SingleTradeModalProps> = (props) => {
  const [editableTrade, setEditableTrade] = React.useState<Trade | undefined>(
    props.trade ?? getDefaultTrade()
  );
  const [cashflowModalOpen, setCashflowModalOpen] = React.useState(false);
  const [generatedCashflows, setGeneratedCashflows] = React.useState<
    CashflowDTO[]
  >([]);
  const [snackbarOpen, setSnackbarOpen] = React.useState(false);
  const [snackbarMsg, setSnackbarMsg] = React.useState("");
  const [snackbarType, setSnackbarType] = React.useState<"success" | "error">(
    "success"
  );
  const [loading, setLoading] = React.useState(false);
  // Tracks whether the settlement upsert failed and requires a retry.
  const [settlementUnsaved, setSettlementUnsaved] = React.useState(false);

  React.useEffect(() => {
    setEditableTrade(props.trade ?? getDefaultTrade());
    setCashflowModalOpen(false);
    setGeneratedCashflows([]);
    setSnackbarOpen(false);
    setSnackbarMsg("");
    setSnackbarType("success");
  }, [props.trade, props.isOpen]);

  // Refactor: save settlement asynchronously after trade save so trade is not
  // blocked when settlement is optional. This triggers a retry UI state on
  // failure rather than preventing the trade save.
  const saveSettlementAsync = async (tradeId: string, text: string) => {
    // Check whether the parent provided a saveSettlement function on props
    if (!props.saveSettlement) return;
    // Log for debugging so we can confirm the async save was initiated
    console.debug("Initiating async settlement save", { tradeId, text });
    try {
      await props.saveSettlement(tradeId, text); //Calls the parent-provided saveSettlement function with the trade id and tex
      // If previously marked unsaved, clear that state and inform user
      if (settlementUnsaved) setSettlementUnsaved(false);
      setSnackbarMsg((prevMsg) =>
        prevMsg ? prevMsg + " (Settlement saved)" : "Settlement saved"
      );
      setSnackbarType("success");
      setSnackbarOpen(true);
    } catch (err) {
      console.error("Async settlement save failed", err);
      setSettlementUnsaved(true);
      // Extract useful message from axios error if present
      let detail = "";
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const anyErr: any = err;
      if (anyErr && anyErr.message) detail = String(anyErr.message);
      if (anyErr && anyErr.response && anyErr.response.data) {
        try {
          detail = JSON.stringify(anyErr.response.data);
        } catch {
          detail = String(anyErr.response.data);
        }
      }
      setSnackbarMsg((prevMsg) =>
        prevMsg
          ? prevMsg + " (Settlement save failed: " + detail + ")"
          : "Settlement save failed: " + detail
      );
      setSnackbarType("error");
      setSnackbarOpen(true);
    }
  };

  /**
   * Handles field changes in the trade header
   */
  const handleFieldChange = (key: keyof Trade, value: unknown) => {
    setEditableTrade((prev) =>
      prev
        ? {
            ...prev,
            [key]: value,
          }
        : prev
    );
  };

  /**
   * Generates cashflows for the current trade
   */
  const generateCashflows = async () => {
    setLoading(true);
    if (!editableTrade) return;
    try {
      // Helper: sanitize numeric strings by removing commas/whitespace
      const sanitizeNumber = (v: number | string | undefined) => {
        if (v === undefined || v === null) return undefined;
        if (typeof v === "number") return v;
        const cleaned = String(v).replace(/[,\s]/g, "");
        const parsed = parseFloat(cleaned);
        return Number.isNaN(parsed) ? undefined : parsed;
      };

      const legsDto = editableTrade.tradeLegs.map((leg) => {
        // Sanitize incoming rate values. If the leg is Floating and no
        // explicit rate is provided by the UI (common when using index
        // fixings externally), inject a demo fallback so the backend
        // produces numeric cashflows for demo/test purposes.
        const sanitizedRate = sanitizeNumber(leg.rate) as number | undefined;
        let rateForDto: number | undefined;
        if (sanitizedRate !== undefined && sanitizedRate !== null) {
          rateForDto = sanitizedRate;
        } else {
          const lt = (leg.legType || "").toLowerCase();
          if (lt === "floating") rateForDto = DEFAULT_FALLBACK_FLOATING_RATE;
          else if (lt === "fixed") rateForDto = DEFAULT_FALLBACK_FIXED_RATE;
          else rateForDto = undefined;
        }

        return {
          legType: leg.legType,
          notional: sanitizeNumber(leg.notional) as number,
          rate: rateForDto as number | undefined,
          index: leg.index,
          calculationPeriodSchedule: leg.calculationPeriodSchedule,
          paymentBusinessDayConvention: leg.paymentBusinessDayConvention,
          payReceiveFlag: leg.payReceiveFlag,
        };
      });

      const response = await api.post("/cashflows/generate", {
        legs: legsDto,
        // Use normalized backend date format so generation logic receives
        // consistent date-times (YYYY-MM-DDTHH:MM). This avoids timezone
        // or format interpretation differences that can shift schedule
        // dates on the server.
        tradeStartDate: formatDateForBackend(editableTrade.startDate),
        tradeMaturityDate: formatDateForBackend(editableTrade.maturityDate),
      });

      const allCashflows: CashflowDTO[] = response.data;

      const updatedLegs = editableTrade.tradeLegs.map((leg) => ({
        ...leg,
        cashflows: allCashflows.filter(
          (cf) =>
            cf.payRec?.toLowerCase() ===
              (leg.payReceiveFlag || "").toLowerCase() &&
            cf.paymentType?.toLowerCase() === (leg.legType || "").toLowerCase()
        ),
      }));

      setEditableTrade((trade) =>
        trade ? { ...trade, tradeLegs: updatedLegs } : trade
      );
      setGeneratedCashflows(allCashflows);
      setCashflowModalOpen(true);
    } catch (err) {
      console.error("Cashflow generation failed", err);
      setSnackbarMsg("Failed to generate cashflows");
      setSnackbarType("error");
      setSnackbarOpen(true);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handles field changes in trade legs
   */
  const handleLegFieldChange = (
    legIdx: number,
    key: keyof TradeLeg,
    value: unknown
  ) => {
    setEditableTrade((prev) => {
      if (!prev) return prev;
      // Always update the full array, not a slice
      const updatedLegs = prev.tradeLegs.map((leg, idx) =>
        idx === legIdx ? { ...leg, [key]: value } : leg
      );
      return {
        ...prev,
        tradeLegs: updatedLegs,
      };
    });
  };

  /**
   * Handles saving the trade
   */
  const handleSaveTrade = async () => {
    setLoading(true);
    if (!editableTrade) return;
    // Auto-generate UTI if missing
    if (!editableTrade.utiCode || String(editableTrade.utiCode).trim() === "") {
      const datePart = new Date().toISOString().split("T")[0].replace(/-/g, "");
      const randomPart = Math.floor(Math.random() * 10000)
        .toString()
        .padStart(4, "0");
      const generatedUti = `UTI-${datePart}-${randomPart}`;
      // Update UI state so generated UTI is visible to the user
      setEditableTrade((prev) =>
        prev ? { ...prev, utiCode: generatedUti } : prev
      );
    }
    // Inject demo default fixed rates into the editable trade before
    // validation so the save flow and backend validators receive a
    // populated Fixed rate. This mutation is intentionally local and
    // only applied when the UI left the Fixed rate empty. The default
    // is demo/test-only and should be removed once a MarketData/RateProvider
    // is available.
    const tradeWithDefaults: Trade = {
      ...(editableTrade as Trade),
      tradeLegs: (editableTrade.tradeLegs || []).map((leg) => {
        const legType = (leg.legType || "").toLowerCase();
        // If Fixed leg lacks a rate, set the demo fixed fallback as a string
        // to keep the UI representation consistent with existing defaults.
        if (
          legType === "fixed" &&
          (leg.rate === undefined || leg.rate === null || leg.rate === "")
        ) {
          return { ...leg, rate: String(DEFAULT_FALLBACK_FIXED_RATE) };
        }
        return { ...leg };
      }),
    };
    // Persist the injected defaults into UI state so the user sees them
    // before we validate/save.
    setEditableTrade(tradeWithDefaults);

    const validationError = validateTrade(tradeWithDefaults);
    if (validationError) {
      setSnackbarMsg(validationError);
      setSnackbarType("error");
      setSnackbarOpen(true);
      return;
    }

    // Build DTO and normalise empty strings to null. The converter function
    // returns a union that can include `unknown`, so cast the result to the
    // expected Record shape for the API call. This is a pragmatic runtime
    // safety measure; the backend expects these fields to be null rather
    // than empty strings.
    let tradeDto: Record<string, unknown> =
      formatTradeForBackend(editableTrade);
    tradeDto = convertEmptyStringsToNull(tradeDto) as Record<string, unknown>;
    // Prepared DTO for saving we do not inject additional derived
    // fields here to avoid sending incorrectly-typed values (e.g. sending
    // a username where the backend expects a numeric traderUserId). The
    // backend's DTO expects `traderUserId` (Long) and `traderUserName`
    // (String); the UI populates `traderUserName` via the dropdown. If the
    // backend requires a numeric user id then the mapping should come from
    // a proper id/value lookup (server-supplied) rather than attempting to
    // infer it here. Keep the payload minimal and predictable.
    console.debug("Prepared trade DTO for save:", tradeDto);

    // DEV: Defensive DTO coercion and UTI handling. Use safe numeric parsing
    // and explicit null fallbacks to avoid sending NaN or unexpected types
    // to the backend which can cause ownership checks to fail.
    const editable: Trade = editableTrade as Trade;
    const dto: Record<string, unknown> = tradeDto as Record<string, unknown>;

    const toNumberOrNull = (v: unknown) => {
      if (v === undefined || v === null || v === "") return null;
      const n = Number(v);
      return Number.isFinite(n) ? n : null;
    };

    // CHANGED: introduced safe numeric coercion helper above to avoid sending
    // NaN or non-numeric values to the backend. Previously code used Number()
    // directly which could produce NaN and trigger server-side errors.

    // UTI: prefer a non-empty UTI from the UI; otherwise keep existing DTO
    // value or null so the backend can auto-generate where appropriate.
    dto.utiCode =
      editable?.utiCode && String(editable.utiCode).trim() !== ""
        ? editable.utiCode
        : dto.utiCode ?? null;

    // Helper: resolve a user id either from an explicit id, a displayed name
    // (mapped via staticStore.userValues), or fallback to null.
    const resolveUserId = (idField: unknown, nameField: unknown) => {
      // 1) explicit id (string or number)
      const explicit = toNumberOrNull(idField);
      if (explicit !== null) return explicit;

      // 2) try mapping from displayed name using staticStore.userValues
      if (nameField) {
        const nameStr = String(nameField);
        const match = staticStore.userValues.find(
          (u) => String(u.label) === nameStr || String(u.value) === nameStr
        );
        const mapped = match ? toNumberOrNull(match.value) : null;
        if (mapped !== null) return mapped;
      }

      return null;
    };

    // CHANGED: resolveUserId tries (1) explicit numeric id, (2) mapping from
    // displayed name via staticStore.userValues. This avoids sending login
    // names in numeric id fields and keeps backend DTO types consistent.

    // Refactored to fix 400 error: If the UI accidentally put a non-numeric login
    // string into the id fields (e.g. editable.traderUserId === "sakhiya"),
    // move that string into the corresponding name field and ensure the id
    // field is null so the JSON types match the backend DTO (Long vs String).
    // We add explicit per-line comments below to make the intent clear.
    if (
      typeof editable?.traderUserId === "string" &&
      toNumberOrNull(editable.traderUserId) === null
    ) {
      // Move non-numeric trader id string into traderUserName so backend will
      // see it as a name (String) rather than try to parse it as a Long.
      dto.traderUserName = dto.traderUserName ?? String(editable.traderUserId); // set name if missing
      dto.traderUserId = null; // clear numeric id so Spring doesn't try to deserialize a string  // CHANGED: moved non-numeric id to name to preserve backend types
    }
    if (
      typeof editable?.tradeInputterUserId === "string" &&
      toNumberOrNull(editable.tradeInputterUserId) === null
    ) {
      // Move non-numeric inputter id string into inputterUserName similarly.
      dto.inputterUserName =
        dto.inputterUserName ?? String(editable.tradeInputterUserId); // set name fallback
      dto.tradeInputterUserId = null; // ensure numeric id is null  // CHANGED: moved non-numeric id to inputterUserName to avoid type mismatch
    }

    // Resolve trader and inputter ids safely
    // CHANGED: assign resolved numeric ids into the DTO. We intentionally
    // avoid overwriting name fields here; names are sent via separate fields
    // (`traderUserName` / `inputterUserName`) when the UI uses them.
    dto.traderUserId =
      resolveUserId(editable?.traderUserId, editable?.traderUserName) ??
      dto.traderUserId ??
      null; // CHANGED: ensure traderUserId is numeric or null before send
    dto.tradeInputterUserId =
      resolveUserId(
        editable?.tradeInputterUserId,
        editable?.inputterUserName
      ) ??
      dto.tradeInputterUserId ??
      null;

    // FALLBACK: auto-assign from currently authenticated user ONLY when
    // the values are still null. Ensure the current user id is numeric.
    try {
      const currentUser = (userStore && userStore.user) || null;
      // CHANGED: Auto-assign numeric user ids from the authenticated user
      // only when still missing. This ensures the DTO includes numeric ids
      // (Long) rather than login strings. We also populate username fallbacks
      // for display where available.
      if (
        (dto.traderUserId === undefined || dto.traderUserId === null) &&
        currentUser &&
        toNumberOrNull(currentUser.id) !== null
      ) {
        dto.traderUserId = toNumberOrNull(currentUser.id);
        dto.traderUserName =
          dto.traderUserName ??
          String(currentUser.loginId ?? currentUser.loginId ?? "");
        console.debug(
          "Auto-assigned traderUserId from current user",
          dto.traderUserId
        );
        // CHANGED: auto-fill trader id from authenticated user when missing
      }
      if (
        (dto.tradeInputterUserId === undefined ||
          dto.tradeInputterUserId === null) &&
        currentUser &&
        toNumberOrNull(currentUser.id) !== null
      ) {
        dto.tradeInputterUserId = toNumberOrNull(currentUser.id);
        dto.inputterUserName =
          dto.inputterUserName ??
          String(currentUser.loginId ?? currentUser.loginId ?? "");
        console.debug(
          "Auto-assigned tradeInputterUserId from current user",
          dto.tradeInputterUserId
        );
        // CHANGED: auto-fill inputter id from authenticated user when missing
      }
    } catch (ex) {
      console.warn("Could not auto-assign current user ids", ex);
    }

    // Debug: print the final DTO we are about to send to the backend
    console.debug("Sending trade DTO to backend", dto);

    // Ensure the settlementInstructions key is present in the DTO so the
    // backend sees the field during create/update requests. The UI keeps
    // settlement text in the parent (`props.settlement`) so prefer that
    // when available. If there is no settlement text we still add the
    // key as an empty string so swagger-like payloads and programmatic
    // consumers behave consistently.
    // NOTE: tradeUtils.convertEmptyStringsToNull will later convert an
    // explicit empty string to null for fields listed there when called
    // earlier; we intentionally include the key here to ensure the
    // backend mapping receives the field in the JSON payload.
    dto.settlementInstructions = props.settlement ?? dto.settlement ?? "";

    // ADDED: client-side settlement validation helper.
    // Purpose: Check settlement text before attempting async persistence.
    // Rules enforced here match the UI rules in SettlementTextArea:
    // - Trim whitespace
    // - Length must be between 10 and 500 characters
    // - Forbid characters ; ' " < > which are considered invalid
    // Behaviour: show a snackbar error and return false when invalid.
    const validateSettlementText = (text?: string) => {
      if (!text) return true; // nothing to validate
      const trimmedText = String(text).trim();
      const invalidChars = /[;'"<>]/; // ADDED: illegal-character guard used by validation
      // Validate length: settlement text must be between 10 and 500 characters
      if (trimmedText.length < 10 || trimmedText.length > 500) {
        setSnackbarMsg(
          "Settlement instructions must be between 10 and 500 characters long."
        );
        setSnackbarType("error");
        setSnackbarOpen(true);
        return false;
      }
      if (invalidChars.test(trimmedText)) {
        setSnackbarMsg("Settlement instructions contain invalid characters.");
        setSnackbarType("error");
        setSnackbarOpen(true);
        return false;
      }
      return true;
    };

    try {
      if (editableTrade.tradeId) {
        await api.put(`/trades/${editableTrade.tradeId}`, tradeDto);
        setSnackbarMsg(
          `Trade updated successfully! Trade ID: ${editableTrade.tradeId}`
        );
        setSnackbarType("success");
        setSnackbarOpen(true);

        // REFACTOR: attempt to persist settlement immediately after the
        // trade update using the parent-provided handler. This keeps the
        // save semantics atomic from the user's perspective: clicking
        // "Save Trade" results in both the trade and its settlement being
        // stored. Note we intentionally do not fail the whole operation if
        // settlement persistence fails; the trade update succeeded and we
        // surface the settlement failure in the UI.
        if (props.saveSettlement && props.settlement) {
          try {
            console.debug(
              "Saving settlement after trade update",
              editableTrade.tradeId
            );
            // Validate settlement text before attempting async save. If
            // validation fails we skip saving settlement but do not roll
            // back the trade update.
            if (validateSettlementText(props.settlement)) {
              void saveSettlementAsync(
                editableTrade.tradeId,
                props.settlement ?? ""
              );
            }
          } catch (err) {
            console.error("Failed to initiate async settlement save", err);
          }
        }

        const response = await api.get(`/trades/${editableTrade.tradeId}`);
        // formatDatesFromBackend may return an untyped object; cast to Trade
        // as we expect the backend to return a complete trade entity here.
        const updatedTrade = formatDatesFromBackend(response.data) as Trade;
        setEditableTrade(updatedTrade);
      } else {
        // Save new trade
        const response = await api.post("/trades", tradeDto);
        const newTradeId = response.data?.tradeId || response.data?.id || "";
        // Ensure UI state reflects the newly created trade id so subsequent
        // actions (e.g., saving settlement or navigating) operate on the
        // persisted trade. Previously the UI left editableTrade.tradeId as
        // null which made it look like the trade wasn't created.
        if (newTradeId) {
          setEditableTrade((prev) =>
            prev ? { ...prev, tradeId: newTradeId } : prev
          );
        }
        // NOTE: The createTrade controller already persists settlement when
        // `settlementInstructions` is present in the POST payload. To avoid
        // duplicate records we do NOT call the separate settlement PUT here
        // for new trades. The parent `saveSettlement` handler is still used
        // for explicit updates (see update branch above) where the controller
        // does not persist settlement on PUT.
        setSnackbarMsg(`Trade saved successfully! Trade ID: ${newTradeId}`);
        setSnackbarType("success");
        setSnackbarOpen(true);
      }
    } catch (e) {
      setSnackbarMsg(
        "Failed to save trade: " +
          (e instanceof Error ? e.message : "Unknown error")
      );
      setSnackbarType("error");
      setSnackbarOpen(true);
    } finally {
      // Snackbar auto-closes itself; do not forcibly hide it here.
      setLoading(false);
    }
  };

  /**
   * Handles terminating the trade
   */
  const handleTerminateTrade = async () => {
    setLoading(true);

    if (!editableTrade?.tradeId) {
      setSnackbarMsg("Cannot terminate: Trade ID is missing");
      setSnackbarType("error");
      setSnackbarOpen(true);
      return;
    }

    if (editableTrade.tradeStatus === "TERMINATED") {
      setSnackbarMsg("This trade has already been terminated");
      setSnackbarType("error");
      setSnackbarOpen(true);
      return;
    }

    try {
      await api.post(`/trades/${editableTrade.tradeId}/terminate`);

      setSnackbarMsg(`Trade ${editableTrade.tradeId} terminated successfully!`);
      setSnackbarType("success");
      setSnackbarOpen(true);

      setEditableTrade((prev) =>
        prev
          ? {
              ...prev,
              tradeStatus: "TERMINATED",
            }
          : prev
      );
    } catch (error) {
      const errorMessage =
        error instanceof Error
          ? error.message
          : "An unknown error occurred while terminating the trade";

      setSnackbarMsg(`Failed to terminate trade: ${errorMessage}`);
      setSnackbarType("error");
      setSnackbarOpen(true);
    } finally {
      // Let the Snackbar handle auto-dismiss so messages remain visible
      // long enough for users to read them.
      setLoading(false);
    }
  };

  const tradeLegs = editableTrade?.tradeLegs
    ? editableTrade.tradeLegs.slice(0, 2)
    : [];

  return (
    <div className={"flex flex-col"}>
      <div className={"flex flex-row justify-center w-full"}>
        <h2 className={"text-2xl font-semibold justify-center"}>
          {props.mode === "edit" ? "Edit Trade" : "View Trade"}
        </h2>
      </div>
      {loading ? (
        <LoadingSpinner />
      ) : (
        <div className={"flex flex-row"}>
          <div key={"Trade Header"} className="flex flex-col ml-2">
            <h3 className="text-lg font-semibold text-center mb-2">
              Trade Header
            </h3>
            <TradeDetails
              trade={editableTrade}
              mode={props.mode}
              onFieldChange={
                props.mode === "edit" ? handleFieldChange : undefined
              }
            />
          </div>
          {tradeLegs.length > 0 && (
            <div className="flex flex-row gap-x-8 h-fit justify-center mt-0">
              {tradeLegs.map((leg, idx) => (
                <div
                  key={leg.legId || idx + "-" + leg.payReceiveFlag}
                  className="flex flex-col ml-2"
                >
                  <h3 className="text-lg font-semibold text-center mb-2">
                    Leg {idx + 1}
                  </h3>
                  <TradeLegDetails
                    leg={leg}
                    mode={props.mode}
                    onFieldChange={
                      props.mode === "edit"
                        ? (key, value) => handleLegFieldChange(idx, key, value)
                        : undefined
                    }
                  />
                  {props.mode === "edit" && idx === tradeLegs.length - 1 && (
                    <div className="mt-4 flex justify-end gap-x-2">
                      <Button
                        variant={"primary"}
                        type={"button"}
                        size={"sm"}
                        disabled={generatedCashflows.length === 0}
                        className={
                          generatedCashflows.length === 0
                            ? "opacity-50 cursor-not-allowed"
                            : ""
                        }
                        onClick={handleSaveTrade}
                        title={
                          generatedCashflows.length === 0
                            ? "Please generate cashflows first before saving the trade"
                            : ""
                        }
                      >
                        Save Trade
                      </Button>
                      <Button
                        variant={"primary"}
                        type={"button"}
                        size={"sm"}
                        onClick={generateCashflows}
                        className={"!bg-amber-400 hover:!bg-amber-600"}
                      >
                        Cashflows
                      </Button>
                      {userStore.authorization === "TRADER_SALES" && (
                        <Button
                          variant={"secondary"}
                          type={"button"}
                          size={"sm"}
                          onClick={handleTerminateTrade}
                        >
                          Terminate Trade
                        </Button>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
      {cashflowModalOpen && (
        <CashflowModal
          isOpen={cashflowModalOpen}
          onClose={() => setCashflowModalOpen(false)}
          cashflows={generatedCashflows}
        />
      )}
      <Snackbar
        open={snackbarOpen}
        message={snackbarMsg}
        type={snackbarType}
        onClose={() => setSnackbarOpen(false)}
        // Added: Retry action when settlement save previously failed
        actionLabel={settlementUnsaved ? "Retry" : undefined}
        onAction={
          settlementUnsaved
            ? () => {
                if (editableTrade?.tradeId && props.settlement) {
                  void saveSettlementAsync(
                    editableTrade.tradeId,
                    props.settlement
                  );
                }
              }
            : undefined
        }
      />
    </div>
  );
};

export default SingleTradeModal;
