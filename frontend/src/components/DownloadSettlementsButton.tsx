import React, { useState } from "react";

//Accepts an optional endpoint and authToken
export default function DownloadSettlementsButton(props: {
  endpoint?: string;
  authToken?: string;
}) {
  // endpoint: allow overriding the default URL for testing.Uses the provided endpoint if passed; otherwise uses the default export endpoint
  // Default endpoint: request only non-standard settlements for the
  // authenticated user. Passing `endpoint` prop overrides this default
  // (useful for testing). This keeps the UI focused on TRUE rows by default.
  const endpoint =
    props.endpoint ??
    "/api/trades/exports/settlements?nonStandardOnly=true&mineOnly=true";

  // local UI state: true while the request is in progress.Sets up component state to track whether the download is running. Initially false
  const [loading, setLoading] = useState(false);

  // handler invoked when the user clicks the button
  async function handleDownload() {
    setLoading(true); // show spinner / disable button  and changes its label
    try {
      // Send a GET request to the backend export endpoint.
      // Calls backend credentials: 'include' ensures cookie-based auth is sent (common setup).
      const response = await fetch(endpoint, {
        method: "GET",
        credentials: "include",
        headers: {
          // Ask the server to return CSV where possible. Some servers may
          // ignore this, but it's a helpful hint and ensures I don't
          // accidentally get HTML pages when the backend supports content
          // negotiation.
          Accept: "text/csv, text/plain, */*",
        },
      });

      // Throw if the response is not 2xx so the catch block can show an error
      if (!response.ok) {
        const text = await response.text().catch(() => "");
        //If the server returns an error status (like 401 or 500), read the response text and throw an error so I show a message.
        throw new Error(
          `Export failed: ${response.status} ${response.statusText} ${text}`
        );
      }

      // If the backend returned HTML (for example a login page or an error
      // page served as HTML), abort and show the text so the user sees a
      // helpful message instead of downloading an HTML file named .csv.
      const contentType = response.headers.get("Content-Type") || "";
      if (contentType.toLowerCase().includes("text/html")) {
        const text = await response.text().catch(() => "");
        throw new Error(
          `Export returned HTML instead of CSV: ${text.substring(0, 1000)}`
        );
      }

      // Read the response body as a Blob (binary data) this handles CSV file to create a file.
      const blob = await response.blob();

      // Read the Content-Disposition header sent by the server.
      // If it is missing, fall back to an empty string so the rest of the code still runs safely.
      const contentDisposition =
        response.headers.get("Content-Disposition") || "";

      /*
  Use a small regular expression to extract the filename from the header.
  The regex: /filename="?([^";]+)"?/i

  - filename=         =find the literal text "filename=" in the header
  - "?                =there may or may not be a quotation mark after the =
  - (                 =start capturing group (this is the part I want to extract)
  - [^";]+            =match one or more characters that are NOT " or ;I
                        (this ensures I only capture the filename itself,
                        stopping before a quote or another header attribute)
  - )                 =end capturing group
  - "?                =optionally match a closing quotation mark
  - i                 =case-insensitive (so it matches FILENAME=, Filename=, etc.)
examples:
  - filename="report.csv"        =captures report.csv
  - filename=report.csv          =captures report.csv
  - filename="report.csv"; size= =captures report.csv
*/
      const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/i);

      // Set a sensible fallback filename if the server does not provide one.
      // Uses today’s date in ISO format, e.g. "settlements-2025-11-06.csv".
      let filename = `settlements-${new Date().toISOString().slice(0, 10)}.csv`;

      // If the regular expression found a filename (group 1), use it.
      // filenameMatch[1] contains only the actual filename extracted from the header.
      if (filenameMatch && filenameMatch[1]) {
        filename = filenameMatch[1];
      }

      // Create a temporary object URL for the blob the browser can use to download the blob.
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      //create a link, set it to point to the file, and click it so the browser downloads it. Then clean up the element
      a.href = url;
      a.download = filename; // suggested filename for the browser
      document.body.appendChild(a);
      a.click(); // click the link to start download
      a.remove();
      window.URL.revokeObjectURL(url); // Free the temporary memory used by the object URL
    } catch (err) {
      // Very simple error handling. Replace with a toast in your app.
      console.error(err);
      alert((err as Error).message || "Failed to download CSV");
    } finally {
      setLoading(false); // Whether success or failure, clear the loading state so the button becomes usable again
    }
  }

  // Render a simple button. Keep styling consistent with the app's utilities.
  return (
    <button
      onClick={handleDownload}
      disabled={loading}
      className={`px-3 py-2 rounded shadow ${
        loading ? "bg-gray-300 text-gray-600" : "bg-[#E41E26] text-white"
      }`}
      aria-busy={loading}
    >
      {loading ? "Preparing…" : "Download settlements CSV"}
    </button>
  );
}
