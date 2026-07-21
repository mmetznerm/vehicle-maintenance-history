type VehicleHistoryMarkProps = {
  className?: string;
};

export function VehicleHistoryMark({ className = "" }: VehicleHistoryMarkProps) {
  const classes = ["vehicle-history-mark", className].filter(Boolean).join(" ");

  return (
    <span className={classes} aria-hidden>
      <svg viewBox="0 0 48 48" fill="none">
        <path className="vehicle-history-mark-arc" d="M13.5 17.5A14 14 0 0 1 36 15" />
        <path className="vehicle-history-mark-arrow" d="M36 10.5V15h-4.5" />
        <path
          className="vehicle-history-mark-car"
          d="M10 28.5h3.3l3-6.1a3 3 0 0 1 2.7-1.7h10.2a3 3 0 0 1 2.5 1.4l4.1 6.4H38a2 2 0 0 1 2 2v3.2H8v-3.2a2 2 0 0 1 2-2Z"
        />
        <path className="vehicle-history-mark-window" d="m18.3 23-2.6 5.5h16.7L29.1 23H18.3Z" />
        <circle className="vehicle-history-mark-wheel" cx="15" cy="34" r="3.2" />
        <circle className="vehicle-history-mark-wheel" cx="33" cy="34" r="3.2" />
      </svg>
    </span>
  );
}
