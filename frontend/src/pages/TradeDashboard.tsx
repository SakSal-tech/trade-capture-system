import React, { useEffect, useState } from "react";
import Layout from "../components/Layout";
import { useNavigate } from "react-router-dom";
import userStore from "../stores/userStore";
import { AxiosError } from "axios";
import {
  getDashboardMyTrades,
  getDashboardSummary,
  getDashboardDailySummary,
} from "../utils/api";
// ADDED: TradeDashboard scaffold page. Simple dashboard with summary cards,
// charts and a recent trades table. Charts use Recharts and sample data so
// the page works without backend integration yet.
import {
  ResponsiveContainer,
  LineChart,
  Line,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  PieChart,
  Pie,
  Legend,
  Cell,
} from "recharts";
import DownloadSettlementsButton from "../components/DownloadSettlementsButton";

function SummaryCard(props: { title: string; value: string }) {
  // Small reusable card used in the dashboard summary row. Keeps styling
  // consistent across the Total Trades / Notional / Active Books / Exposure cards.
  return (
    <div className="bg-white rounded-lg shadow p-4">
      <div className="text-sm text-gray-500">{props.title}</div>
      <div className="text-2xl font-bold mt-1">{props.value}</div>
    </div>
  );
}

// Format a numeric or string value as USD currency where possible.
// If the backend provides a non-numeric delta (e.g. a string message) we
// display it as-is. If the value is missing I show a dash to avoid
// presenting a hardcoded placeholder number in the UI.
function formatCurrency(value?: number | string | null) {
  if (value === undefined || value === null) return "-";
  //Refactored from any type after error message.  If it's already a number use it directly
  // (the equivalent of a double). For display I use the built-in
  // toLocaleString formatter. If the backend sends a non-numeric string
  //  it should return it as it is e.g : if the value is a non-numeric string (for example "N/A"), don't try to format it as USD
  if (typeof value === "number") {
    if (Number.isNaN(value)) return String(value);
    //value = "1234", parses to number and returns "$1,234.00"
    return value.toLocaleString(undefined, {
      style: "currency",
      currency: "USD",
    });
  }

  // value is a string here (or other non-number), try to parse a numeric
  // representation. Avoid casting to `any` to satisfy ESLint rules.
  const n = Number(value);
  if (Number.isNaN(n)) return String(value);
  return n.toLocaleString(undefined, { style: "currency", currency: "USD" });
}
// Minimal types to avoid explicit `any` while keeping the page flexible.
// Introduced to remove usages of `any` which trigger the
// `@typescript-eslint/no-explicit-any` rule during the build. These
// small local types provide enough structure for the dashboard UI without
// importing full backend DTOs and help satisfy lint/type checks.
type WeeklyComparison = { tradeCount?: number };
type DashboardSummary = {
  weeklyComparisons?: WeeklyComparison[];
  notionalByCurrency?: Record<string, number | string>;
  bookActivitySummaries?: Record<string, unknown>;
  riskExposureSummary?: { delta?: number | string };
  todaysTradeCount?: number;
  tradesByTypeAndCounterparty?: Record<string, number | string>;
  totalActiveBooks?: number;
};
type DailySummary = {
  todaysTradeCount?: number;
  todaysNotionalByCurrency?: Record<string, number | string>;
};

