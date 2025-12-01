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
  formatDateTimeForBackend,
} from "../utils/dateUtils";
import LoadingSpinner from "../components/LoadingSpinner";
import userStore from "../stores/userStore";
import staticStore from "../stores/staticStore";
import {
  DEFAULT_FALLBACK_FLOATING_RATE,
  DEFAULT_FALLBACK_FIXED_RATE,
} from "../utils/tradeUtils";

interface SingleTradeModalProps {
  mode: "view" | "edit";
  trade?: Trade;
  isOpen: boolean;
  onClear?: () => void;
  settlement?: string;
  saveSettlement?: (tradeId: string, text: string) => Promise<void>;
  onTradeUpdated?: (updatedTrade: Trade) => void;
}

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
  const [settlementUnsaved, setSettlementUnsaved] = React.useState(false);

  React.useEffect(() => {
    setEditableTrade(props.trade ?? getDefaultTrade());
    setCashflowModalOpen(false);
    setGeneratedCashflows([]);

    // FIXED: Don't reset snackbar when trade ID gets updated after successful save
    // Only reset when opening modal or switching to completely different trade
    const shouldResetSnackbar =
      !props.isOpen ||
      (snackbarMsg && !snackbarMsg.includes("saved successfully"));

    if (shouldResetSnackbar) {
      setSnackbarOpen(false);
      setSnackbarMsg("");
    }

    setSnackbarType("success");
  }, [props.trade, props.isOpen]);

  const saveSettlementAsync = async (tradeId: string, text: string) => {
    if (!props.saveSettlement) {
      console.warn("No saveSettlement function provided");
      return;
    }
    console.debug("Initiating async settlement save", {
      tradeId,
      text,
      textLength: text.length,
    });
    try {
      console.debug("Calling props.saveSettlement...");
      await props.saveSettlement(tradeId, text);
      console.debug("Settlement save completed successfully");
      if (settlementUnsaved) setSettlementUnsaved(false);
      // FIXED: Append to existing message instead of replacing it
      // This preserves the "Trade saved" message and adds settlement confirmation
      setSnackbarMsg((prev) => {
        if (prev && prev.includes("Trade saved")) {
          return prev + " + Settlement saved";
        }
        return prev ? prev + " (Settlement saved)" : "Settlement saved";
      });
      setSnackbarType("success");
      setSnackbarOpen(true);
    } catch (err: unknown) {
      console.error("Async settlement save failed", err);
      setSettlementUnsaved(true);
      let detail = "";
      if (err?.message) detail = String(err.message);
      if (err?.response?.data) {
        try {
          detail = JSON.stringify(err.response.data);
        } catch {
          detail = String(err.response.data);
        }
      }
      setSnackbarMsg((prev) =>
        prev
          ? prev + " (Settlement save failed: " + detail + ")"
          : "Settlement save failed: " + detail
      );
      setSnackbarType("error");
      setSnackbarOpen(true);
    }
  };

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

  const generateCashflows = async () => {
    setLoading(true);
    if (!editableTrade) return;
    try {
      const sanitizeNumber = (v: number | string | undefined) => {
        if (v === undefined || v === null) return undefined;
        if (typeof v === "number") return v;
        const cleaned = String(v).replace(/[,\s]/g, "");
        const parsed = parseFloat(cleaned);
        return Number.isNaN(parsed) ? undefined : parsed;
      };

      const legsDto = editableTrade.tradeLegs.map((leg) => {
        const sanitizedRate = sanitizeNumber(leg.rate) as number | undefined;
        let rateForDto: number | undefined;

        if (sanitizedRate !== undefined && sanitizedRate !== null) {
          rateForDto = sanitizedRate;
        } else {
          const lt = (leg.legType || "").toLowerCase();
          if (lt === "floating") rateForDto = DEFAULT_FALLBACK_FLOATING_RATE;
          else if (lt === "fixed") rateForDto = DEFAULT_FALLBACK_FIXED_RATE;
        }

        return {
          legType: leg.legType,
          notional: sanitizeNumber(leg.notional),
          rate: rateForDto,
          index: leg.index,
          calculationPeriodSchedule: leg.calculationPeriodSchedule,
          paymentBusinessDayConvention: leg.paymentBusinessDayConvention,
          payReceiveFlag: leg.payReceiveFlag,
        };
      });

      const response = await api.post("/cashflows/generate", {
        legs: legsDto,
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

  const handleLegFieldChange = (
    legIdx: number,
    key: keyof TradeLeg,
    value: unknown
  ) => {
    setEditableTrade((prev) => {
      if (!prev) return prev;
      const updatedLegs = prev.tradeLegs.map((leg, idx) =>
        idx === legIdx ? { ...leg, [key]: value } : leg
      );
      return {
        ...prev,
        tradeLegs: updatedLegs,
      };
    });
  };

  // FIXES APPLIED:
  // 1. Use tradeWithDefaults (with propagated maturity dates) instead of editableTrade
  // 2. Add settlement saving for new trades (was only working for existing trades)
  // 3. Loading state prevents duplicate trade creation from double-clicks
  const handleSaveTrade = async () => {
    setLoading(true);
    if (!editableTrade) return;

    if (!editableTrade.utiCode || String(editableTrade.utiCode).trim() === "") {
      const datePart = new Date().toISOString().split("T")[0].replace(/-/g, "");
      const randomPart = Math.floor(Math.random() * 10000)
        .toString()
        .padStart(4, "0");
      const generatedUti = `UTI-${datePart}-${randomPart}`;
      setEditableTrade((prev) =>
        prev ? { ...prev, utiCode: generatedUti } : prev
      );
    }

    const tradeWithDefaults: Trade = {
      ...(editableTrade as Trade),
      tradeLegs: (editableTrade.tradeLegs || []).map((leg) => {
        const legType = (leg.legType || "").toLowerCase();
        if (
          legType === "fixed" &&
          (leg.rate === undefined || leg.rate === null || leg.rate === "")
        ) {
          return { ...leg, rate: String(DEFAULT_FALLBACK_FIXED_RATE) };
        }
        return { ...leg };
      }),
    };

    setEditableTrade(tradeWithDefaults);

    const validationError = validateTrade(tradeWithDefaults);
    if (validationError) {
      setSnackbarMsg(validationError);
      setSnackbarType("error");
      setSnackbarOpen(true);
      return;
    }

    // FIXED: Use tradeWithDefaults instead of editableTrade to ensure maturity date
    // propagation to legs has been applied by validateTrade function
    let tradeDto: Record<string, unknown> =
      formatTradeForBackend(tradeWithDefaults);
    tradeDto = convertEmptyStringsToNull(tradeDto) as Record<string, unknown>;

    const editable: Trade = editableTrade as Trade;
    const dto: Record<string, unknown> = tradeDto as Record<string, unknown>;

    const toNumberOrNull = (v: unknown) => {
      if (v === undefined || v === null || v === "") return null;
      const n = Number(v);
      return Number.isFinite(n) ? n : null;
    };

    dto.utiCode =
      editable?.utiCode && String(editable.utiCode).trim() !== ""
        ? editable.utiCode
        : dto.utiCode ?? null;

    const resolveUserId = (idField: unknown, nameField: unknown) => {
      const explicit = toNumberOrNull(idField);
      if (explicit !== null) return explicit;

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

    if (
      typeof editable?.traderUserId === "string" &&
      toNumberOrNull(editable.traderUserId) === null
    ) {
      dto.traderUserName = dto.traderUserName ?? String(editable.traderUserId);
      dto.traderUserId = null;
    }

    if (
      typeof editable?.tradeInputterUserId === "string" &&
      toNumberOrNull(editable.tradeInputterUserId) === null
    ) {
      dto.inputterUserName =
        dto.inputterUserName ?? String(editable.tradeInputterUserId);
      dto.tradeInputterUserId = null;
    }

    dto.traderUserId =
      resolveUserId(editable?.traderUserId, editable?.traderUserName) ??
      dto.traderUserId ??
      null;

    dto.tradeInputterUserId =
      resolveUserId(
        editable?.tradeInputterUserId,
        editable?.inputterUserName
      ) ??
      dto.tradeInputterUserId ??
      null;

    try {
      const currentUser = (userStore && userStore.user) || null;

      if (
        (dto.traderUserId === undefined || dto.traderUserId === null) &&
        currentUser &&
        toNumberOrNull(currentUser.id) !== null
      ) {
        dto.traderUserId = toNumberOrNull(currentUser.id);
        dto.traderUserName =
          dto.traderUserName ??
          String(currentUser.loginId ?? currentUser.loginId ?? "");
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
      }
    } catch (ex) {
      console.warn("Could not auto-assign current user ids", ex);
    }

    console.debug("Sending trade DTO to backend", dto);

    // DEBUG: Log settlement information
    console.debug("Settlement debugging info:", {
      propsSettlement: props.settlement,
      dtoSettlement: dto.settlement,
      settlementLength: props.settlement?.length || 0,
      hasSettlement: !!props.settlement,
    });

    dto.settlementInstructions = props.settlement ?? dto.settlement ?? "";

    const validateSettlementText = (text?: string) => {
      if (!text) return true;
      const trimmedText = String(text).trim();
      const invalidChars = /[;'"<>]/;

      // DEBUG: Log validation details
      console.debug("Settlement validation:", {
        text: text,
        trimmedText: trimmedText,
        length: trimmedText.length,
        hasInvalidChars: invalidChars.test(trimmedText),
      });

      // TEMPORARILY RELAXED: Allow shorter settlements for debugging
      if (trimmedText.length < 5 || trimmedText.length > 500) {
        console.error(
          "Settlement length validation failed:",
          trimmedText.length
        );
        setSnackbarMsg(
          `Settlement instructions must be between 5 and 500 characters long. Current length: ${trimmedText.length}`
        );
        setSnackbarType("error");
        setSnackbarOpen(true);
        return false;
      }
      if (invalidChars.test(trimmedText)) {
        console.error("Settlement character validation failed:", trimmedText);
        setSnackbarMsg("Settlement instructions contain invalid characters.");
        setSnackbarType("error");
        setSnackbarOpen(true);
        return false;
      }
      console.debug("Settlement validation passed");
      return true;
    };

    // CRITICAL FIX: Remove backend-generated fields before sending DTO
    // These fields cause 400 errors when sent from frontend
    delete dto.version; // Backend manages versions
    delete dto.createdDate; // Backend auto-generates on create
    delete dto.lastTouchTimestamp; // Backend auto-updates on save
    delete dto.deactivatedDate; // Backend manages lifecycle
    delete dto.additional_fields_id; // Internal backend reference

    // Handle tradeId properly: only send for updates, not for new trades
    if (!editableTrade.tradeId) {
      delete dto.tradeId; // Creating new trade - backend generates ID
    } else {
      dto.tradeId = editableTrade.tradeId; // Updating existing trade
    }

    console.debug("Cleaned DTO for backend", dto);

    try {
      if (editableTrade.tradeId) {
        // CHANGED HERE: send final DTO instead of tradeDto
        await api.put(`/trades/${editableTrade.tradeId}`, dto);

        setSnackbarMsg(
          `Trade updated successfully! Trade ID: ${editableTrade.tradeId}`
        );
        setSnackbarType("success");
        setSnackbarOpen(true);

        if (props.saveSettlement && props.settlement) {
          try {
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
        const updatedTrade = formatDatesFromBackend(response.data) as Trade;
        setEditableTrade(updatedTrade);
      } else {
        // CHANGED HERE: send final DTO instead of tradeDto
        const response = await api.post("/trades", dto);

        const newTradeId = response.data?.tradeId || response.data?.id || "";
        console.debug("Trade creation response:", {
          responseData: response.data,
          extractedTradeId: newTradeId,
          tradeIdType: typeof newTradeId,
          businessTradeId: response.data?.tradeId,
          primaryKeyId: response.data?.id,
        });

        if (newTradeId) {
          const updatedTrade = { ...editableTrade, tradeId: newTradeId };
          setEditableTrade(updatedTrade);

          // FIXED: Notify parent component of the updated trade with new ID
          // This ensures parent state stays in sync after trade creation
          if (props.onTradeUpdated) {
            props.onTradeUpdated(updatedTrade as Trade);
          }

          // FIXED: Save settlement instructions for new trades
          // Previously settlements were only saved for existing trades (PUT operations)
          // Now settlements are saved for both new (POST) and existing (PUT) trades
          if (props.saveSettlement && props.settlement) {
            try {
              if (validateSettlementText(props.settlement)) {
                console.debug(
                  "Calling saveSettlementAsync with tradeId:",
                  newTradeId,
                  "settlement:",
                  props.settlement
                );
                // Add delay to let trade success message show first, then append settlement
                setTimeout(() => {
                  void saveSettlementAsync(
                    String(newTradeId),
                    props.settlement ?? ""
                  );
                }, 1000); // 1 second delay to show trade message first
              }
            } catch (err) {
              console.error("Failed to initiate async settlement save", err);
            }
          }
        }

        // Show trade success message first
        setSnackbarMsg(`Trade saved successfully! Trade ID: ${newTradeId}`);
        setSnackbarType("success");
        setSnackbarOpen(true);
      }
    } catch (e: unknown) {
      setSnackbarMsg(
        "Failed to save trade: " +
          (e instanceof Error ? e.message : "Unknown error")
      );
      setSnackbarType("error");
      setSnackbarOpen(true);
    } finally {
      setLoading(false);
    }
  };

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
    } catch (error: unknown) {
      const errorMessage =
        error instanceof Error
          ? error.message
          : "An unknown error occurred while terminating the trade";

      setSnackbarMsg(`Failed to terminate trade: ${errorMessage}`);
      setSnackbarType("error");
      setSnackbarOpen(true);
    } finally {
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
                      {/* FIXED: Added loading state to prevent duplicate trade creation */}
                      {/* Disable button during API calls to prevent double-clicks */}
                      <Button
                        variant={"primary"}
                        type={"button"}
                        size={"sm"}
                        disabled={generatedCashflows.length === 0 || loading}
                        className={
                          generatedCashflows.length === 0 || loading
                            ? "opacity-50 cursor-not-allowed"
                            : ""
                        }
                        onClick={handleSaveTrade}
                        title={
                          generatedCashflows.length === 0
                            ? "Please generate cashflows first before saving the trade"
                            : loading
                            ? "Saving trade..."
                            : ""
                        }
                      >
                        {loading ? "Saving..." : "Save Trade"}
                      </Button>
                      {/* FIXED: Added loading state to prevent race conditions with Save Trade */}
                      <Button
                        variant={"primary"}
                        type={"button"}
                        size={"sm"}
                        disabled={loading}
                        onClick={generateCashflows}
                        className={
                          loading
                            ? "opacity-50 cursor-not-allowed !bg-amber-400"
                            : "!bg-amber-400 hover:!bg-amber-600"
                        }
                      >
                        Cashflows
                      </Button>
                      {userStore.authorization === "TRADER_SALES" && (
                        <Button
                          variant={"secondary"}
                          type={"button"}
                          size={"sm"}
                          disabled={loading}
                          onClick={handleTerminateTrade}
                        >
                          {loading ? "Processing..." : "Terminate Trade"}
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
