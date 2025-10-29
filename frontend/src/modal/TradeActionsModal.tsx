import React, { useState } from "react";
import Input from "../components/Input";
import Button from "../components/Button";
import api from "../utils/api";
import axios from "axios";
// ADDED: import the SettlementTextArea UI component so this modal can
// display and persist settlement instructions for the selected trade.
// Maps to business requirements: capture at booking, editable during
// amendments, and support for audit/ownership enforced by the backend.
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
          <SingleTradeModal
            key={modalKey}
            mode={mode}
            trade={trade}
            isOpen={!!trade}
            onClear={handleClearAll}
          />
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
        {trade && (
          <div className="mt-4 p-4 bg-white rounded">
            <h3 className="text-lg font-semibold mb-2">
              Settlement Instructions
            </h3>
            <SettlementTextArea
              initialValue={settlement}
              onSave={async (text: string) => {
                if (!trade?.tradeId) throw new Error("No trade selected");
                setSubmitting(true);
                try {
                  const payload = {
                    entityType: "TRADE",
                    entityId: Number(trade.tradeId),
                    fieldName: "SETTLEMENT_INSTRUCTIONS",
                    fieldValue: text,
                  };
                  await api.put(
                    `/trades/${trade.tradeId}/settlement-instructions`,
                    payload
                  );
                  setSnackbarOpen(true);
                  setSnackbarMessage("Settlement instructions saved");
                  setSettlement(text);
                } catch (err) {
                  setSnackbarOpen(true);
                  setSnackbarMessage(
                    "Failed to save settlement instructions: " +
                      (err instanceof Error ? err.message : "Unknown error")
                  );
                  // Re-throw so the child component can also log/handle if it wants
                  throw err;
                } finally {
                  setSubmitting(false);
                  setTimeout(() => setSnackbarOpen(false), 3000);
                }
              }}
            />
          </div>
        )}
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
