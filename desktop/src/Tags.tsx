import { useEffect, useState } from "react";
import { collection, doc, onSnapshot, setDoc, deleteDoc, serverTimestamp, type Firestore } from "firebase/firestore";
type Tag={id:string;name:string;color:string};
export function Tags({db,userId}:{db:Firestore;userId:string}){
 const [tags,setTags]=useState<Tag[]>([]),[error,setError]=useState("");
 useEffect(()=>onSnapshot(collection(db,"users",userId,"tags"),s=>setTags(s.docs.map(d=>({...d.data(),id:d.id} as Tag))),e=>setError(e.message)),[db,userId]);
 const add=async()=>{const name=prompt("새 태그 이름");if(!name?.trim())return;const id=crypto.randomUUID();try{await setDoc(doc(db,"users",userId,"tags",id),{id,name:name.trim(),color:"#4776ef",createdAt:serverTimestamp()})}catch(e){setError(String(e))}};
 return <section className="content"><div className="heading"><div><h1>태그</h1><p>{tags.length}개의 태그</p></div><button className="primary" onClick={add}>+ 태그 추가</button></div>{error&&<p className="error">{error}</p>}{tags.map(t=><div className="tag-row" key={t.id}><span className="tag-dot" style={{background:t.color}}/> <strong>{t.name}</strong><input type="color" aria-label="태그 색상" value={t.color} onChange={e=>setDoc(doc(db,"users",userId,"tags",t.id),{color:e.target.value},{merge:true}).catch(err=>setError(String(err)))}/><button onClick={()=>{const name=prompt("태그 이름",t.name);if(name?.trim())setDoc(doc(db,"users",userId,"tags",t.id),{name:name.trim()},{merge:true}).catch(e=>setError(String(e)))}}>이름 변경</button><button className="danger" onClick={()=>{if(confirm("태그를 삭제할까요?"))deleteDoc(doc(db,"users",userId,"tags",t.id)).catch(e=>setError(String(e)))}}>삭제</button></div>)}</section>
}
