import React, { useState } from "react";
import { useLocation, useSearchParams, useNavigate } from "react-router-dom";
import Button from "./Button";

const navItems = [
  // Home quick actions (vertical nav on left for the Home page)
  {
    label: "Trade Dashboard",
    aria: "home-dashboard",
    parent: "home",
    path: "/dashboard",
  },
  {
    label: "History",
    aria: "trade-history",
    parent: "trade",
    param: "history",
  },
  {
    label: "User Actions",
    aria: "user-actions",
    parent: "admin",
    param: "user-actions",
  },
  { label: "All Users", aria: "user-all", parent: "admin", param: "user-all" }, // Added for user history
  // Keep other admin / middle-office / support entries but avoid duplicating the "Trade Actions" label
  {
    label: "Static Data Management",
    aria: "static-actions",
    parent: "middle-office",
    param: "static",
  },
  {
    label: "View Trade",
    aria: "view-trade",
    parent: "support",
    param: "actions",
  },
];
//ESLint was giving me a warning that searchParams was assigned but never used. Replacing the name with an empty slot tells TypeScript/ESLint the first return value is intentionally unused while keeping setSearchParams when called available.

const Sidebar = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  // Global buttons that appear in the vertical nav on every page.
  // These provide the horizontal-like top-level navigation in the sidebar.
  // Use explicit search params in the path for actions/history so clicking the global
  // buttons lands on the correct subview of the /trade page.
  const globalItems = [
    { label: "Home", aria: "home", path: "/home" },
    {
      label: "Trade Actions",
      aria: "home-trade-actions",
      path: "/trade?view=actions",
    },
    {
      label: "Trade Dashboard",
      aria: "home-dashboard-global",
      path: "/dashboard",
    },
    { label: "History", aria: "home-history", path: "/trade?view=history" },
  ];

  const handleSideBarClick = (item: { param?: string; path?: string }) => {
    if (item.path) {
      // direct route (dashboard, trade page, etc.)
      navigate(item.path);
    } else if (item.param) {
      // change the current page view using search params
      setSearchParams({ view: item.param });
    }
  };
  const location = useLocation();
  const pathSegments = location.pathname.split("/").filter(Boolean);
  const parentRoot = pathSegments[0];
  // Exclude any nav items whose label is already provided in the globalItems list
  // to avoid duplicate labels in the sidebar.
  const filteredItems = navItems.filter(
    (item) =>
      item.parent === parentRoot &&
      !globalItems.some((g) => g.label === item.label)
  );

  return (
    <aside
      className={`transition-all duration-300 h-screen shadow-lg mt-1 p-4 flex flex-col rounded-2xl ubs-sidebar ${
        collapsed ? "w-16" : "w-64"
      }`}
    >
      <button
        className="mb-4 p-2 h-fit w-fit rounded hover:bg-gray-200 transition self-end flex"
        onClick={() => setCollapsed(!collapsed)}
        aria-label={collapsed ? "Expand Sidebar" : "Collapse Sidebar"}
      >
        <span className="material-icons">
          {collapsed ? "chevron_right" : "chevron_left"}
        </span>
      </button>
      <ul className="space-y-2">
        {/* Global top-level buttons visible on every page */}
        {globalItems.map((item) => (
          <li
            key={item.aria}
            className={`${collapsed ? "hidden" : "text-xl transition"}`}
            aria-label={item.aria}
          >
            <Button
              variant="primary"
              size="md"
              className="w-full text-left !text-black bg-transparent hover:bg-indigo-300  text-grey shadow-none rounded-lg cursor-pointer"
              onClick={() => {
                if (item.path) navigate(item.path);
              }}
            >
              {item.label}
            </Button>
          </li>
        ))}
        {filteredItems.map((item) => (
          <li
            key={item.aria}
            className={`${collapsed ? "hidden" : "text-xl transition"}`}
            aria-label={item.aria}
          >
            <Button
              variant="primary"
              size="md"
              className="w-full text-left !text-black bg-transparent hover:bg-indigo-300  text-grey shadow-none rounded-lg cursor-pointer"
              onClick={() => handleSideBarClick(item)}
            >
              {item.label}
            </Button>
          </li>
        ))}
      </ul>
    </aside>
  );
};

export default Sidebar;
