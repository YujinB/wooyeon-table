/** 둥근 테이블에 6명이 둘러앉은 모양 (온보딩 자료의 앱 아이콘을 재현) */
export function LogoMark({ size = 32, color = '#a65a2a' }: { size?: number; color?: string }) {
  const seats = [0, 60, 120, 180, 240, 300].map((deg) => {
    const rad = ((deg - 90) * Math.PI) / 180;
    return { cx: 60 + 41 * Math.cos(rad), cy: 60 + 41 * Math.sin(rad) };
  });
  return (
    <svg width={size} height={size} viewBox="0 0 120 120" aria-hidden="true">
      <circle cx="60" cy="60" r="27" fill={color} />
      {seats.map((seat, i) => (
        <circle key={i} cx={seat.cx} cy={seat.cy} r="9" fill={color} />
      ))}
    </svg>
  );
}

export function LogoIcon({ size = 72 }: { size?: number }) {
  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: size * 0.24,
        background: '#fbf1e4',
        display: 'grid',
        placeItems: 'center',
        boxShadow: '0 8px 24px rgba(122,62,26,0.18)',
      }}
    >
      <LogoMark size={size * 0.78} />
    </div>
  );
}
