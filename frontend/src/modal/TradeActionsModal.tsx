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
import { text } from "stream/consumers";

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
  // Keep only the submitting setter because we use it to mark save in-progress
  // for UX reasons; the value itself isn't read here.
  const [, setSubmitting] = useState(false);

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
        // 404 means no settlement saved yet — treat as empty; otherwise surface error via snackbar
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
        {userStore.authorization === "TRADER_SALES" && (
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
        {trade && !loading && (
          <div className="flex flex-col gap-6">
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

            {/* 
  Refactored: Adjusted margin and width to position the Settlement Instructions box
  slightly higher (closer to the trade form) and further right (under
  the Save Trade / Cashflows / Terminate buttons area). This keeps it
  visually connected to the trade form layout without making it appear
  detached or floating too far right.
*/}
            <div className="flex justify-end">
              <div className="w-[50%] mr-60 -mt-70">
                <div className="p-4 bg-white rounded shadow-md">
                  <h3 className="text-lg font-semibold mb-2">
                    Settlement Instructions
                  </h3>
                  {/*Refactored: to solve issues with settlement not saving as I was not passing any onChange or onSave callback to update the local settlement state*/}
                  <SettlementTextArea
                    initialValue={settlement}
                    onSave={(text) => setSettlement(text)} //Optional save trigger
                    // Also uodate on every change to keep parent in sync
                    onChange={(text) => setSettlement(text)}
                  />
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
