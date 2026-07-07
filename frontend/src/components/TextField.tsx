import type { ReactNode } from "react";

type TextFieldProps = {
  id: string;
  label: string;
  type?: string;
  value: string;
  placeholder?: string;
  autoComplete?: string;
  required?: boolean;
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
  leadingIcon,
  trailingAction,
  onChange,
}: TextFieldProps) {
  return (
    <div className="field-group">
      <label className="field-label" htmlFor={id}>
        {label}
      </label>
      <div className="field-control">
        {leadingIcon ? <span className="field-icon">{leadingIcon}</span> : null}
        <input
          id={id}
          className="field-input"
          type={type}
          value={value}
          placeholder={placeholder}
          autoComplete={autoComplete}
          required={required}
          onChange={(event) => onChange(event.target.value)}
        />
        {trailingAction ? <span className="field-action">{trailingAction}</span> : null}
      </div>
    </div>
  );
}
