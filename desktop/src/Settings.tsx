import { useEffect, useState } from "react";
export type Preferences={
 theme:"system"|"light"|"dark";accent:string;density:"comfortable"|"compact";
 weekStartsOn:0|1;dateFormat:"yyyy-mm-dd"|"mm/dd/yyyy"|"dd/mm/yyyy";timeFormat:"12h"|"24h";
 defaultView:"today"|"inbox"|"all";defaultPriority:0|1|2|3;defaultDue:"none"|"today"|"tomorrow";
 showCompleted:boolean;confirmDelete:boolean;focusMinutes:number;shortBreakMinutes:number;longBreakMinutes:number;
 focusSound:boolean;desktopNotifications:boolean;startMinimized:boolean;autoLaunch:boolean;language:"ko"|"en";
};
export const defaults:Preferences={theme:"system",accent:"#4776ef",density:"comfortable",weekStartsOn:0,dateFormat:"yyyy-mm-dd",timeFormat:"24h",defaultView:"today",defaultPriority:0,defaultDue:"none",showCompleted:false,confirmDelete:true,focusMinutes:25,shortBreakMinutes:5,longBreakMinutes:15,focusSound:true,desktopNotifications:true,startMinimized:false,autoLaunch:false,language:"ko"};
const storageKey="bettertick.desktop.preferences.v1";
export function loadPreferences():Preferences{try{const raw=localStorage.getItem(storageKey);return raw?{...defaults,...JSON.parse(raw)}:defaults}catch{return defaults}}
export function savePreferences(p:Preferences){localStorage.setItem(storageKey,JSON.stringify(p))}
export function applyPreferences(p:Preferences){
 const dark=p.theme==="dark"||(p.theme==="system"&&matchMedia("(prefers-color-scheme: dark)").matches);
 document.documentElement.dataset.theme=dark?"dark":"light";
 document.documentElement.dataset.density=p.density;
 document.documentElement.style.setProperty("--accent",p.accent);
}
export function Settings({value,onChange,onClose}:{value:Preferences;onChange:(p:Preferences)=>void;onClose:()=>void}){
 const [tab,setTab]=useState("general");
 useEffect(()=>applyPreferences(value),[value]);
 const update=<K extends keyof Preferences>(key:K,next:Preferences[K])=>onChange({...value,[key]:next});
 const select=<K extends keyof Preferences>(key:K,options:[string,Preferences[K]][])=> <select value={String(value[key])} onChange={e=>update(key,options.find(o=>String(o[1])===e.target.value)![1])}>{options.map(([label,v])=><option key={String(v)} value={String(v)}>{label}</option>)}</select>;
 const toggle=(key:keyof Preferences)=><input type="checkbox" checked={Boolean(value[key])} onChange={e=>onChange({...value,[key]:e.target.checked})}/>;
 const number=(key:keyof Preferences,min:number,max:number)=><input type="number" min={min} max={max} value={Number(value[key])} onChange={e=>onChange({...value,[key]:Math.max(min,Math.min(max,Number(e.target.value)||min))})}/>;
 return <section className="settings-page"><div className="heading"><h1>설정</h1><button onClick={onClose}>닫기 ×</button></div><div className="settings-layout"><nav>{[["general","일반"],["appearance","화면"],["tasks","할 일"],["focus","집중"],["notifications","알림"],["desktop","데스크톱"]].map(([id,label])=><button key={id} className={tab===id?"active":""} onClick={()=>setTab(id)}>{label}</button>)}</nav><div className="settings-options">
 {tab==="general"&&<><h2>일반</h2><label>언어{select("language",[["한국어","ko"],["English","en"]])}</label><label>한 주의 시작{select("weekStartsOn",[["일요일",0],["월요일",1]])}</label><label>날짜 형식{select("dateFormat",[["2026-11-12","yyyy-mm-dd"],["11/12/2026","mm/dd/yyyy"],["12/11/2026","dd/mm/yyyy"]])}</label><label>시간 형식{select("timeFormat",[["24시간","24h"],["12시간","12h"]])}</label><label>시작 화면{select("defaultView",[["오늘","today"],["받은 편지함","inbox"],["전체","all"]])}</label></>}
 {tab==="appearance"&&<><h2>화면</h2><label>테마{select("theme",[["시스템","system"],["라이트","light"],["다크","dark"]])}</label><label>강조 색상<input type="color" value={value.accent} onChange={e=>update("accent",e.target.value)}/></label><label>화면 밀도{select("density",[["기본","comfortable"],["압축","compact"]])}</label></>}
 {tab==="tasks"&&<><h2>할 일</h2><label>기본 우선순위{select("defaultPriority",[["없음",0],["낮음",1],["보통",2],["높음",3]])}</label><label>새 할 일 기본 날짜{select("defaultDue",[["없음","none"],["오늘","today"],["내일","tomorrow"]])}</label><label>완료한 할 일 표시{toggle("showCompleted")}</label><label>삭제 전 확인{toggle("confirmDelete")}</label></>}
 {tab==="focus"&&<><h2>집중</h2><label>집중 시간(분){number("focusMinutes",1,180)}</label><label>짧은 휴식(분){number("shortBreakMinutes",1,60)}</label><label>긴 휴식(분){number("longBreakMinutes",1,120)}</label><label>종료 알림음{toggle("focusSound")}</label></>}
 {tab==="notifications"&&<><h2>알림</h2><label>데스크톱 알림{toggle("desktopNotifications")}</label><p className="settings-note">시스템 알림 권한이 필요하며 알림 전달 기능은 별도 연동이 필요합니다.</p></>}
 {tab==="desktop"&&<><h2>데스크톱</h2><label>시작 시 최소화{toggle("startMinimized")}</label><label>시스템 시작 시 실행{toggle("autoLaunch")}</label><p className="settings-note">이 두 옵션은 현재 선호도 저장만 지원합니다. 실제 OS 동작 연결은 추가 작업이 필요합니다.</p></>}
 <div className="settings-footer"><button onClick={()=>onChange(defaults)}>기본값 복원</button><span>설정은 이 기기에 자동 저장됩니다.</span></div></div></div></section>
}
