import React from "react";
import { useSearchParams } from "react-router-dom";
import Layout from "../components/Layout";
import { HomeContent } from "../components/HomeContent";
import { TradeActionsModal } from "../modal/TradeActionsModal";

const Support: React.FC = () => {
  const [searchParams] = useSearchParams();
  const view = searchParams.get("view") || "default";
  return (
    <Layout>
      <div className="w-full px-6 py-4">
        <h1 className="text-2xl font-semibold">Support</h1>
      </div>
      {view === "default" && <HomeContent />}
      {view === "actions" && <TradeActionsModal />}
    </Layout>
  );
};

export default Support;
