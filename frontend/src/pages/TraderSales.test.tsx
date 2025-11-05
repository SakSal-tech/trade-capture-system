import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/react";
import "@testing-library/jest-dom"; // <-- add this to enable toBeInTheDocument
import TraderSales from "./TraderSales";
// import React from "react"; // unused, remove
import { renderWithRouter } from "../utils/test-utils";

describe("TraderSales Page", () => {
  it("renders the welcome heading", () => {
    renderWithRouter(<TraderSales />);
    expect(
      screen.getByText(/Welcome to the Trade Platform/i)
    ).toBeInTheDocument();
  });
});
