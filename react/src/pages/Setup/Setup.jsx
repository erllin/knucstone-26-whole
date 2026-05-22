import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useUser } from "../../components/UserProvider";

import "./Setup.css"

function Setup() {
  const navigate = useNavigate();
  const { user, userProfile, loginWithGoogle, updateProfile } = useUser();

  // 초기값 유지
  const [formData, setFormData] = useState({
    university: "",
    department: "",
    admission: new Date().getFullYear(),
    majorType: 1,
  });

  // 프로필이 이미 등록된 유저는 자동으로 홈으로 리다이렉트
  useEffect(() => {
    if (user && userProfile?.university) {
      localStorage.setItem('userVisited', 'true');
      navigate("/home");
    }
  }, [user, userProfile, navigate]);

  const handleComplete = async () => {
    if (!formData.university || !formData.department) {
      alert("모든 정보를 선택해주세요.");
      return;
    }
    
    try {
      // 스프링 백엔드 시큐리티 적용에 따른 프로필 업데이트 수행
      await updateProfile({
        ...formData,
        setupCompleted: true,
      });

      localStorage.setItem('userVisited', 'true');
      navigate("/home");
    } catch (error) {
      console.error("학사 정보 저장 중 오류 발생:", error);
      alert("정보 저장에 실패했습니다. 다시 시도해 주세요.");
    }
  };

  return (
    <div className="setup-wrapper page">
      <div className="setup-container framework con-theme border-r">
        {!user ? (
          /* 구글 로그인 단계 */
          <div className="login-step">
            <h1 className="section-title">환영합니다!</h1>
            <p className="setup-muted-text">
              서비스 이용을 위해 로그인이 필요합니다.
            </p>
            <button className="setup-btn" onClick={loginWithGoogle}>
              구글로 로그인하기
            </button>
          </div>
        ) : (
          /* 학사 정보 입력 단계 (초기값 인식 오류 해결) */
          <div className="form-step">
            <div className="form-header">
              <h1 className="section-title">학사 정보 설정</h1>
              <p className="setup-muted-text">
                {user.displayName}님의 학적 정보를 입력해주세요.
              </p>
            </div>
            
            {/* 대학교 선택 */}
            <div className="fr-title border-r">
              <select 
                className="setup-input"
                value={formData.university}
                onChange={e => setFormData({...formData, university: e.target.value})} 
              >
                {/* 빈 값에 대응하는 기본 placeholder 옵션을 반드시 두어야 변경이 먹힙니다. */}
                <option value="" disabled>--- 대학교 선택 ---</option>
                <option value="강원대학교">강원대학교</option>
              </select>
            </div>

            {/* 학과 선택 */}
            <div className="fr-title border-r">
              <select
                className="setup-input"
                value={formData.department}
                onChange={e => setFormData({...formData, department: e.target.value})} 
              >
                <option value="" disabled>--- 학과 선택 ---</option>
                <option value="컴퓨터공학과">컴퓨터공학과</option>
              </select>
            </div>

            <div className="setup-row">
              {/* 학번 선택 */}
              <div className="fr-title border-r fr-title-select">
                <select 
                  value={formData.admission}
                  onChange={e => setFormData({...formData, admission: Number(e.target.value)})}
                >
                  <option value="2026">2026학번</option>
                  <option value="2025">2025학번</option>
                  <option value="2024">2024학번</option>
                </select>
              </div>

              {/* 전공 유형 선택 */}
              <div className="fr-title border-r fr-title-select">
                <select 
                  value={formData.majorType}
                  onChange={e => setFormData({...formData, majorType: Number(e.target.value)})}
                >
                  <option value={1}>단일전공</option>
                  <option value={2}>단일부전공</option>
                  <option value={3}>복합부전공</option>
                  <option value={4}>복수전공</option>
                </select>
              </div>
            </div>

            <button className="setup-btn primary" onClick={handleComplete}>
              설정 완료 및 시작하기
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default Setup;