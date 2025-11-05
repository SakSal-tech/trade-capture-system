import React, { useState } from "react";
import Input from "../components/Input";
import Button from "../components/Button";
import api from "../utils/api";
import axios from "axios";
import { SettlementTextArea } from "./SettlementTextArea";
import { SingleTradeModal } from "./SingleTradeModal";
import { getDefaultTrade } from "../utils/tradeUtils";
import userStore from "../stores/userStore";
import LoadingSpinner from "../components/LoadingSpinner";
import Snackbar from "../components/Snackbar";
import { observer } from "mobx-react-lite";
import { useQuery } from "@tanstack/react-query";
import staticStore from "../stores/staticStore";
import { Trade, TradeLeg } from "../utils/tradeTypes";
// removed unused import 'text'

export const TradeActionsModal: React.FC = observer(() => {
  const [tradeId, setTradeId] = React.useState<string>("");
  const [snackBarOpen, setSnackbarOpen] = React.useState<boolean>(false);
  const [trade, setTrade] = React.useState<Trade | null>(null);
  const [loading, setLoading] = React.useState<boolean>(false);
  const [snackbarMessage, setSnackbarMessage] = React.useState<string>("");
  const [isLoadError, setIsLoadError] = React.useState<boolean>(false);
  const [modalKey, setModalKey] = React.useState(0);
  /*
    ADDED: local state to hold settlement instructions for the currently
    selected trade. We keep a local copy for the editor to display so the
    user sees up-to-date data immediately after selecting a trade.

    Business mapping:
    - "Settlement instructions visible in trade views"
    - "Amendment handling: settlement instructions editable during trade amendments"
    The authoritative persistence is on the server (AdditionalInfo). This
    client-side state is strictly a UI cache that mirrors what the server
    returns or what the user saves.
  */
  // REFACTOR NOTE:
  // Previously the settlement editor performed its own HTTP request and
  // persistence. To improve maintainability and ensure a single source of
  // truth for settlements (so Save Trade and the settlement UI behave
  // identically), we refactored persistence into a single parent-level
  // handler `saveSettlement`. This lets the parent decide when and how to
  // persist (for example, save settlement after a trade create/update)
  // and keeps `SettlementTextArea` focused on UI concerns only (insert
  // templates, caret handling, validation). The refactor reduces duplicated
  // network logic and makes error-handling / UX messaging consistent.
  const [settlement, setSettlement] = useState<string>("");
  // `touched` state was previously added but is not used in this parent.
  // If we later need to show saving state, reintroduce submitting state.

  const { isSuccess, error } = useQuery({
    queryKey: ["staticValues"],
    queryFn: () => staticStore.fetchAllStaticValues(),
    refetchInterval: 30000,
    refetchOnWindowFocus: false,
  });

  React.useEffect(() => {
    if (isSuccess) {
      staticStore.isLoading = false;
      console.log("Static values loaded successfully");
    }
    if (error) {
      staticStore.error = error.message || "Unknown error";
    }
  }, [isSuccess, error]);

  // When a trade is selected/loaded, fetch its current settlement instructions
  React.useEffect(() => {
    let cancelled = false;
    const fetchSettlement = async () => {
      if (!trade?.tradeId) {
        setSettlement("");
        return;
      }
      try {
        const resp = await api.get(
          `/trades/${trade.tradeId}/settlement-instructions`
        );
        // Backend returns AdditionalInfoDTO; extract fieldValue
        const current = resp?.data?.fieldValue ?? "";
        if (!cancelled) setSettlement(current);
      } catch (err) {
        // 404 means no settlement saved yet treat as empty; otherwise surface error via snackbar
        if (axios.isAxiosError(err) && err.response?.status === 404) {
          if (!cancelled) setSettlement("");
        } else {
          setSnackbarOpen(true);
          setSnackbarMessage(
            "Failed to load settlement instructions: " +
              (err instanceof Error ? err.message : "Unknown error")
          );
        }
      }
    };
    fetchSettlement();
    return () => {
      cancelled = true;
    };
  }, [trade]);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    console.log("Searching for trade ID:", tradeId);
    setLoading(true);
    try {
      const tradeResponse = await api.get(`/trades/${tradeId}`);
      if (tradeResponse.status === 200) {
        const convertToDate = (val: string | undefined) =>
          val ? new Date(val) : undefined;
        const tradeData = tradeResponse.data;
        const dateFields = [
          "tradeDate",
          "startDate",
          "maturityDate",
          "executionDate",
          "lastTouchTimestamp",
          "validityStartDate",
        ];

        const formatDateForInput = (date: Date | undefined) =>
          date ? date.toISOString().slice(0, 10) : undefined;
        dateFields.forEach((field) => {
          if (tradeData[field]) {
            const dateObj = convertToDate(tradeData[field]);
            tradeData[field] = formatDateForInput(dateObj);
          }
        });
        if (Array.isArray(tradeData.tradeLegs)) {
          console.log(
            `Found ${tradeData.tradeLegs.length} trade legs in the response`
          );
          tradeData.tradeLegs = tradeData.tradeLegs.map((leg: TradeLeg) => {
            console.log("Processing leg:", leg);
            return {
              ...leg,
              legId: leg.legId || "",
              legType: leg.legType || "",
              rate: leg.rate !== undefined ? leg.rate : "",
              index: leg.index || "",
            };
          });
        } else {
          console.warn("No trade legs found in the response!");
          tradeData.tradeLegs = [];
        }
        setTrade(tradeData);
        setSnackbarOpen(true);
        setSnackbarMessage("Successfully fetched trade details");
      } else {
        console.error("Error fetching trade:", tradeResponse.statusText);
        setSnackbarMessage(
          "Error fetching trade details: " + tradeResponse.statusText
        );
        setIsLoadError(true);
      }
    } catch (error) {
      console.error("Error fetching trade:", error);
      setIsLoadError(true);
      setSnackbarOpen(true);
      setSnackbarMessage(
        "Error fetching trade details: " +
          (error instanceof Error ? error.message : "Unknown error")
      );
    } finally {
      setTimeout(() => {
        setSnackbarOpen(false);
        setSnackbarMessage("");
        setIsLoadError(false);
      }, 3000);
      setLoading(false);
      setTradeId("");
    }
  };
  const handleClearAll = () => {
    setTrade(null);
    setTradeId("");
    setSnackbarOpen(false);
    setSnackbarMessage("");
    setIsLoadError(false);
    setLoading(false);
  };
  const handleBookNew = () => {
    const defaultTrade = getDefaultTrade();
    console.log("DEBUG getDefaultTrade:", defaultTrade);
    setTrade(defaultTrade);
    setModalKey((prev) => prev + 1);
  };
  // DEV: Parent modal owns settlement persistence. The saveSettlement
  // function below centralises persistence for the SettlementTextArea and
  // ensures consistency and single-point error handling. See
  // docs/DeveloperLog-30-10-2025-detailed.md
  // REFACTOR: extracted a single save function so both the standalone
  // `SettlementTextArea` and the main trade save flow can reuse identical
  // persistence logic. Rationale:
  // - Avoid duplicated network calls and inconsistent error handling.
  // - Make it straightforward for the trade save flow to persist settlement
  //   as part of the same user action (book/update), ensuring data
  //   consistency between the trade entity and its AdditionalInfo record.
  // - Keep the UI component (`SettlementTextArea`) free of network logic
  //   so it can be tested independently and remain a simple controlled
  //   input with insert-at-cursor/template behaviour.
  const saveSettlement = async (tradeId: string, text: string) => {
    const id = tradeId ?? trade?.tradeId;
    console.debug(
      "saveSettlement called for tradeId",
      id,
      "text:length",
      text?.length
    );
    if (!id) throw new Error("No trade id available to save settlement");
    const payload = {
      entityType: "TRADE",
      entityId: Number(id),
      fieldName: "SETTLEMENT_INSTRUCTIONS",
      fieldValue: text,
    };
    try {
      await api.put(`/trades/${id}/settlement-instructions`, payload);
    } catch (err) {
      // Surface permission issues clearly so caller can show appropriate UX
      // Log the full axios error for diagnostics
      console.error("saveSettlement failed", err);
      if (axios.isAxiosError(err)) {
        const status = err.response?.status;
        const serverMsg = err.response?.data ?? err.message;
        if (status === 403) {
          throw new Error(
            `Forbidden (403): insufficient privilege to edit settlement for trade ${id}`
          );
        }
        // Propagate server-sent message when available
        throw new Error(
          `API error ${status || ""}: ${JSON.stringify(serverMsg)}`
        );
      }
      throw err;
    }
  };
  const mode =
    userStore.authorization === "TRADER_SALES" ||
    userStore.authorization === "MO"
      ? "edit"
      : "view";

  return (
    <div
      className={
        "flex flex-col rounded-lg drop-shadow-2xl mt-0 bg-indigo-50 w-full h-full"
      }
    >
      <div
        className={
          "flex flex-row items-center justify-center p-4 h-fit w-fit gap-x-2 mb-2 mx-auto"
        }
      >
        <Input
          size={"sm"}
          type={"search"}
          required
          placeholder={"Search by Trade ID"}
          key={"trade-id"}
          value={tradeId}
          onChange={(e) => setTradeId(e.currentTarget.value)}
          className={"bg-white h-fit w-fit"}
        />
        <Button
          variant={"primary"}
          type={"button"}
          size={"sm"}
          onClick={handleSearch}
          className={"w-fit h-fit"}
        >
          Search
        </Button>
        <Button
          variant={"primary"}
          type={"button"}
          size={"sm"}
          onClick={handleClearAll}
          className={"w-fit h-fit !bg-gray-500 hover:!bg-gray-700"}
        >
          Clear
        </Button>
        {mode === "edit" && (
          <Button
            variant={"primary"}
            type={"button"}
            size={"sm"}
            onClick={handleBookNew}
            className={"w-fit h-fit"}
          >
            Book New
          </Button>
        )}
      </div>
      <div>
        {loading ? <LoadingSpinner /> : null}
        {/* When no trade is selected (empty workspace), show a helpful
            Instructions panel so users understand what actions are
            available and how to use the trade actions modal. This mirrors
            the guidance previously provided on the standalone /trade page
            and keeps the application streamlined by consolidating
            instructions into the trade action area. */}
        {!trade && !loading && (
          <div className="w-full p-6">
            <h1 className="text-3xl font-bold">Instructions</h1>

            <section className="w-full mt-4">
              <h2 className="text-2xl font-semibold mb-2">
                How to search for an existing trade
              </h2>
              <p className="text-lg text-gray-700">
                Use the Search input above to find trades by trade id,
                counterparty, or date. If you don&apos;t see a trade, try
                widening the date range.
              </p>
            </section>

            <section className="w-full mt-4">
              <h2 className="text-2xl font-semibold mb-2">
                How to add settlement details
              </h2>
              {/* Escaped inner quotes to satisfy the react/no-unescaped-entities
                  ESLint rule. Unescaped quotes/apostrophes in JSX text cause
                  lint errors that block production builds. */}
              <p className="text-lg text-gray-700">
                Open the trade and select &quot;Edit&quot; or &quot;Add
                settlement&quot;. Fill in the settlement date, currency and
                amount. The system will validate required fields and notify you
                if anything is missing or inconsistent.
              </p>
            </section>

            <section className="w-full mt-4">
              <h2 className="text-2xl font-semibold mb-2">
                How to use the Trade Dashboard
              </h2>
              <p className="text-lg text-gray-700">
                The Trade Dashboard provides an at-a-glance view of recent
                activity and exposures. Use it to spot unusual items and drill
                into trades for details. Filters at the top let you focus on
                specific books, desks or date ranges.
              </p>
            </section>

            <section className="w-full mt-4">
              <h2 className="text-2xl font-semibold mb-2">
                How to book a new trade
              </h2>
              {/* Escaped double quotes in the instruction paragraph to avoid
                  react/no-unescaped-entities lint failures during build. */}
              <p className="text-lg text-gray-700 mb-3">
                Click &quot;Book New&quot; above to create a trade. The form
                checks important business rules before the trade is accepted
                below is a friendly list of the key checks.
              </p>

              <ul className="list-disc pl-6 space-y-2 text-lg text-gray-700">
                <li>
                  <strong>Both legs must have identical maturity dates</strong>:
                  each side of the trade should settle on the same date.
                </li>
                <li>
                  <strong>Legs must have opposite pay/receive flags</strong>:
                  one side pays and the other receives.
                </li>
                <li>
                  <strong>Floating legs must have an index specified</strong>:
                  enter the appropriate reference rate (e.g. LIBOR-like index).
                </li>
                <li>
                  <strong>Fixed legs must have a valid rate</strong> enter the
                  agreed fixed rate for the fixed leg.
                </li>
              </ul>

              <h3 className="text-xl font-semibold mt-4">
                Entity status checks
              </h3>
              <ul className="list-disc pl-6 space-y-2 text-lg text-gray-700">
                <li>
                  <strong>User, book, and counterparty must be active</strong>:
                  trades can only be booked against active records in the
                  system.
                </li>
                <li>
                  <strong>All reference data must exist and be valid</strong>:
                  currencies, indices, and other lookup values must be present.
                </li>
              </ul>
            </section>
          </div>
        )}

        {trade && !loading && (
          <div className="flex flex-row gap-6 items-start">
            {/* Left: the main trade editor (takes remaining space) */}
            <div className="flex-1">
              <SingleTradeModal
                key={modalKey}
                mode={mode}
                trade={trade}
                isOpen={!!trade}
                onClear={handleClearAll}
                settlement={settlement}
                saveSettlement={saveSettlement}
              />
            </div>

            {/* Right: settlement editor as a fixed-width column aligned to top
                so it lines up with the trade form (execution date / pay/rec area).
            */}
            <div className="w-80 self-start">
              <div className="p-4 bg-white rounded shadow-md">
                <h3 className="text-lg font-semibold mb-2">
                  Settlement Instructions
                </h3>
                <SettlementTextArea
                  initialValue={settlement}
                  onSave={(text) => setSettlement(text)}
                  onChange={(text) => setSettlement(text)}
                />
                <div className="flex gap-2 mt-3">
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={async () => {
                      try {
                        if (!trade?.tradeId) {
                          setSnackbarOpen(true);
                          setSnackbarMessage(
                            "Save the trade first to persist settlement"
                          );
                          return;
                        }
                        await saveSettlement(String(trade.tradeId), settlement);
                        setSnackbarOpen(true);
                        setSnackbarMessage("Settlement saved");
                      } catch (err) {
                        setSnackbarOpen(true);
                        setSnackbarMessage(
                          err instanceof Error ? err.message : String(err)
                        );
                      }
                    }}
                  >
                    Save
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => setSettlement("")}
                  >
                    Clear
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}
        {/* 
          ADDED: Settlement instructions editor. To capture settlement instructions at trade booking so operations have immediate access and to reduce manual coordination. So the backend persists instructions in
          To enable traders (and SALES role) to be able to edit settlement
            text during amendments this UI allows authorised users to save
            changes.
         UX: templates + insert-at-cursor speed entry and reduce formatting
            errors; client-side validation gives immediate feedback (10-500
            chars, forbid angle brackets) but final validation and sanitisation
            happen on the server.
          Implementation note: the parent fetches current instructions and
          supplies an onSave callback that sends an AdditionalInfoRequestDTO to
          the server (PUT /trades/:id/settlement-instructions). 404 responses
          are treated as "no existing instruction" and show an empty editor.
        */}
      </div>
      <Snackbar
        open={snackBarOpen}
        message={snackbarMessage}
        onClose={() => setSnackbarOpen(false)}
        type={isLoadError ? "error" : "success"}
      />
    </div>
  );
});
