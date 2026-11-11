import type { Tag } from "./domain";
export function Tags({
  tags,
  onSave,
  onDelete,
  onFilter,
}: {
  tags: Tag[];
  onSave: (t: Tag) => void;
  onDelete: (id: string) => void;
  onFilter: (id: string) => void;
}) {
  function add() {
    const name = prompt("새 태그 이름");
    if (name?.trim() && !tags.some((t) => t.name === name.trim()))
      onSave({ id: crypto.randomUUID(), name: name.trim(), color: "#4776ef" });
  }
  return (
    <section className="content">
      <div className="heading">
        <div>
          <h1>태그</h1>
          <p>{tags.length}개의 태그 · 태그를 눌러 할 일을 모아보세요</p>
        </div>
        <button className="primary" onClick={add}>
          + 태그 추가
        </button>
      </div>
      {!tags.length && (
        <div className="empty">
          <h3>첫 태그를 추가해 보세요</h3>
        </div>
      )}
      {tags.map((t) => (
        <div className="tag-row" key={t.id}>
          <span className="tag-dot" style={{ background: t.color }} />
          <button className="tag-name" onClick={() => onFilter(t.id)}>
            {t.name}
          </button>
          <input
            type="color"
            aria-label={t.name + " 색상"}
            value={t.color}
            onChange={(e) => onSave({ ...t, color: e.target.value })}
          />
          <button
            onClick={() => {
              const name = prompt("태그 이름", t.name);
              if (name?.trim()) onSave({ ...t, name: name.trim() });
            }}
          >
            이름 변경
          </button>
          <button
            className="danger"
            onClick={() => {
              if (confirm("태그를 삭제할까요?")) onDelete(t.id);
            }}
          >
            삭제
          </button>
        </div>
      ))}
    </section>
  );
}
