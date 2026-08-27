import axiosInstance from './axiosInstance';

// 백엔드가 파일을 EC2 로컬 디스크에 저장하고 접근 가능한 절대 URL을 돌려준다.
// Content-Type은 axios가 FormData를 보고 boundary까지 알아서 채우므로 직접 지정하지 않는다.
export const uploadImage = (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosInstance
        .post<{ data: { url: string } }>('/api/v1/images/upload', formData)
        .then(r => r.data.data.url);
};
