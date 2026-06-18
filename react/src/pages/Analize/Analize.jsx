import React from "react";
import { useUser } from "../../components/UserProvider"
// CSS
import "../../global.css";
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
    <div className="page dfx-col main-theme">
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
          <div className="analize-course dfx-col br-20 con-theme">
            <h2 className="section-title">AI 맞춤형 학업 컨설팅 리포트</h2>
            
            {/* 분석 실행 트리거 영역 */}
            <div className="suggestion-container br-15">
              <div className="suggestion-header">
                <h5>실시간 학사 진단</h5>
                <button 
                  onClick={handleFetchReport} 
                  disabled={isAiLoading}
                  className={`btn-analyze br-10 ${isAiLoading ? "disabled" : ""}`}
                >
                  {isAiLoading ? 'AI 진단 및 연산 중... (잠시만 기다려주세요.)' : 'AI 진단 및 추천 받기'}
                </button>
              </div>
            </div>

            {/* 에러 발생 시 알림창 */}
            {aiError && (
              <div className="analize-error br-10">
                <span>{aiError}</span>
              </div>
            )}

            {/* AI 리포트 */}
            {aiReport && (
              <div className="analize-content dfx-col">
                <section className="analize-section dfx-col br-15">
                  <h3 className="sub-section-title">추천 커리큘럼 트랙</h3>
                  <div className="track-item br-10">
                    <span className="tag-rec br-5">주트랙</span> 
                    <div className="track-info dfx-col">
                      <span className="name">{aiReport.recommendedTracks.primaryTrack.trackName}</span>
                      <p className="reason">{aiReport.recommendedTracks.primaryTrack.reason}</p>
                    </div>
                  </div>
                  <div className="track-item br-10">
                    <span className="tag-sub br-5">부트랙</span> 
                    <div className="track-info">
                      <span className="name">{aiReport.recommendedTracks.secondaryTrack.trackName}</span>
                      <p className="reason">{aiReport.recommendedTracks.secondaryTrack.reason}</p>
                    </div>
                  </div>
                </section>

                <section className="analize-section dfx-col br-15">
                  <h3 className="sub-section-title">다음 학기 추천 수강 과목</h3>
                  <h5 className="list-group-title">※ AI의 대답에는 오류가 있을 수 있습니다.</h5>
                  <div className="suggestion-list">
                    {aiReport.nextSemesterPlan.recommendedCourses.map((course) => (
                      <div key={course.courseId} className="suggest-item br-10">
                        <span className="tag-rec br-5">추천</span>
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
                <section className="analize-section alert-border dfx-col br-15">
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