import React from "react";
import Layout from "../components/Layout";
import { SingleTradeModal } from "../modal/SingleTradeModal";
import { getDefaultTrade } from "../utils/tradeUtils";
import { useNavigate } from "react-router-dom";
import api from "../utils/api";

const TradeBooking: React.FC = () => {
  const navigate = useNavigate();

  // Parent-provided saveSettlement used by SingleTradeModal when saving
  // trade + settlement together. We keep it simple: persist the
  // settlement via the existing API endpoint. If saving fails the
  // SingleTradeModal will show a retry state.
  const saveSettlement = async (tradeId: string, text: string) => {
    const id = tradeId;
    if (!id) throw new Error("No trade id available to save settlement");
    const payload = {
      entityType: "TRADE",
      entityId: Number(id),
      fieldName: "SETTLEMENT_INSTRUCTIONS",
      fieldValue: text,
    };
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
        {/* Render the SingleTradeModal in edit mode for booking a new trade.
            We pass a default trade so the form is pre-filled with the
            standard structure. */}
        <SingleTradeModal
          mode="edit"
          trade={getDefaultTrade()}
          isOpen={true}
          onClear={handleClear}
          saveSettlement={saveSettlement}
        />
      </div>
    </Layout>
  );
};

export default TradeBooking;
