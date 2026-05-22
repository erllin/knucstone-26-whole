import React from "react";
import { useUser } from "../../components/UserProvider"
// CSS
import "../../components/skins/options.css";
import "./Analize.css";

const Analize = () => {
  // 전역 상태 관리 컨텍스트에서 AI 리포트 호출 함수 추출
  const { fetchAiConsultingReport, aiReport, isAiLoading, aiError } = useUser();

  const handleFetchReport = async () => {
    try {
      // fetchAiConsultingReport 기본값 사용하려고 안 넣음.
      await fetchAiConsultingReport();
    } catch (err) {
      console.error("화면단에서 AI 전역 통신 예외 트래킹:", err);
    }
  };

  return (
    <div className="page main-theme">
      <header className="header">
        <div className="header-top">
          <div className="ct-wrapper">
            <div className="logo">MYCURRI</div>
            <nav className="menu">분석</nav>
          </div>
        </div>
      </header>
      
      <main className="ct">
        <div className="ct-wrapper">
          <div className="analize-course con-theme border-r">
            <h2 className="section-title">AI 맞춤형 학업 컨설팅 리포트</h2>
            
            {/* 분석 실행 트리거 영역 */}
            <div className="suggestion-container">
              <div className="suggestion-header">
                <h5>실시간 학사 진단</h5>
                <button 
                  onClick={handleFetchReport} 
                  disabled={isAiLoading}
                  className={`btn-analyze ${isAiLoading ? "disabled" : ""}`}
                >
                  {isAiLoading ? 'AI 진단 및 연산 중... (잠시만 기다려주세요.)' : 'AI 진단 및 추천 받기'}
                </button>
              </div>
            </div>

            {/* 에러 발생 시 알림창 */}
            {aiError && (
              <div className="analize-error">
                <span>{aiError}</span>
              </div>
            )}

            {/* AI 결과 데이터 바인딩 뷰포트 */}
            {aiReport && (
              <div className="analize-content">
                
                {/* 추천 트랙 섹션 */}
                <section className="analize-section">
                  <h3 className="sub-section-title">추천 커리큘럼 트랙</h3>
                  <div className="track-item">
                    <span className="tag-rec">주트랙</span> 
                    <div className="track-info">
                      <span className="name">{aiReport.recommendedTracks.primaryTrack.trackName}</span>
                      <p className="reason">{aiReport.recommendedTracks.primaryTrack.reason}</p>
                    </div>
                  </div>
                  <div className="track-item">
                    <span className="tag-sub">부트랙</span> 
                    <div className="track-info">
                      <span className="name">{aiReport.recommendedTracks.secondaryTrack.trackName}</span>
                      <p className="reason">{aiReport.recommendedTracks.secondaryTrack.reason}</p>
                    </div>
                  </div>
                </section>

                {/* 차기 학기 수강 신청 계획 */}
                <section className="analize-section">
                  <h3 className="sub-section-title">다음 학기 추천 수강 과목</h3>
                  <h5 className="list-group-title">※ AI의 대답에는 오류가 있을 수 있습니다. 전필, 꿈-설계 교과목 등 학과에서 필수로 지정한 과목을 우선하세요.</h5>
                  <div className="suggestion-list">
                    {aiReport.nextSemesterPlan.recommendedCourses.map((course) => (
                      <div key={course.courseId} className="suggest-item">
                        <span className="tag-rec">추천</span>
                        <div className="suggest-info">
                          <span className="name">{course.courseName}</span>
                          <span className="code-credit">({course.courseId})</span>
                          <p className="reason">{course.reason}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </section>

                {/* 트랙 가이드라인 */}
                <section className="analize-section alert-border">
                  <h3 className="sub-section-title alert-text">트랙 가이드라인</h3>
                  
                  <h5 className="list-group-title">AI 핵심 역량 지침</h5>
                  <ul className="learningsolution-list weak">
                    {aiReport.trackLearningSolution.trackGuides.map((pol, idx) => (
                      <li key={idx}>{pol}</li>
                    ))}
                  </ul>

                  <h5 className="list-group-title">AI 학습 플랜</h5>
                  <ul className="learningsolution-list solution">
                    {aiReport.trackLearningSolution.actionPlans.map((plan, idx) => (
                      <li key={idx}>{plan}</li>
                    ))}
                  </ul>
                </section>

              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
};

export default Analize;