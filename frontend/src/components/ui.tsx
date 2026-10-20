import { ReactNode } from 'react';
import { Option } from '../api/types';

export function Chips({ options, value, onChange, max }: {
  options: Option[];
  value: string[];
  onChange: (next: string[]) => void;
  max?: number;
}) {
  const toggle = (code: string) => {
    if (value.includes(code)) {
      onChange(value.filter((v) => v !== code));
    } else if (!max || value.length < max) {
      onChange([...value, code]);
    }
  };
  return (
    <div className="chips">
      {options.map((option) => (
        <button
          type="button"
          key={option.code}
          className={`chip ${value.includes(option.code) ? 'on' : ''}`}
          onClick={() => toggle(option.code)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

export function SingleChips({ options, value, onChange }: {
  options: Option[];
  value: string | null;
  onChange: (next: string) => void;
}) {
  return (
    <div className="chips">
      {options.map((option) => (
        <button
          type="button"
          key={option.code}
          className={`chip ${value === option.code ? 'on' : ''}`}
          onClick={() => onChange(option.code)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

export function Field({ label, children, required, error }: {
  label: string;
  children: ReactNode;
  required?: boolean;
  error?: string | null;
}) {
  return (
    <label className={`field ${error ? 'invalid' : ''}`} data-invalid={error ? true : undefined}>
      <span>{label}{required && <em className="required" aria-label="필수">*</em>}</span>
      {children}
      {error && <small className="field-error">{error}</small>}
    </label>
  );
}

export function ErrorBox({ message }: { message: string | null }) {
  return message ? <div className="alert error">{message}</div> : null;
}

export function Loading() {
  return <div className="loading">불러오는 중…</div>;
}
