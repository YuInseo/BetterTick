# Block editor references

Daydrift's Android editor is implemented in Kotlin/Jetpack Compose. Its slash-command and block-side-menu interaction design references [BlockNote](https://github.com/TypeCellOS/BlockNote), a React/ProseMirror editor. This is an independent native implementation; BlockNote packages and upstream source files are not bundled.

Reviewed upstream revision: `58a40d2f55553110266de6bd644026b863ae5445`.

- [Slash menu documentation](https://www.blocknotejs.org/docs/react/components/suggestion-menus): `/` opens suggestions and subsequent text filters commands.
- [Block side menu documentation](https://www.blocknotejs.org/docs/react/components/side-menu): per-block insertion and block actions.
- [Core slash-menu source](https://github.com/TypeCellOS/BlockNote/blob/58a40d2f55553110266de6bd644026b863ae5445/packages/core/src/extensions/SuggestionMenu/getDefaultSlashMenuItems.ts): empty/trigger-only blocks are converted in place; the cursor continues in an editable block after non-text content.
- [Upstream license notice](https://github.com/TypeCellOS/BlockNote/blob/58a40d2f55553110266de6bd644026b863ae5445/LICENSE.txt): core source is MPL-2.0; XL packages have separate GPL-3.0/commercial terms. Daydrift does not include XL packages.

Implemented native behaviors: filtered Korean/English slash commands, Backspace at a block's start (heading/checklist → text, empty block removed with the caret moved to the end of the block above, otherwise merged into it, a divider above removed), placeholders only in the focused empty block, text/heading/check/divider conversion, insert-after, Enter continuation, empty-checkbox exit, and preservation of per-occurrence scope. This is not a claim of BlockNote API or feature parity. Image and file blocks continue using Daydrift's existing local attachment handling.

## 키보드 블록 바와 블록 종류 (노션식)

노션 모바일의 키보드 위 도구 모음을 참고해 Daydrift 코드로 새로 만들었다. 노션 코드는 쓰지 않았다.

- 블록 종류: 텍스트, 제목1·2·3, 체크리스트, 글머리 기호·번호 목록, 토글 목록, 토글 제목1·2·3, 인용, 콜아웃, 코드, 구분선, 목차, 링크·임베드(YouTube·지도·Figma·Gist·Drive·X 등은 링크 카드로 연다), 이미지·파일.
- 들여쓰기는 블록의 `indent`(0~4)로 저장한다. 토글이 접히면 바로 아래의 더 깊은 블록이 숨는다. 들여쓰기·내어쓰기는 자식 블록도 같이 옮긴다.
- 글자 서식은 본문에 마크다운 표시로 저장하고 편집 중에는 표시를 숨긴다: `**굵게**`, `*기울임*`, `__밑줄__`, `~~취소선~~`, `` `코드` ``, `==형광펜==`, `[링크](주소)`.
- 블록 색은 `color`에 저장한다(`red`는 글자 색, `bg_red`는 배경 색).
- 마크다운 단축 입력: `- `, `* `, `1. `, `# `·`## `·`### `, `[] `, `> `(토글), `" `(인용), ` ``` `(코드), `---`(구분선).
- 실행 취소·다시 실행은 입력이 0.5초 멈출 때마다 문서 전체를 저장해 둔다(최대 100단계).
- 빈 목록 항목에서 Enter를 누르면 한 단계 내어쓰고, 맨 바깥이면 일반 텍스트로 바뀐다. 들여쓴 블록 맨 앞에서 Backspace를 누르면 먼저 내어쓴다.

## 페이지 블록 (하위 페이지)

노션의 하위 페이지처럼 블록 안에서 새 페이지를 만든다. 페이지는 그 할일의 하위 할일(`parentId`)로 저장되고, 블록은 `TaskMemo.pageId`로 그 할일을 가리킨다. 그래서 목록·달력에서도 상위 할일 아래에 보인다.

- 만들기: `/페이지`, 또는 키보드 바 `+` › 페이지. 빈 블록에서 만들면 그 자리가 페이지 블록이 된다.
- 열기: 블록을 누르면 같은 상세 화면이 그 페이지로 위에 열린다. 그 안에서 또 페이지를 만들 수 있다.
- 페이지 제목을 바꾸면 블록의 글자도 같이 바뀐다.
- 블록을 지워도 하위 할일은 지워지지 않는다.
