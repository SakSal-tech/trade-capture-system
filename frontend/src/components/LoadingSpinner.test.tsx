import { describe, it, expect } from "vitest";
import { render } from "@testing-library/react";
import * as matchers from "@testing-library/jest-dom/matchers";
import LoadingSpinner from "./LoadingSpinner";

expect.extend(matchers);

describe("LoadingSpinner Component", () => {
  it("renders the spinner svg", () => {
    const { container } = render(<LoadingSpinner />);
    expect(container.querySelector("svg")).not.toBeNull();
  });

  it("renders without throwing", () => {
    render(<LoadingSpinner />);
    // Test passes if component renders without throwing
  });
});
