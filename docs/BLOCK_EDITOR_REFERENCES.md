# Block editor references

BetterTick's Android editor is implemented in Kotlin/Jetpack Compose. Its slash-command and block-side-menu interaction design references [BlockNote](https://github.com/TypeCellOS/BlockNote), a React/ProseMirror editor. This is an independent native implementation; BlockNote packages and upstream source files are not bundled.

Reviewed upstream revision: `58a40d2f55553110266de6bd644026b863ae5445`.

- [Slash menu documentation](https://www.blocknotejs.org/docs/react/components/suggestion-menus): `/` opens suggestions and subsequent text filters commands.
- [Block side menu documentation](https://www.blocknotejs.org/docs/react/components/side-menu): per-block insertion and block actions.
- [Core slash-menu source](https://github.com/TypeCellOS/BlockNote/blob/58a40d2f55553110266de6bd644026b863ae5445/packages/core/src/extensions/SuggestionMenu/getDefaultSlashMenuItems.ts): empty/trigger-only blocks are converted in place; the cursor continues in an editable block after non-text content.
- [Upstream license notice](https://github.com/TypeCellOS/BlockNote/blob/58a40d2f55553110266de6bd644026b863ae5445/LICENSE.txt): core source is MPL-2.0; XL packages have separate GPL-3.0/commercial terms. BetterTick does not include XL packages.

Implemented native behaviors: filtered Korean/English slash commands, Backspace at a block's start (heading/checklist → text, empty block removed with the caret moved to the end of the block above, otherwise merged into it, a divider above removed), placeholders only in the focused empty block, text/heading/check/divider conversion, insert-after, Enter continuation, empty-checkbox exit, and preservation of per-occurrence scope. This is not a claim of BlockNote API or feature parity. Image and file blocks continue using BetterTick's existing local attachment handling.
