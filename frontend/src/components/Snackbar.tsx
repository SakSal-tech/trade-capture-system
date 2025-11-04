import React from "react";

// ADDED: Snackbar default behaviour changed — moved to top-left and
// increased default duration to 10s so messages (e.g., cashflow errors)
// are visible and not obscured by modal dialogs. Callers can override
// the `duration` prop per usage.

interface SnackbarProps {
  open: boolean;
  message: string;
  type?: "success" | "error";
  onClose: () => void;
  // How long the snackbar stays visible (ms). If omitted, defaults to 6000ms.
  duration?: number;
  // Optional action shown as a small button (e.g., Retry)
  actionLabel?: string;
  onAction?: () => void;
}

const typeStyles = {
  success: "bg-emerald-500 text-white",
  error: "bg-rose-500 text-white",
};

const Snackbar: React.FC<SnackbarProps> = ({
  open,
  message,
  type = "success",
  onClose,
  // ADDED: Extended default duration to 10s and moved placement to top-left
  // to avoid being visually obscured by centered modals like CashflowModal.
  duration = 10000,
  actionLabel,
  onAction,
}) => {
  React.useEffect(() => {
    if (!open) return;
    const timer = setTimeout(() => {
      try {
        onClose();
      } catch {
        // ignore
      }
    }, duration);
    return () => clearTimeout(timer);
  }, [open, duration, onClose]);

  if (!open) return null;
  return (
    // Position the snackbar on the left side of the viewport so it does not
    // get obscured by centered modal dialogs (like the Cashflow window).
    // Default placement is top-left with a high z-index; keep pointer-events
    // disabled on the backdrop so only the snackbar itself captures clicks.
    <div className="fixed left-6 top-6 z-60 flex items-start justify-start pointer-events-none">
      <div
        className={`pointer-events-auto px-4 py-3 rounded-xl shadow-lg font-semibold text-center transition-all duration-300 flex items-center gap-3 max-w-3xl mx-4 ${typeStyles[type]}`}
        role="alert"
        aria-live={type === "error" ? "assertive" : "polite"}
      >
        <div className="flex-1 text-center cursor-pointer" onClick={onClose}>
          {message}
        </div>
        {actionLabel && onAction && (
          // Short: render action button for contextual actions (Retry)
          <button
            className="bg-white text-gray-800 px-3 py-1 rounded-md font-medium"
            onClick={(e) => {
              e.stopPropagation();
              onAction();
            }}
          >
            {actionLabel}
          </button>
        )}
      </div>
    </div>
  );
};

export default Snackbar;
