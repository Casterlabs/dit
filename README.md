Configure the env like so:
```
BOT_TOKEN=EXAMPLE
FORUM_PARENT_CHANNEL=1234567890
AI_KEY=EXAMPLE
AI_URL=https://example.com/api
AI_MODEL=llama3.2:latest
```

Make a folder called `docs`, in there stick files in any format (.md, .html, .txt, anything human-readable).

Make a file called prompt.txt and populate it with your prompt. `<doclist>` is a placeholder for the contents of the docs folder.


`npm run start` when you're ready.