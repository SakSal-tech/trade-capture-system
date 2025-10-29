import React, { FC, useState, useRef, useEffect } from "react"; // React: JSX runtime
// FC: Functional Component type for typing the component
// useState: controlled value storage for the textarea
// useRef: hold a reference to the <textarea> so we can insert at cursor
// useEffect: small lifecycle needs (e.g., focus or syncing initial value)

//This exports a TypeScript type (compile-time only) so Other files can import it for typing props or variables
import type { ChangeEvent } from "react";
// ChangeEvent: Type for the textarea onChange handler (keeps TS happy)
import Button from "../components/Button";
import LoadingSpinner from "../components/LoadingSpinner"; // A small SVG spinner used to indicate loading state (visual cue while an async action is in progress).
// This component will use plain elements so no other imports are required.

//To type the component props so callers know what to pass and I  get autocompletion. it defines the "shape" (the property names and types). typing props gives you editor autocompletion and lets TypeScript check callers pass the correct fields (helps prevent bugs
export interface SettlementTextareaProps {
  initialValue?: string; //Optional prop to pre-fill the textarea (e.g., existing settlement instructions when editing a trade)
  templates?: { value: string; label: string }[]; // ? means Optional array of templates the component can show in a dropdown; each template has a machine
  // Optional callback the parent can pass to save the settlement instructions.
  // Allows the component to remain UI-only when not provided.
  onSave?: (text: string) => Promise<void> | void;
}
//types the variable as SettlementTextareaProps meaning TypeScript what kind of value a variable will hold. This says “the variable SettlementTextArea has the type FC<SettlementTextareaProps. So TypeScript will error if callers pass wrong props or if you use the props incorrectly inside the component
export const SettlementTextArea: FC<SettlementTextareaProps> = ({
  initialValue = "",
  templates = [], //Destructing: take props, pull out initialValue and templates, and if thye're undefined give them default values
  onSave,
}) => {
  const textareaRef = useRef<HTMLTextAreaElement | null>(null); //useRef is a React hook that gives a stable, per-instance ref object.

  // A React state hook that creates a piece of component state named value and an updater function setValue. initialValue: the runtime initial value for that state (comes from props via destructuring).
  const [value, setValue] = useState<string>(initialValue);
  //This is used only on first render — if the initialValue prop later changes, state won't update automatically unless I use a useEffect
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

  //Reads the string from the textarea event and calls setValue(...) to update component state so the textarea can be used as a controlled component.
  function handleChange(evt: ChangeEvent<HTMLTextAreaElement>) {
    setValue(evt.currentTarget.value);
    // Mark as touched when the user types so validation/Save state updates immediately
    // (keeps UX responsive rather than waiting for blur).
    setTouched(true);
  }

  // Local saving indicator for async onSave handlers so the UI can show a spinner
  // and disable the Save button while the parent persists data.
  const [isSaving, setIsSaving] = useState<boolean>(false);
  // Take a template (or any text) and insert it into  textarea at the current caret/selection position. ChangeEvent<HTMLTextAreaElement>, which gives the handler a correctly typed event.
  function insertAtCursor(text: string) {
    const txtArea = textareaRef.current;
    //If null (the DOM node isn’t available — e.g., not mounted, ref not attached, or running on server
    if (!txtArea) {
      setValue((prevValue) => prevValue + text); //takes the current state (prevValue) and concatenates text to it.If prevValue is an empty string, result becomes "" + text.
      return;
    }
    //reads the settlement-instructions textarea caret/selection start index (an integer). If that value is null/undefined, it falls back to the end of the current text (txtArea.value.length).
    const start = txtArea.selectionStart ?? txtArea.value.length;
    const end = txtArea.selectionEnd ?? start; //reads: the selection end index.Insert-at-cursor: puts templates exactly where the trader wants (improves speed and accuracy for settlement info).

    //takes the previous textarea string and produce a new string where the substring from index start to end is replaced by text, keeps everything before the caret/selection, + text is the inserted template, + prevValue.slice(end) keeps everything after the selection. Insert-at-cursor: inserts exactly where the trader placed the caret or replaces the highlighted selection (so a trader can replace an old beneficiary line with the UBS template in one action).

    //
    // Insert the template at the caret/selection using a functional state updater
    // so we never clobber concurrent updates. We calculate the new caret position
    // (start + text.length) now and restore it after React updates the DOM below.
    setValue(
      (prevValue) => prevValue.slice(0, start) + text + prevValue.slice(end)
    );

    // 1) Mark the field as "touched" because template insertion is explicit user input.
    //    This ensures inline validation and "Save" enablement behave consistently.
    setTouched(true);

    // 2) Restore focus and move the caret to the end of the inserted text.
    //    We use requestAnimationFrame so the browser has applied the DOM value change
    //    before we set selection; this produces reliable caret placement across browsers.
    const newCaretPos = start + text.length;
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
    // Note: we already used requestAnimationFrame above to focus and set selection.
    // The duplicate call was removed to avoid running the focus/selection twice.
  }

  // The array exists to speed and standardise trader input if UBS prefers traders always type from scratch, I can remove it or replace it with an empty.
  const defaultTemplates = [
    {
      value:
        "BENEFICIARY: UBS AG, Zurich / SWIFT: UBSWCHZH80A / IBAN: CHxx xxxx xxxx xxxx xxxx x",
      label: "UBS AG — Beneficiary (IBAN placeholder)",
    },
    {
      value:
        "OUR BANK: UBS AG, Zurich / SWIFT: UBSWCHZH80A / Account: 123-456789.0 (local format)",
      label: "UBS AG — Our account (local)",
    },
    {
      value:
        "PAYMENT: UBS AG / UBS ADDRESS: Bahnhofstrasse 45, CH-8001 Zurich / SWIFT: UBSWCHZH80A / CHARGES: OUR",
      label: "UBS — Payment block (OUR charges)",
    },
    {
      value:
        "INTERMEDIARY: UBS AG NY / SWIFT: BKTRUS33 / FOR CREDIT TO: UBS AG Zurich / IBAN: CHxx xxxx xxxx xxxx xxxx x",
      label: "UBS — Intermediary + credit instruction",
    },
    {
      value:
        "UBS PAYMENT INSTRUCTIONS: Please pay via UBS AG (SWIFT UBSWCHZH80A). Beneficiary: [NAME]. Account/IBAN: [IBAN]. Reference: [TRADE ID].",
      label: "UBS — Short payment instruction (templated)",
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
    // [<>] — matches either < or > which we forbid to reduce XSS risk.
    if (trimmed.length < 10 || trimmed.length > 500 || /[<>]/.test(trimmed))
      return false;
    return true;
  }
  // temporary minimal render so the component is valid while I keep editing.
  return (
    <div>
      {/* Wrapper for the label, templates dropdown, textarea and any messages */}
      {/* Label is linked to the textarea by id/htmlFor so screen readers announce the field */}
      {/* Templates picker: a simple select of approved settlement text.
    - When the user picks a template we insert it at the caret or replace the selection.
    - Mark the field as edited so validation and Save behave correctly.
    - Reset the select to the placeholder so the user can pick another template.
    This gives traders a quick, editable way to insert standard settlement instructions. */}
      {/* mb-3 = 0.75rem (12px) */}

      <select
        className="mb-3"
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
        <option value="">Insert template…</option>
        {templatesToUse.map((t) => (
          <option key={t.label} value={t.value}>
            {t.label}
          </option>
        ))}
      </select>
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
      {/* Save / Clear actions: Save is disabled when validation fails or while saving. Clear resets the field and focuses the textarea. */}
      <div className="flex gap-2 mt-2">
        {/*The disabled expression prevents clicks while the current text is invalid or a save is already in progress. That stops both submission of bad input (isValid(value) false) and accidental double-submits (isSaving true). The onClick handler is async and does a no-op when no onSave callback is provided, avoiding runtime errors. It sets a local saving flag (setIsSaving(true)) before calling the parent-supplied onSave and uses finally to clear the flag so the UI always returns to a non-saving state even if onSave rejects or throws. Awaiting onSave(value) makes the component wait for the save to complete before toggling isSaving back off. Because without try/catch await onSave(value), any error will propagate to the caller which may be fine if a higher-level error handler exists,  to show a local error message unless I added try/catch handling here*/}
        <Button
          disabled={!onSave || !isValid(value) || isSaving}
          onClick={async () => {
            if (!onSave) return;
            const payload = value; // capture current text
            setIsSaving(true);
            try {
              await onSave(payload);
            } catch (err) {
              // show feedback / log error
              console.error("Save failed", err);
            } finally {
              setIsSaving(false);
            }
          }}
        >
          {isSaving ? <LoadingSpinner /> : "Save"}
        </Button>
        <Button
          variant="secondary"
          onClick={() => {
            setValue("");
            setTouched(false);
            textareaRef.current?.focus();
          }}
        >
          Clear
        </Button>
      </div>
    </div>
  );
};
