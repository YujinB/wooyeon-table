/** 선 아이콘 (이모지 대신). 24x24, 현재 글자색을 따른다 */
const PATHS: Record<string, string[]> = {
  home: ['M3 10.5 12 3l9 7.5', 'M5 9.5V20h5v-6h4v6h5V9.5'],
  dining: ['M7 3v8a2 2 0 0 0 2 2v8', 'M5 3v6', 'M9 3v6', 'M16 21V3c2.2 1.2 3 3.6 3 6.5V13h-3'],
  chat: ['M4 5h16v11H9l-5 4z'],
  user: ['M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6'],
  bell: ['M6 16V11a6 6 0 0 1 12 0v5l2 2H4z', 'M10 20a2 2 0 0 0 4 0'],
  back: ['M15 5l-7 7 7 7'],
  check: ['M5 12.5l4.5 4.5L19 7.5'],
  pin: ['M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z', 'M12 12a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z'],
  calendar: ['M4 6h16v15H4z', 'M4 10h16', 'M8 3v4', 'M16 3v4'],
  sparkle: ['M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z'],
  alert: ['M12 3 2 20h20z', 'M12 10v4', 'M12 17v.5'],
  clock: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z', 'M12 7v5l3 2'],
  users: ['M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z', 'M2.5 20c1-3.5 3.5-5.5 6.5-5.5s5.5 2 6.5 5.5', 'M16 4.5a3.5 3.5 0 0 1 0 6.5', 'M18 14.8c1.8.7 3 2.5 3.5 5.2'],
  star: ['M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z'],
  flag: ['M5 21V4', 'M5 4h11l-2 4 2 4H5'],
  shield: ['M12 3l8 3v6c0 4.5-3.4 8-8 9-4.6-1-8-4.5-8-9V6z'],
  send: ['M4 12 20 4l-6 16-3-7z', 'M11 13l9-9'],
  chevron: ['M9 5l7 7-7 7'],
  logout: ['M15 4h4v16h-4', 'M10 8l-4 4 4 4', 'M6 12h10'],
  ticket: ['M3 8a2 2 0 0 0 0 4v4h18v-4a2 2 0 0 1 0-4V4H3z', 'M14 4v12'],
  refund: ['M4 12a8 8 0 1 0 2.3-5.7', 'M4 4v4h4'],
  map: ['M9 4 3 6v14l6-2 6 2 6-2V4l-6 2z', 'M9 4v14', 'M15 6v14'],
};

export function Icon({ name, size = 22, stroke = 1.8 }: { name: keyof typeof PATHS | string; size?: number; stroke?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke}
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {(PATHS[name] ?? []).map((d) => <path key={d} d={d} />)}
    </svg>
  );
}
