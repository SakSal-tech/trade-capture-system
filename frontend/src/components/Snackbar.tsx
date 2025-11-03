import React from "react";

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
  // Default duration increased to 10 seconds so users have time to read error messages
  duration = 15000,
  actionLabel,
  onAction,
}) => {
  React.useEffect(() => {
    if (!open) return;
    const timer = setTimeout(() => {
      try {
        onClose();
      } catch (e) {
        // ignore
      }
    }, duration);
    return () => clearTimeout(timer);
  }, [open, duration, onClose]);

  if (!open) return null;
  return (
    // Position the snackbar at the bottom-center of the viewport so it
    // appears below modal dialogs (e.g., CashflowModal) and remains readable.
    // Use a higher z-index to ensure it isn't obscured by modal overlays.
    <div className="fixed inset-x-0 bottom-6 z-60 flex items-center justify-center pointer-events-none">
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
