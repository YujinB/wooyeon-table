import { Link, useNavigate, useParams } from 'react-router-dom';
import { SLOT_STATUS_LABEL, SlotMatchingResult } from '../api/adminTypes';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime, day } from '../format';
import { useApi } from '../useApi';

/** 회차 매칭 결과: 실행 요약, 테이블 목록, 못 들어간 사람의 사유와 처리 (MAT-12) */
export default function SlotPage() {
  const { slotId } = useParams();
  const navigate = useNavigate();
  const result = useApi<SlotMatchingResult>(`/api/admin/slots/${slotId}/matching`);

  if (!result.data) return result.error ? <ErrorBox message={result.error} /> : <Loading />;
  const r = result.data;
  const run = r.run;

  return (
    <div>
      <div className="admin-head">
        <div>
          <div className="crumb"><Link to="/admin">대시보드</Link> ›</div>
          <h1 className="h1">{day(r.eventDate)} 회차</h1>
          <p className="muted" style={{ margin: 0 }}>
            <span className="pill">{SLOT_STATUS_LABEL[r.slotStatus]}</span> 매칭 마감 {dateTime(r.matchAt)}
          </p>
        </div>
      </div>

      {!run ? (
        <div className="alert info">아직 매칭 전이에요. 마감 시각에 자동으로 실행돼요.</div>
      ) : (
        <>
          <div className="kpis">
            <div className="kpi"><b>{run.candidateCount}</b><span>후보 (결제 완료)</span></div>
            <div className="kpi"><b>{run.assignedCount}</b><span>확정</span></div>
            <div className="kpi"><b>{run.tableCount}</b><span>테이블</span></div>
            <div className="kpi"><b>{run.carriedOverCount}</b><span>다음 날짜로 이월</span></div>
            <div className="kpi"><b>{run.alternativeOfferedCount}</b><span>대체 일정 안내</span></div>
            <div className="kpi"><b>{run.reallocations}</b><span>재배치 · 해제 {run.dissolvedTables}</span></div>
          </div>
          <p className="small muted" style={{ marginTop: 8 }}>
            {run.algorithmVersion} · {dateTime(run.finishedAt)} 실행{r.appliedPolicy && ` · ${r.appliedPolicy}`}
          </p>

          <p className="section-title">테이블 {r.tables.length}개</p>
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr>
                  <th>테이블</th><th>지역</th><th className="num">인원</th><th>나이</th><th>여:남</th>
                  <th className="num">점수</th>
                </tr>
              </thead>
              <tbody>
                {r.tables.map((t) => (
                  <tr key={t.tableId} className="link" onClick={() => navigate(`/admin/tables/${t.tableId}`)}>
                    <td><b>{t.tableNo}번</b></td>
                    <td>{t.regionLabel}</td>
                    <td className="num">{t.size}명</td>
                    <td>{t.minAge}~{t.maxAge}세</td>
                    <td>{t.femaleCount}:{t.maleCount}</td>
                    <td className="num"><b>{t.totalScore.toFixed(1)}</b></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {r.failures.length > 0 && (
            <>
              <p className="section-title">테이블에 들지 못한 신청 {r.failures.length}건</p>
              <div className="table-wrap">
                <table className="data">
                  <thead>
                    <tr><th>이름</th><th>사유</th><th>처리</th><th className="num">재배치 시도</th></tr>
                  </thead>
                  <tbody>
                    {r.failures.map((f) => (
                      <tr key={f.applicationId}>
                        <td>{f.name}</td>
                        <td>{f.reasonDescription}</td>
                        <td>
                          <span className={`pill ${f.outcome === 'CARRIED_OVER' ? '' : 'warn'}`}>
                            {f.outcome === 'CARRIED_OVER' ? `${f.nextEventDate ? day(f.nextEventDate) : ''} 이월` : '대체 안내'}
                          </span>
                        </td>
                        <td className="num">{f.relocationCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}
