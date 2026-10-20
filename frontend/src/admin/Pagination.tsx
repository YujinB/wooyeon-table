import { useEffect, useState } from 'react';

export const PAGE_SIZE = 20;

/** 긴 목록을 페이지로 나눈다. 필터가 바뀌어 목록 길이가 달라지면 1페이지로 */
export function usePaged<T>(rows: T[], pageSize = PAGE_SIZE) {
  const [page, setPage] = useState(1);
  const pageCount = Math.max(1, Math.ceil(rows.length / pageSize));
  useEffect(() => setPage(1), [rows.length]);
  const current = Math.min(page, pageCount);
  return {
    rows: rows.slice((current - 1) * pageSize, current * pageSize),
    page: current,
    pageCount,
    total: rows.length,
    setPage,
  };
}

/** 현재 페이지 앞뒤 2쪽과 처음·끝 쪽만 번호로 보여 준다 */
function pageNumbers(page: number, pageCount: number): (number | null)[] {
  const shown = new Set([1, pageCount]);
  for (let p = page - 2; p <= page + 2; p++) if (p >= 1 && p <= pageCount) shown.add(p);
  const sorted = [...shown].sort((a, b) => a - b);
  const result: (number | null)[] = [];
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) result.push(null);
    result.push(p);
  });
  return result;
}

export function Pagination({ page, pageCount, total, setPage, pageSize = PAGE_SIZE }: {
  page: number; pageCount: number; total: number; setPage: (page: number) => void; pageSize?: number;
}) {
  if (pageCount <= 1) return null;
  const from = (page - 1) * pageSize + 1;
  const to = Math.min(page * pageSize, total);
  return (
    <nav className="pager" aria-label="페이지">
      <span className="muted small">{total}건 중 {from}–{to}</span>
      <div className="pager-buttons">
        <button className="pager-btn" disabled={page === 1} onClick={() => setPage(page - 1)}>이전</button>
        {pageNumbers(page, pageCount).map((p, i) => p === null
          ? <span key={`gap-${i}`} className="pager-gap">…</span>
          : (
            <button key={p} className={`pager-btn ${p === page ? 'on' : ''}`} aria-current={p === page ? 'page' : undefined}
              onClick={() => setPage(p)}>{p}</button>
          ))}
        <button className="pager-btn" disabled={page === pageCount} onClick={() => setPage(page + 1)}>다음</button>
      </div>
    </nav>
  );
}
