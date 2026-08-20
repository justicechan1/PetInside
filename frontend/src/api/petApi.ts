import axiosInstance from './axiosInstance';

export interface Pet {
    id: number;
    petName: string;
    petType: string;
    petBirthday: string | null;
    petIntro: string | null;
    petImageUrl: string | null;
    createdAt: string;
}

export interface PetForm {
    petName: string;
    petType: string;
    petBirthday: string;
    petIntro: string;
    petImageUrl: string;
}

export const getMyPets = () =>
    axiosInstance.get<{ data: Pet[] }>('/api/v1/users/me/pets').then(r => r.data.data);

export const getUserPets = (userId: number) =>
    axiosInstance.get<{ data: Pet[] }>(`/api/v1/users/${userId}/pets`).then(r => r.data.data);

export const createPet = (data: PetForm) =>
    axiosInstance.post<{ data: Pet }>('/api/v1/pets', data).then(r => r.data.data);

export const updatePet = (petId: number, data: PetForm) =>
    axiosInstance.put<{ data: Pet }>(`/api/v1/pets/${petId}`, data).then(r => r.data.data);

export const deletePet = (petId: number) =>
    axiosInstance.delete(`/api/v1/pets/${petId}`);

export interface PetPhoto {
    id: number;
    imageUrl: string;
    caption: string | null;
    createdAt: string;
}

export const getPetPhotos = (petId: number) =>
    axiosInstance.get<{ data: PetPhoto[] }>(`/api/v1/pets/${petId}/photos`).then(r => r.data.data);

export const addPetPhoto = (petId: number, imageUrl: string, caption?: string) =>
    axiosInstance.post<{ data: PetPhoto }>(`/api/v1/pets/${petId}/photos`, { imageUrl, caption }).then(r => r.data.data);

export const deletePetPhoto = (petId: number, photoId: number) =>
    axiosInstance.delete(`/api/v1/pets/${petId}/photos/${photoId}`);
