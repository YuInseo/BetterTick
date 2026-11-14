import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  initializeTestEnvironment, assertFails, assertSucceeds,
} from '@firebase/rules-unit-testing';
import {
  doc, collection, setDoc, getDoc, getDocs, updateDoc, deleteDoc,
  Timestamp, arrayUnion, arrayRemove, query, where,
} from 'firebase/firestore';

let env;
const task = (extra = {}) => ({ title: '야간 근무', priority: 0, ...extra });
const db = (uid) => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();
const fixtures = {
  tasks: task(),
  lists: { name: '기본함', id: '', default: true }, // DataSeeder stores blank IDs.
  tags: { name: '중요', parentTagId: null },
  habits: { name: '운동', archived: false, targetDays: [1, 2, 3] },
  habitLogs: { habitId: 'exercise', date: '2026-11-14', completed: true, note: '성공' },
  focusCategories: { name: '공부', id: '' },
  focusSessions: { activityName: '공부', durationSeconds: 1500, completed: true },
  diary: { dateStr: '2026-11-14', content: '오늘의 일기', mood: 2 },
  'settings/tabbar': { enabledIds: ['tasks', 'calendar', 'more'], maxTabs: 5 },
  'settings/matrix': { quadrants: [{ id: 'I' }, { id: 'II' }, { id: 'III' }, { id: 'IV' }] },
};
const fixturePath = (key) => key.startsWith('settings/') ? `users/alice/${key}` : `users/alice/${key}/entry`;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-bettertick-security',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});
beforeEach(async () => env.clearFirestore());
after(async () => { if (env) await env.cleanup(); });

for (const [key, data] of Object.entries(fixtures)) {
  test(`${key}: owner CRUD allowed; anonymous and another user denied`, async () => {
    const path = fixturePath(key);
    const owner = doc(db('alice'), path);
    await assertSucceeds(setDoc(owner, data));
    await assertSucceeds(getDoc(owner));
    await assertSucceeds(setDoc(owner, data, { merge: true }));
    for (const uid of [undefined, 'bob']) {
      const stranger = doc(db(uid), path);
      await assertFails(getDoc(stranger));
      await assertFails(setDoc(stranger, data));
      await assertFails(deleteDoc(stranger));
    }
    await assertSucceeds(deleteDoc(owner));
    for (const uid of [undefined, 'bob']) {
      await assertFails(setDoc(doc(db(uid), path), data));
    }
  });
}

test('profile ownership, queries and privilege fields', async () => {
  const profile = doc(db('alice'), 'users/alice');
  await assertSucceeds(setDoc(profile, { id: 'alice', email: 'alice@example.com', displayName: 'Alice' }));
  await assertSucceeds(updateDoc(profile, { displayName: '주인님', settings: { theme: 'dark' } }));
  await assertFails(updateDoc(profile, { admin: true }));
  await assertFails(updateDoc(profile, { role: 'admin' }));
  await assertFails(updateDoc(profile, { id: 'bob' }));
  for (const uid of [undefined, 'bob']) {
    const ref = doc(db(uid), 'users/alice');
    await assertFails(getDoc(ref));
    await assertFails(setDoc(ref, { id: 'alice', email: '', displayName: 'x' }));
    await assertFails(deleteDoc(ref));
  }
  await assertFails(getDocs(collection(db('alice'), 'users')));
  await assertSucceeds(deleteDoc(profile));
});

test('task collections allow current sync queries only inside the owner namespace', async () => {
  await setDoc(doc(db('alice'), 'users/alice/tasks/t'), task({ updatedAt: Timestamp.now() }));
  await assertSucceeds(getDocs(collection(db('alice'), 'users/alice/tasks')));
  await assertSucceeds(getDocs(query(collection(db('alice'), 'users/alice/tasks'), where('updatedAt', '>', Timestamp.fromMillis(0)))));
  await assertFails(getDocs(collection(db('bob'), 'users/alice/tasks')));
  await assertFails(getDocs(collection(db(), 'users/alice/tasks')));
});

