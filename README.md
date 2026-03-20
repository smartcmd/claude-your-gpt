# claude-your-gpt

GPT is a smart model — it can solve problems and find bugs accurately, but it has recurring code style issues. This is likely a side effect of OpenAI's reinforcement learning stage. The goal of this project is to correct GPT's code style through prompts.

## How we did that?

First, I provided several small but representative development tasks for GPT and Claude to complete independently. Then, Claude analyzed the differences in code style between the two and summarized a report.

I then provided several new feature development tasks on top of the existing codebase, also having the two models work independently and asking Claude to analyze the differences and summarize them.

Finally, the two summaries are merged and distilled into a concise, language-agnostic set of code guidelines, manually adjusted to be used as prompts for GPT.

## How to use it?

Download [AGENTS.md](AGENTS.md) and put it into your Codex home directory (defaults to `~/.codex`, unless you set `CODEX_HOME`) or project root. It also works with other AI coding tools that support custom instructions or system prompts.