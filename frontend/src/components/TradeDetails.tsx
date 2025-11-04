// Import React library so React components can be defined
import React from "react";

// Import a reusable label component for displaying field names
import Label from "./Label";

// Import a reusable component that decides whether to show an input box or a dropdown
import FieldRenderer from "./FieldRenderer";

// Import the Trade type so TypeScript knows the shape of a trade object
import { Trade } from "../utils/tradeTypes";

// Import the list of trade form fields and the list of fields that must remain disabled
import { TRADE_FIELDS, DISABLED_FIELDS } from "../utils/tradeFormFields";

// Define the shape of one trade field entry from the TRADE_FIELDS list
type TradeField = { key: keyof Trade & string; label: string; type: string };

/**
 * Interface that defines the properties the TradeDetails component can receive
 */
export interface TradeDetailsProps {
  trade?: Trade; // Holds the trade object to be displayed or edited; may be undefined
  mode: "view" | "edit"; // Determines whether the form is editable or read-only
  onFieldChange?: (key: keyof Trade, value: unknown) => void; // Optional function to call when a field value changes
}

/**
 * Component that displays and manages the trade details section of the form
 */
function TradeDetails(props: TradeDetailsProps) {
  // If there is no trade object provided, the component should not render anything
  if (!props.trade) {
    return null;
  }

  // Define a variable holding the CSS class names used for all field labels
  const labelClass =
    "h-9 flex items-center font-semibold rounded shadow bg-violet-50 px-2 py-1 ml-1 min-w-[200px] text-sm";

  /**
   * Function that renders one row in the trade details form.
   * Each row contains a label and its corresponding input or dropdown box.
   */
  function renderField(field: TradeField) {
    return (
      // Outer container for a single label and input pair
      <div className="flex flex-row gap-x-10 items-center mb-1" key={field.key}>
        {/* Component that displays the label for the field */}
        <Label className={labelClass} htmlFor={field.key as string}>
          {/* The label text is taken from the trade form definition */}
          {field.label}
        </Label>

        {/* Container that holds the editable input or dropdown */}
        <div className="flex flex-col w-full">
          {/* Special help text for the maturity field: clarify that this value
              will be used as Leg 1 maturity and copied into Leg 2 if Leg 2 is empty */}
          {field.key === "maturityDate" && (
            <div className="text-xs text-gray-600 mb-1 ml-1">
              This value is treated as Leg 1 maturity and will be copied to Leg
              2 maturity if Leg 2 has no maturity date set.
            </div>
          )}
          <div className="flex flex-row justify-end rounded px-2 py-1 h-fit w-full">
            {/* Component that decides whether to show an input, dropdown, or date field */}
            <FieldRenderer
              field={
                field as unknown as {
                  key: string;
                  label: string;
                  type: string;
                  options?:
                    | string[]
                    | (() => string[] | { value: string; label: string }[])
                    | { value: string; label: string }[];
                }
              } // cast to satisfy FieldRenderer's expected shape
              value={
                props.trade![field.key] as string | number | null | undefined
              } // assert trade exists and cast to allowed value types
              // Field is disabled if the mode is not "edit" or if the field key is in the disabled list
              disabled={
                props.mode !== "edit" ||
                DISABLED_FIELDS.includes(field.key as string)
              }
              // Function that runs when the user changes a field value
              onChange={function (value) {
                // Check that an onFieldChange function was actually provided
                if (props.onFieldChange) {
                  // Call the parent’s change handler with the key and the new value
                  props.onFieldChange(field.key, value);
                }
              }}
            />
          </div>
        </div>
      </div>
    );
  }

  // The outer container that holds all trade fields in a grid layout
  return (
    <div className="mt-4 grid p-0 bg-violet-100 rounded shadow text-sm w-fit ml-1">
      {/* Go through every field in the list of trade fields and render each one */}
      {TRADE_FIELDS.map(renderField)}
    </div>
  );
}

// Export the component so it can be used in other parts of the application
export default TradeDetails;
