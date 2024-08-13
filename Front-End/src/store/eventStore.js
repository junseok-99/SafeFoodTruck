import { create } from 'zustand';

const useEventStore = create((set, get) => ({
    // (사장님) 주문 들어왔을때
    ownerOrderNotice: false,
    ownerOrderNoticeMessage: null,
    setOwnerOrderNotice: (ownerOrderNotice) => set({ ownerOrderNotice }),
    setOwnerOrderNoticeMessage: (ownerOrderNoticeMessage) => set({ ownerOrderNoticeMessage }),
    
    // 방송 시작
    ownerLiveStratFlag: false,
    setOwnerLiveStratFlag: (ownerLiveStratFlag) => set({ownerLiveStratFlag}),

    // 방송 종료
    ownerLiveEndFlag: false,
    setOwnerLiveEndFlag: (ownerLiveEndFlag) => set({ownerLiveEndFlag}),
}));

export default useEventStore;

