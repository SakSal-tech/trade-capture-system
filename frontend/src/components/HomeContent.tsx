import React from "react";

// ADDED: Home page quick actions (Search, New Booking, Trade Dashboard).
// These buttons are simple navigation helpers for beginner-friendly demo
// flows and link to the newly added Trade Dashboard route.
//
// NOTE: Previously this file contained local navigation helper functions
// (goToNewBooking, goToSearch, goToDashboard) and a `useNavigate` import.
// Those helpers were removed because they were not referenced anywhere and
// caused ESLint `no-unused-vars` errors during `pnpm build`. Navigation
// is implemented via the left-sidebar and routed components; if these
// helpers are needed again they can be reintroduced in context where
// they are actually used.

export const HomeContent: React.FC = () => {
  return (
    <div className="flex flex-col items-center justify-start w-full h-full pt-12 space-y-6">
      <h1 className="text-4xl font-bold">Welcome to the Trade Platform</h1>
      <p className="text-xl text-gray-700 max-w-2xl text-center">
        This is a simple trading utility to create and view bookings, check
        trade rules, and explore the Trade Dashboard. If you&apos;re new here,
        the quick buttons below will help you get started.
      </p>

      {/* Navigation buttons moved to the left sidebar for consistency across pages. */}

      <div className="max-w-3xl text-center mt-6">
        <h2 className="text-2xl font-semibold mb-2">How to use this site</h2>
        <p className="text-lg text-gray-700">
          The Trade Dashboard gives you a quick snapshot of recent activity and
          exposures. Use the &quot;New Booking&quot; button to create trades:
          the booking form validates key fields so your trade passes downstream
          checks. The Trade Rules page explains common business rules applied to
          bookings. If something looks off, open a trade and use the actions
          menu to view details or submit corrections.
        </p>
      </div>
    </div>
  );
};
