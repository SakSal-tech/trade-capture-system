import { FC, useState, useRef, useEffect, ChangeEvent } from "react"; // React: JSX runtime
// Functional Component type for typing the component
// useState: controlled value storage for the textarea
// useRef: hold a reference to the <textarea> so can insert at cursor
// useEffect: small lifecycle needs (e.g., focus or syncing initial value)
// ChangeEvent: Type for the textarea onChange handler (keeps TS happy)
// ChangeEvent: Type for the textarea onChange handler (keeps TS happy)
import Button from "../components/Button";
// This component will use plain elements so no other imports are required.

//To type the component props so callers know what to pass and I  get autocompletion. it defines the "shape" (the property names and types). typing props gives me editor autocompletion and lets TypeScript check callers pass the correct fields (helps prevent bugs
export interface SettlementTextareaProps {
  initialValue?: string; //Optional prop to pre-fill the textarea (e.g., existing settlement instructions when editing a trade)
  templates?: { value: string; label: string }[]; // ? means Optional array of templates the component can show in a dropdown; each template has a machine
  // Optional callback the parent can pass to save the settlement instructions.
  // Allows the component to remain UI-only when not provided.
  onSave?: (text: string) => Promise<void> | void;
  onChange?: (text: string) => void; //Added: to pass onChange from TradeActionModel
}
//types the variable as SettlementTextareaProps meaning TypeScript what kind of value a variable will hold. This says “the variable SettlementTextArea has the type FC<SettlementTextareaProps. So TypeScript will error if callers pass wrong props or if I use the props incorrectly inside the component
export const SettlementTextArea: FC<SettlementTextareaProps> = ({
  initialValue = "",
  templates = [], //Destructuring: take props, pull out initialValue and templates, and if they're undefined give them default values
  onSave,
  onChange,
}) => {
  // REFACTOR
  // This component was intentionally refactored to be a UI-only, controlled
  // textarea with helper features (templates, insert-at-cursor, validation)
  // and an optional `onSave` callback. The component does not perform any
  // network calls itself; instead it delegates persistence to a parent
  // handler. Rationale:
  // - Separation of concerns: UI logic stays in this component, networking
  //   and transactional flow remain in the parent modal.
  // - Testability: the component can be unit-tested without mocking HTTP.
  // - Consistency: the parent can orchestrate saving settlement together
  //   with the associated trade (single Save Trade action).
  //
  // USER-FRIENDLY REFACTOR SUMMARY:
  // This refactor was driven by UX goals – the textarea and template picker
  // are explicitly designed to be easy to discover, read and operate on by
  // traders. Key user-friendly choices made here:
  // - Controlled component model: keeps the parent and textarea in sync so
  //   template inserts always appear in the outgoing DTO.
  // - Templates insert at caret and restore focus/caret reliably (requestAnimationFrame)
  //   so users can quickly insert and edit standard instructions.
  // - Increased template control visibility (larger font, accent background,
  //   focus ring and chevron) so the picker is easy to find and use.
  // - Validation uses trimmed length and explicit forbidden characters to
  //   provide immediate, clear feedback before network requests.
  // - Accessibility: label + aria-describedby and keyboard focus styles were
  //   added so the control is usable by keyboard and screen-reader users.
  // The overall goal: make settlement editing faster, less error-prone and more
  // discoverable while keeping persistence and audit responsibilities in the parent service.
  const textareaRef = useRef<HTMLTextAreaElement | null>(null); //useRef is a React hook that gives a stable, per-instance ref object.

  // A React state hook that creates a piece of component state named value and an updater function setValue. initialValue: the runtime initial value for that state (comes from props via destructuring).
  const [value, setValue] = useState<string>(initialValue);
  //This is used only on first render if the initialValue prop later changes, state won't update automatically unless I use a useEffect
  const [touched, setTouched] = useState<boolean>(false); //Reset touched when deliberately replacing the content (e.g., switching trades, after successful save)

  useEffect(() => {
    // Only overwrite internal state when the user hasn't interacted. This lets the parent
    // update the initial value (e.g., when changing which trade is loaded) but won't stomp
    // the user's in-progress edits.
    if (!touched) setValue(initialValue);
  }, [initialValue, touched]);
  // Controlled value for the templates <select>. Making the select controlled makes it easier
  // to reset programmatically and simpler to write deterministic tests. Leave empty string
  // as the placeholder value so the first visible option is the prompt.
  const [templateSelectValue, setTemplateSelectValue] = useState<string>("");
  //state to hold the matched keyword (or null)
  const [nonStandardKeyword, setNonStandardKeyword] = useState<string | null>(
    null
  );
  //start of an array of indicator phrases (UBS to extend as business rule)
  const nonStandardIndicators = [
    // Keep only the intentionally non-standard markers here. The JPM "Further Credit" template
    // is considered standard for our flows, so do NOT include "further credit".
    // This array can be extended later via config or a server-side ruleset.
    "manual",
    "non-dvp",
    "non dvp",
  ];
  // detects nonstandard settlement as extra layer client side validation security. showing a yellow banner while the user types avoids waiting for network latency and makes booking safer and faster
  function runNonStandardDetection(settlementText: string): string | null {
    // convert the whole settlement text to lowercase so comparisons are case-insensitive
    const lower = settlementText.toLowerCase();

    for (let i = 0; i < nonStandardIndicators.length; i++) {
      // search the string for the substring nonStandardIndicators[i]
      if (lower.indexOf(nonStandardIndicators[i]) !== -1) {
        // set the state to the matched phrase (e.g., "manual") and return it
        setNonStandardKeyword(nonStandardIndicators[i]);
        return nonStandardIndicators[i];
      }
    }

    // no indicators matched clear previous match and return null
    setNonStandardKeyword(null);
    return null;
  }

  // Ensure detection runs whenever the textarea value changes (covers parent-driven
  // updates, template inserts and programmatic clears). This keeps the banner
  // visibility consistent with the current content.
  useEffect(() => {
    runNonStandardDetection(value);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  //Reads the string from the textarea event and calls setValue(...) to update component state so the textarea can be used as a controlled component.
  function handleChange(evt: ChangeEvent<HTMLTextAreaElement>) {
    const newValue = evt.currentTarget.value;
    setValue(newValue);
    // Mark as touched when the user types so validation/Save state updates immediately
    // (keeps UX responsive rather than waiting for blur).
    setTouched(true);
    onChange?.(newValue); // To inform parent every time the text changes
    // run client-side detector for immediate feedback
    runNonStandardDetection(newValue);
  }

  // the component remains UI-only and delegates persistence to the parent.
  // Reference `onSave` so linters do not mark it as unused when callers omit it.
  useEffect(() => {
    void onSave;
  }, [onSave]);
  // Take a template (or any text) and insert it into  textarea at the current caret/selection position. ChangeEvent<HTMLTextAreaElement>, which gives the handler a correctly typed event.
  function insertAtCursor(text: string) {
    const txtArea = textareaRef.current;
    //If null (the DOM node isn’t available e.g., not mounted, ref not attached, or running on server
    if (!txtArea) {
      // Update internal state and inform parent when no DOM ref is available
      const newVal = (value || "") + text;
      setValue(newVal);
      onChange?.(newVal);
      return;
    }
    //reads the settlement-instructions textarea caret/selection start index (an integer). If that value is null/undefined, it falls back to the end of the current text (txtArea.value.length).
    const start = txtArea.selectionStart ?? txtArea.value.length;
    const end = txtArea.selectionEnd ?? start; //reads: the selection end index.Insert-at-cursor: puts templates exactly where the trader wants (improves speed and accuracy for settlement info).

    //takes the previous textarea string and produce a new string where the substring from index start to end is replaced by text, keeps everything before the caret/selection, + text is the inserted template, + prevValue.slice(end) keeps everything after the selection. Insert-at-cursor: inserts exactly where the trader placed the caret or replaces the highlighted selection (so a trader can replace an old beneficiary line with the UBS template in one action).

    //
    // Insert the template at the caret/selection using a functional state updater
    // so never clobber concurrent updates. We calculate the new caret position
    // (start + text.length) now and restore it after React updates the DOM below.
    // Compute the new value deterministically from the textarea DOM so can
    // synchronously inform the parent via onChange with the exact new content.
    // Prevent accidental concatenation when inserting templates next to existing
    // text (e.g. "...team for" + "Settle via JPM..."). If the character
    // before the insertion point is not whitespace and the template doesn't
    // start with whitespace, add a single separating space. Likewise, if the
    // character after the selection is not whitespace and the template doesn't
    // end with whitespace, append a separating space.
    const beforeChar = start > 0 ? txtArea.value.charAt(start - 1) : null;
    const afterChar =
      end < txtArea.value.length ? txtArea.value.charAt(end) : null;

    let adjustedText = text;
    if (beforeChar && !/\s/.test(beforeChar) && !/^\s/.test(adjustedText)) {
      adjustedText = " " + adjustedText;
    }
    if (afterChar && !/\s/.test(afterChar) && !/\s$/.test(adjustedText)) {
      adjustedText = adjustedText + " ";
    }

    const newValue =
      txtArea.value.slice(0, start) + adjustedText + txtArea.value.slice(end);
    setValue(newValue);
    onChange?.(newValue);

    // Run client-side detection after inserting a template so the warning
    // appears immediately when a template contains non-standard phrases.
    runNonStandardDetection(newValue);

    // 1) Mark the field as "touched" because template insertion is explicit user input.
    //    This ensures inline validation and "Save" enablement behave consistently.
    setTouched(true);

    // 2) Restore focus and move the caret to the end of the inserted text.
    //    We use requestAnimationFrame so the browser has applied the DOM value change
    //    before set selection; this produces reliable caret placement across browsers.
    const newCaretPos = start + adjustedText.length;
    requestAnimationFrame(() => {
      if (!txtArea) return; // defensive check; txtArea was captured above
      try {
        txtArea.focus();
        txtArea.setSelectionRange(newCaretPos, newCaretPos);
      } catch {
        // setSelectionRange can throw on some older mobile browsers; ignore silently
        // because the insertion itself is still correct and editable.
      }
    });
    // already used requestAnimationFrame above to focus and set selection.
    // The duplicate call was removed to avoid running the focus/selection twice.
  }

  // The array exists to speed and standardise trader input if UBS prefers traders always type from scratch, I can remove it or replace it with an empty.

  const defaultTemplates = [
    {
      value:
        "Settle via JPM New York, Account: 123456789, Further Credit: ABC Corp Trading Account",
      label: "UBS JPM - Beneficiary (IBAN placeholder)",
    },
    {
      // added explicit 'manual' phrase so client-side detector flags this template
      value:
        "DVP settlement through Euroclear, ISIN confirmation required before settlement, manual processing required",
      label: "UBS DVP - Our account (local)",
    },
    {
      // added explicit 'non-dvp' marker to make this template intentionally non-standard
      value:
        "PAYMENT: UBS AG / UBS ADDRESS: Bahnhofstrasse 45, CH-8001 Zurich / SWIFT: UBSWCHZH80A / CHARGES: OUR (note: non-dvp routing)",
      label: "UBS - Payment block (OUR charges)",
    },
    {
      value:
        "Cash settlement only, wire instructions: Federal Reserve Bank routing 123456789",
      label: "UBS - FRB + credit instruction",
    },
    {
      value:
        "Physical delivery to warehouse facility, contact operations team for coordination",
      label: "UBS - Short delivery instruction",
    },
  ];
  //Picks which template list the component should use. If the parent passes a non-empty list of templates (e.g., desk- or user-specific templates from the server), traders will see those choices.If no templates were provided (or the provided array is empty), the UI falls back to defaultTemplates so traders still have useful quick-insert options
  const templatesToUse =
    templates && templates.length ? templates : defaultTemplates;

  //isValid will be a reusable helper I can call elsewhere to check whether a string meets your rules.
  function isValid(text: string): boolean {
    const trimmed = text.trim();
    // Use the trimmed value for all validation checks so leading/trailing
    // whitespace can't be used to bypass the length check and so the
    // angle-bracket check is applied to the visible content.
    // [<>] matches either < or > which forbid to reduce XSS risk.
    // ADDED: validation rules enforced by the UI for settlement text.
    // These match server expectations where possible and provide
    // immediate feedback before attempting to persist:
    // - Must be between 10 and 500 characters (trimmed)
    // - Forbid characters that the backend rejects (semicolon, single/double quotes,
    //   and angle brackets) to reduce invalid input and XSS-like content.
    if (trimmed.length < 10 || trimmed.length > 500 || /[;'"<>]/.test(trimmed))
      return false;
    return true;
  }
  // temporary minimal render so the component is valid while I keep editing.
  return (
    <div>
      {/* Wrapper for the label, templates dropdown, textarea and any messages */}
      {/* Label is linked to the textarea by id/htmlFor so screen readers announce the field */}
      {/* Templates picker: a simple select of approved settlement text.
    - When the user picks a template insert it at the caret or replace the selection.
    - Mark the field as edited so validation and Save behave correctly.
    - Reset the select to the placeholder so the user can pick another template.
    This gives traders a quick, editable way to insert standard settlement instructions. */}
      {/* mb-3 = 0.75rem (12px) */}

      <div className="mb-3">
        {/* Accessible label so users and screen readers know what this dropdown is for */}
        <label
          htmlFor="template-select"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Drop Down Quick templates
        </label>
        <div className="relative">
          <select
            id="template-select"
            className="w-full appearance-none px-4 py-3 text-base bg-yellow-50 border-2 border-yellow-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-yellow-400 focus:border-yellow-400"
            aria-describedby="template-help"
            value={templateSelectValue}
            onChange={(event) => {
              const v = event.currentTarget.value; // the template string chosen by the user
              setTemplateSelectValue(v); // keep select controlled (helpful for tests and clear reset)
              if (v) {
                insertAtCursor(v); // insert template at caret/replace selection
                setTemplateSelectValue(""); // reset to placeholder so user can pick again
              }
            }}
          >
            <option value="" disabled>
              Choose from dropdown
            </option>
            {/* Render an <option> for each template: */}
            {templatesToUse.map((txt) => (
              // key: unique id for React's list diffing
              // value: the template text inserted into the textarea
              // visible label: shown to the user in the dropdown
              <option key={txt.label} value={txt.value}>
                {txt.label}
              </option>
            ))}
          </select>

          {/* Decorative chevron to indicate dropdown */}
          <div className="pointer-events-none absolute inset-y-0 right-3 flex items-center">
            <svg
              className="h-5 w-5 text-gray-600"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path d="M5.23 7.21a.75.75 0 011.06.02L10 10.94l3.71-3.71a.75.75 0 111.06 1.06l-4.24 4.24a.75.75 0 01-1.06 0L5.21 8.29a.75.75 0 01.02-1.06z" />
            </svg>
          </div>
        </div>

        <p id="template-help" className="text-xs text-gray-600 mt-1">
          Select a template to insert at the caret editable after insertion.
        </p>
      </div>
      {/* The editable multi-line input used for settlement instructions */}
      {/* DOM ref so insertAtCursor can read/set selection and focus */}
      {/* Controlled value comes from component state */}
      {/* Typed handler that updates state as the user types */}
      {/* Mark the field as touched when the user leaves it (for UX) */}
      <textarea
        id="settlement-instructions"
        ref={textareaRef}
        value={value}
        onChange={handleChange}
        onBlur={() => setTouched(true)}
        className="w-full min-h-[140px] border border-gray-300 rounded p-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-200"
      />
      {/*Shows current trimmed char count and the 500-char limit.
Turns red when the count exceeds 500, giving immediate visual feedback before Save is wired.
*/}
      <div
        className={
          value.trim().length > 500
            ? "text-sm text-red-600 mt-1"
            : "text-sm text-gray-600 mt-1"
        }
      >
        {value.trim().length} / 500
      </div>

      {/* Show the following error block only after user interacted and validation fails */}
      {touched && !isValid(value) && (
        <div className="text-sm text-red-600">
          {/* User-facing validation message; entities render literal < and > */}
          Must be 10-500 chars and may not contain &lt; or &gt;.
        </div>
      )}

      {/*if nonStandardIndicators exist then apply darker yellow text and  visually a warning info banner in this position and margins*/}
      {nonStandardKeyword && (
        // Red banner: indicates attention required. Non-standard settlement
        // instructions often need manual intervention by operations or
        // risk; use a red background so the issue is visible immediately.
        <div
          role="status"
          aria-live="polite"
          aria-atomic="true"
          className="mt-2 p-2 bg-red-600 text-white text-sm rounded"
        >
          Non-standard settlement detected: {nonStandardKeyword}
        </div>
      )}
      {/* Actions: Clear remains available. The small per-field Save button
          was removed in favour of saving settlement together with the trade
          (user clicks 'Save Trade'). This avoids duplicate save buttons and
          ensures settlement is persisted as part of the same booking
          transaction. */}
      <div className="flex flex-col gap-2 mt-2">
        <div className="text-sm text-gray-600">
          Settlement is saved when you click &quot;Save Trade&quot;.
        </div>

        {/* Local Clear button: clears the textarea, informs the parent via onChange,
            clears the non-standard banner and restores focus to the textarea. */}
        <div className="flex items-center gap-2">
          <Button
            variant={"secondary"}
            type="button"
            onClick={() => {
              setValue("");
              onChange?.("");
              setTouched(true);
              setNonStandardKeyword(null);
              // focus the textarea after clearing so the user can start typing
              requestAnimationFrame(() => textareaRef.current?.focus());
            }}
          >
            Clear
          </Button>
        </div>
      </div>
    </div>
  );
};
