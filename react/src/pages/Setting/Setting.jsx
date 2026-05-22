import React from "react";
import { logout } from "../../firebase";
// CSS 임포트
import "../../components/skins/options.css";
import "./Setting.css";

const Setting = () =>  {

  const handleLogout = async () => {
    try {
      await logout();
      alert("로그아웃 성공");
    } catch (error) {
      console.error(error);
    }
  }

  return (
    <div className="page main-theme">
      <header className="header">
        <div className="header-top">
          <div className="ct-wrapper">
            <div className="logo">MYCURRI</div>
            <nav className="menu">설정</nav>
          </div>
        </div>
      </header>
      
      <main className="ct">
        <div className="ct-wrapper">
          {/* 설정 페이지 컨텐츠 영역 */}
          <div className="settings-container">
            
            {/* 로그아웃 더미 컴포넌트 블록 */}
            <div className="settings-section logout-box">
              <div className="logout-info">
                <h3>계정 관리</h3>
                <p>현재 기기에서 안전하게 로그아웃합니다.</p>
              </div>
              <button className="btn-logout" onClick={handleLogout}>
                로그아웃
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Setting;