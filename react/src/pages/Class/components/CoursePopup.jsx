import React, { useState, useEffect, useCallback, useRef } from "react";
import axios from "axios";
// context
import { useUser } from "../../../components/UserProvider";
// css
import "../Class.css"
import "./CoursePopup.css"

const grades = ["A+", "A0", "B+", "B0", "C+", "C0", "D+", "D0", "F", "P", "NP"];
const categories = ["전필", "전선", "교양", "자선"];
const termWeight = { "1": 1, "S": 2, "2": 3, "W": 4 };

// const CourseEntity: 개별 수강 이력에 대한 엔티티.
const CourseEntity = React.memo(({ course, index, onUpdate, onDelete, grades }) => {
    const [localCourseId, setLocalCourseId] = useState(course.courseId);

    // course.courseId가 변경되는 경우 동기화 수행.
    useEffect(() => {
        setLocalCourseId(course.courseId);
    }, [course.courseId]);

    // 검색 업데이트 트리거
    const handleTrigger = useCallback(() => {
        // 값이 바뀌었을 때만 업데이트 실행
        if (localCourseId !== course.courseId) {
            onUpdate(index, "courseId", localCourseId);
        }
    }, [localCourseId, course.courseId, index, onUpdate]);

    const handleKeyDown = useCallback((e) => {
        if (e.key === 'Enter') {
            e.preventDefault(); // 엔터 시 줄바꿈 등 방지
            e.currentTarget.blur();
        }
    }, []);

    return (
        <div className='course-row'>
            <div className="col-cid">
                <input className="dark-input" value={localCourseId} 
                    onChange={(e) => setLocalCourseId(e.target.value)} 
                    onKeyDown={handleKeyDown}
                    onBlur={handleTrigger}
                    placeholder="코드" 
                />
            </div>
            <div className="col-name">
                <input className="dark-input" 
                    value={course.courseName} readOnly
                    onChange={(e) => onUpdate(index, "courseName", e.target.value)} 
                    placeholder="강의명" 
                />
            </div>
            <div className="col-ctype">
                <input className="dark-input"
                    value={course.category} readOnly
                    onChange={(e) => onUpdate(index, "category", e.target.value)}
                    placeholder="유형"
                />
            </div>
            <div className="col-credit">
                <input type="number" className="dark-input" style={{ textAlign: 'center' }}
                    value={course.credits} readOnly
                    onChange={(e) => onUpdate(index, "credits", Number(e.target.value))}
                />
            </div>
            <div className="col-grade">
                <select className="dark-select" value={course.grade} 
                    onChange={(e) => onUpdate(index, "grade", e.target.value)}>
                    {grades.map(g => <option key={g} value={g}>{g}</option>)}
                </select>
            </div>
            <div className="col-del">
                <button className="del-btn" onClick={() => onDelete(index)} title="delete">
                    ×
                </button>
            </div>
        </div>
    );
});