test('existing Android recurrence, memo, leave and boolean-alias updates remain usable', async () => {
  const ref = doc(db('alice'), 'users/alice/tasks/shift');
  await assertSucceeds(setDoc(ref, task({
    id: 'shift', dueDate: null, repeatRule: 'FREQ=DAILY', repeatEnd: null,
    isCompleted: false, completed: false, isPinned: true, pinned: true,
    isAbandoned: false, abandoned: false,
    completedAt: null, abandonedAt: null, createdAt: Timestamp.now(), updatedAt: Timestamp.now(),
    parentId: '', durationMinutes: 480, listId: '', tagIds: [], exceptions: [],
    notes: '', attachments: [], reminders: [10], sortOrder: 0,
    notionUrl: '', kanbanColumn: '', completedDates: [], checkWholeSeries: false,
    leaves: { '2026-11-14': '병가' }, dayShifts: { '2026-11-15': '일근' },
    dayNotes: {}, memos: [{ id: 'm1', text: '메모', day: '' }],
  })));
  await assertSucceeds(updateDoc(ref, {
    completedDates: arrayUnion('2026-11-14'), updatedAt: Timestamp.now(),
  }));
  await assertSucceeds(updateDoc(ref, { completedDates: arrayRemove('2026-11-14') }));
  await assertSucceeds(setDoc(ref, { isCompleted: true, completed: true, completedAt: Timestamp.now() }, { merge: true }));
  await assertFails(updateDoc(ref, { id: 'another-task' }));
  await assertFails(updateDoc(ref, { title: 123 }));
  await assertFails(updateDoc(ref, { priority: 4 }));
  await assertFails(updateDoc(ref, { completed: 'yes' }));
  await assertFails(updateDoc(ref, { createdAt: 'yesterday' }));
  await assertFails(updateDoc(ref, { admin: true }));
});

test('malformed and oversized payloads are denied', async () => {
  const store = db('alice');
  await assertFails(setDoc(doc(store, 'users/alice/tasks/t'), task({ notes: 'x'.repeat(200001) })));
  await assertFails(setDoc(doc(store, 'users/alice/tasks/t'), task({ tagIds: 'not-a-list' })));
  await assertFails(setDoc(doc(store, 'users/alice/tasks/t'), task({ reminders: Array(101).fill(0) })));
  await assertFails(setDoc(doc(store, 'users/alice/tasks/t'), { completed: true }));
  await assertFails(setDoc(doc(store, 'users/alice/diary/d'), { dateStr: '', content: 1 }));
  await assertFails(setDoc(doc(store, 'users/alice/focusSessions/f'), { activityName: '', durationSeconds: -1 }));
  await assertFails(setDoc(doc(store, 'users/alice/locationRecords/l'), { dateStr: '', latitude: 91, longitude: 127 }));
  await assertFails(setDoc(doc(store, 'users/alice/favoritePlaces/l'), { name: '', latitude: 37, longitude: -181 }));
  await assertFails(setDoc(doc(store, 'users/alice/settings/tabbar'), { enabledIds: [], maxTabs: 'five' }));
  await assertFails(setDoc(doc(store, 'users/alice/settings/matrix'), { quadrants: [] }));
});

test('unlisted collections, nested documents and public roots default to deny', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice/tasks/t/private/nested'), { secret: true });
  });
  for (const path of ['admin/settings', 'public/anything', 'users/alice/unknown/doc', 'users/alice/settings/admin', 'users/alice/tasks/t/private/nested']) {
    await assertFails(getDoc(doc(db('alice'), path)));
    await assertFails(setDoc(doc(db('alice'), path), { name: 'x' }));
  }
});

for (const kind of ['locationRecords', 'favoritePlaces']) {
  test(`${kind}: retired feature rejects old-client writes and preserves historical data access`, async () => {
    const path = `users/alice/${kind}/old-record`;
    const old = { id: 'old-record', name: '기존 기록', latitude: 37.5, longitude: 127 };
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), path), old);
    });
    const ref = doc(db('alice'), path);
    await assertSucceeds(getDoc(ref));
    await assertFails(updateDoc(ref, { latitude: 38 }));
    await assertFails(setDoc(doc(db('alice'), `users/alice/${kind}/new-record`), old));
    for (const uid of [undefined, 'bob']) {
      await assertFails(getDoc(doc(db(uid), path)));
      await assertFails(deleteDoc(doc(db(uid), path)));
    }
    await assertSucceeds(deleteDoc(ref));
  });
}
