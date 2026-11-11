import { Tags } from "./Tags";
import { Habits, Matrix } from "./ExtraViews";
import { initializeApp, getApps } from "firebase/app";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged, type User } from "firebase/auth";
import { getFirestore, collection, onSnapshot, doc, setDoc, deleteDoc, serverTimestamp } from "firebase/firestore";
import { useEffect, useMemo, useState } from "react";
import { CalendarDays, Check, ChevronLeft, ChevronRight, Circle, Clock3, Flag, Hash, Inbox, LayoutGrid, ListTodo, Menu, Plus, Search, Settings2, Sun, Tag, Timer, Trash2, X } from "lucide-react";
import "./style.css";

type Task = { id: string; title: string; notes?: string; listId: string; dueDate?: any; isCompleted: boolean; priority: number; repeatRule?: string|null; tagIds?: string[]; createdAt?: any; sortOrder?: number };
type TaskList = { id: string; name: string; color: string; icon?: string; sortOrder?: number };
type View = "inbox" | "today" | "upcoming" | "all" | "completed" | "calendar" | "focus" | "board" | string;
const config = { apiKey: import.meta.env.VITE_FIREBASE_API_KEY, authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN, projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID, appId: import.meta.env.VITE_FIREBASE_APP_ID };
const configured = Object.values(config).every(Boolean);
const app = configured ? (getApps()[0] ?? initializeApp(config)) : null;
const auth = app ? getAuth(app) : null;
const db = app ? getFirestore(app) : null;
const dateKey = (d: Date) => [d.getFullYear(),String(d.getMonth()+1).padStart(2,"0"),String(d.getDate()).padStart(2,"0")].join("-");
const taskDate = (task: Task) => task.dueDate?.toDate?.() ?? (task.dueDate ? new Date(task.dueDate) : null);
const today = dateKey(new Date());
const uid = () => crypto.randomUUID();
function App() {
 const [user,setUser] = useState<User|null>(null);
 const [tasks,setTasks] = useState<Task[]>([]);
 const [lists,setLists] = useState<TaskList[]>([]);
 const [tags,setTags] = useState<{id:string;name:string;color:string}[]>([]);
 const [view,setView] = useState<View>("today");
 const [selected,setSelected] = useState<string|null>(null);
 const [search,setSearch] = useState("");
 const [draft,setDraft] = useState("");
 const [sidebar,setSidebar] = useState(true);
 const [showCompleted,setShowCompleted] = useState(false);
 const [month,setMonth] = useState(new Date());
 const [focusSeconds,setFocusSeconds] = useState(25*60);
 const [focusRunning,setFocusRunning] = useState(false);
 const [error,setError] = useState("");
 useEffect(()=>auth ? onAuthStateChanged(auth,setUser) : undefined,[]);
 useEffect(()=>{
  if(!user || !db) { setTasks([]); setLists([]); return; }
  const path = "users/"+user.uid;
  const a = onSnapshot(collection(db,path,"tasks"), snap=>setTasks(snap.docs.map(d=>({ ...d.data(),id:d.id } as Task))), e=>setError(e.message));
  const b = onSnapshot(collection(db,path,"lists"), snap=>setLists(snap.docs.map(d=>({ ...d.data(),id:d.id } as TaskList))), e=>setError(e.message));
  const c = onSnapshot(collection(db,path,"tags"),snap=>setTags(snap.docs.map(d=>({ ...d.data(),id:d.id } as {id:string;name:string;color:string}))),e=>setError(e.message));
  return ()=>{a();b();c()};
 },[user]);
 useEffect(()=>{if(!focusRunning)return;const timer=window.setInterval(()=>setFocusSeconds(s=>{if(s<=1){setFocusRunning(false);return 0;}return s-1;}),1000);return ()=>clearInterval(timer)},[focusRunning]);
 useEffect(()=>{const handler=(e:KeyboardEvent)=>{if((e.ctrlKey||e.metaKey)&&e.key==="k"){e.preventDefault();document.getElementById("search")?.focus()}if((e.ctrlKey||e.metaKey)&&e.key==="n"){e.preventDefault();document.getElementById("quick-add")?.focus()}};window.addEventListener("keydown",handler);return ()=>window.removeEventListener("keydown",handler)},[]);
 const write = async (task:Task) => {if(!user||!db)return;try{await setDoc(doc(db,"users",user.uid,"tasks",task.id),{...task,updatedAt:serverTimestamp()},{merge:true})}catch(e){setError(String(e))}};
 const addTask = async () => {if(!draft.trim()||!user)return;const id=uid();const task:Task={id,title:draft.trim(),notes:"",listId:lists.find(l=>l.id===view)?.id ?? "",isCompleted:false,priority:0,dueDate:view==="today"?new Date():null,sortOrder:Date.now()};setDraft("");await write(task);setSelected(id)};
 const addList = async () => {if(!user||!db)return;const name=prompt("새 목록 이름");if(!name?.trim())return;const id=uid();try{await setDoc(doc(db,"users",user.uid,"lists",id),{id,name:name.trim(),color:"#4776ef",icon:"list",sortOrder:lists.length,createdAt:serverTimestamp()});setView(id)}catch(e){setError(String(e))}};
 const completeTask = async (t:Task) => {
  const completed=!t.isCompleted;
  await write({...t,isCompleted:completed,completedAt:completed?new Date():null} as Task);
  if(!completed||!t.repeatRule||!taskDate(t))return;
  const next=new Date(taskDate(t)!);
  if(t.repeatRule==="DAILY")next.setDate(next.getDate()+1);
  else if(t.repeatRule==="WEEKLY")next.setDate(next.getDate()+7);
  else if(t.repeatRule==="MONTHLY")next.setMonth(next.getMonth()+1);
  else return;
  const id=uid();
  await write({...t,id,isCompleted:false,dueDate:next,sortOrder:Date.now()});
 };
 const removeTask = async (id:string)=>{if(!user||!db)return;try{await deleteDoc(doc(db,"users",user.uid,"tasks",id));setSelected(null)}catch(e){setError(String(e))}};
 const visible = useMemo(()=>tasks.filter(t=>{
  if(search&&!((t.title+" "+(t.notes??"")).toLowerCase().includes(search.toLowerCase())))return false;
  if(view==="completed")return t.isCompleted;
  if(!showCompleted&&t.isCompleted)return false;
  const due=taskDate(t);const key=due?dateKey(due):"";
  if(view==="today")return !!due&&key<=today;
  if(view==="upcoming")return !!due&&key>today;
  if(view==="inbox")return !t.listId;
  if(view==="all"||view==="calendar"||view==="focus"||view==="board")return true;
  return t.listId===view;
 }).sort((a,b)=>(a.isCompleted===b.isCompleted?0:a.isCompleted?1:-1)||b.priority-a.priority||(a.sortOrder??0)-(b.sortOrder??0)),[tasks,view,search,showCompleted]);
 const active = tasks.find(t=>t.id===selected);
 const nav = [{id:"today",name:"오늘",icon:Sun},{id:"upcoming",name:"예정",icon:CalendarDays},{id:"inbox",name:"받은 편지함",icon:Inbox},{id:"all",name:"전체",icon:ListTodo},{id:"completed",name:"완료",icon:Check}];
 if(!configured)return <div className="center"><h1>BetterTick Desktop</h1><p>Firebase 설정이 필요합니다. desktop/.env.example을 .env로 복사하고 Android 프로젝트와 동일한 Firebase 웹 앱 설정을 입력하세요.</p></div>;
 if(!user)return <div className="center"><div className="login-card"><div className="logo-mark">✓</div><h1>BetterTick</h1><p>모바일과 PC에서 할 일을 함께 관리하세요.</p><button className="primary" onClick={()=>auth&&signInWithPopup(auth,new GoogleAuthProvider()).catch(e=>setError(e.message))}>Google 계정으로 로그인</button>{error&&<p className="error">{error}</p>}</div></div>;
 return <div className="shell">
  {sidebar&&<aside className="sidebar">
   <div className="brand"><div className="logo-mark">✓</div><strong>BetterTick</strong><button className="icon-button push" title="사이드바 닫기" onClick={()=>setSidebar(false)}><Menu size={17}/></button></div>
   <div className="profile"><div className="avatar">{(user.displayName??"U").slice(0,1)}</div><span>{user.displayName??user.email}</span><button className="icon-button" title="로그아웃" onClick={()=>auth&&signOut(auth)}><Settings2 size={16}/></button></div>
   <div className="nav-section">{nav.map(item=><button key={item.id} className={"nav-item "+(view===item.id?"active":"")} onClick={()=>{setView(item.id);setSelected(null)}}><item.icon size={18}/><span>{item.name}</span><span className="count">{item.id==="today"?tasks.filter(t=>!t.isCompleted&&taskDate(t)&&dateKey(taskDate(t)!)<=today).length:""}</span></button>)}</div>
   <div className="section-heading">내 목록 <button className="icon-button" onClick={addList} title="목록 추가"><Plus size={17}/></button></div>
   <div className="nav-section">{lists.slice().sort((a,b)=>(a.sortOrder??0)-(b.sortOrder??0)).map(l=><button key={l.id} className={"nav-item "+(view===l.id?"active":"")} onClick={()=>{setView(l.id);setSelected(null)}}><span className="list-dot" style={{background:l.color||"#4776ef"}}/><span>{l.name}</span><span className="count">{tasks.filter(t=>t.listId===l.id&&!t.isCompleted).length}</span></button>)}</div>
   <div className="sidebar-bottom"><button className={"nav-item "+(view==="calendar"?"active":"")} onClick={()=>setView("calendar")}><CalendarDays size={18}/> 캘린더</button><button className={"nav-item "+(view==="tags"?"active":"")} onClick={()=>setView("tags")}><Tag size={18}/> 태그</button><button className={"nav-item "+(view==="habits"?"active":"")} onClick={()=>setView("habits")}><Check size={18}/> 습관</button><button className={"nav-item "+(view==="matrix"?"active":"")} onClick={()=>setView("matrix")}><LayoutGrid size={18}/> 아이젠하워</button><button className={"nav-item "+(view==="board"?"active":"")} onClick={()=>setView("board")}><LayoutGrid size={18}/> 칸반 보드</button><button className={"nav-item "+(view==="focus"?"active":"")} onClick={()=>setView("focus")}><Timer size={18}/> 집중</button></div>
  </aside>}
  <main className="main">
   <header className="toolbar">{!sidebar&&<button className="icon-button" onClick={()=>setSidebar(true)}><Menu size={19}/></button>}<div className="search-box"><Search size={17}/><input id="search" value={search} onChange={e=>setSearch(e.target.value)} placeholder="할 일 검색 (Ctrl+K)"/></div><button className="icon-button" title="오늘" onClick={()=>{setView("today");setSelected(null)}}><Sun size={19}/></button></header>
   {view==="tags"&&db?<Tags db={db} userId={user.uid}/> : view==="habits"&&db?<Habits db={db} userId={user.uid}/> : view==="matrix"?<Matrix tasks={tasks} onSelect={id=>{setSelected(id);setView("all")}}/> : view==="focus"?<section className="focus"><Timer size={36}/><h1>집중 타이머</h1><div className="timer">{String(Math.floor(focusSeconds/60)).padStart(2,"0")}:{String(focusSeconds%60).padStart(2,"0")}</div><div className="actions"><button className="primary" onClick={()=>setFocusRunning(!focusRunning)}>{focusRunning?"일시정지":"시작"}</button><button onClick={()=>{setFocusRunning(false);setFocusSeconds(1500)}}>초기화</button></div></section>:
   view==="calendar"?<section className="content"><div className="heading"><h1>캘린더</h1><div className="actions"><button onClick={()=>setMonth(new Date(month.getFullYear(),month.getMonth()-1,1))}><ChevronLeft size={18}/></button><strong>{month.getFullYear()}년 {month.getMonth()+1}월</strong><button onClick={()=>setMonth(new Date(month.getFullYear(),month.getMonth()+1,1))}><ChevronRight size={18}/></button></div></div><div className="calendar">{["일","월","화","수","목","금","토"].map(d=><div className="weekday" key={d}>{d}</div>)}{Array.from({length:Math.ceil((new Date(month.getFullYear(),month.getMonth(),1).getDay()+new Date(month.getFullYear(),month.getMonth()+1,0).getDate())/7)*7},(_,i)=>{const d=i-new Date(month.getFullYear(),month.getMonth(),1).getDay()+1;const date=new Date(month.getFullYear(),month.getMonth(),d);const matching=tasks.filter(t=>taskDate(t)&&dateKey(taskDate(t)!)===dateKey(date));return <div key={i} className={"day "+(date.getMonth()!==month.getMonth()?"muted":"")}><span className={dateKey(date)===today?"today-pill":""}>{date.getDate()}</span>{matching.slice(0,3).map(t=><button className="calendar-task" key={t.id} onClick={()=>{setSelected(t.id);setView("all")}}>{t.title}</button>)}</div>})}</div></section>:
   view==="board"?<section className="content"><div className="heading"><h1>칸반 보드</h1></div><div className="board">{[0,1,2,3].map(p=><div className="board-column" key={p}><h3>{["우선순위 없음","낮음","보통","높음"][p]} <span>{visible.filter(t=>t.priority===p).length}</span></h3>{visible.filter(t=>t.priority===p).map(t=><button className="board-card" key={t.id} onClick={()=>setSelected(t.id)}>{t.title}</button>)}</div>)}</div></section>:
   <section className="content"><div className="heading"><div><h1>{[...nav,{id:"board",name:"칸반 보드"}].find(n=>n.id===view)?.name??lists.find(l=>l.id===view)?.name??"할 일"}</h1><p>{visible.length}개의 할 일</p></div><button className="subtle" onClick={()=>setShowCompleted(!showCompleted)}>{showCompleted?"완료 숨기기":"완료 표시"}</button></div>
   <form className="add-task" onSubmit={e=>{e.preventDefault();void addTask()}}><Plus size={19}/><input id="quick-add" value={draft} onChange={e=>setDraft(e.target.value)} placeholder="할 일 추가 (Ctrl+N)"/><button type="submit" disabled={!draft.trim()}>추가</button></form>
   <div className="tasks">{visible.length===0?<div className="empty"><Circle size={35}/><h3>할 일이 없어요</h3><p>새로운 할 일을 추가해 보세요.</p></div>:visible.map(t=><div className={"task "+(selected===t.id?"selected":"")} key={t.id}><button className={"check "+(t.isCompleted?"checked":"")} title="완료 전환" onClick={()=>completeTask(t)}>{t.isCompleted&&<Check size={13}/>}</button><button className="task-body" onClick={()=>setSelected(t.id)}><span className={t.isCompleted?"done":""}>{t.title}</span><small>{taskDate(t)?dateKey(taskDate(t)!):"날짜 없음"}{t.notes?" · 메모":""}</small></button>{t.priority>0&&<Flag size={15} className={"priority p"+t.priority}/>}</div>)}</div></section>}
  </main>
  {active&&<aside className="detail"><div className="detail-top"><span>할 일 상세</span><button className="icon-button" onClick={()=>setSelected(null)}><X size={19}/></button></div><textarea className="detail-title" value={active.title} onChange={e=>setTasks(ts=>ts.map(t=>t.id===active.id?{...t,title:e.target.value}:t))} onBlur={()=>write(active)}/><label><CalendarDays size={17}/> 날짜 <input type="date" value={taskDate(active)?dateKey(taskDate(active)!):""} onChange={e=>write({...active,dueDate:e.target.value?new Date(e.target.value+"T12:00:00"):null})}/></label><label><Flag size={17}/> 우선순위 <select value={active.priority} onChange={e=>write({...active,priority:Number(e.target.value)})}>{[["0","없음"],["1","낮음"],["2","보통"],["3","높음"]].map(([v,n])=><option key={v} value={v}>{n}</option>)}</select></label><label><Hash size={17}/> 목록 <select value={active.listId||""} onChange={e=>write({...active,listId:e.target.value})}><option value="">받은 편지함</option>{lists.map(l=><option value={l.id} key={l.id}>{l.name}</option>)}</select></label><label><Clock3 size={17}/> 반복 <select value={active.repeatRule??""} onChange={e=>write({...active,repeatRule:e.target.value||null})}><option value="">반복 안 함</option><option value="DAILY">매일</option><option value="WEEKLY">매주</option><option value="MONTHLY">매월</option></select></label><label><Tag size={17}/> 태그 <select value="" onChange={e=>{if(e.target.value&&!active.tagIds?.includes(e.target.value))write({...active,tagIds:[...(active.tagIds??[]),e.target.value]})}}><option value="">태그 추가</option>{tags.filter(t=>!active.tagIds?.includes(t.id)).map(t=><option value={t.id} key={t.id}>{t.name}</option>)}</select></label><div className="task-tags">{(active.tagIds??[]).map(id=>{const tag=tags.find(t=>t.id===id);return tag?<button key={id} onClick={()=>write({...active,tagIds:(active.tagIds??[]).filter(x=>x!==id)})} style={{background:tag.color+"22",color:tag.color}}>{tag.name} ×</button>:null})}</div><div className="notes-label">메모</div><textarea className="notes" placeholder="상세 내용을 입력하세요..." value={active.notes??""} onChange={e=>setTasks(ts=>ts.map(t=>t.id===active.id?{...t,notes:e.target.value}:t))} onBlur={()=>write(active)}/><div className="detail-footer"><button className="danger" onClick={()=>removeTask(active.id)}><Trash2 size={16}/> 삭제</button><button className="primary" onClick={()=>write(active)}>저장</button></div></aside>}
  {error&&<div className="toast" onClick={()=>setError("")}>{error} <X size={14}/></div>}
 </div>;
}
export default App;