// const CoursePopup: 팝업 본체
const CoursePopup = ({ targetSem, onClose }) => {
    const { user, userCourses, userSemesters, fetchCourseInfo, updateProfile, getAuthHeader, deleteCourse } = useUser();

    const [activeTaken, setActiveTaken] = useState(targetSem?.taken || "");
    const [inputYear, setInputYear] = useState(targetSem?.taken?.split('-')[0] || new Date().getFullYear());
    const [inputTerm, setInputTerm] = useState(targetSem?.taken?.split('-')[1] || (targetSem.isNonRegularTerm === 0 ? "1" : "S"));
    const [deleteCourseQueue, setDeleteCourseQueue] = useState([]);
    const [localCourses, setLocalCourses] = useState(() => {
        if (targetSem?.taken) {
            return userCourses.filter(cs => cs.taken === targetSem.taken);
        } else {
            return [];
        }
    });
    // handleUpdateRow 락
    const isUpdating = useRef(false);

    // const handleUpdateRow: 개별 수강이력 업데이트 처리자 (courseId 자동 검색 포함)
    const handleUpdateRow = useCallback(async (index, field, value) => {
        // 락
        if (isUpdating.current) { return; }
        // 락 활성화
        isUpdating.current = true;       
        // 동기 처리 (async 계열을 사용하지 않는 요소들)
        if (field !== "courseId") {
            setLocalCourses(prev => {
                const next = [...prev];
                next[index] = { ...next[index], [field]: value };
                return next;
            });
            // 예외 상황에서의 락 해제 (동기에서 멈추는 경우, try 밖이어서 finally 적용 X)
            isUpdating.current = false;
            return;
        }
        // !courseId 변경하는 경우 초기화 수행. (비동기 검색 준비)
        const trimCourseId = value.trim();
        try {
            if (!trimCourseId) {
                setLocalCourses(prev => {
                    const next = [...prev];
                    next[index] = { ...next[index], courseId: "", courseName: "", credits: 0 };
                    return next;
                });
                return;
            }
            // !중복 체크 
            const isDup = localCourses.some((cs, idx) => idx !== index && cs.courseId === trimCourseId);
            if (isDup) {
                alert('학기 내 중복된 과목 코드입니다!');
                return;
            }
            // !재수강 판단
            const isRetake = userCourses.find(cs => cs.courseId === trimCourseId && cs.taken !== activeTaken);
            let retakeFlag = false;
            if (isRetake) {
                if (window.confirm(`[${isRetake.courseName}] 수강 기록이 존재합니다. 이전 기록을 지우고 새로 등록하시겠습니까?`)) {
                    retakeFlag = true;
                    const targetDeleteId = isRetake.id;

                    setDeleteCourseQueue(prev => prev.includes(targetDeleteId) ? prev : [...prev, targetDeleteId]);
                } else {
                    // 취소 시 init으로 돌림
                    setLocalCourses(prev => {
                        const next = [...prev];
                        next[index] = {...next[index], courseId: "", courseName: "", retake: false };
                        return next;
                    });
                    return;
                }
            }
            // !비동기 처리 (fetchCourseInfo: DB 탐색, UserProvider.jsx에 정의.)
            const found = await fetchCourseInfo(trimCourseId);
            setLocalCourses(prev => {
                const next = [...prev];
                // 자동 검색 부분 (!!isRetake 확인할 것)
                if (found) {
                    next[index] = { ...next[index], ...found, retake: retakeFlag, fromDB: true };
                } else {
                    // (검색실패시)
                    next[index] = { ...next[index], courseId: trimCourseId, retake: retakeFlag, fromDB: false };
                }
                return next;
            }); 
        } catch (error) {
            console.error("갱신오류: " , error);
        } finally {
            // 락 해제.
            isUpdating.current = false;
        }
    }, [fetchCourseInfo, activeTaken, userCourses, localCourses, setDeleteCourseQueue]);

    // const handleAddRow: 수강이력 추가 기능
    const handleAddRow = () => {
        if (!activeTaken) {
            alert("학기 입력을 먼저 완료해주세요."); 
            return;
        }
        setLocalCourses([...localCourses, { 
            courseId: "", courseName: "", category: "자선", credits: 3, grade: "A+", 
            taken: activeTaken, retake: false 
        }]);
    };
    // const handleDeleteRow: 수강이력 삭제 기능
    const handleDeleteRow = (index) => {
        setLocalCourses(prev => prev.filter((_, i) => i !== index));
    };

    // const handleApplyTaken: 학기키(taken) 변경 버튼 핸들러
    const handleApplyTaken = () => {
        const newTaken = `${inputYear}-${inputTerm}`;
        const newSortKey = parseInt(inputYear) * 100 + termWeight[inputTerm];

        if (newTaken === activeTaken) return;
        // !중복 체크
        const isDup = userSemesters.some(sem => sem.taken === newTaken && sem.id !== targetSem.id);
        if (isDup) {
            alert(`이미 [${newTaken}] 학기 정보가 존재합니다.`);
            return;
        }
        // !시간 순서 체크
        const isInvalid = userSemesters.some(sem => {
            if (sem.id === targetSem.id || sem.isNonRegularTerm !== targetSem.isNonRegularTerm) {
                return false;
            }
            if (targetSem.term > sem.term && newSortKey <= sem.sortKey) {
                return true;
            }
            if (targetSem.term < sem.term && newSortKey >= sem.sortKey) {
                return true;
            }

            return false;
        })
        if (isInvalid) {
            alert('학기 순서가 올바르지 않습니다.');
            return;
        }
        
        if (window.confirm(`학기를 [${newTaken}]으로 변경하시겠습니까?`)) {
            setActiveTaken(newTaken);
        }
    };

    // 실제 저장 (+ 유효성 검사)
    const handleSave = async () => {
        if (!activeTaken) return alert("학기 적용을 먼저 완료해주세요.");
        if (!user) return alert("로그인 정보가 없습니다."); // 유저 검증 강화
        
        // 유효성 검사 (순회식)
        for (let i = 0; i < localCourses.length; i++) {
            const { courseId, courseName, credits } = localCourses[i];
            if (!courseId?.trim() || !courseName?.trim() || !credits) {
                return alert(`${i + 1}번째 과목의 정보를 모두 입력해주세요.`);
            }
        }

        try {
            // Firebase로부터 최신 JWT 인증 헤더를 받아옵니다.
            const header = await getAuthHeader();
            if (!header.headers) {
                return alert("인증 토큰을 가져오지 못했습니다. 다시 시도해 주세요.");
            }

            if (deleteCourseQueue && deleteCourseQueue.length > 0) {
                for (const targetId of deleteCourseQueue) {
                    await deleteCourse(targetId);
                }
                setDeleteCourseQueue([]);
            }

            // 백엔드로 보낼 가중치 sortKey 계산
            const takenSplit = activeTaken.split('-');
            const sortKey = parseInt(takenSplit[0]) * 100 + termWeight[takenSplit[1]];

            // 💡 백엔드 DTO 포맷에 맞게 JSON 바디 가공
            const payload = {
                activeTaken: activeTaken,
                sortKey: sortKey,
                targetSemId: targetSem.id || null, 
                isNonRegularTerm: targetSem.isNonRegularTerm,
                term: targetSem.term,
                oldTaken: targetSem.taken || null, 
                localCourses: localCourses.map(cs => ({
                    courseId: cs.courseId,
                    courseName: cs.courseName,
                    category: cs.category,
                    credits: Number(cs.credits),
                    grade: cs.grade,
                    retake: cs.retake
                }))
            };

            // !서버에 올리는 경우 IP값으로 변경해야함. (localhost:8080 부분)
            await axios.post(
                `http://localhost:8080/api/users/${user.uid}/semesters/save-all`, 
                payload, 
                header
            );
            
            // 저장 직후 화면에 새 데이터를 즉시 동기화
            await updateProfile({}); 

            alert("성공적으로 저장되었습니다.");
            onClose();

        } catch (error) {
            console.error("저장 실패:", error);
            const errorMsg = error.response?.data?.error || "백엔드 콘솔을 확인하세요.";
            alert(`저장 중 오류가 발생했습니다. (${errorMsg})`);
        }
    }
    

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="cp-container border-r" onClick={e => e.stopPropagation()}>
                <header className="cp-header">
                    <h3>수강이력 편집</h3>
                    {!activeTaken && <p className="warning-text">연도와 학기를 설정하고 '적용'을 눌러주세요.</p>}
                    
                    <div className="taken-selector">
                        <div className="selector-group">
                            <input 
                                type="number" 
                                className="dark-input input-year" 
                                value={inputYear} 
                                onChange={e => setInputYear(e.target.value)} 
                            />
                            <span>년</span>
                            <select 
                                className="dark-select select-term" 
                                value={inputTerm} 
                                onChange={e => setInputTerm(e.target.value)}
                            >
                                {targetSem.isNonRegularTerm === 0 ? (
                                    <><option value="1">1학기</option>
                                    <option value="2">2학기</option></>
                                ) : (
                                    <><option value="S">여름</option>
                                    <option value="W">겨울</option></>
                                )}
                            </select>
                            <button className="apply-btn" onClick={handleApplyTaken}>적용</button>
                        </div>
                        <div className="active-badge">
                            <b>{activeTaken || "미지정"}</b>
                        </div>
                    </div>
                </header>

                <div className="modal-body">
                    <div className="table-header">
                        <span className="col-cid">과목코드</span>
                        <span className="col-name">과목명</span>
                        <span className="col-ctype">과목유형</span>
                        <span className="col-credit">학점</span>
                        <span className="col-grade">성적</span>
                        <span className="col-del"></span>
                    </div>      
                    {localCourses.map((course, idx) => (
                        <CourseEntity 
                            key={course.courseId || idx} // courseId가 없을 땐 idx를 키로 활용
                            index={idx} 
                            course={course} 
                            onUpdate={handleUpdateRow} 
                            onDelete={handleDeleteRow}
                            grades={grades}
                            categories={categories}
                        />
                    ))}
                    <button className="add-btn" onClick={handleAddRow}>
                        + 과목 추가
                    </button>
                </div>

                <footer className="modal-footer">
                    <button className="save-btn" onClick={handleSave}>저장</button>
                    <button className="cancel-btn" onClick={onClose}>취소</button>
                </footer>
            </div>
        </div>
    );
};

export default CoursePopup;