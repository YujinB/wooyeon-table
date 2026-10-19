import { useEffect, useState } from 'react';

/**
 * 되돌리기 어려운 운영 동작용 버튼. 한 번 누르면 "한 번 더 누르면 실행"으로 바뀌고, 3초 안에 다시 누르면 실행한다
 */
export function ConfirmButton({ label, confirmLabel, onConfirm, disabled, className = 'btn btn-primary btn-sm' }: {
  label: string;
  confirmLabel?: string;
  onConfirm: () => void;
  disabled?: boolean;
  className?: string;
}) {
  const [armed, setArmed] = useState(false);

  useEffect(() => {
    if (!armed) return;
    const timer = window.setTimeout(() => setArmed(false), 3000);
    return () => window.clearTimeout(timer);
  }, [armed]);

  return (
    <button className={`${className} ${armed ? 'btn-danger' : ''}`} disabled={disabled}
      onClick={() => {
        if (armed) {
          setArmed(false);
          onConfirm();
        } else {
          setArmed(true);
        }
      }}>
      {armed ? (confirmLabel ?? `${label} — 한 번 더 누르면 실행`) : label}
    </button>
  );
}
