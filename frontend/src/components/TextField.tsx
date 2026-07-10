import type { ReactNode } from "react";

type TextFieldProps = {
  id: string;
  label: string;
  type?: string;
  value: string;
  placeholder?: string;
  autoComplete?: string;
  required?: boolean;
  errorMessage?: string;
  leadingIcon?: ReactNode;
  trailingAction?: ReactNode;
  onChange: (value: string) => void;
};

export function TextField({
  id,
  label,
  type = "text",
  value,
  placeholder,
  autoComplete,
  required = false,
  errorMessage,
  leadingIcon,
  trailingAction,
  onChange,
}: TextFieldProps) {
  const errorId = `${id}-error`;

  return (
    <div className="field-group">
      <label className="field-label" htmlFor={id}>
        {label}
      </label>
      <div className={`field-control${errorMessage ? " has-error" : ""}`}>
        {leadingIcon ? <span className="field-icon">{leadingIcon}</span> : null}
        <input
          id={id}
          className="field-input"
          type={type}
          value={value}
          placeholder={placeholder}
          autoComplete={autoComplete}
          required={required}
          aria-invalid={errorMessage ? "true" : undefined}
          aria-describedby={errorMessage ? errorId : undefined}
          onChange={(event) => onChange(event.target.value)}
        />
        {trailingAction ? <span className="field-action">{trailingAction}</span> : null}
      </div>
      {errorMessage ? (
        <p className="field-error" id={errorId} role="alert">
          {errorMessage}
        </p>
      ) : null}
    </div>
  );
}
