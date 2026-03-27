# Copilot Instructions

## Language

All Norwegian text MUST use proper characters: æ, Æ, ø, Ø, å, Å — never ASCII substitutes like `a`, `o`, `aa`, `ae`, or `oe`.

## Response style

- Avoid assumptions; state facts only.
- Respond concisely and token-efficiently.
- Never include irrelevant information.

## User-facing content

- No emojis, em-dashes, `---` lines, or marketing fluff.
- Use an objective, professional but friendly tone of voice.

## Task delegation

- Use subagents for large multi-step tasks.
- When the primary task involves Java, delegate to **JavaDev** (`runSubagent` with `agentName: "JavaDev"`).
  - Exception: trivial single-line fixes and read-only searches.

## Project navigation

- Always read `AGENTS.md` or `CLAUDE.md` in a subdirectory before working in it.

## Documentation

- For up-to-date documentation lookup, use the `context7` MCP over web search.
