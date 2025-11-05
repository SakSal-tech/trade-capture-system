import React from "react";
import { useSearchParams } from "react-router-dom";
import Layout from "../components/Layout";
import { TradeBlotterModal } from "../modal/TradeBlotterModal";
import { TradeActionsModal } from "../modal/TradeActionsModal";

const TraderSales = () => {
  const [searchParams] = useSearchParams();
  const view = searchParams.get("view") || "default";

  return (
    <div>
      <Layout>
        {view === "default" && (
          <div className="w-full flex flex-col items-start gap-6 px-8 py-6">
            <h1 className="text-3xl font-bold">
              Welcome to the Trade Platform
            </h1>

            <section className="w-full">
              <h2 className="text-2xl font-semibold mb-2">
                How to search for an existing trade
              </h2>
              {/* Escaped apostrophe in "don't" to satisfy react/no-unescaped-entities
                  ESLint rule. This prevents build-time lint errors. */}
              <p className="text-lg text-gray-700">
                Use the Search option in the left navigation to find trades by
                trade id, counterparty, or date. If you don&apos;t see a trade,
                try widening the date range.
              </p>
            </section>

            <section className="w-full">
              <h2 className="text-2xl font-semibold mb-2">
                How to add settlement details
              </h2>
              {/* Escaped inner quotes to avoid react/no-unescaped-entities lint
                  failures. Keeping text readable while satisfying the linter. */}
              <p className="text-lg text-gray-700">
                Open the trade and select &quot;Edit&quot; or &quot;Add
                settlement&quot;. Fill in the settlement date, currency and
                amount. The system will validate required fields and notify you
                if anything is missing or inconsistent.
              </p>
            </section>

            <section className="w-full">
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

            <section className="w-full">
              <h2 className="text-2xl font-semibold mb-2">
                How to book a new trade
              </h2>
              {/* Escaped double quotes here as well to keep the JSX lint rule
                  satisfied and avoid build blockers. */}
              <p className="text-lg text-gray-700 mb-3">
                Click &quot;Book New&quot; from the Trade Actions area to create
                a trade. The form checks important business rules before the
                trade is accepted below is a friendly list of the key checks.
              </p>

              <ul className="list-disc pl-6 space-y-2 text-lg text-gray-700">
                <li>
                  <strong>
                    Both legs must have identical maturity dates:{" "}
                  </strong>{" "}
                  each side of the trade should settle on the same date.
                </li>
                <li>
                  <strong>Legs must have opposite pay/receive flags: </strong>
                  one side pays and the other receives.
                </li>
                <li>
                  <strong>Floating legs must have an index specified: </strong>
                  enter the appropriate reference rate (e.g. LIBOR-like index).
                </li>
                <li>
                  <strong>Fixed legs must have a valid rate: </strong> enter the
                  agreed fixed rate for the fixed leg.
                </li>
              </ul>

              <h3 className="text-xl font-semibold mt-4">
                Entity status checks
              </h3>
              <ul className="list-disc pl-6 space-y-2 text-lg text-gray-700">
                <li>
                  <strong>User, book, and counterparty must be active: </strong>
                  trades can only be booked against active records in the
                  system.
                </li>
                <li>
                  <strong>All reference data must exist and be valid: </strong>
                  currencies, indices, and other lookup values must be present.
                </li>
              </ul>
            </section>
          </div>
        )}
        {view === "actions" && <TradeActionsModal />}
        {view === "history" && <TradeBlotterModal />}
      </Layout>
    </div>
  );
};

export default TraderSales;