function TradeDashboard() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [myTrades, setMyTrades] = useState<Record<string, unknown>[] | null>(
    null
  );
  // UI state: which summary panel (if any) is currently open.
  // - 'weekly' = show the weekly summary using the already-fetched `summary` object
  // - 'daily' = show the daily summary (fetched on demand into `dailySummary`)
  const [selectedSummary, setSelectedSummary] = useState<
    "weekly" | "daily" | null
  >(null);

  // Stores the payload returned by getDashboardDailySummary when the user
  // requests the Daily Summary. Kept separate from `summary` to avoid
  // overwriting the weekly aggregates already loaded on page mount.
  const [dailySummary, setDailySummary] = useState<DailySummary | null>(null);

  function goToTrades() {
    // The standalone /trade route was removed; send users to Home where
    // they can access trade actions via the Trade Actions area.
    navigate("/home");
  }

  function goToBooks() {
    navigate("/books");
  }

  useEffect(() => {
    // Fetch live dashboard data for the signed-in user if available.
    // This runs once when the userStore.user is available. We load two
    // things in parallel using Promise.all:
    // - getDashboardSummary: weekly aggregates and per-currency totals
    // - getDashboardMyTrades: recent trades for the user's blotter
    // The function is defensive: if the fetch fails log and keep sample data.
    const loginId = userStore.user?.loginId;
    if (!loginId) return; // nothing to do until user is signed in

    let mounted = true;
    async function load() {
      setLoading(true);
      try {
        const [summaryRes, tradesRes] = await Promise.all([
          getDashboardSummary(loginId!),
          getDashboardMyTrades(loginId!),
        ]);
        if (!mounted) return;

        // DEBUG: Log the actual response data to understand the structure
        console.debug("Dashboard summary response:", summaryRes.data);
        console.debug("Dashboard trades response:", tradesRes.data);

        setSummary(summaryRes.data);
        setMyTrades(Array.isArray(tradesRes.data) ? tradesRes.data : []);
      } catch (e) {
        // ENHANCED: Show more detailed error information
        console.error("Failed to load dashboard data:", e);
        if (e && typeof e === "object" && "response" in e) {
          const axiosError = e as AxiosError;
          console.error("API Error Status:", axiosError.response?.status);
          console.error("API Error Data:", axiosError.response?.data);
        }
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, [userStore.user]);

  return (
    <Layout>
      <div className="w-full p-6">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-semibold">UBS Trader Dashboard</h1>
          <div className="flex items-center gap-4">
            {/* Brief guidance label next to the summary buttons. This helps
                users understand that the buttons will show a weekly or daily
                summary when clicked. Kept short to avoid UI clutter. */}
            <div className="text-sm text-gray-600">
              Click &quot;Weekly Summary&quot; to view weekly aggregates; click
              &quot;Daily Summary&quot; to fetch today&apos;s summary.
            </div>

            <div className="flex gap-2">
              {/* Weekly Summary button: toggles the inline weekly summary panel. */}
              <button
                className={`px-3 py-2 rounded shadow ${
                  selectedSummary === "weekly"
                    ? "bg-[#E41E26] text-white"
                    : "bg-gray-200 text-gray-800"
                }`}
                onClick={() => setSelectedSummary("weekly")}
              >
                Weekly Summary
              </button>

              {/* Daily Summary button: when clicked fetch a small daily
                  summary payload (todaysTradeCount, todaysNotionalByCurrency).
                  We fetch on-demand to avoid extra requests during page load. */}
              <button
                className={`px-3 py-2 rounded shadow ${
                  selectedSummary === "daily"
                    ? "bg-[#E41E26] text-white"
                    : "bg-gray-200 text-gray-800"
                }`}
                onClick={async () => {
                  setSelectedSummary("daily");
                  try {
                    const loginId = userStore.user?.loginId;
                    if (!loginId) return;
                    const res = await getDashboardDailySummary(loginId);
                    // dailySummary contains only today's numbers; keep it
                    // separate from `summary` which stores weekly aggregates.
                    setDailySummary(res.data);
                  } catch (e) {
                    console.debug("Failed to load daily summary:", e);
                    setDailySummary(null);
                  }
                }}
              >
                Daily Summary
              </button>
            </div>
            {/* Add the export button here so users can quickly download settlements CSV */}
            <DownloadSettlementsButton />
          </div>
        </div>

        {/* Summary toggle panel: shows either weekly or daily summary details when selected */}
        {selectedSummary && (
          <div className="mb-6">
            <div className="bg-white rounded-lg shadow p-4">
              <div className="flex items-center justify-between mb-2">
                <div className="text-lg font-semibold">
                  {selectedSummary === "weekly"
                    ? "Weekly Summary"
                    : "Daily Summary"}
                </div>
                <div>
                  <button
                    className="px-2 py-1 text-sm text-gray-600"
                    onClick={() => {
                      setSelectedSummary(null);
                      setDailySummary(null);
                    }}
                  >
                    Close
                  </button>
                </div>
              </div>

              {selectedSummary === "weekly" ? (
                <div>
                  <div className="text-sm text-gray-700 mb-2">
                    Total trades this week:{" "}
                    {/* Use WeeklyComparison to avoid `any` in the reducer and
                        make the reducer's intent explicit: each item may have
                        an optional numeric `tradeCount`. */}
                    {summary?.weeklyComparisons?.reduce(
                      (a: number, b: WeeklyComparison) =>
                        a + (b.tradeCount || 0),
                      0
                    ) ?? "-"}
                  </div>
                  <div className="text-sm text-gray-600">
                    {/* Escaped quotes/apostrophes here to satisfy ESLint's
                        react/no-unescaped-entities rule. Unescaped quotes in
                        JSX text cause lint errors and block the build. */}
                    Click &quot;Weekly Summary&quot; to view weekly aggregates;
                    click &quot;Daily Summary&quot; to fetch today&apos;s
                    summary.
                  </div>
                  <div className="text-sm text-gray-700">
                    Notional by currency:{" "}
                    {summary?.notionalByCurrency
                      ? Object.entries(summary.notionalByCurrency)
                          .map(
                            ([k, v]) => `${k}: ${Number(v).toLocaleString()}`
                          )
                          .join(" • ")
                      : "-"}
                  </div>
                </div>
              ) : (
                <div>
                  <div className="text-sm text-gray-700 mb-2">
                    Today&apos;s trades:{" "}
                    {dailySummary?.todaysTradeCount ??
                      summary?.todaysTradeCount ??
                      "-"}
                  </div>
                  <div className="text-sm text-gray-700">
                    Today&apos;s notional by currency:{" "}
                    {dailySummary?.todaysNotionalByCurrency
                      ? Object.entries(dailySummary.todaysNotionalByCurrency)
                          .map(
                            ([k, v]) => `${k}: ${Number(v).toLocaleString()}`
                          )
                          .join(" • ")
                      : "-"}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        <div className="grid grid-cols-4 gap-4 mb-6">
          <SummaryCard
            title="Total Trades"
            value={
              myTrades && Array.isArray(myTrades)
                ? String(myTrades.length)
                : "-"
            }
          />
          <SummaryCard
            title="Total Notional"
            value={
              summary && summary.notionalByCurrency
                ? // sum notional values across currencies (best-effort parsing)
                  Object.values(summary.notionalByCurrency)
                    .map((v: unknown) => Number((v as number) ?? 0))
                    .reduce((a: number, b: number) => a + b, 0)
                    .toLocaleString(undefined, {
                      style: "currency",
                      currency: "USD",
                    })
                : "$-"
            }
          />
          <SummaryCard
            title="Active Books"
            value={(() => {
              // FIXED: More robust Active Books count with better error handling
              if (!summary) {
                return loading ? "Loading..." : "0";
              }

              // Check if bookActivitySummaries exists and is an object
              if (
                summary.bookActivitySummaries &&
                typeof summary.bookActivitySummaries === "object"
              ) {
                const count = Object.keys(summary.bookActivitySummaries).length;
                console.debug(
                  "Active Books count:",
                  count,
                  "from:",
                  summary.bookActivitySummaries
                );
                return String(count);
              }

              // Fallback: check other possible fields for book count
              if (summary.totalActiveBooks !== undefined) {
                return String(summary.totalActiveBooks);
              }

              console.warn("No book activity data found in summary:", summary);
              return "0";
            })()}
          />
          <SummaryCard
            title="Risk Net Exposure"
            value={formatCurrency(summary?.riskExposureSummary?.delta)}
          />
        </div>

        <div className="grid grid-cols-3 gap-4">
          <div className="col-span-2 bg-white rounded-lg shadow p-4">
            <div className="text-lg font-semibold mb-2">Daily Activity</div>
            <div className="h-48">
              {/* ADDED: Recharts LineChart to visualise daily trade counts.
                  Data mapping notes:
                  - The backend model provides `weeklyComparisons` as an array
                    of objects containing `tradeCount` for the last N days.
                  - The code maps those values to {date, tradeCount} labels
                    by constructing ISO date labels for the last N days.
                  - If `summary` is unavailable fall back to sample data
                    so the chart still renders during development. */}
              <ResponsiveContainer width="100%" height={180}>
                <LineChart
                  data={
                    summary && Array.isArray(summary.weeklyComparisons)
                      ? // map weekly comparisons to {date, tradeCount}. We don't get dates from the API model,
                        // so build dates for the last N days (oldest -> newest).
                        summary.weeklyComparisons.map(
                          (item: WeeklyComparison, idx: number, arr) => {
                            // Use the mapped array `arr` to calculate the index-based
                            // label rather than referencing `summary.weeklyComparisons`
                            // directly. This avoids a TypeScript 'possibly undefined'
                            // error while preserving the original semantics.
                            const daysAgo = arr.length - 1 - idx;
                            const d = new Date();
                            d.setDate(d.getDate() - daysAgo);
                            const label = d.toISOString().slice(0, 10);
                            return {
                              date: label,
                              tradeCount: item.tradeCount ?? 0,
                            };
                          }
                        )
                      : [
                          { date: "2025-10-28", tradeCount: 5 },
                          { date: "2025-10-29", tradeCount: 8 },
                          { date: "2025-10-30", tradeCount: 4 },
                          { date: "2025-10-31", tradeCount: 6 },
                          { date: "2025-11-01", tradeCount: 7 },
                        ]
                  }
                >
                  <CartesianGrid stroke="#f0f0f0" />
                  <XAxis dataKey="date" />
                  <YAxis />
                  <Tooltip />
                  <Line
                    type="monotone"
                    dataKey="tradeCount"
                    stroke="#E41E26"
                    strokeWidth={2}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-4">
            <div className="text-lg font-semibold mb-2">
              Counterparty Breakdown
            </div>
            <div className="h-48">
              {/* ADDED: Recharts PieChart showing distribution by counterparty.
                  Implementation notes:
                  - We derive `counterpartyData` from `summary.tradesByTypeAndCounterparty`.
                  - A fixed small colour palette (`COUNTERPARTY_COLORS`) is used
                    to ensure visually distinct slices. 
              */}
              <ResponsiveContainer width="100%" height={180}>
                <PieChart>
                  {/* Build data once so can map colours to cells */}
                  {(() => {
                    const counterpartyData =
                      summary && summary.tradesByTypeAndCounterparty
                        ? Object.entries(
                            summary.tradesByTypeAndCounterparty ?? {}
                          ).map(([k, v]) => ({ name: k, value: Number(v) }))
                        : [
                            { name: "Counterparty A", value: 45 },
                            { name: "Counterparty B", value: 30 },
                            { name: "Counterparty C", value: 25 },
                          ];

                    const COUNTERPARTY_COLORS = [
                      "#E41E26",
                      "#2CA02C",
                      "#1F77B4",
                      "#FF7F0E",
                      "#9467BD",
                      "#8C564B",
                    ];

                    return (
                      <>
                        <Pie
                          data={counterpartyData}
                          dataKey="value"
                          nameKey="name"
                          cx="50%"
                          cy="50%"
                          outerRadius={60}
                          label
                        >
                          {counterpartyData.map((entry, index) => (
                            <Cell
                              key={`cell-${index}`}
                              fill={
                                COUNTERPARTY_COLORS[
                                  index % COUNTERPARTY_COLORS.length
                                ]
                              }
                            />
                          ))}
                        </Pie>
                        {/* Add a legend so users can see counterparty names and colours */}
                        <Legend verticalAlign="bottom" height={36} />
                        <Tooltip />
                      </>
                    );
                  })()}
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        <div className="mt-6 bg-white rounded-lg shadow p-4">
          <div className="text-lg font-semibold mb-2">Recent Trades</div>
          <div className="overflow-x-auto">
            <table className="min-w-full text-left">
              <thead>
                <tr className="text-sm text-gray-600 border-b">
                  <th className="py-2">Trade ID</th>
                  <th>Type</th>
                  <th>Counterparty</th>
                  <th>Book</th>
                  <th>Notional</th>
                  <th>Status</th>
                  <th>Maturity</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="py-2">T-1001</td>
                  <td>Swap</td>
                  <td>Counterparty A</td>
                  <td>Book A</td>
                  <td>$5,000,000</td>
                  <td>Active</td>
                  <td>2026-11-01</td>
                </tr>
                <tr className="border-t">
                  <td className="py-2">T-1002</td>
                  <td>Swap</td>
                  <td>Counterparty B</td>
                  <td>Book B</td>
                  <td>$3,000,000</td>
                  <td>Active</td>
                  <td>2026-05-20</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </Layout>
  );
}

export default TradeDashboard;
