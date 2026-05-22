import { useMemo } from "react";
import { useUser } from "./UserProvider";

const GRADE_MAP = {
    "A+": 4.5, "A0": 4.0, "B+": 3.5, "B0": 3.0, "C+": 2.5, "C0": 2.0, "D+":1.5, "D0": 1.0, "F": 0.0,
    "P": null, "NP": null
};

const COURSE_TYPE = {
    "전필": 0, "전선": 0, "심화": 0,
    "교양": 1, "기교": 1, "균교": 1, "글로컬": 1, "G-Share": 1,
    "자선": 2
}

const useStats = () => {
    // 성적 데이터 (userCourses)
    const { userCourses, userSemesters, reqCredits } = useUser();
    
    // const calcGpa: 평점 계산 
    const calcGpa = (point, count) => {
        return (count > 0 ? (point / count).toFixed(2) : "0.00")
    };

    const calcCore = (courses) => {
        const result = {
            gpa : {
                general: { point: 0, count: 0 },
                major: { point: 0, count: 0 },
                other: { point: 0, count: 0 }
            },
            credit: {
                general: 0, major: 0, mRequire: 0, mElective: 0,
                liberal: 0, lBasic: 0, lBalance: 0, lGlocal:0, lGShare: 0,
                free: 0
            }
        }

        courses.forEach(cs => {
            const grade = cs.grade;
            const p = GRADE_MAP[grade];
            const c = Number(cs.credits);
            // 성적 판별
            const isMajor = (COURSE_TYPE[cs.category] == 0);
            const isLib = (COURSE_TYPE[cs.category] == 1);
            // NP, F는 미이수 과목으로 판단
            if (grade === "NP" || grade === "F") { return; }

            // 범용
            result.credit.general += c;
            if (isMajor) { result.credit.major += c; }
            else if (isLib) { result.credit.liberal += c; }
            

            switch (cs.category) {
                case "전필": result.credit.mRequire += c; break;
                case "전선": result.credit.mElective += c; break;
                case "기교": result.credit.lBasic += c; break;
                case "균교": result.credit.lBalance += c; break;
                case "글로컬": result.credit.lGlocal += c; break;
                case "G-Share": result.credit.lGShare += c; break;
                default: result.credit.free += c; break;            // 자선 포함
            }

            if (typeof p === 'number') {
                const w = p * c;
                result.gpa.general.point += w;
                result.gpa.general.count += c;

                if (isMajor) {
                    result.gpa.major.point += w;
                    result.gpa.major.count += c;
                } else {
                    result.gpa.other.point += w;
                    result.gpa.other.count += c;
                }
            }
        });

        return result;
    };

    // const semesterStats: 모든 학기의 평점, 학점을 변환해줌
    const semesterStats = useMemo(() => {
        // 정규 학기 필터링 & 정렬 
        // (term으로도 괜찮을지도?, sortKey)
        const regularSem = userSemesters
            .sort((a, b) => (a.sortKey || 0) - (b.sortKey || 0));

        // CourseLog의 semStats를 이곳으로 통합하는 것이 좋을 것 같다.
        return regularSem.map(sem => {
                const courses = userCourses.filter(cs => cs.taken === sem.taken);
                const { gpa, credit } = calcCore(courses);

                return {
                    id: sem.id,
                    term: sem.term,
                    isNonRegularTerm: sem.isNonRegularTerm,
                    credit: credit,
                    gpa: {
                        general: calcGpa(gpa.general.point, gpa.general.count),
                        major: calcGpa(gpa.major.point, gpa.major.count),
                        other: calcGpa(gpa.other.point, gpa.other.count)
                    }
                };
            });
    }, [userCourses, userSemesters]);

    const generalStats = useMemo(() => {
        const { gpa, credit } = calcCore(userCourses);

        // 특수 케이스
        // 분야 초과 학점 -> 자선
        // 전선의 경우: 전선 -> 심화 -> 자선 (다만, 전선/심화 구분이 없어서 모두 전선으로 두었음.)
        const matchedDetail = reqCredits?.matchedDetail;
        const major = matchedDetail?.major;
        const liberal = matchedDetail?.generalEducation;

        let tmpCredit;
        // 교양 학점
        credit.lBalance = (credit.lBalance > liberal.balanced) ? liberal.balanced : credit.lBalance;
        credit.lBasic = (credit.lBasic > liberal.basic) ? liberal.basic : credit.lBasic;
        credit.lGShare = (credit.lGShare > liberal.gShare) ? liberal.gShare : credit.lGShare;
        credit.lGlocal = (credit.lGlocal > liberal.glocal) ? liberal.glocal : credit.lGlocal;
        // 자선 넘어감
        tmpCredit = credit.lBalance + credit.lBasic + credit.lGShare + credit.lGlocal
        credit.free += credit.liberal - tmpCredit;
        credit.liberal = tmpCredit;
        // 전공 학점
        credit.mRequire = (credit.mRequire > major.required) ? major.required : credit.mRequire;
        tmpCredit = major.elective + major.advanced;
        credit.mElective = (credit.mElective > tmpCredit) ? tmpCredit : credit.mElective;
        // 자선 넘어감
        tmpCredit = credit.mRequire + credit.mElective;
        credit.free += credit.major - tmpCredit;

        return {
            credit: credit,
            gpa: {
                general: calcGpa(gpa.general.point, gpa.general.count),
                major: calcGpa(gpa.major.point, gpa.major.count),
                other: calcGpa(gpa.other.point, gpa.other.count)
            }
        }
        }, [userCourses, reqCredits]);

    return { generalStats, semesterStats };
}

export default useStats;