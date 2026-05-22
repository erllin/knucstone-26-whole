import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut } from "firebase/auth";
// Appcheck(X), AI Logic(X)

// FIREBASE 관련 환경변수
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
};

const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();

// Ai 텍스트 모델 사용 (무료용, firebase Ai logic을 이용함.) >> 백엔드로 넘김
// ~유감스럽게도, 토큰 한도가 줄어들어서 잘 사용해야함...
/* 
      gemini-2.5-flash      (일 최대 20회/ 분 15회)
      gemini-2.5-flash-lite (일 최대 20회/ 분 15회)
*/

// 구글 로그인에서 토큰을 가져와서 리턴. (추후 SpringBoot에서 검증될 예정)
export const loginWithGoogle = async () => {
  try {
    const result = await signInWithPopup(auth, googleProvider);
    const idToken = await result.user.getIdToken();

    return { user: result.user, idToken }
  } catch (error) {
    console.error("구글 로그인이 실패하였습니다.: ", error);
    throw error;
  }
};
export const logout = () => signOut(auth);