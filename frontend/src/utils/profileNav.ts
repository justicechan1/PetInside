// 작성자 닉네임/프로필 사진 클릭 시 이동할 경로 결정 (F-33)
// 본인이면 수정 가능한 마이페이지로, 타인이면 조회 전용 공개 프로필로 이동
export const profilePath = (authorId: number, authorNickname: string): string =>
    authorNickname === localStorage.getItem('nickname') ? '/mypage' : `/users/${authorId}`;
