import React, { useState } from "react";
import Layout from "../components/Layout";
import { SingleTradeModal } from "../modal/SingleTradeModal";
import { SettlementTextArea } from "../modal/SettlementTextArea";
import { getDefaultTrade } from "../utils/tradeUtils";
import { useNavigate } from "react-router-dom";
import api from "../utils/api";

const TradeBooking: React.FC = () => {
  const navigate = useNavigate();

  // FIXED: Add settlement state management (was missing - causing settlement not to save)
  const [settlement, setSettlement] = useState<string>("");

  // Parent-provided saveSettlement used by SingleTradeModal when saving
  // trade + settlement together. We keep it simple: persist the
  // settlement via the existing API endpoint. If saving fails the
  // SingleTradeModal will show a retry state.
  const saveSettlement = async (tradeId: string, text: string) => {
    const id = tradeId;
    console.debug("TradeBooking saveSettlement called with:", {
      tradeId: tradeId,
      tradeIdType: typeof tradeId,
      convertedId: id,
      numericEntityId: Number(id),
      text: text,
    });

    if (!id) throw new Error("No trade id available to save settlement"); // validation
    const payload = {
      entityType: "TRADE",
      entityId: Number(id),
      fieldName: "SETTLEMENT_INSTRUCTIONS",
      fieldValue: text,
    };

    console.debug("Settlement API call:", {
      url: `/trades/${id}/settlement-instructions`,
      payload: payload,
    });

    await api.put(`/trades/${id}/settlement-instructions`, payload);
  };

  const handleClear = () => {
    // After booking/closing, navigate back to the trade actions area
    navigate("/trade");
  };

  return (
    <Layout>
      <div className="w-full p-6">
        <h1 className="text-2xl font-semibold mb-4">Book New Trade</h1>

        {/* FIXED: Add proper settlement management layout matching TradeActionsModal pattern */}
        <div className="flex gap-6">
          {/* Left: Main trade editor */}
          <div className="flex-1">
            <SingleTradeModal
              mode="edit"
              trade={getDefaultTrade()}
              isOpen={true}
              onClear={handleClear}
              settlement={settlement}
              saveSettlement={saveSettlement}
              onTradeUpdated={(updatedTrade) => {
                console.debug(
                  "TradeBooking received trade update:",
                  updatedTrade.tradeId
                );
                // Note: TradeBooking doesn't need to maintain trade state like TradeActionsModal
                // Settlement save happens automatically when "Save Trade" is clicked
              }}
            />
          </div>

          {/* Right: Settlement Instructions panel */}
          <div className="w-80 self-start">
            <div className="p-4 bg-white rounded shadow-md">
              <h3 className="text-lg font-semibold mb-2">
                Settlement Instructions
              </h3>
              <SettlementTextArea
                initialValue={settlement}
                onChange={(text: string) => setSettlement(text)}
              />
              <div className="mt-3">
                <div className="text-sm text-gray-600">
                  Settlement is saved when you click &quot;Save Trade&quot;.
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default TradeBooking;
