import React, { useState, useMemo } from "react";
import { useUser } from "../../../components/UserProvider";

import "./FrameworkViewer.css";
import "../Home.css";

const FrameworkViewer = () => {
    // Track을 useUser 컨텍스트 통해 가져옴.
    const { tracks, loading } = useUser();
    const [selectedTrack, setSelectedTrack] = useState(null);

    // const currentTrack: 화면에 표시될 트랙을 useMemo를 이용해 결정함.
    const currentTrack = useMemo(() => {
        if (!tracks || tracks.length === 0) { return null; }

        // 선택된 트랙이 있는지 .find로 확인하고 없다면 기본값 0번 트랙 반환.
        const view = tracks.find(t => t.trackNo === selectedTrack) || tracks[0];
        return view;
    }, [tracks, selectedTrack]) // tracks, selectedTrack이 의존값.

    // 로딩, 트랙 정보 없음 처리
    if (loading) { 
        return (<div className="loading">데이터를 불러오는 중...</div>);
    }
    if (!currentTrack) {
        return (
            <div className="error">등록된 트랙 정보 없음</div>
        )
    }

    return (
        <div className="framework con-theme border-r">
            <h2>프레임워크</h2>
            <div className="fr-title border-r">
                <div className="fr-title-select">
                    {/* 트랙 선택 박스 */}
                    <select 
                        onChange={(e) => setSelectedTrack(Number(e.target.value))}
                        value={currentTrack.trackNo}
                    >
                        {tracks.map(track => (
                            <option key={track.trackNo} value={track.trackNo}>
                                {track.trackName}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {/* 교과목 리스트 */}
            <div className="fr-cslist border-r">
                <div className="subject-list">
                    {currentTrack.subjects?.map((subject) => (
                        <div key={subject.id} className="subject-item">
                            <span className="sub-id">{subject.id}</span>
                            <span className="sub-name">{subject.name}</span>
                            <span className="sub-info">({subject.year}학년 {subject.semester}학기)</span>
                        </div>
                    ))}
                </div>
            </div>

            {/* 이수 목표 */}
            <div className="fr-goal border-r">
                <h3 className="section-title">이수 목표</h3>
                <p>{currentTrack.completionGoal}</p>
            </div>

            {/* 직업 */}
            <div className="fr-job border-r">
                <h3 className="section-title">관련 직업</h3>
                <div className="job-list">
                    {currentTrack.targetOccupations.map((job, index) => (
                        <span key={index} className="job-tag">{job}</span>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default FrameworkViewer;