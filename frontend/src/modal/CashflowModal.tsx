// REFACTORED: Cashflow grouping filters made tolerant to DTO label variations .
// Rationale: backend sometimes returns abbreviated pay/rec labels (e.g. "Rec") or
// different casing. This front-end change uses normalized, prefix-based matching
// to avoid hiding legitimately-generated cashflows in the UI. Long-term: centralise
// normalization in a shared util and align backend enums with frontend constants.
/*
 * REFACTORED:
 * - Made pay/receive and paymentType matching tolerant to abbreviations
 *   (e.g. 'Rec' vs 'Receive') using a normaliser and prefix matching.
 * - Added developer-facing comments describing rationale and next steps.
 * - Replaced single-character parameter names to respect the coding
 *   guideline of avoiding one-character variables.
 */
import React from "react";
import { Dialog } from "@headlessui/react";
import { CashflowDTO } from "../utils/tradeTypes";
import { DEFAULT_FALLBACK_FLOATING_RATE } from "../utils/tradeUtils";
import AGGridTable from "../components/AGGridTable";

interface CashflowModalProps {
  isOpen: boolean;
  onClose: () => void;
  cashflows: CashflowDTO[];
}

const CashflowModal: React.FC<CashflowModalProps> = ({
  isOpen,
  onClose,
  cashflows,
}) => {
  // Normaliser parameter renamed from `s` to `inputStr` to avoid
  // one-character variable names per project coding guidelines.
  const norm = (inputStr?: string) =>
    (inputStr ?? "").toString().trim().toLowerCase();

  /**
   * REFACTOR :
   * The cashflow grouping logic used to rely on strict, full-word
   * equality checks (e.g. payRec === 'receive'). In practice the
   * backend/DTO sometimes returns abbreviated labels (e.g. 'Rec') or
   * slightly different casing. That caused valid leg-2 cashflows to be
   * filtered out and the right-hand table to show "No Rows To Show".
   *
   * Change summary:
   * - Introduce a small normaliser `norm` which lowercases and trims
   *   incoming strings.
   * - Use prefix-based matching (.startsWith) for pay/rec and
   *   paymentType to make the matching tolerant of abbreviations and
   *   minor variations (e.g. 'Rec' / 'Receive', 'Floating' / 'Float').
   *
   * Rationale:
   * - This is a low-risk UI-side fix that restores visibility for
   *   legitimately-generated cashflows while keeping the backend DTO
   *   unchanged. A longer-term approach would be to normalise enums
   *   on the backend and use a shared constant/enum on the frontend.
   */

  // Group cashflows primarily by pay/receive value. This ensures that
  // Leg 1 shows all 'pay' cashflows and Leg 2 shows all 'rec'/'receive'
  // cashflows regardless of whether the payment type is Fixed or
  // Floating. This makes the modal resilient to different trade
  // configurations (e.g. Fixed/Fixed, Fixed/Floating).
  const leg1Cashflows = cashflows.filter((cashflow) =>
    norm(cashflow.payRec).startsWith("pay")
  );

  const leg2Cashflows = cashflows.filter((cashflow) =>
    norm(cashflow.payRec).startsWith("rec")
  );
  // Detect whether any cashflows for a leg used the demo fallback rate.
  // Toconsider the fallback used when the paymentType is 'floating'
  // (case-insensitive) and the rate exactly equals the shared demo rate.
  // this heuristic is intentionally simple integrate a
  // proper RateProvider, remove this UI-only label logic.
  const usedFallback = (cfs: CashflowDTO[]) =>
    cfs.some(
      (cf) =>
        cf &&
        (cf.paymentType ?? "").toString().toLowerCase().startsWith("float") &&
        cf.rate === DEFAULT_FALLBACK_FLOATING_RATE
    );
  const leg1UsedFallback = usedFallback(leg1Cashflows);
  const leg2UsedFallback = usedFallback(leg2Cashflows);
  const columnDefs = [
    { headerName: "Value Date", field: "valueDate" },
    { headerName: "Payment Value", field: "paymentValue" },
    { headerName: "Pay/Rec", field: "payRec" },
    { headerName: "Type", field: "paymentType" },
    { headerName: "BDC", field: "paymentBusinessDayConvention" },
    { headerName: "Rate", field: "rate" },
  ];
  return (
    <Dialog
      open={isOpen}
      onClose={onClose}
      className="fixed inset-0 z-50 flex items-start justify-center"
    >
      <div
        aria-hidden="true"
        className={`fixed inset-0 bg-black/30 transition-opacity duration-300 ${
          isOpen ? "opacity-100" : "opacity-0"
        }`}
      />
      <div
        className="relative bg-white rounded-lg shadow-lg mt-10 p-6 transition-transform duration-400 ease-in-out transform animate-slide-down-tw"
        style={{ width: "1200px", maxWidth: "98vw" }}
      >
        <Dialog.Title className="text-xl font-bold mb-4">
          Generated Cashflows
        </Dialog.Title>
        <div className="flex flex-row gap-8">
          <div className="flex-1">
            <div className="font-semibold text-center mb-2">
              Leg 1 Cashflows
              {leg1UsedFallback && (
                <div className="text-xs text-yellow-700 mt-1">
                  values computed using demo fallback rate (
                  {(DEFAULT_FALLBACK_FLOATING_RATE * 100).toFixed(2)}
                  %)
                </div>
              )}
            </div>
            <AGGridTable
              columnDefs={columnDefs}
              rowData={leg1Cashflows}
              rowSelection="single"
            />
          </div>
          <div className="flex-1">
            <div className="font-semibold text-center mb-2">
              Leg 2 Cashflows
              {leg2UsedFallback && (
                <div className="text-xs text-yellow-700 mt-1">
                  values computed using demo fallback rate (
                  {(DEFAULT_FALLBACK_FLOATING_RATE * 100).toFixed(2)}
                  %)
                </div>
              )}
            </div>
            <AGGridTable
              columnDefs={columnDefs}
              rowData={leg2Cashflows}
              rowSelection="single"
            />
          </div>
        </div>
        <div className="flex justify-end mt-4">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-800"
          >
            Close
          </button>
        </div>
      </div>
      <style>{`
        .animate-slide-down-tw {
          @apply translate-y-[-100px] opacity-0;
          animation: slideDownTW 0.4s cubic-bezier(0.4, 0, 0.2, 1) forwards;
        }
        @keyframes slideDownTW {
          0% { transform: translateY(-100px); opacity: 0; }
          100% { transform: translateY(0); opacity: 1; }
        }
      `}</style>
    </Dialog>
  );
};

export default CashflowModal;
