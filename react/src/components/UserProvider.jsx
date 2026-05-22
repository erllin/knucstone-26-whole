import React, { useState, useEffect, useContext, useCallback, useMemo, createContext } from "react";
import { auth, loginWithGoogle, logout } from "../firebase";
import { onAuthStateChanged } from "firebase/auth";
import axios from "axios";

const UserContext = createContext();
const BASE_URL = "http://localhost:8080/api";   //!사용시, 서버 IP로 변경해야함
const courseCache = {};

// !기존 localStorage -> Firebase -> Axios 식으로 넘어와서 일부 문제가 있을 수 있음.
export const UserProvider = ({ children }) => {
    // 페이지 넘어가면 증발해버리는 문제가 있어서, 데이터 값들은 이쪽 컨텍스트에 담는 것이 좋음.
    // 불러오기도 적게 들어서 효율적 :)
    const [user, setUser] = useState(null);
    const [userProfile, setUserProfile] = useState(null);
    const [userCourses, setUserCourses] = useState([]);
    const [userSemesters, setUserSemesters] = useState([]);
    const [reqCredits, setReqCredits] = useState([]);
    const [tracks, setTracks] = useState([]);
    const [loading, setLoading] = useState(true);
    // Ai 분석용
    const [aiReport, setAiReport] = useState(null);
    const [isAiLoading, setIsAiLoading] = useState(false);
    const [aiError, setAiError] = useState(null);

    // getAuthHeader: Firebase에서 유효한 JWT 토큰을 실시간으로 가져와 Axios 헤더 객체로 매핑하는 메소드
    const getAuthHeader = useCallback(async (currentUser) => {
        const targetUser = currentUser || auth.currentUser;
        if (!targetUser) return {};
        try {
            // getIdToken(true)는 만료된 토큰을 즉시 강제 갱신하여 받아옴.
            // 모든 처리에는 이 토큰이 백엔드로 같이 전송되어야 함.
            const token = await targetUser.getIdToken(true);
            return {
                headers: {
                    Authorization: `Bearer ${token}`
                }
            };
        } catch (error) {
            console.error("Firebase 토큰 갱신 및 로드 실패:", error);
            return {};
        }
    }, []);

    // fetchAllUserData: 로그인 상태가 확보되었을 때 백엔드로부터 프로필, 수강이력, 학기데이터를 일괄 GET
    const fetchAllUserData = useCallback(async (uid, currentUser) => {
        try {
            const header = await getAuthHeader(currentUser);
            if (!header.headers) return; // 헤더 구성 실패 시 요청 차단

            const res = await axios.get(`${BASE_URL}/users/${uid}`, header);
            setUserProfile(res.data.userProfile);
            setUserCourses(res.data.userCourses || []);
            setUserSemesters(res.data.userSemesters || []);
        } catch (error) {
            console.error("유저 데이터 동기화에 실패하였습니다: ", error);
        }
    }, [getAuthHeader]);
    
    // 전공 트랙 목록 로드
    useEffect(() => {
        const loadTracks = async () => {
            try {
                const header = await getAuthHeader();
                const res = await axios.get(`${BASE_URL}/tracks`, header);
                setTracks(res.data);
            } catch (err) {
                console.error("트랙 로드 실패: ", err);
            }
        };

        if (user) {
            loadTracks();
        }
    }, [user, getAuthHeader]);

    // fetchReqCredit: 이수학점표를 변경된 사용자 학사정보(전공유형)에 맞게 가져오는 메소드.
    const fetchReqCredit = useCallback(async (uid) => {
        const targetUid = uid || user?.uid;
        if (!targetUid) return;

        try {
            const header = await getAuthHeader();
            if (!header.headers) return;

            // !해당 부분은 나중에 다른 학과가 추가되는 경우, 학과 도큐먼트나 이런 것을 따로 만들어 배치하는 것이 좋을 듯
            // !현재는 컴퓨터공학과만을 추가하는 것을 목표로 하였기에, 해당 부분은 cs_2026으로 남김.
            const reqUrl = `${BASE_URL}/users/${targetUid}/credit_tables/cs_2026`;
            const res = await axios.get(reqUrl, header);
            
            if (res.data) {
                setReqCredits(res.data);
                console.log(`${res.data.department} (유형 ${res.data.matchedDetail.majorType}) 학점표를 동기화했습니다.`);
            }
        } catch (error) {
            console.error("이수학점 데이터를 가져오는 데 실패했습니다:", error);
        }
    }, [user, getAuthHeader]);

    // unsubscribeAuth: 로그인 상태가 바뀌면 동기화 함수를 호출해줌. (다만, 크게 쓰일 일은 없을 것 같습니다.)
    useEffect(() => {
        const unsubscribeAuth = onAuthStateChanged(auth, async (currentUser) => {
            setUser(currentUser);
            if (currentUser) {
                await fetchAllUserData(currentUser.uid, currentUser);
                await fetchReqCredit(currentUser.uid);
            } else {
                setUserProfile(null);
                setUserCourses([]);
                setUserSemesters([]);
            }
            setLoading(false);
        });

        return () => unsubscribeAuth();
    }, [fetchAllUserData, fetchReqCredit]);

    // addCourse: 수강 이력 추가 및 중복 수강 차단 처리
    const addCourse = useCallback(async (cs) => {
        if (!user) { return; }

        // 중복 수강 방지 유효성 체크 로직 (과목 고유 ID인 courseId 문자열 비교 일치 체크)
        const isAlreadyTaken = userCourses.some(course => String(course.courseId) === String(cs.courseId));
        if (isAlreadyTaken) {
            alert("이미 수강 신청이 완료된 중복 과목입니다.");
            console.error("과목 추가 거부: 중복 수강 식별됨.");
            return;
        }

        try {
            const header = await getAuthHeader();
            // axios.post(url, data, config)
            await axios.post(`${BASE_URL}/users/${user.uid}/courses/${String(cs.courseId)}`, cs, header);
            await fetchAllUserData(user.uid);
        } catch (error) {
            console.error("수강 과목 추가 실패: ", error);
        }
    }, [user, userCourses, getAuthHeader, fetchAllUserData]);

    // deleteCourse: 단일 수강 이력 삭제 메소드
    const deleteCourse = useCallback(async (courseId) => {
        if (!user) return;
        try {
            const header = await getAuthHeader();
            // axios.delete(url, config) -> 2번째 인자에 헤더 탑재
            await axios.delete(`${BASE_URL}/users/${user.uid}/courses/${String(courseId)}`, header);
            await fetchAllUserData(user.uid);
        } catch (error) {
            console.error("과목 삭제 실패: ", error);
        }
    }, [user, getAuthHeader, fetchAllUserData]);

    // addSemester: 학기 추가 메소드
    const addSemester = useCallback(async (sem) => {
        if (!user) return;

        const isDup = userSemesters.some(s => s.taken === sem.taken);
        if (isDup) {
            alert("중복된 학기가 존재합니다.");
            return;
        }

        try {
            const header = await getAuthHeader();
            // axios.post(url, data, config)
            await axios.post(`${BASE_URL}/users/${user.uid}/semesters`, sem, header);
            await fetchAllUserData(user.uid);
        } catch (error) {
            console.error("학기 추가 실패: ", error);
        }
    }, [user, userSemesters, getAuthHeader, fetchAllUserData]);

    // updateSemesterTaken: 학기 번호(taken) 갱신 메소드
    const updateSemesterTaken = useCallback(async (semId, taken) => {
        if (!user) return;
        if (userSemesters.some(sem => sem.taken === taken)) {
            alert("학기 변경 실패: 이미 사용 중인 학기 번호입니다.");
            return;
        }

        const tSem = userSemesters.find(sem => sem.id === semId);
        if (!tSem) return;

        try {
            const header = await getAuthHeader();
            const requestData = {
                semId: semId,
                takenOld: tSem.taken,
                takenNew: taken
            };
            // axios.put(url, data, config)
            await axios.put(`${BASE_URL}/users/${user.uid}/semester-taken`, requestData, header);
            await fetchAllUserData(user.uid);
        } catch (error) {
            console.error("학기 변경에 실패하였습니다: ", error);
        }
    }, [user, userSemesters, getAuthHeader, fetchAllUserData]);

    // deleteSemester: 학기 + 관련 교과목 연쇄 삭제
    const deleteSemester = useCallback(async (tType, tNum) => {
        if (!user) return false;

        const tSem = userSemesters.find(sem => sem.isNonRegularTerm === tType && sem.term === tNum);
        if (!tSem) return false;

        if (!window.confirm('학기를 삭제하면, 하위 모든 수강이력이 함께 영구 삭제됩니다.')) {
            return false;
        }

        try {
            const tokenHeader = await getAuthHeader();
            const config = {
                headers: tokenHeader.headers,
                params: { taken: tSem.taken }
            };

            await axios.delete(`${BASE_URL}/users/${user.uid}/semesters/${tSem.id}`, config);
            await fetchAllUserData(user.uid);

            return true;
        } catch (error) {
            console.error("학기 삭제 실패: ", error);
            return false;
        }
    }, [user, userSemesters, getAuthHeader, fetchAllUserData]);

    // updateProfile: 학사 프로필 정보 업데이트
    const updateProfile = useCallback(async (data) => {
        if (!user) return;
        try {
            const header = await getAuthHeader();
            // axios.put(url, data, config)
            await axios.put(`${BASE_URL}/users/${user.uid}/profile`, data, header);
            await fetchAllUserData(user.uid);
            await fetchReqCredit(user.uid);
        } catch (error) {
            console.error("프로필 업데이트 실패: ", error);
        }
    }, [user, getAuthHeader, fetchAllUserData, fetchReqCredit]);

    // fetchCourseInfo: 개별 교과목 정보 검색, courseCache를 이용해 캐싱.
    const fetchCourseInfo = useCallback(async (courseId) => {
        const trimCourseId = courseId.trim();
        if (!trimCourseId) return null;

        if (courseCache[trimCourseId]) {
            return courseCache[trimCourseId];
        }

        try {
            const header = await getAuthHeader();
            // axios.get(url, config)
            const res = await axios.get(`${BASE_URL}/courses/${trimCourseId}`, header);
            if (res.data) {
                const mapped = {
                    courseId: res.data.id,
                    courseName: res.data.name,
                    category: res.data.metadata?.category,
                    credits: res.data.metadata?.credits,
                    fromDB: true
                };
                courseCache[trimCourseId] = mapped;
                return mapped;
            }
            return null;
        } catch (error) {
            console.log("교과목 탐색 실패: ", error);
            return null;
        }
    }, [getAuthHeader]);

    // fetchAiConsultingReport: AI 분석 호출 (이수학점 테이블 ID 기본 = cs_2026)
    const fetchAiConsultingReport = useCallback(async (creditTableDocId = 'cs_2026') => {
        if (!user) { return null; }
        if (isAiLoading) { return null; }

        setIsAiLoading(true);
        setAiError(null);

        try {
            const header = await getAuthHeader();
            if (!header.headers) { return null; }

            const res = await axios.get(`${BASE_URL}/ai/consulting-report`, {
                ...header,
                params: { creditTableDocId }
            });

            if (res.data) {
                setAiReport(res.data);
                return res.data;
            } else {
                setAiError('AI 분석 응답을 받아오는데 실패하였습니다.');
                return null;
            }

        } catch (error) {
            console.error("AI 학업 컨설팅 리포트 수신 실패:", error);
            const errMsg = error.response?.data?.message || "AI 분석 도중 문제가 발생했습니다.";
            setAiError(errMsg)
            return null;

        } finally {
            setIsAiLoading(false);
        }

    }, [user, getAuthHeader, isAiLoading]);

    const val = useMemo(() => ({
        user, userProfile, userCourses, userSemesters, loading, tracks, reqCredits,
        aiReport, aiError, isAiLoading,
        getAuthHeader, fetchReqCredit,
        loginWithGoogle, logout, updateProfile, addCourse, deleteCourse,
        addSemester, updateSemesterTaken, deleteSemester,
        fetchCourseInfo, fetchAiConsultingReport
    }), [
        user, userProfile, userCourses, userSemesters, loading, tracks, reqCredits,
        aiReport, aiError, isAiLoading,
        getAuthHeader, fetchReqCredit,
        updateProfile, addCourse, deleteCourse,
        addSemester, updateSemesterTaken, deleteSemester,
        fetchCourseInfo, fetchAiConsultingReport
    ]);

    return (
        <UserContext.Provider value={val}>
            {!loading ? children : <div style={{textAlign: 'center', marginTop: '20%'}}>사용자 인증 상태 로딩 중...</div>}
        </UserContext.Provider>
    );
};

export const useUser = () => {
    const context = useContext(UserContext);
    if (!context) {
        throw new Error("UserProvider 사용 컴포넌트는 UserProvider 내부에 위치해야 함.");
    }
    return context;
};